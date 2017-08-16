package sync.pds.solver;

import wpds.interfaces.Location;

public interface SyncPDSUpdateListener<Stmt extends Location, Fact, Field extends Location>  {

	public void onReachableNodeAdded(SyncPDSSolver<Stmt, Fact, Field>.QueuedNode reachableNode);

}
