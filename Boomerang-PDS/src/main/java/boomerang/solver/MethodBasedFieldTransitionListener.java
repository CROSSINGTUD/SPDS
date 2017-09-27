package boomerang.solver;

import boomerang.jimple.Field;
import boomerang.jimple.Statement;
import boomerang.jimple.Val;
import soot.SootMethod;
import sync.pds.solver.nodes.INode;
import sync.pds.solver.nodes.Node;
import wpds.impl.Transition;
import wpds.impl.Weight;
import wpds.impl.Weight.NoWeight;
import wpds.interfaces.WPAUpdateListener;

public abstract class MethodBasedFieldTransitionListener implements WPAUpdateListener<Field, INode<Node<Statement,Val>>, NoWeight>{
	private final SootMethod method;
	public MethodBasedFieldTransitionListener(SootMethod method) {
		this.method = method;
	}
	
	public SootMethod getMethod(){
		return method;
	}
	
	@Override
	public void onWeightAdded(Transition<Field, INode<Node<Statement, Val>>> t, Weight w) {
		onAddedTransition(t);
	}

	public abstract void onAddedTransition(Transition<Field, INode<Node<Statement, Val>>> t);
}
