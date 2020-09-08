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
package boomerang.stats;

import boomerang.BackwardQuery;
import boomerang.ForwardQuery;
import boomerang.Query;
import boomerang.WeightedBoomerang;
import boomerang.results.BackwardBoomerangResults;
import boomerang.results.ForwardBoomerangResults;
import boomerang.scene.ControlFlowGraph.Edge;
import boomerang.scene.Method;
import boomerang.scene.Val;
import boomerang.solver.AbstractBoomerangSolver;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import sync.pds.solver.nodes.INode;
import sync.pds.solver.nodes.Node;
import wpds.impl.Transition;
import wpds.impl.Weight;
import wpds.impl.WeightedPAutomaton;
import wpds.interfaces.WPAUpdateListener;

/** Created by johannesspath on 06.12.17. */
public class SimpleBoomerangStats<W extends Weight> implements IBoomerangStats<W> {

  private Map<Query, AbstractBoomerangSolver<W>> queries = Maps.newHashMap();
  private Set<Method> callVisitedMethods = Sets.newHashSet();
  private Set<Method> fieldVisitedMethods = Sets.newHashSet();

  @Override
  public void registerSolver(Query key, final AbstractBoomerangSolver<W> solver) {
    if (queries.containsKey(key)) {
      return;
    }
    queries.put(key, solver);

    solver
        .getCallAutomaton()
        .registerListener(
            new WPAUpdateListener<Edge, INode<Val>, W>() {
              @Override
              public void onWeightAdded(
                  Transition<Edge, INode<Val>> t,
                  W w,
                  WeightedPAutomaton<Edge, INode<Val>, W> aut) {
                callVisitedMethods.add(t.getLabel().getMethod());
              }
            });
    solver
        .getFieldAutomaton()
        .registerListener(
            (t, w, aut) -> fieldVisitedMethods.add(t.getStart().fact().stmt().getMethod()));
  }

  @Override
  public void registerFieldWritePOI(WeightedBoomerang<W>.FieldWritePOI key) {}

  public String toString() {
    String s = "=========== Boomerang Stats =============\n";
    int forwardQuery = 0;
    int backwardQuery = 0;
    for (Query q : queries.keySet()) {
      if (q instanceof ForwardQuery) {
        forwardQuery++;
      } else backwardQuery++;
    }
    s +=
        String.format(
            "Queries (Forward/Backward/Total): \t\t %s/%s/%s\n",
            forwardQuery, backwardQuery, queries.keySet().size());
    s +=
        String.format(
            "Visited Methods (Field/Call): \t\t %s/%s/(%s/%s)\n",
            fieldVisitedMethods.size(),
            callVisitedMethods.size(),
            Sets.difference(fieldVisitedMethods, callVisitedMethods).size(),
            Sets.difference(callVisitedMethods, fieldVisitedMethods).size());
    s += "\n";
    return s;
  }

  @Override
  public Collection<? extends Node<Edge, Val>> getForwardReachesNodes() {
    Set<Node<Edge, Val>> res = Sets.newHashSet();
    for (Query q : queries.keySet()) {
      if (q instanceof ForwardQuery) res.addAll(queries.get(q).getReachedStates());
    }
    return res;
  }

  @Override
  public Set<Method> getCallVisitedMethods() {
    return Sets.newHashSet(callVisitedMethods);
  }

  @Override
  public void terminated(ForwardQuery query, ForwardBoomerangResults<W> forwardBoomerangResults) {}

  @Override
  public void terminated(
      BackwardQuery query, BackwardBoomerangResults<W> backwardBoomerangResults) {}
}
