package ideal;

import boomerang.jimple.Statement;
import boomerang.jimple.Val;
import sync.pds.solver.nodes.Node;
import wpds.impl.Weight;

public interface NonOneFlowListener<W extends Weight> {

	void nonOneFlow(Node<Statement, Val> curr, W weight);

}
