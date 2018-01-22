/*******************************************************************************
 * Copyright (c) 2018 Fraunhofer IEM, Paderborn, Germany.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Johannes Spaeth - initial API and implementation
 *******************************************************************************/
package boomerang;

import com.google.common.base.Optional;

import boomerang.jimple.AllocVal;
import boomerang.jimple.Val;
import soot.RefType;
import soot.SootMethod;
import soot.Unit;
import soot.Value;
import soot.jimple.*;
import soot.jimple.toolkits.ide.icfg.BiDiInterproceduralCFG;

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
	public Optional<AllocVal> getAllocationVal(SootMethod m, Stmt stmt, Val fact, BiDiInterproceduralCFG<Unit, SootMethod> icfg) {
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
			for(SootMethod callee : icfg.getCalleesOfCallAt(as)){
				for(Unit u : icfg.getEndPointsOf(callee)){
					if(u instanceof ReturnStmt && isAllocationVal(((ReturnStmt) u).getOp())){
						return Optional.of(new AllocVal(as.getLeftOp(), m,((ReturnStmt) u).getOp()));
					}
				}
			}
		}
		return super.getAllocationVal(m, stmt, fact, icfg);
	}

	@Override
	public boolean trackStrings() {
		return true;
	}
}
