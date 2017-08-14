package test.core.selfrunning;

import soot.Value;
import soot.jimple.Stmt;

public class Query {
	private Value val;
	private Stmt stmt;

	public Query(Stmt stmt, Value val){
		this.stmt = stmt;
		this.val = val;
	}
	
	public Stmt stmt(){
		return stmt;
	}
	
	public Value fact(){
		return val;
	}
}
