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
package boomerang;

import boomerang.scene.AnalysisScope;
import boomerang.scene.CallGraph;
import boomerang.scene.ControlFlowGraph.Edge;
import boomerang.scene.DataFlowScope;
import boomerang.scene.Statement;
import java.util.Collection;
import java.util.Collections;
import wpds.impl.Weight;

public abstract class WholeProgramBoomerang<W extends Weight> extends WeightedBoomerang<W> {
  private int reachableMethodCount;
  private int allocationSites;
  private CallGraph callGraph;

  public WholeProgramBoomerang(CallGraph cg, DataFlowScope scope, BoomerangOptions opts) {
    super(cg, scope, opts);
    this.callGraph = cg;
  }

  public WholeProgramBoomerang(CallGraph cg, DataFlowScope scope) {
    this(cg, scope, new DefaultBoomerangOptions());
  }

  public void wholeProgramAnalysis() {
    long before = System.currentTimeMillis();
    AnalysisScope scope =
        new AnalysisScope(callGraph) {
          @Override
          protected Collection<? extends Query> generate(Edge cfgEdge) {
            Statement stmt = cfgEdge.getStart();
            if (stmt.isAssign()) {
              if (stmt.getRightOp().isNewExpr()) {
                return Collections.singleton(new ForwardQuery(cfgEdge, stmt.getRightOp()));
              }
            }
            return Collections.emptySet();
          }
        };
    for (Query s : scope.computeSeeds()) {
      solve((ForwardQuery) s);
    }

    long after = System.currentTimeMillis();
    System.out.println("Analysis Time (in ms):\t" + (after - before));
    System.out.println("Analyzed methods:\t" + reachableMethodCount);
    System.out.println("Total solvers:\t" + this.getSolvers().size());
    System.out.println("Allocation Sites:\t" + allocationSites);
    System.out.println(options.statsFactory());
  }

  @Override
  protected void backwardSolve(BackwardQuery query) {}
}
