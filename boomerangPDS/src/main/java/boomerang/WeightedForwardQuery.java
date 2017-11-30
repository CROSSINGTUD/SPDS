package boomerang;

import boomerang.jimple.Statement;
import boomerang.jimple.Val;
import wpds.impl.Weight;

public class WeightedForwardQuery<W extends Weight> extends ForwardQuery{

	private final W weight;

	public WeightedForwardQuery(Statement stmt, Val variable, W weight) {
		super(stmt, variable);
		this.weight = weight;
	}
	
	public W weight(){
		return weight;
	};

}
