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
package boomerang.preanalysis;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.beust.jcommander.internal.Sets;

import soot.Body;
import soot.BodyTransformer;
import soot.Local;
import soot.MethodOrMethodContext;
import soot.Scene;
import soot.SootMethod;
import soot.Unit;
import soot.Value;
import soot.ValueBox;
import soot.jimple.AssignStmt;
import soot.jimple.Constant;
import soot.jimple.InstanceFieldRef;
import soot.jimple.ReturnStmt;
import soot.jimple.StaticFieldRef;
import soot.jimple.Stmt;
import soot.jimple.internal.JAssignStmt;
import soot.jimple.internal.JNopStmt;
import soot.jimple.internal.JReturnStmt;
import soot.jimple.internal.JimpleLocal;
import soot.jimple.toolkits.callgraph.ReachableMethods;
import soot.util.queue.QueueReader;

public class BoomerangPretransformer extends BodyTransformer {

	private static BoomerangPretransformer instance;
	private int replaceCounter;
	private boolean applied;

	@Override
	protected void internalTransform(Body b, String phaseName, Map<String, String> options) {
		addNopStmtToMethods(b);
		transformConstantAtFieldWrites(b);
	}

	private void transformConstantAtFieldWrites(Body body) {
		Set<Unit> cwnc = getStmtsWithConstants(body);
		for (Unit u : cwnc) {
			if (u instanceof AssignStmt) {
				AssignStmt assignStmt = (AssignStmt) u;
				if (isFieldRef(assignStmt.getLeftOp()) && assignStmt.getRightOp() instanceof Constant) {
					String label = "varReplacer" + new Integer(replaceCounter++).toString();
					Local paramVal = new JimpleLocal(label, assignStmt.getRightOp().getType());
					AssignStmt newUnit = new JAssignStmt(paramVal, assignStmt.getRightOp());
					body.getLocals().add(paramVal);
					body.getUnits().insertBefore(newUnit, u);
					AssignStmt other = new JAssignStmt(assignStmt.getLeftOp(), paramVal);
					body.getUnits().insertBefore(other, u);
					body.getUnits().remove(u);
				}
			}
			if (u instanceof Stmt && ((Stmt) u).containsInvokeExpr()) {
				Stmt stmt = (Stmt) u;
				List<ValueBox> useBoxes = stmt.getInvokeExpr().getUseBoxes();
				for (Value v : stmt.getInvokeExpr().getArgs()) {
					if (v instanceof Constant) {
						String label = "varReplacer" + new Integer(replaceCounter++).toString();
						Local paramVal = new JimpleLocal(label, v.getType());
						AssignStmt newUnit = new JAssignStmt(paramVal, v);
						body.getLocals().add(paramVal);
						body.getUnits().insertBefore(newUnit, u);
						for (ValueBox b : useBoxes) {
							if (b.getValue().equals(v)) {
								b.setValue(paramVal);
							}
						}
					}
				}
			}
			if (u instanceof ReturnStmt) {
				ReturnStmt returnStmt = (ReturnStmt) u;
				String label = "varReplacer" + new Integer(replaceCounter++).toString();
				Local paramVal = new JimpleLocal(label, returnStmt.getOp().getType());
				AssignStmt newUnit = new JAssignStmt(paramVal, returnStmt.getOp());
				body.getLocals().add(paramVal);
				body.getUnits().insertBefore(newUnit, u);
				JReturnStmt other = new JReturnStmt(paramVal);
				body.getUnits().insertBefore(other, u);
				body.getUnits().remove(u);
			}
		}
	}

	private void addNopStmtToMethods(Body b) {
		b.getUnits().addFirst(new JNopStmt());
	}

	private Set<Unit> getStmtsWithConstants(Body methodBody) {
		Set<Unit> retMap = Sets.newHashSet();
		for (Unit u : methodBody.getUnits()) {
			if (u instanceof AssignStmt) {
				AssignStmt assignStmt = (AssignStmt) u;
				if (isFieldRef(assignStmt.getLeftOp()) && assignStmt.getRightOp() instanceof Constant) {
					retMap.add(u);
				}
			}
			if (u instanceof Stmt && ((Stmt) u).containsInvokeExpr()) {
				Stmt stmt = (Stmt) u;
				for (Value v : stmt.getInvokeExpr().getArgs()) {
					if (v instanceof Constant) {
						retMap.add(u);
					}
				}
			}
			if (u instanceof ReturnStmt) {
				ReturnStmt assignStmt = (ReturnStmt) u;
				if (assignStmt.getOp() instanceof Constant) {
					retMap.add(u);
				}
			}
		}
		return retMap;
	}

	private boolean isFieldRef(Value op) {
		return op instanceof InstanceFieldRef || op instanceof StaticFieldRef;
	}

	public void apply() {	
		if(applied)
			return;
		ReachableMethods reachableMethods = Scene.v().getReachableMethods();
		QueueReader<MethodOrMethodContext> listener = reachableMethods.listener();
		while (listener.hasNext()) {
			SootMethod method = listener.next().method();
			if(method.hasActiveBody()) {
				internalTransform(method.getActiveBody(),"",new HashMap<>());
			}
		}
		applied = true;
	}

	public boolean isApplied() {
		return applied;
	}
	
	public static BoomerangPretransformer v() {
		if(instance == null) {
			instance = new BoomerangPretransformer();
		}
		return instance;
	}
}
