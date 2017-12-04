package ideal;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import boomerang.WeightedForwardQuery;
import com.google.common.collect.Maps;

import boomerang.Query;
import boomerang.jimple.AllocVal;
import boomerang.jimple.Statement;
import heros.InterproceduralCFG;
import soot.MethodOrMethodContext;
import soot.Scene;
import soot.SootMethod;
import soot.Unit;
import soot.jimple.Stmt;
import soot.jimple.toolkits.callgraph.ReachableMethods;
import soot.util.queue.QueueReader;
import sync.pds.solver.nodes.Node;
import wpds.impl.Weight;

public class IDEALAnalysis<W extends Weight> {

	public static boolean ENABLE_STRONG_UPDATES = true;
	public static boolean SEED_IN_APPLICATION_CLASS_METHOD = false;
	public static boolean PRINT_OPTIONS = false;

	private final InterproceduralCFG<Unit, SootMethod> icfg;
	protected final IDEALAnalysisDefinition<W> analysisDefinition;
	private int timeoutCount;
	private int seedCount;

	public IDEALAnalysis(IDEALAnalysisDefinition<W> analysisDefinition) {
		this.analysisDefinition = analysisDefinition;
		this.icfg = analysisDefinition.icfg();
	}

	public Map<WeightedForwardQuery<W>, IDEALSeedSolver<W>> run() {
		printOptions();
		long before = System.currentTimeMillis();
		Set<WeightedForwardQuery<W>> initialSeeds = computeSeeds();
		long after = System.currentTimeMillis();
		System.out.println("Computed seeds in: "+ (after-before)  + " ms");
		if (initialSeeds.isEmpty())
			System.err.println("No seeds found!");
		else
			System.err.println("Analysing " + initialSeeds.size() + " seeds!");
		Map<WeightedForwardQuery<W>, IDEALSeedSolver<W>> seedToSolver = Maps.newHashMap();
		for (WeightedForwardQuery<W> seed : initialSeeds) {
			seedCount++;
			System.err.println("Analyzing "+ seed);
			try {
				seedToSolver.put(seed, run(seed));
			} catch(IDEALSeedTimeout e){
				seedToSolver.put(seed, (IDEALSeedSolver<W>) e.getSolver());
				timeoutCount++;
			}
			System.err.println("Analyzed (finished,timedout): \t (" + (seedCount -timeoutCount)+ "," + timeoutCount + ") of "+ initialSeeds.size() + " seeds! ");
		}
		return seedToSolver;
	}
	public IDEALSeedSolver<W> run(Query seed) {
		IDEALSeedSolver<W> idealAnalysis = new IDEALSeedSolver<W>(analysisDefinition, seed);
		idealAnalysis.run();
		return idealAnalysis;
	}
	private void printOptions() {
		if(PRINT_OPTIONS)
			System.out.println(analysisDefinition);
	}

	public Set<WeightedForwardQuery<W>> computeSeeds() {
		Set<WeightedForwardQuery<W>> seeds = new HashSet<>();
		ReachableMethods rm = Scene.v().getReachableMethods();
		QueueReader<MethodOrMethodContext> listener = rm.listener();
		while (listener.hasNext()) {
			MethodOrMethodContext next = listener.next();
			seeds.addAll(computeSeeds(next.method()));
		}
		return seeds;
	}

	private Collection<WeightedForwardQuery<W>> computeSeeds(SootMethod method) {
		Set<WeightedForwardQuery<W>> seeds = new HashSet<>();
		if (!method.hasActiveBody())
			return seeds;
		if (SEED_IN_APPLICATION_CLASS_METHOD && !method.getDeclaringClass().isApplicationClass())
			return seeds;
		for (Unit u : method.getActiveBody().getUnits()) {
			Collection<SootMethod> calledMethods = (icfg.isCallStmt(u) ? icfg.getCalleesOfCallAt(u)
					: new HashSet<SootMethod>());
			seeds.addAll(analysisDefinition.generate(method, u, calledMethods));
		}
		return seeds;
	}

}
