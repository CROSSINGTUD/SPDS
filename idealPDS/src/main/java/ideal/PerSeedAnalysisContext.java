package ideal;

import boomerang.Boomerang;
import boomerang.ForwardQuery;
import boomerang.debugger.Debugger;
import boomerang.jimple.Field;
import boomerang.jimple.Statement;
import boomerang.jimple.Val;
import soot.SootMethod;
import soot.Unit;
import soot.jimple.toolkits.ide.icfg.BiDiInterproceduralCFG;
import sync.pds.solver.OneWeightFunctions;
import sync.pds.solver.WeightFunctions;
import sync.pds.solver.nodes.Node;
import wpds.impl.Weight;

public class PerSeedAnalysisContext<W extends Weight> {

	private IDEALAnalysisDefinition<W> analysisDefinition;
	private Node<Statement, Val> seed;

	public PerSeedAnalysisContext(IDEALAnalysisDefinition<W> analysisDefinition, Node<Statement, Val> seed) {
		this.analysisDefinition = analysisDefinition;
		this.seed = seed;
	}

	public void run() {
		Boomerang<W> boomerang = new Boomerang<W>() {
			
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
				return analysisDefinition.weightFunctions();
			}

			@Override
			protected WeightFunctions<Statement, Val, Field, W> getForwardFieldWeights() {
				return new OneWeightFunctions<Statement, Val, Field, W>(analysisDefinition.weightFunctions().getZero(), analysisDefinition.weightFunctions().getOne());
			}

			@Override
			protected WeightFunctions<Statement, Val, Field, W> getBackwardFieldWeights() {
				return new OneWeightFunctions<Statement, Val, Field, W>(analysisDefinition.weightFunctions().getZero(), analysisDefinition.weightFunctions().getOne());
			}

			@Override
			protected WeightFunctions<Statement, Val, Statement, W> getBackwardCallWeights() {
				return new OneWeightFunctions<Statement, Val, Statement, W>(analysisDefinition.weightFunctions().getZero(), analysisDefinition.weightFunctions().getOne());
			}

		};
		boomerang.solve(new ForwardQuery(seed.stmt(), seed.fact()));
	}

}
