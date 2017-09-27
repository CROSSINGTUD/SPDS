package typestate;

import java.util.Collection;

import boomerang.jimple.Statement;
import boomerang.jimple.Val;
import ideal.IDEALAnalysisDefinition;
import soot.SootMethod;
import soot.Unit;
import sync.pds.solver.WeightFunctions;

public abstract class TypestateAnalysisProblem extends IDEALAnalysisDefinition<TransitionFunction> {
	private TypestateChangeFunction func;

	@Override
	public WeightFunctions<Statement, Val, Statement, TransitionFunction> weightFunctions() {
		return new TypestateEdgeFunctions(getOrCreateTransitionFunctions());
	}

	private TypestateChangeFunction getOrCreateTransitionFunctions() {
		if(func == null)
			func = createTypestateChangeFunction();
		return func;
	}

	public abstract TypestateChangeFunction createTypestateChangeFunction();

	@Override
	public Collection<Val> generate(SootMethod method, Unit stmt, Collection<SootMethod> optional) {
		return getOrCreateTransitionFunctions().generateSeed(method, stmt, optional);
	}

}
