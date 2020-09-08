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
package test;

import boomerang.results.ForwardBoomerangResults;
import boomerang.scene.ControlFlowGraph.Edge;
import boomerang.scene.Statement;
import boomerang.scene.Val;
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Table;
import com.google.common.collect.Table.Cell;
import java.util.Map.Entry;
import java.util.Set;
import sync.pds.solver.nodes.Node;
import wpds.impl.Weight;

public class TestingResultReporter<W extends Weight> {
  private Multimap<Statement, Assertion> stmtToResults = HashMultimap.create();

  public TestingResultReporter(Set<Assertion> expectedResults) {
    for (Assertion e : expectedResults) {
      if (e instanceof ComparableResult) stmtToResults.put(((ComparableResult) e).getStmt(), e);
    }
  }

  public void onSeedFinished(Node<Edge, Val> seed, final ForwardBoomerangResults<W> res) {
    Table<Edge, Val, W> resultsAsCFGEdges = res.asStatementValWeightTable();
    Table<Statement, Val, W> results = HashBasedTable.create();

    for (Cell<Edge, Val, W> c : resultsAsCFGEdges.cellSet()) {
      results.put(c.getRowKey().getTarget(), c.getColumnKey(), c.getValue());
    }

    for (final Entry<Statement, Assertion> e : stmtToResults.entries()) {
      if (e.getValue() instanceof ComparableResult) {
        final ComparableResult<W, Val> expectedResults = (ComparableResult) e.getValue();
        W w2 = results.get(e.getKey(), expectedResults.getVal());
        if (w2 != null) {
          expectedResults.computedResults(w2);
        }
      }
      // check if any of the methods that should not be analyzed have been analyzed
      if (e.getValue() instanceof ShouldNotBeAnalyzed) {
        final ShouldNotBeAnalyzed shouldNotBeAnalyzed = (ShouldNotBeAnalyzed) e.getValue();
        Statement analyzedUnit = e.getKey();
        if (analyzedUnit.equals(shouldNotBeAnalyzed.unit)) {
          shouldNotBeAnalyzed.hasBeenAnalyzed();
        }
      }
    }
  }
}
