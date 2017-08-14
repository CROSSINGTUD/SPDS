package boomerang.jimple;

import soot.jimple.Stmt;

public class ReturnSite extends Statement {

	private Stmt callSite;

	public ReturnSite(Stmt delegate, Stmt callSite) {
		super(delegate);
		this.callSite = callSite;
	}
	
	public Stmt getCallSite(){
		return callSite;
	}

}
