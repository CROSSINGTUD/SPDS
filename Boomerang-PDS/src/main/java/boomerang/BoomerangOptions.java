package boomerang;

import soot.Value;

public interface BoomerangOptions {

	
	public boolean staticFlows();
	
	
	public boolean arrayFlows();
	public boolean fastForwardFlows();
	public boolean typeCheck();
	public boolean onTheFlyCallGraph();
	public boolean throwFlows();
	
	public boolean callSummaries();
	public boolean fieldSummaries();

	public boolean isAllocationVal(Value val);
}
