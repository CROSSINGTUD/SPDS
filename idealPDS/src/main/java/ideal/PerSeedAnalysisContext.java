package ideal;

import java.util.Map.Entry;

import javax.sound.midi.Synthesizer;

import boomerang.BackwardQuery;
import boomerang.Boomerang;
import boomerang.ForwardQuery;
import boomerang.Query;
import boomerang.debugger.Debugger;
import boomerang.jimple.Field;
import boomerang.jimple.Statement;
import boomerang.jimple.Val;
import boomerang.solver.AbstractBoomerangSolver;
import soot.SootMethod;
import soot.Unit;
import soot.jimple.toolkits.ide.icfg.BiDiInterproceduralCFG;
import sync.pds.solver.EmptyStackWitnessListener;
import sync.pds.solver.OneWeightFunctions;
import sync.pds.solver.WeightFunctions;
import sync.pds.solver.nodes.Node;
import wpds.impl.Weight;

public class PerSeedAnalysisContext<W extends Weight> {

	private final IDEALAnalysisDefinition<W> analysisDefinition;
	private final ForwardQuery seed;
	private final IDEALWeightFunctions<W> idealWeightFunctions;
	private final W zero;
	private final W one;
	public static enum Phases {
		ObjectFlow, ValueFlow
	};

	public PerSeedAnalysisContext(IDEALAnalysisDefinition<W> analysisDefinition, Node<Statement, Val> seed) {
		this.analysisDefinition = analysisDefinition;
		this.seed = new ForwardQuery(seed.stmt(), seed.fact());
		this.idealWeightFunctions = new IDEALWeightFunctions<W>(analysisDefinition.weightFunctions());
		this.zero = analysisDefinition.weightFunctions().getZero();
		this.one = analysisDefinition.weightFunctions().getOne();
	}

	public void run() {
		runPhase(Phases.ObjectFlow);
		runPhase(Phases.ValueFlow);
	}

	private void runPhase(final Phases phase) {
		final Boomerang<W> boomerang = new Boomerang<W>() {
			@Override
			public BiDiInterproceduralCFG<Unit, SootMethod> icfg() {
				return analysisDefinition.icfg();
			}

			@Override
			public Debugger createDebugger() {
				return null;
			}

			@Override
			protected WeightFunctions<Statement, Val, Statement, W> getForwardCallWeights() {
				return idealWeightFunctions;
			}

			@Override
			protected WeightFunctions<Statement, Val, Field, W> getForwardFieldWeights() {
				return new OneWeightFunctions<Statement, Val, Field, W>(zero, one);
			}

			@Override
			protected WeightFunctions<Statement, Val, Field, W> getBackwardFieldWeights() {
				return new OneWeightFunctions<Statement, Val, Field, W>(zero, one);
			}

			@Override
			protected WeightFunctions<Statement, Val, Statement, W> getBackwardCallWeights() {
				return new OneWeightFunctions<Statement, Val, Statement, W>(zero, one);
			}

		};
		idealWeightFunctions.setPhase(phase);
		boomerang.solve(seed);
		idealWeightFunctions.registerListener(new NonOneFlowListener<W>() {
			@Override
			public void nonOneFlow(final Node<Statement, Val> curr, final W weight) {
				if(phase.equals(Phases.ValueFlow)){
					return;
				}
				Boomerang<W> weightsLessBoomerang = weightsLessBoomerang();
				
				weightsLessBoomerang.solve(new BackwardQuery(curr.stmt(),curr.fact()));
				idealWeightFunctions.potentialStrongUpdate(curr.stmt(), weight);
				for(final Entry<Query, AbstractBoomerangSolver<W>> e : weightsLessBoomerang.getSolvers().entrySet()){
					if(e.getKey() instanceof ForwardQuery){
						e.getValue().synchedEmptyStackReachable(curr, new EmptyStackWitnessListener<Statement, Val>() {
							@Override
							public void witnessFound(Node<Statement, Val> targetFact) {
								if(!e.getKey().asNode().equals(seed.asNode())){
									System.out.println("No strong update " + curr);
									idealWeightFunctions.weakUpdate(curr.stmt());
								}
							}
						});
					}
				}
			}
		});
		if(phase.equals(Phases.ValueFlow)){
			analysisDefinition.resultReporter().onSeedFinished(seed, boomerang.getSolvers().getOrCreate(seed));
		}
	}

	private Boomerang<W> weightsLessBoomerang() {
		return new Boomerang<W>() {
			@Override
			public BiDiInterproceduralCFG<Unit, SootMethod> icfg() {
				return analysisDefinition.icfg();
			}

			@Override
			public Debugger createDebugger() {
				return null;
			}

			@Override
			protected WeightFunctions<Statement, Val, Statement, W> getForwardCallWeights() {
				return new OneWeightFunctions<Statement, Val, Statement, W>(zero, one);
			}

			@Override
			protected WeightFunctions<Statement, Val, Field, W> getForwardFieldWeights() {
				return new OneWeightFunctions<Statement, Val, Field, W>(zero, one);
			}

			@Override
			protected WeightFunctions<Statement, Val, Field, W> getBackwardFieldWeights() {
				return new OneWeightFunctions<Statement, Val, Field, W>(zero, one);
			}

			@Override
			protected WeightFunctions<Statement, Val, Statement, W> getBackwardCallWeights() {
				return new OneWeightFunctions<Statement, Val, Statement, W>(zero, one);
			}

		};
	}

}
