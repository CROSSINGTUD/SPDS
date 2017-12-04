package ideal;

import java.util.Map.Entry;

import boomerang.*;
import boomerang.debugger.Debugger;
import boomerang.jimple.Field;
import boomerang.jimple.Statement;
import boomerang.jimple.Val;
import boomerang.solver.AbstractBoomerangSolver;
import com.google.common.base.Stopwatch;
import soot.SootMethod;
import soot.Unit;
import soot.jimple.toolkits.ide.icfg.BiDiInterproceduralCFG;
import sync.pds.solver.EmptyStackWitnessListener;
import sync.pds.solver.OneWeightFunctions;
import sync.pds.solver.WeightFunctions;
import sync.pds.solver.nodes.INode;
import sync.pds.solver.nodes.Node;
import wpds.impl.ConnectPushListener;
import wpds.impl.Weight;
import wpds.impl.WeightedPAutomaton;

public class IDEALSeedSolver<W extends Weight> {

	private final IDEALAnalysisDefinition<W> analysisDefinition;
	private final Query seed;
	private final IDEALWeightFunctions<W> idealWeightFunctions;
	private final W zero;
	private final W one;
	private final WeightedBoomerang<W> phase1Solver;
	private final WeightedBoomerang<W> phase2Solver;
	private final Stopwatch analysisStopwatch = Stopwatch.createUnstarted();
	private WeightedBoomerang<W> timedoutSolver;


    public enum Phases {
		ObjectFlow, ValueFlow
	};

	public IDEALSeedSolver(IDEALAnalysisDefinition<W> analysisDefinition, Query seed) {
		this.analysisDefinition = analysisDefinition;
		this.seed = seed;
		this.idealWeightFunctions = new IDEALWeightFunctions<W>(analysisDefinition.weightFunctions());
		this.zero = analysisDefinition.weightFunctions().getZero();
		this.one = analysisDefinition.weightFunctions().getOne();
		this.phase1Solver = createSolver();
		this.phase2Solver = createSolver();
	}

	public WeightedBoomerang<W> run() {
		try{
			runPhase(this.phase1Solver,Phases.ObjectFlow);
		} catch(BoomerangTimeoutException e){
			System.err.println(e);
			analysisStopwatch.stop();
			timedoutSolver = this.phase1Solver;
			throw new IDEALSeedTimeout(this,this.phase1Solver,e);
		}
		try{
			return runPhase(this.phase2Solver,Phases.ValueFlow);
		} catch(BoomerangTimeoutException e){
			System.err.println(e);
			analysisStopwatch.stop();
			timedoutSolver = this.phase2Solver;
			throw new IDEALSeedTimeout(this,this.phase2Solver,e);
		}
	}

	private WeightedBoomerang<W> createSolver() {
		return new WeightedBoomerang<W>() {
			@Override
			public BiDiInterproceduralCFG<Unit, SootMethod> icfg() {
				return analysisDefinition.icfg();
			}

			@Override
			public Debugger<W> createDebugger() {
				return analysisDefinition.debugger();
			}

			@Override
			protected WeightFunctions<Statement, Val, Statement, W> getForwardCallWeights(ForwardQuery sourceQuery) {
				if(sourceQuery.equals(seed))
				return idealWeightFunctions;
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

	private WeightedBoomerang<W> runPhase(final WeightedBoomerang<W> boomerang, final Phases phase) {
		analysisStopwatch.start();
		idealWeightFunctions.setPhase(phase);
		final WeightedPAutomaton<Statement, INode<Val>, W> callAutomaton = boomerang.getSolvers().getOrCreate(seed).getCallAutomaton();
		callAutomaton.registerConnectPushListener(new ConnectPushListener<Statement, INode<Val>,W>() {

			@Override
			public void connect(Statement callSite, Statement returnSite, INode<Val> returnedFact, W w) {
				if(!callSite.getMethod().equals(returnedFact.fact().m()))
					return;
				if(!callSite.getMethod().equals(returnSite.getMethod()))
					return;
				if(!w.equals(one)){
					idealWeightFunctions.addOtherThanOneWeight(new Node<Statement,Val>(callSite, returnedFact.fact()), w);
				}
			}
		});
		boomerang.solve(seed);
		idealWeightFunctions.registerListener(new NonOneFlowListener<W>() {
			@Override
			public void nonOneFlow(final Node<Statement, Val> curr, final W weight) {
				if(phase.equals(Phases.ValueFlow)){
					return;
				}
				boomerang.solve(new BackwardQuery(curr.stmt(),curr.fact()));
				idealWeightFunctions.potentialStrongUpdate(curr.stmt(), weight);
				for(final Entry<Query, AbstractBoomerangSolver<W>> e : boomerang.getSolvers().entrySet()){
					if(e.getKey() instanceof ForwardQuery){
						e.getValue().synchedEmptyStackReachable(curr, new EmptyStackWitnessListener<Statement, Val>() {
							@Override
							public void witnessFound(Node<Statement, Val> targetFact) {
								if(!e.getKey().asNode().equals(seed.asNode())){
									idealWeightFunctions.weakUpdate(curr.stmt());
								}
							}
						});
					}
				}
			}
		});
//		System.out.println("");
		boomerang.debugOutput();
		analysisStopwatch.stop();
		return boomerang;
	}

	public WeightedBoomerang<W> getPhase1Solver() {
		return phase1Solver;
	}

	public WeightedBoomerang<W> getPhase2Solver() {
		return phase2Solver;
	}

	public Stopwatch getAnalysisStopwatch() {
		return analysisStopwatch;
	}

	public boolean isTimedOut(){
		return timedoutSolver != null;
	}

	public WeightedBoomerang getTimedoutSolver() {
		return timedoutSolver;
	}
	public Query getSeed() {
		return seed;
	}
}
