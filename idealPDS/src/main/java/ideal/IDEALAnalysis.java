package ideal;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import boomerang.jimple.Statement;
import boomerang.jimple.Val;
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

	public IDEALAnalysis(IDEALAnalysisDefinition<W> analysisDefinition) {
		this.analysisDefinition = analysisDefinition;
		this.icfg = analysisDefinition.icfg();
	}

	public void run() {
		printOptions();
		long before = System.currentTimeMillis();
		Set<Node<Statement,Val>> initialSeeds = computeSeeds();
		long after = System.currentTimeMillis();
		System.out.println("Computed seeds in: "+ (after-before)  + " ms");
		if (initialSeeds.isEmpty())
			System.err.println("No seeds found!");
		else
			System.err.println("Analysing " + initialSeeds.size() + " seeds!");
		
		for (Node<Statement, Val> seed : initialSeeds) {
			run(seed);
		}
	}
	public PerSeedAnalysisContext<W> run(Node<Statement, Val> seed) {
		PerSeedAnalysisContext<W> idealAnalysis = new PerSeedAnalysisContext<W>(analysisDefinition, seed);
		idealAnalysis.run();
		return idealAnalysis;
	}
	private void printOptions() {
		if(PRINT_OPTIONS)
			System.out.println(analysisDefinition);
	}

	public Set<Node<Statement,Val>> computeSeeds() {
		Set<Node<Statement,Val>> seeds = new HashSet<>();
		ReachableMethods rm = Scene.v().getReachableMethods();
		QueueReader<MethodOrMethodContext> listener = rm.listener();
		while (listener.hasNext()) {
			MethodOrMethodContext next = listener.next();
			seeds.addAll(computeSeeds(next.method()));
		}
		return seeds;
	}

	private Collection<Node<Statement,Val>> computeSeeds(SootMethod method) {
		Set<Node<Statement,Val>> seeds = new HashSet<>();
		if (!method.hasActiveBody())
			return seeds;
		if (SEED_IN_APPLICATION_CLASS_METHOD && !method.getDeclaringClass().isApplicationClass())
			return seeds;
		for (Unit u : method.getActiveBody().getUnits()) {
			Collection<SootMethod> calledMethods = (icfg.isCallStmt(u) ? icfg.getCalleesOfCallAt(u)
					: new HashSet<SootMethod>());
			for (Val fact : analysisDefinition.generate(method, u, calledMethods)) {
				seeds.add(new Node<Statement,Val>(new Statement((Stmt)u, method),fact));
			}
		}
		return seeds;
	}

}
