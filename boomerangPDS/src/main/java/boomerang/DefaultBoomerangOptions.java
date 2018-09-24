/*******************************************************************************
 * Copyright (c) 2018 Fraunhofer IEM, Paderborn, Germany.
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *  
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Johannes Spaeth - initial API and implementation
 *******************************************************************************/
package boomerang;

import boomerang.callgraph.CalleeListener;
import boomerang.callgraph.ObservableICFG;
import boomerang.jimple.AllocVal;
import boomerang.jimple.Statement;
import boomerang.jimple.Val;
import boomerang.stats.IBoomerangStats;
import boomerang.stats.SimpleBoomerangStats;
import com.google.common.base.Optional;
import soot.*;
import soot.jimple.*;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

public class DefaultBoomerangOptions implements BoomerangOptions {
	
	public boolean isAllocationVal(Value val) {
		if (!trackStrings() && isStringAllocationType(val.getType())) {
			return false;
		}
		if(trackNullAssignments() && val instanceof NullConstant){
			return true;
		}
		if(arrayFlows() && isArrayAllocationVal(val)){
			return true;
		}
		if(trackStrings() && val instanceof StringConstant){
			return true;
		}	
		if (!trackAnySubclassOfThrowable() && isThrowableAllocationType(val.getType())) {
			return false;
		}
		
		
		return val instanceof NewExpr;
	}

	private boolean isThrowableAllocationType(Type type) {
		return Scene.v().getOrMakeFastHierarchy().canStoreType(type, Scene.v().getType("java.lang.Throwable"));
	}

	private boolean isStringAllocationType(Type type) {
		return type.toString().equals("java.lang.String") || type.toString().equals("java.lang.StringBuilder")
				|| type.toString().equals("java.lang.StringBuffer");
	}

	protected boolean isArrayAllocationVal(Value val) {
		if(val instanceof NewArrayExpr){
			NewArrayExpr expr = (NewArrayExpr) val;
			return expr.getBaseType() instanceof RefType;
		} else if(val instanceof NewMultiArrayExpr){
			return true;
		}
		return false;
	}
	
	@Override
	public boolean staticFlows() {
		return true;
	}

	@Override
	public boolean arrayFlows() {
		return true;
	}

	@Override
	public boolean fastForwardFlows() {
		return false;
	}

	@Override
	public boolean typeCheck() {
		return true;
	}

	@Override
	public boolean onTheFlyCallGraph() {
		return true;
	}

	@Override
	public boolean throwFlows() {
		return false;
	}

	@Override
	public boolean callSummaries() {
		return false;
	}

	@Override
	public boolean fieldSummaries() {
		return false;
	}

	public boolean trackAnySubclassOfThrowable(){
		return false;
	}

	public boolean trackStrings(){
		return false;
	}
	
	
	public boolean trackNullAssignments(){
		return false;
	}
	
	@Override
	public boolean isIgnoredMethod(SootMethod method) {
		return trackAnySubclassOfThrowable() && Scene.v().getFastHierarchy().canStoreType(method.getDeclaringClass().getType(), Scene.v().getType("java.lang.Throwable"));
	}
	@Override
	public Optional<AllocVal> getAllocationVal(SootMethod m, Stmt stmt, Val fact, ObservableICFG<Unit, SootMethod> icfg) {
		if (!(stmt instanceof AssignStmt)) {
			return Optional.absent();
		}
		AssignStmt as = (AssignStmt) stmt;
		if (!as.getLeftOp().equals(fact.value())) {
			return Optional.absent();
		}
		if(isAllocationVal(as.getRightOp())) {
			return Optional.of(new AllocVal(as.getLeftOp(), m,as.getRightOp(),new Statement(stmt,m)));
		}
		if(as.containsInvokeExpr()){
            AtomicReference<AllocVal> returnValue = new AtomicReference<>();
			icfg.addCalleeListener(new AllocationValCalleeListener(returnValue, as, icfg, m));
            if (returnValue.get() != null){
                return Optional.of(returnValue.get());
            }
		}
		return Optional.absent();
	}

	protected class AllocationValCalleeListener implements CalleeListener<Unit,SootMethod>{
		AtomicReference<AllocVal> returnValue;
		AssignStmt as;
		ObservableICFG<Unit, SootMethod> icfg;
		SootMethod m;

		AllocationValCalleeListener(AtomicReference<AllocVal> returnValue, AssignStmt as,
									ObservableICFG<Unit, SootMethod> icfg, SootMethod m){
			this.returnValue = returnValue;
			this.as = as;
			this.icfg = icfg;
			this.m = m;
		}
		@Override
		public Unit getObservedCaller() {
			return as;
		}

		@Override
		public void onCalleeAdded(Unit unit, SootMethod sootMethod) {
			for(Unit u : icfg.getEndPointsOf(sootMethod)){
				if(u instanceof ReturnStmt && isAllocationVal(((ReturnStmt) u).getOp())){
					returnValue.set(new AllocVal(as.getLeftOp(), m, ((ReturnStmt) u).getOp(), new Statement((Stmt) u,m)));
				}
			}
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || getClass() != o.getClass()) return false;
			AllocationValCalleeListener that = (AllocationValCalleeListener) o;
			return Objects.equals(returnValue, that.returnValue) &&
					Objects.equals(as, that.as) &&
					Objects.equals(m, that.m);
		}

		@Override
		public int hashCode() {

			return Objects.hash(returnValue, as, m);
		}
	}

	@Override
	public int analysisTimeoutMS() {
		return 60000;
	}

	@Override
	public IBoomerangStats statsFactory(){
		return new SimpleBoomerangStats();
	}

	@Override
	public boolean aliasing() {
		return true;
	}

	@Override
	public boolean killNullAtCast() {
		return false;
	}
}
