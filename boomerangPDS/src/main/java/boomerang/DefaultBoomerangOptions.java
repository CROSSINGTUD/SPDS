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

import com.google.common.base.Optional;

import boomerang.jimple.AllocVal;
import boomerang.jimple.Statement;
import boomerang.jimple.Val;
import boomerang.stats.IBoomerangStats;
import boomerang.stats.SimpleBoomerangStats;
import soot.RefType;
import soot.Scene;
import soot.SootMethod;
import soot.Type;
import soot.Unit;
import soot.Value;
import soot.jimple.AssignStmt;
import soot.jimple.NewArrayExpr;
import soot.jimple.NewExpr;
import soot.jimple.NewMultiArrayExpr;
import soot.jimple.NullConstant;
import soot.jimple.ReturnStmt;
import soot.jimple.Stmt;
import soot.jimple.StringConstant;
import soot.jimple.toolkits.ide.icfg.BiDiInterproceduralCFG;

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

	protected boolean isThrowableAllocationType(Type type) {
		return Scene.v().getOrMakeFastHierarchy().canStoreType(type, Scene.v().getType("java.lang.Throwable"));
	}

	protected boolean isStringAllocationType(Type type) {
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
	public boolean typeCheck() {
		return true;
	}

	@Override
	public boolean trackReturnOfInstanceOf() {
		return false;
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
	public Optional<AllocVal> getAllocationVal(SootMethod m, Stmt stmt, Val fact, BiDiInterproceduralCFG<Unit, SootMethod> icfg) {
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
			for(SootMethod callee : icfg.getCalleesOfCallAt(as)){
				for(Unit u : icfg.getEndPointsOf(callee)){
					if(u instanceof ReturnStmt && isAllocationVal(((ReturnStmt) u).getOp())){
						return Optional.of(new AllocVal(as.getLeftOp(), m,((ReturnStmt) u).getOp(),new Statement((Stmt) u,m)));
					}
				}
			}
		}
		return Optional.absent();
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
