package sync.pds.solver;

import wpds.interfaces.Location;

public interface SyncPDSUpdateListener<Stmt extends Location, Fact, Field extends Location>  {

	public void onReachableNodeAdded(WitnessNode<Stmt,Fact,Field> reachableNode);

}
