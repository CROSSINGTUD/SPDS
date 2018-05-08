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

import boomerang.callgraph.CallListener;
import boomerang.callgraph.ObservableICFG;
import boomerang.jimple.AllocVal;
import boomerang.jimple.Val;
import soot.SootMethod;
import soot.Unit;
import soot.Value;
import soot.jimple.AssignStmt;
import soot.jimple.IntConstant;
import soot.jimple.LengthExpr;
import soot.jimple.NewArrayExpr;
import soot.jimple.NewMultiArrayExpr;
import soot.jimple.ReturnStmt;
import soot.jimple.Stmt;

import java.util.concurrent.atomic.AtomicReference;

public class IntAndStringBoomerangOptions extends DefaultBoomerangOptions {

	public boolean isAllocationVal(Value val) {
		if(val instanceof IntConstant){
			return true;
		}
		return super.isAllocationVal(val);
	}

	@Override
	protected boolean isArrayAllocationVal(Value val) {
		return (val instanceof NewArrayExpr || val instanceof NewMultiArrayExpr);
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
		if(as.getRightOp() instanceof LengthExpr){
			return Optional.of(new AllocVal(as.getLeftOp(), m,as.getRightOp()));
		}
		if(as.containsInvokeExpr()){
			AtomicReference<AllocVal> returnValue = new AtomicReference<>();
			icfg.addCallListener((CallListener<Unit,SootMethod>) (unit, sootMethod) -> {
				if (unit.equals(as)){
					for(Unit u : icfg.getEndPointsOf(sootMethod)){
						if(u instanceof ReturnStmt && isAllocationVal(((ReturnStmt) u).getOp())){
							returnValue.set(new AllocVal(as.getLeftOp(), m, ((ReturnStmt) u).getOp()));
						}
					}
				}
			});
			if (returnValue.get() != null){
				return Optional.of(returnValue.get());
			}
		}
		return super.getAllocationVal(m, stmt, fact, icfg);
	}

	@Override
	public boolean trackStrings() {
		return true;
	}
}
