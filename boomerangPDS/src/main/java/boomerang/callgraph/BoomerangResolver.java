package boomerang.callgraph;

import boomerang.BackwardQuery;
import boomerang.Boomerang;
import boomerang.ForwardQuery;
import boomerang.Query;
import boomerang.SolverCreationListener;
import boomerang.WeightedBoomerang;
import boomerang.results.ExtractAllocationSiteStateListener;
import boomerang.scene.CallGraph;
import boomerang.scene.ControlFlowGraph.Edge;
import boomerang.scene.DataFlowScope;
import boomerang.scene.DeclaredMethod;
import boomerang.scene.InvokeExpr;
import boomerang.scene.Method;
import boomerang.scene.Statement;
import boomerang.scene.Type;
import boomerang.scene.Val;
import boomerang.scene.WrappedClass;
import boomerang.solver.AbstractBoomerangSolver;
import boomerang.solver.ForwardBoomerangSolver;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sync.pds.solver.nodes.INode;
import sync.pds.solver.nodes.Node;
import wpds.impl.Weight;

public class BoomerangResolver implements ICallerCalleeResolutionStrategy {
  public static final Factory FACTORY = (solver, cg) -> new BoomerangResolver(solver, cg);

  private static final Logger logger = LoggerFactory.getLogger(BoomerangResolver.class);

  public enum NoCalleeFoundFallbackOptions {
    PRECOMPUTED,
    BYPASS
  };

  private static final String THREAD_CLASS = "java.lang.Thread";
  private static final String THREAD_START_SIGNATURE = "<java.lang.Thread: void start()>";
  private static final String THREAD_RUN_SUB_SIGNATURE = "void run()";

  private static NoCalleeFoundFallbackOptions FALLBACK_OPTION = NoCalleeFoundFallbackOptions.BYPASS;
  private static Multimap<DeclaredMethod, WrappedClass> didNotFindMethodLog = HashMultimap.create();

  private CallGraph precomputedCallGraph;
  private WeightedBoomerang<? extends Weight> solver;
  private Set<Statement> queriedInvokeExprAndAllocationSitesFound = Sets.newHashSet();
  private Set<Statement> queriedInvokeExpr = Sets.newHashSet();;

  public BoomerangResolver(CallGraph cg, DataFlowScope scope) {
    this.solver = new Boomerang(cg, scope);
    this.precomputedCallGraph = cg;
  }

  public BoomerangResolver(WeightedBoomerang<? extends Weight> solver, CallGraph initialCallGraph) {
    this(solver, true, initialCallGraph);
  }

  public BoomerangResolver(
      WeightedBoomerang<? extends Weight> solver,
      boolean enableExceptions,
      CallGraph initialCallGraph) {
    this.solver = solver;
    this.precomputedCallGraph = initialCallGraph;
  }

  @Override
  public void computeFallback(ObservableDynamicICFG observableDynamicICFG) {
    int refined = 0;
    int precomputed = 0;
    for (Statement s : Lists.newArrayList(queriedInvokeExpr)) {
      if (!queriedInvokeExprAndAllocationSitesFound.contains(s)) {
        logger.debug("Call graph ends at {}", s);
        precomputed++;
        if (FALLBACK_OPTION == NoCalleeFoundFallbackOptions.PRECOMPUTED) {
          for (CallGraph.Edge e : precomputedCallGraph.edgesOutOf(s)) {
            // TODO Refactor. Should not be required, if the backward analysis is sound (data-flow
            // of static fields)
            observableDynamicICFG.addCallIfNotInGraph(e.src(), e.tgt());
          }
        }
        if (FALLBACK_OPTION == NoCalleeFoundFallbackOptions.BYPASS) {
          observableDynamicICFG.notifyNoCalleeFound(s);
        }
      } else {
        refined++;
      }
    }
    logger.debug("Refined edges {}, fallback to precomputed {}", refined, precomputed);
  }

  @Override
  public Method resolveSpecialInvoke(InvokeExpr ie) {
    Collection<Method> methodFromClassOrFromSuperclass =
        getMethodFromClassOrFromSuperclass(ie.getMethod(), ie.getMethod().getDeclaringClass());
    if (methodFromClassOrFromSuperclass.size() > 1) {
      throw new RuntimeException(
          "Illegal state, a special call should exactly resolve to one target");
    }
    return Iterables.getFirst(methodFromClassOrFromSuperclass, null);
  }

  @Override
  public Method resolveStaticInvoke(InvokeExpr ie) {
    Collection<Method> methodFromClassOrFromSuperclass =
        getMethodFromClassOrFromSuperclass(ie.getMethod(), ie.getMethod().getDeclaringClass());
    if (methodFromClassOrFromSuperclass.size() > 1) {
      throw new RuntimeException(
          "Illegal state, a static call should exactly resolve to one target");
    }
    return Iterables.getFirst(methodFromClassOrFromSuperclass, null);
  }

  @Override
  public Collection<Method> resolveInstanceInvoke(Statement stmt) {
    return queryForCallees(stmt);
  }

  private Collection<Method> queryForCallees(Statement resolvingStmt) {
    logger.debug("Queried for callees of '{}'.", resolvingStmt);
    // Construct BackwardQuery, so we know which types the object might have
    InvokeExpr invokeExpr = resolvingStmt.getInvokeExpr();
    queriedInvokeExpr.add(resolvingStmt);
    Val value = invokeExpr.getBase();

    Collection<Method> res = new ArrayList<>();

    // Not using cfg here because we are iterating backward
    for (Statement pred :
        resolvingStmt.getMethod().getControlFlowGraph().getPredsOf(resolvingStmt)) {
      BackwardQuery query = BackwardQuery.make(new Edge(pred, resolvingStmt), value);
      solver.solve(query, false);
      res.addAll(forAnyAllocationSiteOfQuery(query, resolvingStmt, pred));
    }

    return res;
  }

  @SuppressWarnings("rawtypes")
  private Collection<Method> forAnyAllocationSiteOfQuery(
      BackwardQuery query, Statement resolvingStmt, Statement callSite) {
    IterateSolvers callback = new IterateSolvers(query, callSite, resolvingStmt);
    solver.registerSolverCreationListener(callback);
    return callback.results;
  }

  private Collection<Method> getMethodFromClassOrFromSuperclass(
      DeclaredMethod method, WrappedClass sootClass) {
    Set<Method> res = Sets.newHashSet();
    WrappedClass originalClass = sootClass;
    while (sootClass != null) {
      for (Method candidate : sootClass.getMethods()) {
        if (candidate.getSubSignature().equals(method.getSubSignature())) {
          res.add(candidate);
        }
      }
      handlingForThreading(method, sootClass, res);
      if (!res.isEmpty()) return res;
      if (sootClass.hasSuperclass()) {
        sootClass = sootClass.getSuperclass();
      } else {
        logDidNotFindMethod(method, originalClass);
        return res;
      }
    }
    logDidNotFindMethod(method, originalClass);
    return res;
  }

  private void logDidNotFindMethod(DeclaredMethod method, WrappedClass originalClass) {
    if (didNotFindMethodLog.put(method, originalClass)) {
      logger.debug("Did not find method {} for class {}", method, originalClass);
    }
  }

  private void handlingForThreading(
      DeclaredMethod method, WrappedClass sootClass, Set<Method> res) {
    // throw new RuntimeException("Threading not implemented");
    // if (Scene.v().getFastHierarchy().isSubclass(sootClass,
    // Scene.v().getSootClass(THREAD_CLASS)))
    // {
    // if (method.getSignature().equals(THREAD_START_SIGNATURE)) {
    // for (SootMethod candidate : sootClass.getMethods()) {
    // if (candidate.getSubSignature().equals(THREAD_RUN_SUB_SIGNATURE)) {
    // res.add(candidate);
    // }
    // }
    // }
    // }
  }

  private final class IterateSolvers<W extends Weight> implements SolverCreationListener<W> {
    private final BackwardQuery query;
    private final Statement invokeExpr;
    private final Collection<Method> results = new ArrayList<>();

    private IterateSolvers(BackwardQuery query, Statement unit, Statement invokeExpr) {
      this.query = query;
      this.invokeExpr = invokeExpr;
    }

    @Override
    public void onCreatedSolver(Query q, AbstractBoomerangSolver<W> solver) {
      if (solver instanceof ForwardBoomerangSolver) {
        ForwardQuery forwardQuery = (ForwardQuery) q;
        ForwardBoomerangSolver<W> forwardBoomerangSolver = (ForwardBoomerangSolver<W>) solver;
        for (INode<Node<Edge, Val>> initialState :
            forwardBoomerangSolver.getFieldAutomaton().getInitialStates()) {
          forwardBoomerangSolver
              .getFieldAutomaton()
              .registerListener(
                  new ExtractAllocationSiteStateListener<W>(initialState, query, (ForwardQuery) q) {

                    @Override
                    protected void allocationSiteFound(
                        ForwardQuery allocationSite, BackwardQuery query) {
                      logger.debug("Found AllocationSite '{}'.", forwardQuery);
                      queriedInvokeExprAndAllocationSitesFound.add(invokeExpr);
                      Type type = forwardQuery.getType();
                      if (type.isRefType()) {
                        for (Method calleeMethod :
                            getMethodFromClassOrFromSuperclass(
                                invokeExpr.getInvokeExpr().getMethod(), type.getWrappedClass())) {
                          results.add(calleeMethod);
                        }
                      } else if (type.isArrayType()) {
                        Type base = type.getArrayBaseType();
                        if (base.isRefType()) {
                          for (Method calleeMethod :
                              getMethodFromClassOrFromSuperclass(
                                  invokeExpr.getInvokeExpr().getMethod(), base.getWrappedClass())) {
                            results.add(calleeMethod);
                          }
                        }
                      }
                    };
                  });
        }
      }
    }

    @Override
    public int hashCode() {
      final int prime = 31;
      int result = 1;
      result = prime * result + getOuterType().hashCode();
      result = prime * result + ((query == null) ? 0 : query.hashCode());
      result = prime * result + ((invokeExpr == null) ? 0 : invokeExpr.hashCode());
      return result;
    }

    @Override
    public boolean equals(Object obj) {
      if (this == obj) return true;
      if (obj == null) return false;
      if (getClass() != obj.getClass()) return false;
      IterateSolvers other = (IterateSolvers) obj;
      if (!getOuterType().equals(other.getOuterType())) return false;
      if (query == null) {
        if (other.query != null) return false;
      } else if (!query.equals(other.query)) return false;
      if (invokeExpr == null) {
        if (other.invokeExpr != null) return false;
      } else if (!invokeExpr.equals(other.invokeExpr)) return false;
      return true;
    }

    private BoomerangResolver getOuterType() {
      // TODO why is this type of importance?
      return BoomerangResolver.this;
    }
  }
}
