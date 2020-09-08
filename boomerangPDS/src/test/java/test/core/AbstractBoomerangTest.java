package test.core;

import boomerang.BackwardQuery;
import boomerang.Boomerang;
import boomerang.BoomerangOptions;
import boomerang.DefaultBoomerangOptions;
import boomerang.ForwardQuery;
import boomerang.Query;
import boomerang.WeightedBoomerang;
import boomerang.WholeProgramBoomerang;
import boomerang.results.BackwardBoomerangResults;
import boomerang.scene.AllocVal;
import boomerang.scene.CallGraph;
import boomerang.scene.ControlFlowGraph.Edge;
import boomerang.scene.DataFlowScope;
import boomerang.scene.Field;
import boomerang.scene.SootDataFlowScope;
import boomerang.scene.Val;
import boomerang.scene.jimple.BoomerangPretransformer;
import boomerang.scene.jimple.IntAndStringBoomerangOptions;
import boomerang.scene.jimple.SootCallGraph;
import boomerang.solver.ForwardBoomerangSolver;
import boomerang.util.AccessPath;
import boomerang.util.DefaultValueMap;
import com.google.common.base.Joiner;
import com.google.common.base.Stopwatch;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import java.math.BigInteger;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.Before;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import soot.Scene;
import soot.SceneTransformer;
import sync.pds.solver.OneWeightFunctions;
import sync.pds.solver.WeightFunctions;
import sync.pds.solver.nodes.INode;
import sync.pds.solver.nodes.Node;
import sync.pds.solver.nodes.SingleNode;
import test.core.selfrunning.AbstractTestingFramework;
import wpds.impl.Transition;
import wpds.impl.Weight.NoWeight;
import wpds.impl.WeightedPAutomaton;
import wpds.interfaces.WPAStateListener;

public class AbstractBoomerangTest extends AbstractTestingFramework {

  /**
   * Fails the test cases, when any instance of the interface {@link
   * test.core.selfrunning.NoAllocatedObject} is detected.
   */
  private static final boolean FAIL_ON_IMPRECISE = true;

  /**
   * Fails the test cases, when Boomerang's result set contains any object that does not inherit
   * from {@link test.core.selfrunning.AllocatedObject}.
   */
  private static final boolean TRACK_IMPLICIT_IMPRECISE = false;

  private static final Logger LOGGER = LoggerFactory.getLogger(AbstractBoomerangTest.class);

  private QueryForCallSiteDetector queryDetector;
  private Collection<? extends Query> expectedAllocationSites;
  private Collection<? extends Node<Edge, Val>> explicitlyUnexpectedAllocationSites;
  protected Collection<? extends Query> queryForCallSites;
  protected Collection<Error> unsoundErrors = Sets.newHashSet();
  protected Collection<Error> imprecisionErrors = Sets.newHashSet();
  private static Duration globalQueryTime = Duration.ofMillis(0);

  protected int analysisTimeout = 3000 * 1000;
  private CallGraph callGraph;
  private DataFlowScope dataFlowScope;

  public enum AnalysisMode {
    WholeProgram,
    DemandDrivenBackward;
  }

  protected AnalysisMode[] getAnalyses() {
    return new AnalysisMode[] {
      // AnalysisMode.WholeProgram,
      AnalysisMode.DemandDrivenBackward
    };
  }

  public int getIterations() {
    return 1;
  }

  @Before
  public void beforeTestCaseExecution() {
    super.beforeTestCaseExecution();
  }

  protected SceneTransformer createAnalysisTransformer() {
    return new SceneTransformer() {

      protected void internalTransform(
          String phaseName, @SuppressWarnings("rawtypes") Map options) {
        BoomerangPretransformer.v().reset();
        BoomerangPretransformer.v().apply();
        callGraph = new SootCallGraph();
        dataFlowScope = getDataFlowScope();
        analyzeWithCallGraph();
      }
    };
  }

  private void analyzeWithCallGraph() {
    queryDetector = new QueryForCallSiteDetector(callGraph);
    queryForCallSites = queryDetector.computeSeeds();

    if (queryDetector.integerQueries) {
      Preanalysis an = new Preanalysis(callGraph, new IntegerAllocationSiteOf());
      expectedAllocationSites = an.computeSeeds();
    } else {
      Preanalysis an =
          new Preanalysis(callGraph, new AllocationSiteOf("test.core.selfrunning.AllocatedObject"));
      expectedAllocationSites = an.computeSeeds();
      an =
          new Preanalysis(
              callGraph, new AllocationSiteOf("test.core.selfrunning.NoAllocatedObject"));
      explicitlyUnexpectedAllocationSites =
          an.computeSeeds().stream().map(x -> x.asNode()).collect(Collectors.toList());
    }
    for (int i = 0; i < getIterations(); i++) {
      for (AnalysisMode analysis : getAnalyses()) {
        switch (analysis) {
          case WholeProgram:
            if (!queryDetector.integerQueries) runWholeProgram();
            break;
          case DemandDrivenBackward:
            runDemandDrivenBackward();
            break;
        }
      }
      if (queryDetector.resultsMustNotBeEmpty) return;
      if (!unsoundErrors.isEmpty()) {
        throw new RuntimeException(Joiner.on("\n").join(unsoundErrors));
      }
      if (!imprecisionErrors.isEmpty() && FAIL_ON_IMPRECISE) {
        throw new AssertionError(Joiner.on("\n").join(imprecisionErrors));
      }
    }
  }

  private void runWholeProgram() {
    final Set<Node<Edge, Val>> results = Sets.newHashSet();
    WholeProgramBoomerang<NoWeight> solver =
        new WholeProgramBoomerang<NoWeight>(
            callGraph,
            dataFlowScope,
            new DefaultBoomerangOptions() {
              @Override
              public int analysisTimeoutMS() {
                return analysisTimeout;
              }

              @Override
              public boolean onTheFlyCallGraph() {
                return false;
              }
            }) {

          @Override
          protected WeightFunctions<Edge, Val, Field, NoWeight> getForwardFieldWeights() {
            return new OneWeightFunctions<>(NoWeight.NO_WEIGHT_ONE);
          }

          @Override
          protected WeightFunctions<Edge, Val, Field, NoWeight> getBackwardFieldWeights() {
            return new OneWeightFunctions<>(NoWeight.NO_WEIGHT_ONE);
          }

          @Override
          protected WeightFunctions<Edge, Val, Edge, NoWeight> getBackwardCallWeights() {
            return new OneWeightFunctions<>(NoWeight.NO_WEIGHT_ONE);
          }

          @Override
          protected WeightFunctions<Edge, Val, Edge, NoWeight> getForwardCallWeights(
              ForwardQuery sourceQuery) {
            return new OneWeightFunctions<>(NoWeight.NO_WEIGHT_ONE);
          }
        };
    solver.wholeProgramAnalysis();
    DefaultValueMap<ForwardQuery, ForwardBoomerangSolver<NoWeight>> solvers = solver.getSolvers();
    for (final Query q : solvers.keySet()) {
      for (final Query queryForCallSite : queryForCallSites) {
        solvers
            .get(q)
            .getFieldAutomaton()
            .registerListener(
                new WPAStateListener<Field, INode<Node<Edge, Val>>, NoWeight>(
                    new SingleNode<>(queryForCallSite.asNode())) {

                  @Override
                  public void onOutTransitionAdded(
                      Transition<Field, INode<Node<Edge, Val>>> t,
                      NoWeight w,
                      WeightedPAutomaton<Field, INode<Node<Edge, Val>>, NoWeight>
                          weightedPAutomaton) {
                    if (t.getLabel().equals(Field.empty())
                        && t.getTarget().fact().equals(q.asNode())) {
                      results.add(q.asNode());
                    }
                  }

                  @Override
                  public void onInTransitionAdded(
                      Transition<Field, INode<Node<Edge, Val>>> t,
                      NoWeight w,
                      WeightedPAutomaton<Field, INode<Node<Edge, Val>>, NoWeight>
                          weightedPAutomaton) {}
                });
      }
      for (Node<Edge, Val> s : solvers.get(q).getReachedStates()) {
        if (s.stmt().getMethod().toString().contains("unreachable")
            && !q.toString().contains("dummyClass.main")) {
          throw new RuntimeException("Propagation within unreachable method found: " + q);
        }
      }
    }

    compareQuery(expectedAllocationSites, results, AnalysisMode.WholeProgram);
    System.out.println();
  }

  private void runDemandDrivenBackward() {
    // Run backward analysis
    Set<Node<Edge, Val>> backwardResults = runQuery(queryForCallSites);
    if (queryDetector.integerQueries) {
      compareIntegerResults(backwardResults, AnalysisMode.DemandDrivenBackward);
    } else {
      compareQuery(expectedAllocationSites, backwardResults, AnalysisMode.DemandDrivenBackward);
    }
  }

  private void compareIntegerResults(Set<Node<Edge, Val>> backwardResults, AnalysisMode analysis) {
    if (queryForCallSites.size() > 1) throw new RuntimeException("Not implemented");
    for (Query q : queryForCallSites) {
      Edge stmt = q.cfgEdge();
      boomerang.scene.InvokeExpr ie = stmt.getStart().getInvokeExpr();
      Val arg = ie.getArg(1);
      Collection<String> expectedResults = parse(arg);
      LOGGER.info("Expected results: {}", expectedResults);
      boolean imprecise = false;
      for (Node<Edge, Val> v : backwardResults) {
        if (v.fact() instanceof AllocVal) {
          AllocVal allocVal = (AllocVal) v.fact();
          boolean remove = expectedResults.remove(allocVal.toString());
          if (!remove) imprecise = true;
        } else {
          imprecise = true;
        }
      }
      if (!expectedResults.isEmpty()) {
        unsoundErrors.add(new Error(analysis + " Unsound results!"));
      }
      if (imprecise) imprecisionErrors.add(new Error(analysis + " Imprecise results!"));
    }
  }

  private ArrayList<String> parse(Val arg) {
    String[] split = arg.getStringValue().split(",");
    return Lists.newArrayList(split);
  }

  private Set<Node<Edge, Val>> runQuery(Collection<? extends Query> queries) {
    final Set<Node<Edge, Val>> results = Sets.newHashSet();

    for (final Query query : queries) {
      BoomerangOptions options = createBoomerangOptions();
      Boomerang solver = new Boomerang(callGraph, getDataFlowScope(), options) {};

      if (query instanceof BackwardQuery) {
        Stopwatch watch = Stopwatch.createStarted();
        BackwardBoomerangResults<NoWeight> res = solver.solve((BackwardQuery) query);
        globalQueryTime = globalQueryTime.plus(watch.elapsed());

        LOGGER.info("Solving query took: {}", watch);
        LOGGER.info("Expected results: {}", globalQueryTime);
        for (ForwardQuery q : res.getAllocationSites().keySet()) {
          results.add(q.asNode());

          for (Node<Edge, Val> s : solver.getSolvers().get(q).getReachedStates()) {
            if (s.stmt().getMethod().toString().contains("unreachable")) {
              throw new RuntimeException("Propagation within unreachable method found.");
            }
          }
        }
        if (queryDetector.accessPathQuery) {
          checkContainsAllExpectedAccessPath(res.getAllAliases());
        }
      }
    }
    return results;
  }

  protected DataFlowScope getDataFlowScope() {
    return SootDataFlowScope.make(Scene.v());
  }

  protected BoomerangOptions createBoomerangOptions() {
    return (queryDetector.integerQueries
        ? new IntAndStringBoomerangOptions()
        : new DefaultBoomerangOptions() {
          @Override
          public int analysisTimeoutMS() {
            return analysisTimeout;
          }
        });
  }

  private void compareQuery(
      Collection<? extends Query> expectedResults,
      Collection<? extends Node<Edge, Val>> results,
      AnalysisMode analysis) {
    LOGGER.info("Boomerang Results: {}", results);
    LOGGER.info("Expected Results: {}", expectedResults);
    Collection<Node<Edge, Val>> falseNegativeAllocationSites = new HashSet<>();
    for (Query res : expectedResults) {
      if (!results.contains(res.asNode())) falseNegativeAllocationSites.add(res.asNode());
    }
    Collection<? extends Node<Edge, Val>> falsePositiveAllocationSites = new HashSet<>(results);
    for (Query res : expectedResults) {
      falsePositiveAllocationSites.remove(res.asNode());
    }

    String answer =
        (falseNegativeAllocationSites.isEmpty() ? "" : "\nFN:" + falseNegativeAllocationSites)
            + (falsePositiveAllocationSites.isEmpty()
                ? ""
                : "\nFP:" + falsePositiveAllocationSites + "\n");
    if (!falseNegativeAllocationSites.isEmpty()) {
      unsoundErrors.add(new Error(analysis + " Unsound results for:" + answer));
    }
    if (TRACK_IMPLICIT_IMPRECISE && !falsePositiveAllocationSites.isEmpty())
      imprecisionErrors.add(new Error(analysis + " Imprecise results for:" + answer));

    if (queryDetector.resultsMustNotBeEmpty && results.isEmpty()) {
      throw new RuntimeException(
          "Expected some results, but Boomerang returned no allocation sites.");
    }

    for (Node<Edge, Val> r : results) {
      if (explicitlyUnexpectedAllocationSites.contains(r)) {
        imprecisionErrors.add(new Error(analysis + " Imprecise results for:" + answer));
      }
    }
  }

  private void checkContainsAllExpectedAccessPath(Set<AccessPath> allAliases) {
    HashSet<AccessPath> expected = Sets.newHashSet(queryDetector.expectedAccessPaths);
    expected.removeAll(allAliases);
    if (!expected.isEmpty()) {
      throw new RuntimeException("Did not find all access path! " + expected);
    }
  }

  protected Collection<String> errorOnVisitMethod() {
    return Lists.newLinkedList();
  }

  protected boolean includeJDK() {
    return true;
  }

  /**
   * The methods parameter describes the variable that a query is issued for. Note: We misuse
   * the @Deprecated annotation to highlight the method in the Code.
   */
  public static void queryFor(Object variable) {}

  public static void accessPathQueryFor(Object variable, String aliases) {}

  protected void queryForAndNotEmpty(Object variable) {}

  public static void intQueryFor(int variable, String value) {}

  public static void intQueryFor(BigInteger variable, String value) {}

  /**
   * A call to this method flags the object as at the call statement as not reachable by the
   * analysis.
   *
   * @param variable
   */
  protected void unreachable(Object variable) {}

  /**
   * This method can be used in test cases to create branching. It is not optimized away.
   *
   * @return
   */
  protected boolean staticallyUnknown() {
    return true;
  }

  protected void setupSolver(WeightedBoomerang<NoWeight> solver) {}
}
