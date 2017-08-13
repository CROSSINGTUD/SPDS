package analysis;

import analysis.Solver.PDSSystem;
import wpds.interfaces.State;

public class PopNode<Location> implements State{

	private PDSSystem system;
	private Location location;
	public PopNode(Location location, PDSSystem system) {
		this.system = system;
		this.location = location;
	}
	
	public PDSSystem system(){
		return system;
	}
	

	public Location location(){
		return location;
	}
	@Override
	public String toString() {
		return "Pop " + location();
	}

}
