/*******************************************************************************
 * Copyright (c) 2018 Fraunhofer IEM, Paderborn, Germany.
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *  
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Johannes Spaeth - initial API and implementation
 *******************************************************************************/
package ideal;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.google.common.base.Stopwatch;

import boomerang.ForwardQuery;
import boomerang.Query;
import boomerang.WeightedForwardQuery;
import boomerang.callgraph.ObservableICFG;
import boomerang.callgraph.ObservableStaticICFG;
import boomerang.results.ForwardBoomerangResults;
import boomerang.seedfactory.SeedFactory;
import boomerang.seedfactory.SimpleSeedFactory;
import com.google.common.base.Stopwatch;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import soot.SootMethod;
import soot.Unit;
import soot.jimple.Stmt;
import soot.jimple.toolkits.ide.icfg.JimpleBasedInterproceduralCFG;
import typestate.TransitionFunction;
import wpds.impl.Weight;

import java.util.*;

public class IDEALAnalysis<W extends Weight> {

    private static final Logger logger = LogManager.getLogger();

    public static boolean PRINT_OPTIONS = false;

    protected final IDEALAnalysisDefinition<W> analysisDefinition;
    private final SeedFactory<W> seedFactory;
    private int seedCount;
    private Map<WeightedForwardQuery<W>, Stopwatch> analysisTime = new HashMap<>();
    private Set<WeightedForwardQuery<W>> timedoutSeeds = new HashSet<>();

    public IDEALAnalysis(final IDEALAnalysisDefinition<W> analysisDefinition) {
        this.analysisDefinition = analysisDefinition;
        this.seedFactory = new SeedFactory<W>() {

            @Override
            protected Collection<WeightedForwardQuery<W>> generate(SootMethod method, Stmt stmt) {
                return analysisDefinition.generate(method, stmt);
            }

            @Override
            public ObservableICFG<Unit, SootMethod> icfg() {
                return analysisDefinition.icfg();
            }
        };
    }

    public void run() {
        printOptions();

        Collection<Query> initialSeeds = seedFactory.computeSeeds();

        if (initialSeeds.isEmpty())
            System.out.println("No seeds found!");
        else
            System.out.println("Analysing " + initialSeeds.size() + " seeds!");
        for (Query s : initialSeeds) {
            if (!(s instanceof WeightedForwardQuery))
                continue;
            WeightedForwardQuery<W> seed = (WeightedForwardQuery<W>) s;
            seedCount++;
            logger.info("Analyzing " + seed);
            Stopwatch watch = Stopwatch.createStarted();
            analysisTime.put(seed, watch);
            if (analysisDefinition.icfg() != null)
                analysisDefinition.icfg().resetCallGraph();
            ForwardBoomerangResults<W> res = run(seed);
            watch.stop();
            System.out.println("Analyzed (finished,timedout): \t (" + (seedCount - timedoutSeeds.size()) + ","
                    + timedoutSeeds.size() + ") of " + initialSeeds.size() + " seeds! ");
            analysisDefinition.getResultHandler().report(seed, res);
        }
    }

    public ForwardBoomerangResults<W> run(ForwardQuery seed) {
        IDEALSeedSolver<W> idealAnalysis = new IDEALSeedSolver<W>(analysisDefinition, seed, seedFactory);
        ForwardBoomerangResults<W> res;
        try {
            if (analysisDefinition.icfg() != null) {
                analysisDefinition.icfg().addUnbalancedMethod(seed.stmt().getMethod());
            }
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
