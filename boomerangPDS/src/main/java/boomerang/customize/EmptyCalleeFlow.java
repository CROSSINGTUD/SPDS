package boomerang.customize;

import java.util.Collection;
import java.util.Collections;

import boomerang.jimple.Val;
import soot.Scene;
import soot.SootClass;
import soot.SootMethod;
import soot.jimple.Stmt;
import wpds.interfaces.State;

public abstract class EmptyCalleeFlow {

	protected SootMethod systemArrayCopyMethod;
	protected boolean fetchedSystemArrayCopyMethod;
	
	protected boolean isSystemArrayCopy(SootMethod method) {
		fetchSystemArrayClasses();
		return systemArrayCopyMethod != null && systemArrayCopyMethod.equals(method);
	}

	protected void fetchSystemArrayClasses() {
		if(fetchedSystemArrayCopyMethod)
			return;
		fetchedSystemArrayCopyMethod = true;
		if(Scene.v().containsClass("java.lang.System")){
			SootClass systemClass = Scene.v().getSootClass("java.lang.System");
			for(SootMethod m : systemClass.getMethods()){
				if(m.getName().contains("arraycopy")){
					systemArrayCopyMethod = m;
				}
			}
		}
	}
	

	public Collection<? extends State> getEmptyCalleeFlow(SootMethod caller, Stmt callSite, Val value,
			Stmt returnSite) {
		if(isSystemArrayCopy(callSite.getInvokeExpr().getMethod())){
			return systemArrayCopyFlow(caller, callSite, value, returnSite);
		}
		return Collections.emptySet();
	}

	protected abstract Collection<? extends State> systemArrayCopyFlow(SootMethod caller, Stmt callSite, Val value,
			Stmt returnSite);
}
