package ideal;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.Maps;

import boomerang.ForwardQuery;
import boomerang.Query;
import boomerang.WeightedBoomerang;
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

	public Map<Node<Statement, AllocVal>, IDEALSeedSolver<W>> run() {
		printOptions();
		long before = System.currentTimeMillis();
		Set<Node<Statement,AllocVal>> initialSeeds = computeSeeds();
		long after = System.currentTimeMillis();
		System.out.println("Computed seeds in: "+ (after-before)  + " ms");
		if (initialSeeds.isEmpty())
			System.err.println("No seeds found!");
		else
			System.err.println("Analysing " + initialSeeds.size() + " seeds!");
		Map<Node<Statement, AllocVal>, IDEALSeedSolver<W>> seedToSolver = Maps.newHashMap();
		for (Node<Statement, AllocVal> seed : initialSeeds) {
			seedCount++;
			try {
				seedToSolver.put(seed, run(new ForwardQuery(seed.stmt(), seed.fact())));
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

	public Set<Node<Statement,AllocVal>> computeSeeds() {
		Set<Node<Statement,AllocVal>> seeds = new HashSet<>();
		ReachableMethods rm = Scene.v().getReachableMethods();
		QueueReader<MethodOrMethodContext> listener = rm.listener();
		while (listener.hasNext()) {
			MethodOrMethodContext next = listener.next();
			seeds.addAll(computeSeeds(next.method()));
		}
		return seeds;
	}

	private Collection<Node<Statement,AllocVal>> computeSeeds(SootMethod method) {
		Set<Node<Statement,AllocVal>> seeds = new HashSet<>();
		if (!method.hasActiveBody())
			return seeds;
		if (SEED_IN_APPLICATION_CLASS_METHOD && !method.getDeclaringClass().isApplicationClass())
			return seeds;
		for (Unit u : method.getActiveBody().getUnits()) {
			Collection<SootMethod> calledMethods = (icfg.isCallStmt(u) ? icfg.getCalleesOfCallAt(u)
					: new HashSet<SootMethod>());
			for (AllocVal fact : analysisDefinition.generate(method, u, calledMethods)) {
				seeds.add(new Node<Statement,AllocVal>(new Statement((Stmt)u, method),fact));
			}
		}
		return seeds;
	}

}
