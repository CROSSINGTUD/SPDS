package boomerang.solver;

import boomerang.jimple.Field;
import boomerang.jimple.Statement;
import boomerang.jimple.Val;
import sync.pds.solver.nodes.INode;
import sync.pds.solver.nodes.Node;
import wpds.impl.Transition;
import wpds.impl.Weight;
import wpds.interfaces.WPAUpdateListener;

public abstract class StatementBasedFieldTransitionListener<W extends Weight> implements WPAUpdateListener<Field, INode<Node<Statement,Val>>, W> {

	private final Statement stmt;

	public StatementBasedFieldTransitionListener(Statement stmt){
		this.stmt = stmt;
	}
	public Statement getStmt() {
		return stmt;
	}
	
	
	@Override
	public void onWeightAdded(Transition<Field, INode<Node<Statement, Val>>> t, W w) {
		onAddedTransition(t);
	}

	public abstract void onAddedTransition(Transition<Field, INode<Node<Statement, Val>>> t);

}
