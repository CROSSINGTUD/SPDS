package boomerang.jimple;

import soot.SootMethod;
import soot.jimple.Stmt;

public class ReturnSite extends Statement {

	private Stmt callSite;

	public ReturnSite(Stmt returnSite, SootMethod method, Stmt callSite) {
		super(returnSite, method);
		this.callSite = callSite;
	}
	
	public Stmt getCallSite(){
		return callSite;
	}

}
