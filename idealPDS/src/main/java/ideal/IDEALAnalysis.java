package ideal;

import java.util.Collection;
import java.util.Map;

import com.google.common.base.Stopwatch;
import com.google.common.collect.Maps;

import boomerang.Query;
import boomerang.WeightedForwardQuery;
import boomerang.seedfactory.SeedFactory;
import heros.InterproceduralCFG;
import soot.SootMethod;
import soot.Unit;
import soot.jimple.Stmt;
import soot.jimple.toolkits.ide.icfg.BiDiInterproceduralCFG;
import wpds.impl.Weight;

public class IDEALAnalysis<W extends Weight> {

	public static boolean ENABLE_STRONG_UPDATES = true;
	public static boolean SEED_IN_APPLICATION_CLASS_METHOD = false;
	public static boolean PRINT_OPTIONS = false;

	private final InterproceduralCFG<Unit, SootMethod> icfg;
	protected final IDEALAnalysisDefinition<W> analysisDefinition;
	private final SeedFactory<W> seedFactory;
	private int timeoutCount;
	private int seedCount;

	public IDEALAnalysis(final IDEALAnalysisDefinition<W> analysisDefinition) {
		this.analysisDefinition = analysisDefinition;
		this.icfg = analysisDefinition.icfg();
		this.seedFactory = new SeedFactory<W>(){

			@Override
			public BiDiInterproceduralCFG<Unit, SootMethod> icfg() {
				return analysisDefinition.icfg();
			}
			@Override
			protected Collection<WeightedForwardQuery<W>> generate(SootMethod method, Stmt stmt,
					Collection<SootMethod> calledMethods) {
				return analysisDefinition.generate(method, stmt, calledMethods);
			}
		};
	}

	public Map<WeightedForwardQuery<W>, IDEALSeedSolver<W>> run() {
		printOptions();
		Stopwatch watch = Stopwatch.createStarted();
		Collection<Query> initialSeeds = seedFactory.computeSeeds();
//		System.out.println("Computed seeds in: "+ watch.elapsed() );
		watch.reset();
		watch.start();
		if (initialSeeds.isEmpty())
			System.err.println("No seeds found!");
		else
			System.err.println("Analysing " + initialSeeds.size() + " seeds!");
		Map<WeightedForwardQuery<W>, IDEALSeedSolver<W>> seedToSolver = Maps.newHashMap();
		for (Query s : initialSeeds) {
			if(!(s instanceof WeightedForwardQuery))
				continue;
			WeightedForwardQuery<W> seed = (WeightedForwardQuery) s;
			seedCount++;
			System.err.println("Analyzing "+ seed);
			try {
				IDEALSeedSolver<W> solver = run(seed);
				seedToSolver.put(seed, solver);
//				System.err.println(String.format("Seed Analysis finished in ms (Solver1/Solver2):  %s/%s", solver.getPhase1Solver().getAnalysisStopwatch().elapsed(), solver.getPhase2Solver().getAnalysisStopwatch().elapsed()));
			} catch(IDEALSeedTimeout e){
				seedToSolver.put(seed, (IDEALSeedSolver<W>) e.getSolver());
				timeoutCount++;
			}
			System.err.println("Analyzed (finished,timedout): \t (" + (seedCount -timeoutCount)+ "," + timeoutCount + ") of "+ initialSeeds.size() + " seeds! ");
		}
//		System.out.println("Analysis time for all seeds: "+ watch.elapsed());
		return seedToSolver;
	}
	public IDEALSeedSolver<W> run(Query seed) {
		IDEALSeedSolver<W> idealAnalysis = new IDEALSeedSolver<W>(analysisDefinition, seed, seedFactory);
		idealAnalysis.run();
		return idealAnalysis;
	}
	private void printOptions() {
		if(PRINT_OPTIONS)
			System.out.println(analysisDefinition);
	}

	public Collection<Query> computeSeeds() {
		return seedFactory.computeSeeds();
	}


}
