package boomerang.solver;

import java.util.Collection;

import analysis.DoublePDSSolver;
import analysis.Node;
import boomerang.jimple.Field;
import boomerang.jimple.Stmt;
import soot.SootField;
import soot.Unit;
import soot.Value;
import wpds.interfaces.State;

public class BoomerangSolver extends DoublePDSSolver<Stmt, Value, Field>{

	@Override
	public Field fieldWildCard() {
		return Field.wildcard();
	}

	@Override
	public Collection<State> computeSuccessor(Node<Stmt, Value> node) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Field epsilonField() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Field emptyField() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Stmt epsilonCallSite() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Stmt emptyCallSite() {
		// TODO Auto-generated method stub
		return null;
	}

}
