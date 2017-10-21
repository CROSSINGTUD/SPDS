package sync.pds.solver;

import wpds.interfaces.Location;

public abstract class SyncStatePDSUpdateListener<Stmt extends Location, Fact, Field extends Location>  {
	
	private WitnessNode<Stmt, Fact, Field> node;
	public SyncStatePDSUpdateListener(WitnessNode<Stmt,Fact, Field> node){
		this.node = node;
	}
	public abstract void reachable();
	public WitnessNode<Stmt, Fact, Field> getNode(){
		return node;
	}

}
