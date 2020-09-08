/**
 * ***************************************************************************** Copyright (c) 2018
 * Fraunhofer IEM, Paderborn, Germany. This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * <p>SPDX-License-Identifier: EPL-2.0
 *
 * <p>Contributors: Johannes Spaeth - initial API and implementation
 * *****************************************************************************
 */
package test.core;

import boomerang.BackwardQuery;
import boomerang.Boomerang;
import boomerang.DefaultBoomerangOptions;
import boomerang.Query;
import boomerang.WeightedBoomerang;
import boomerang.results.BackwardBoomerangResults;
import boomerang.scene.AnalysisScope;
import boomerang.scene.CallGraph;
import boomerang.scene.DataFlowScope;
import boomerang.scene.SootDataFlowScope;
import boomerang.scene.Val;
import boomerang.scene.jimple.BoomerangPretransformer;
import boomerang.scene.jimple.SootCallGraph;
import com.google.common.base.Joiner;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.TimeUnit;
import org.junit.Rule;
import org.junit.rules.Timeout;
import soot.RefType;
import soot.Scene;
import soot.SceneTransformer;
import soot.SootClass;
import soot.jimple.NewExpr;
import test.core.selfrunning.AbstractTestingFramework;
import wpds.impl.Weight;

public class MultiQueryBoomerangTest extends AbstractTestingFramework {

  private static final boolean FAIL_ON_IMPRECISE = false;

  @Rule public Timeout timeout = new Timeout(10000000, TimeUnit.MILLISECONDS);
  private Collection<? extends Query> allocationSites;
  protected Collection<? extends Query> queryForCallSites;
  protected Multimap<Query, Query> expectedAllocsForQuery = HashMultimap.create();
  protected Collection<Error> unsoundErrors = Sets.newHashSet();
  protected Collection<Error> imprecisionErrors = Sets.newHashSet();
  private CallGraph callGraph;
  private DataFlowScope dataFlowScope;

  protected int analysisTimeout = 300 * 1000;

  private WeightedBoomerang<Weight.NoWeight> solver;

  protected SceneTransformer createAnalysisTransformer() {
    return new SceneTransformer() {

      protected void internalTransform(
          String phaseName, @SuppressWarnings("rawtypes") Map options) {
        BoomerangPretransformer.v().reset();
        BoomerangPretransformer.v().apply();
        callGraph = new SootCallGraph();
        dataFlowScope = SootDataFlowScope.make(Scene.v());
        AnalysisScope analysisScope = new Preanalysis(callGraph, new FirstArgumentOf("queryFor.*"));

        queryForCallSites = analysisScope.computeSeeds();

        for (Query q : queryForCallSites) {
          Val arg2 = q.cfgEdge().getStart().getInvokeExpr().getArg(1);
          if (arg2.isClassConstant()) {
            Preanalysis analysis =
                new Preanalysis(
                    callGraph, new AllocationSiteOf(arg2.getClassConstantType().toString()));
            expectedAllocsForQuery.putAll(q, analysis.computeSeeds());
          }
        }
        runDemandDrivenBackward();
        if (!unsoundErrors.isEmpty()) {
          throw new RuntimeException(Joiner.on("\n").join(unsoundErrors));
        }
        if (!imprecisionErrors.isEmpty() && FAIL_ON_IMPRECISE) {
          throw new AssertionError(Joiner.on("\n").join(imprecisionErrors));
        }
      }
    };
  }

  private void compareQuery(Query query, Collection<? extends Query> results) {
    Collection<Query> expectedResults = expectedAllocsForQuery.get(query);
    Collection<Query> falseNegativeAllocationSites = new HashSet<>();
    for (Query res : expectedResults) {
      if (!results.contains(res)) falseNegativeAllocationSites.add(res);
    }
    Collection<Query> falsePositiveAllocationSites = new HashSet<>(results);
    for (Query res : expectedResults) {
      falsePositiveAllocationSites.remove(res);
    }

    String answer =
        (falseNegativeAllocationSites.isEmpty() ? "" : "\nFN:" + falseNegativeAllocationSites)
            + (falsePositiveAllocationSites.isEmpty()
                ? ""
                : "\nFP:" + falsePositiveAllocationSites + "\n");
    if (!falseNegativeAllocationSites.isEmpty()) {
      unsoundErrors.add(new Error(" Unsound results for:" + answer));
    }
    if (!falsePositiveAllocationSites.isEmpty())
      imprecisionErrors.add(new Error(" Imprecise results for:" + answer));
    for (Entry<Query, Query> e : expectedAllocsForQuery.entries()) {
      if (!e.getKey().equals(query)) {
        if (results.contains(e.getValue())) {
          throw new RuntimeException(
              "A query contains the result of a different query.\n"
                  + query
                  + " \n contains \n"
                  + e.getValue());
        }
      }
    }
  }

  private void runDemandDrivenBackward() {
    DefaultBoomerangOptions options =
        new DefaultBoomerangOptions() {
          @Override
          public int analysisTimeoutMS() {
            return analysisTimeout;
          }

          @Override
          public boolean onTheFlyCallGraph() {
            return false;
          }

          @Override
          public boolean allowMultipleQueries() {
            return true;
          }
        };
    solver = new Boomerang(callGraph, dataFlowScope, options);
    for (final Query query : queryForCallSites) {
      if (query instanceof BackwardQuery) {
        BackwardBoomerangResults<Weight.NoWeight> res = solver.solve((BackwardQuery) query);
        compareQuery(query, res.getAllocationSites().keySet());
      }
    }
    solver.unregisterAllListeners();
  }

  private boolean allocatesObjectOfInterest(NewExpr rightOp, String type) {
    SootClass interfaceType = Scene.v().getSootClass(type);
    if (!interfaceType.isInterface()) return false;
    RefType allocatedType = rightOp.getBaseType();
    return Scene.v()
        .getActiveHierarchy()
        .getImplementersOf(interfaceType)
        .contains(allocatedType.getSootClass());
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
  public static void queryFor1(Object variable, Class interfaceType) {}

  public static void queryFor2(Object variable, Class interfaceType) {}

  public static void accessPathQueryFor(Object variable, String aliases) {}

  protected void queryForAndNotEmpty(Object variable) {}

  protected void intQueryFor(int variable) {}

  /**
   * A call to this method flags the object as at the call statement as not reachable by the
   * analysis.
   */
  protected void unreachable(Object variable) {}

  /** This method can be used in test cases to create branching. It is not optimized away. */
  protected boolean staticallyUnknown() {
    return true;
  }
}
