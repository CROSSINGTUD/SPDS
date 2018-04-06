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
package boomerang.jimple;

import java.util.HashMap;
import java.util.Map;

import com.google.common.base.Joiner;
import com.google.common.base.Optional;

import soot.SootMethod;
import soot.Value;
import soot.jimple.AssignStmt;
import soot.jimple.IdentityStmt;
import soot.jimple.InstanceFieldRef;
import soot.jimple.InstanceInvokeExpr;
import soot.jimple.NewExpr;
import soot.jimple.Stmt;
import wpds.interfaces.Empty;
import wpds.interfaces.Location;

public class Statement implements Location {
	//Wrapper for stmt so we know the method
	private static Statement epsilon;
	private final Stmt delegate;
	private final SootMethod method;
	private final String rep;

	public Statement(Stmt delegate, SootMethod m) {
		this.delegate = delegate;
		this.method = m;
		this.rep = null;
	}
	
	private Statement(String rep) {
		this.rep = rep;
		this.delegate = null;
		this.method = null;
	}

	public Optional<Stmt> getUnit() {
		if (delegate == null)
			return Optional.absent();
		return Optional.of(delegate);
	}
	
	public boolean isCallsite(){
		return delegate != null && delegate.containsInvokeExpr();
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((delegate == null) ? 0 : delegate.hashCode());
		result = prime * result + ((rep == null) ? 0 : rep.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Statement other = (Statement) obj;
		if (rep == null) {
			if (other.rep != null)
				return false;
		} else if (!rep.equals(other.rep))
			return false;
		if (delegate == null) {
			if (other.delegate != null)
				return false;
		} else if (!delegate.equals(other.delegate))
			return false;
		return true;
	}

	public static Statement epsilon() {
		if (epsilon == null) {
			epsilon = new EpsStatement();
		}
		return epsilon;
	}
	
	private static class EpsStatement extends Statement implements Empty{

		public EpsStatement() {
			super("Eps_s");
		}
		
	}

	@Override
	public String toString() {
		if (delegate == null) {
			return rep;
		}
		if(DEBUG){
			return method.getName() + " " + shortName(delegate);
		}
		return "[" + Integer.toString(methodToInt(method)) + "]" + Integer.toString(stmtToInt(delegate));
	}

	private String shortName(Stmt s) {
		if(s.containsInvokeExpr()){
			String base = "";
			if(s.getInvokeExpr() instanceof InstanceInvokeExpr){
				InstanceInvokeExpr iie = (InstanceInvokeExpr) s.getInvokeExpr();
				base = iie.getBase().toString()+".";
			}
			return base+s.getInvokeExpr().getMethod().getName() + "(" +Joiner.on(",").join(s.getInvokeExpr().getArgs())+")";
		}
		if(s instanceof IdentityStmt){
			return "id";
		}
		if(s instanceof AssignStmt){
			AssignStmt assignStmt = (AssignStmt) s;
			if(assignStmt.getLeftOp() instanceof InstanceFieldRef){
				InstanceFieldRef ifr = (InstanceFieldRef) assignStmt.getLeftOp();
				return ifr.getBase() +"."+ifr.getField().getName() +" = " + assignStmt.getRightOp();
			}
			if(assignStmt.getRightOp() instanceof InstanceFieldRef){
				InstanceFieldRef ifr = (InstanceFieldRef) assignStmt.getRightOp();
				return assignStmt.getLeftOp()  +" = " +ifr.getBase() +"."+ifr.getField().getName();
			}
			if(assignStmt.getRightOp() instanceof NewExpr){
				NewExpr newExpr = (NewExpr) assignStmt.getRightOp();
				return assignStmt.getLeftOp()  +" = new " +newExpr.getBaseType().getSootClass().getShortName();
			}
		}
		return s.toString();
	}

	private static boolean DEBUG = true;
	private static Map<SootMethod, Integer> methodToInteger = new HashMap<>();
	private static Map<Stmt, Integer> statementToInteger = new HashMap<>();

	public int stmtToInt(Stmt s) {
		if (!statementToInteger.containsKey(s)) {
			statementToInteger.put(s, statementToInteger.size());
		}
		return statementToInteger.get(s);
	}

	public int methodToInt(SootMethod method) {
		if (!methodToInteger.containsKey(method)) {
			methodToInteger.put(method, methodToInteger.size());
		}
		return methodToInteger.get(method);
	}

	public SootMethod getMethod() {
		return method;
	}
}
