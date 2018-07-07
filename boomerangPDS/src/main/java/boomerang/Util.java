package boomerang;

import java.util.List;

import boomerang.jimple.Statement;
import boomerang.jimple.Val;
import soot.Local;
import soot.SootMethod;
import soot.jimple.ReturnStmt;
import soot.jimple.Stmt;

public class Util {
	public static boolean isParameterLocal(Val val, SootMethod m) {
		if(val.isStatic())
			return false;
		if(!m.hasActiveBody()) {
			throw new RuntimeException("Soot Method has no active body");
		}
		List<Local> parameterLocals = m.getActiveBody().getParameterLocals();
		return parameterLocals.contains(val.value());
	}

	public static boolean isReturnOperator(Val val, Statement returnStmt) {
		Stmt stmt = returnStmt.getUnit().get();
		return (stmt instanceof ReturnStmt && ((ReturnStmt) stmt).getOp().equals(val));
	}

	public static boolean isThisLocal(Val val, SootMethod m) {
		if(val.isStatic())
			return false;
		if(!m.hasActiveBody()) {
			throw new RuntimeException("Soot Method has no active body");
		}
		if(m.isStatic())
			return false;
		Local parameterLocals = m.getActiveBody().getThisLocal();
		return parameterLocals.equals(val.value());
	}

	
}
