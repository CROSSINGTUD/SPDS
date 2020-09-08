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
package ideal;

import boomerang.ForwardQuery;
import boomerang.Query;
import boomerang.WeightedForwardQuery;
import boomerang.results.ForwardBoomerangResults;
import boomerang.scene.AnalysisScope;
import boomerang.scene.ControlFlowGraph.Edge;
import com.google.common.base.Stopwatch;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import typestate.TransitionFunction;
import wpds.impl.Weight;

public class IDEALAnalysis<W extends Weight> {

  private static final Logger LOGGER = LoggerFactory.getLogger(IDEALAnalysis.class);

  public static boolean PRINT_OPTIONS = false;

  protected final IDEALAnalysisDefinition<W> analysisDefinition;
  private final AnalysisScope seedFactory;
  private int seedCount;
  private Map<WeightedForwardQuery<W>, Stopwatch> analysisTime = new HashMap<>();
  private Set<WeightedForwardQuery<W>> timedoutSeeds = new HashSet<>();

  public IDEALAnalysis(final IDEALAnalysisDefinition<W> analysisDefinition) {
    this.analysisDefinition = analysisDefinition;
    this.seedFactory =
        new AnalysisScope(analysisDefinition.callGraph()) {

          @Override
          protected Collection<WeightedForwardQuery<W>> generate(Edge stmt) {
            return analysisDefinition.generate(stmt);
          }
        };
  }

  public void run() {
    printOptions();

    Collection<Query> initialSeeds = seedFactory.computeSeeds();

    if (initialSeeds.isEmpty()) LOGGER.info("No seeds found!");
    else LOGGER.info("Analysing {} seeds!", initialSeeds.size());
    for (Query s : initialSeeds) {
      if (!(s instanceof WeightedForwardQuery)) continue;
      WeightedForwardQuery<W> seed = (WeightedForwardQuery<W>) s;
      seedCount++;
      LOGGER.info("Analyzing {}", seed);
      Stopwatch watch = Stopwatch.createStarted();
      analysisTime.put(seed, watch);
      run(seed);
      watch.stop();
      LOGGER.debug(
          "Analyzed (finished,timedout): \t ({},{}) of {} seeds",
          (seedCount - timedoutSeeds.size()),
          timedoutSeeds.size(),
          initialSeeds.size());
    }
  }

  public ForwardBoomerangResults<W> run(ForwardQuery seed) {
    IDEALSeedSolver<W> idealAnalysis = new IDEALSeedSolver<W>(analysisDefinition, seed);
    ForwardBoomerangResults<W> res;
    try {
      res = idealAnalysis.run();
    } catch (IDEALSeedTimeout e) {
      res = (ForwardBoomerangResults<W>) e.getLastResults();
      timedoutSeeds.add((WeightedForwardQuery) seed);
    }
    analysisDefinition.getResultHandler().report((WeightedForwardQuery) seed, res);
    return res;
  }

  private void printOptions() {
    if (PRINT_OPTIONS) {
      System.out.println(analysisDefinition);
    }
  }

  public Collection<Query> computeSeeds() {
    return seedFactory.computeSeeds();
  }

  public Stopwatch getAnalysisTime(WeightedForwardQuery<TransitionFunction> key) {
    return analysisTime.get(key);
  }

  public boolean isTimedout(WeightedForwardQuery<TransitionFunction> key) {
    return timedoutSeeds.contains(key);
  }
}
