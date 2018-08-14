package boomerang;

import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
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

	private static long getGcCount() {
		long sum = 0;
		for (GarbageCollectorMXBean b : ManagementFactory.getGarbageCollectorMXBeans()) {
			long count = b.getCollectionCount();
			if (count != -1) {
				sum += count;
			}
		}
		return sum;
	}

	public static long getReallyUsedMemory() {
		long before = getGcCount();
		System.gc();
		while (getGcCount() == before)
			;
		return getCurrentlyUsedMemory();
	}

	private static long getCurrentlyUsedMemory() {
		return ManagementFactory.getMemoryMXBean().getHeapMemoryUsage().getUsed();
			//	+ ManagementFactory.getMemoryMXBean().getNonHeapMemoryUsage().getUsed();
	}
	
}
