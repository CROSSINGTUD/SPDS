package boomerang;

import boomerang.debugger.Debugger;
import boomerang.jimple.Field;
import boomerang.jimple.Statement;
import boomerang.jimple.Val;
import sync.pds.solver.OneWeightFunctions;
import sync.pds.solver.WeightFunctions;
import wpds.impl.Weight.NoWeight;

public abstract class Boomerang extends WeightedBoomerang<NoWeight> {
	
	public Boomerang(){
		super();
	}
	public Boomerang(BoomerangOptions opt){
		super(opt);
	}
	
	@Override
	protected WeightFunctions<Statement, Val, Field, NoWeight> getForwardFieldWeights() {
		return new OneWeightFunctions<Statement, Val, Field, NoWeight>(NoWeight.NO_WEIGHT_ZERO, NoWeight.NO_WEIGHT_ONE);
	}

	@Override
	protected WeightFunctions<Statement, Val, Field, NoWeight> getBackwardFieldWeights() {
		return new OneWeightFunctions<Statement, Val, Field, NoWeight>(NoWeight.NO_WEIGHT_ZERO, NoWeight.NO_WEIGHT_ONE);
	}

	@Override
	protected WeightFunctions<Statement, Val, Statement, NoWeight> getBackwardCallWeights() {
		return new OneWeightFunctions<Statement, Val, Statement, NoWeight>(NoWeight.NO_WEIGHT_ZERO, NoWeight.NO_WEIGHT_ONE);
	}

	@Override
	protected WeightFunctions<Statement, Val, Statement, NoWeight> getForwardCallWeights(ForwardQuery sourceQuery) {
		return new OneWeightFunctions<Statement, Val, Statement, NoWeight>(NoWeight.NO_WEIGHT_ZERO, NoWeight.NO_WEIGHT_ONE);
	}

	@Override
	public Debugger<NoWeight> createDebugger() {
		return new Debugger<>();
	}

}
