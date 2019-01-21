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
package typestate.finiteautomata;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import com.google.common.base.Joiner;
import com.google.common.collect.Sets;

import boomerang.WeightedForwardQuery;
import boomerang.jimple.AllocVal;
import boomerang.jimple.Statement;
import boomerang.jimple.Val;
import soot.Local;
import soot.Scene;
import soot.SootClass;
import soot.SootMethod;
import soot.Unit;
import soot.Value;
import soot.jimple.AssignStmt;
import soot.jimple.InstanceInvokeExpr;
import soot.jimple.InvokeExpr;
import soot.jimple.NewExpr;
import soot.jimple.Stmt;
import sync.pds.solver.WeightFunctions;
import sync.pds.solver.nodes.Node;
import typestate.TransitionFunction;
import typestate.finiteautomata.MatcherTransition.Parameter;
import typestate.finiteautomata.MatcherTransition.Type;

public abstract class TypeStateMachineWeightFunctions implements  WeightFunctions<Statement, Val, Statement, TransitionFunction> {
	public Set<MatcherTransition> transition = new HashSet<>();

	public void addTransition(MatcherTransition trans) {
		transition.add(trans);
	}
	
	@Override
	public TransitionFunction getOne() {
		return TransitionFunction.one();
	}

	@Override
	public TransitionFunction getZero() {
		return TransitionFunction.zero();
	}
	
	public TransitionFunction pop(Node<Statement,Val> curr, Statement returnSite) {
		return getMatchingTransitions(curr.stmt(), curr.fact(), Type.OnReturn, returnSite);
	}

	public TransitionFunction push(Node<Statement,Val> curr, Node<Statement,Val> succ, Statement push) {
		return getMatchingTransitions(succ.stmt(),succ.fact(), Type.OnCall, curr.stmt());
	}
	
	@Override
	public TransitionFunction normal(Node<Statement, Val> curr, Node<Statement, Val> succ) {
		if(succ.stmt().getUnit().isPresent()){
			if(succ.stmt().getUnit().get().containsInvokeExpr()){
				return callToReturn(curr,succ, succ.stmt().getUnit().get().getInvokeExpr());
			}
		}
		return getOne();
	}
	

	public TransitionFunction callToReturn(Node<Statement, Val> curr, Node<Statement, Val> succ, InvokeExpr invokeExpr) {
		Set<Transition> res = Sets.newHashSet();
		if(invokeExpr instanceof InstanceInvokeExpr){
			SootMethod method = invokeExpr.getMethod();
			InstanceInvokeExpr e = (InstanceInvokeExpr) invokeExpr;
			if(e.getBase().equals(succ.fact().value())){
				for (MatcherTransition trans : transition) {
					if(trans.matches(method) && trans.getType().equals(Type.OnCallToReturn)){
						res.add(trans);
					}
				}	
			}
		}
		return (res.isEmpty() ? getOne() : new TransitionFunction(res,Collections.singleton(succ.stmt())));
	}

	private TransitionFunction getMatchingTransitions(Statement statement, Val node, Type type, Statement transitionStmt) {
		Set<ITransition> res = new HashSet<>();
//		if (node.getFieldCount() == 0) { //TODO How do we check this?
			for (MatcherTransition trans : transition) {
				if (trans.matches(statement.getMethod()) && trans.getType().equals(type)) {
					Parameter param = trans.getParam();
					if (param.equals(Parameter.This) && isThisValue(statement.getMethod(), node))
						res.add(new Transition(trans.from(), trans.to()));
					if (param.equals(Parameter.Param1)
							&& statement.getMethod().getActiveBody().getParameterLocal(0).equals(node.value()))
						res.add(new Transition(trans.from(), trans.to()));
					if (param.equals(Parameter.Param2)
							&& statement.getMethod().getActiveBody().getParameterLocal(1).equals(node.value()))
						res.add(new Transition(trans.from(), trans.to()));
				}
//			}
		}
			
		if(res.isEmpty())
			return getOne();
		return new TransitionFunction(res,Collections.singleton(transitionStmt));
	}

	private boolean isThisValue(SootMethod method, Val node) {
		if(method.isStatic())
			return false;
		if(!method.hasActiveBody())
			return false;
		return method.getActiveBody().getThisLocal().equals(node.value());
	}

	protected Set<SootMethod> selectMethodByName(Collection<SootClass> classes, String pattern) {
		Set<SootMethod> res = new HashSet<>();
		for (SootClass c : classes) {
			for (SootMethod m : c.getMethods()) {
				if (Pattern.matches(pattern, m.getName()))
					res.add(m);
			}
		}
		return res;
	}

	protected List<SootClass> getSubclassesOf(String className) {
		SootClass sootClass = Scene.v().getSootClass(className);
		List<SootClass> list = Scene.v().getActiveHierarchy().getSubclassesOfIncluding(sootClass);
		List<SootClass> res = new LinkedList<>();
		for (SootClass c : list) {
			res.add(c);
		}
		return res;
	}

	protected Collection<Val> generateAtConstructor(SootMethod m, Unit unit,
			Collection<SootMethod> calledMethod, MatcherTransition initialTrans) {
		boolean matches = false;
		for (SootMethod method : calledMethod) {
			if (initialTrans.matches(method)) {
				matches = true;
			}
		}
		if (!matches)
			return Collections.emptySet();
		if (unit instanceof Stmt) {
			Stmt stmt = (Stmt) unit;
			if (stmt.containsInvokeExpr())
				if (stmt.getInvokeExpr() instanceof InstanceInvokeExpr) {
					InstanceInvokeExpr iie = (InstanceInvokeExpr) stmt.getInvokeExpr();
					if (iie.getBase() instanceof Local) {
						Local l = (Local) iie.getBase();
						Set<Val> out = new HashSet<>();
						out.add(new Val(l, m));
						return out;
					}
				}
		}
		return Collections.emptySet();
	}

	protected Collection<WeightedForwardQuery<TransitionFunction>> getLeftSideOf(SootMethod m, Unit unit) {
		if (unit instanceof AssignStmt) {
			AssignStmt stmt = (AssignStmt) unit;
			return Collections.singleton(new WeightedForwardQuery<>(new Statement((Stmt) unit,m),new AllocVal(stmt.getLeftOp(), m, stmt.getRightOp(),new Statement((Stmt) unit,m)),initialTransition()));
		}
		return Collections.emptySet();
	}
	
	protected Collection<WeightedForwardQuery<TransitionFunction>> generateThisAtAnyCallSitesOf(SootMethod m, Unit unit,
			Collection<SootMethod> calledMethod, Set<SootMethod> hasToCall) {
		for (SootMethod callee : calledMethod) {
			if (hasToCall.contains(callee)) {
				if (unit instanceof Stmt) {
					if (((Stmt) unit).getInvokeExpr() instanceof InstanceInvokeExpr) {
						InstanceInvokeExpr iie = (InstanceInvokeExpr) ((Stmt) unit).getInvokeExpr();
						Local thisLocal = (Local) iie.getBase();
						return Collections.singleton(new WeightedForwardQuery<>(new Statement((Stmt) unit,m),new AllocVal(thisLocal,m,iie,new Statement((Stmt) unit,m)),initialTransition()));
					}
				}

			}
		}
		return Collections.emptySet();
	}
	

	protected Collection<WeightedForwardQuery<TransitionFunction>> generateAtAllocationSiteOf(SootMethod m, Unit unit, Class allocationSuperType) {
		if(unit instanceof AssignStmt){
			AssignStmt assignStmt = (AssignStmt) unit;
			if(assignStmt.getRightOp() instanceof NewExpr){
				NewExpr newExpr = (NewExpr) assignStmt.getRightOp();
				Value leftOp = assignStmt.getLeftOp();
				soot.Type type = newExpr.getType();
				if(Scene.v().getOrMakeFastHierarchy().canStoreType(type, Scene.v().getType(allocationSuperType.getName()))){
					return Collections.singleton(new WeightedForwardQuery<>(new Statement((Stmt) unit,m),new AllocVal(leftOp,m,assignStmt.getRightOp(),new Statement((Stmt) unit,m)),initialTransition()));
				}
			}
		}
		return Collections.emptySet();
	}
	
	@Override
	public String toString() {
		return Joiner.on("\n").join(transition);
	}

	public abstract Collection<WeightedForwardQuery<TransitionFunction>> generateSeed(SootMethod method, Unit stmt, Collection<SootMethod> calledMethod);

	public TransitionFunction initialTransition(){
		return new TransitionFunction(new Transition(initialState(),initialState()),Collections.emptySet());
	}

	protected abstract State initialState();
}
	
