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
package boomerang.solver;

import java.util.AbstractMap;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import com.google.common.base.Optional;
import com.google.common.collect.Sets;

import boomerang.BackwardQuery;
import boomerang.BoomerangOptions;
import boomerang.jimple.Field;
import boomerang.jimple.Statement;
import boomerang.jimple.StaticFieldVal;
import boomerang.jimple.Val;
import soot.Body;
import soot.Local;
import soot.SootMethod;
import soot.Unit;
import soot.Value;
import soot.jimple.ArrayRef;
import soot.jimple.AssignStmt;
import soot.jimple.CastExpr;
import soot.jimple.InstanceFieldRef;
import soot.jimple.InstanceInvokeExpr;
import soot.jimple.InvokeExpr;
import soot.jimple.ReturnStmt;
import soot.jimple.StaticFieldRef;
import soot.jimple.Stmt;
import soot.jimple.ThrowStmt;
import soot.jimple.toolkits.ide.icfg.BiDiInterproceduralCFG;
import sync.pds.solver.nodes.CallPopNode;
import sync.pds.solver.nodes.ExclusionNode;
import sync.pds.solver.nodes.GeneratedState;
import sync.pds.solver.nodes.INode;
import sync.pds.solver.nodes.Node;
import sync.pds.solver.nodes.NodeWithLocation;
import sync.pds.solver.nodes.PopNode;
import sync.pds.solver.nodes.PushNode;
import wpds.impl.NestedWeightedPAutomatons;
import wpds.impl.Weight;
import wpds.interfaces.State;

public abstract class BackwardBoomerangSolver<W extends Weight> extends AbstractBoomerangSolver<W>{

	public BackwardBoomerangSolver(BiDiInterproceduralCFG<Unit, SootMethod> icfg, BackwardQuery query, Map<Entry<INode<Node<Statement,Val>>, Field>, INode<Node<Statement,Val>>> genField, BoomerangOptions options, NestedWeightedPAutomatons<Statement, INode<Val>, W> callSummaries, NestedWeightedPAutomatons<Field, INode<Node<Statement, Val>>, W> fieldSummaries){
		super(icfg, query, genField, options, callSummaries, fieldSummaries);
	}

	@Override
	protected boolean killFlow(SootMethod m, Stmt curr, Val value) {	
		if(value.isStatic())
			return false;
		if (!m.getActiveBody().getLocals().contains(value.value()))
			return true;
		return false;
	}

	public INode<Node<Statement,Val>> generateFieldState(final INode<Node<Statement, Val>> d, final Field loc) {
		Entry<INode<Node<Statement,Val>>, Field> e = new AbstractMap.SimpleEntry<>(d, loc);
		if (!generatedFieldState.containsKey(e)) {
			generatedFieldState.put(e, new GeneratedState<Node<Statement,Val>,Field>(fieldAutomaton.getInitialState(),loc));
		}
		return generatedFieldState.get(e);
	}
	
	@Override
	protected Collection<? extends State> computeReturnFlow(SootMethod method, Stmt curr, Val value, Stmt callSite, Stmt returnSite) {
		Statement returnSiteStatement = new Statement(returnSite,icfg.getMethodOf(returnSite));
		Set<State> out = Sets.newHashSet();
		if (!method.isStatic()) {
			if (method.getActiveBody().getThisLocal().equals(value.value())) {
				if(callSite.containsInvokeExpr()){
					if(callSite.getInvokeExpr() instanceof InstanceInvokeExpr){
						InstanceInvokeExpr iie = (InstanceInvokeExpr) callSite.getInvokeExpr();
						out.add(new CallPopNode<Val,Statement>(new Val(iie.getBase(),icfg.getMethodOf(callSite)), PDSSystem.CALLS,returnSiteStatement));
					}
				}
			}
		}
		int index = 0;
		for (Local param : method.getActiveBody().getParameterLocals()) {
			if (param.equals(value.value())) {
				if(callSite.containsInvokeExpr()){
					InvokeExpr ie = callSite.getInvokeExpr();
					out.add(new CallPopNode<Val,Statement>(new Val(ie.getArg(index),icfg.getMethodOf(callSite)), PDSSystem.CALLS,returnSiteStatement));
				}
			}
			index++;
		}
		if(value.isStatic()){
			out.add(new CallPopNode<Val,Statement>(new StaticFieldVal(value.value(),((StaticFieldVal) value).field(), icfg.getMethodOf(callSite)), PDSSystem.CALLS,returnSiteStatement));
		}
		return out;
	}

	protected Collection<State> callFlow(SootMethod caller, Stmt callSite, InvokeExpr invokeExpr, Val value) {
		assert icfg.isCallStmt(callSite);
		Set<State> out = Sets.newHashSet();
		boolean onlyStaticInitializer = false;
		for (SootMethod callee : icfg.getCalleesOfCallAt(callSite)) {
			for (Unit calleeSp : icfg.getStartPointsOf(callee)) {
				for (Unit returnSite : icfg.getSuccsOf(callSite)) {
					Collection<? extends State> res = computeCallFlow(caller, new Statement((Stmt) returnSite, caller),
							new Statement((Stmt) callSite, caller), invokeExpr, value, callee, (Stmt) calleeSp);
					onCallFlow(callee, callSite, value, res);
					out.addAll(res);
				}
			}
			addReachable(callee);
			onlyStaticInitializer |= !callee.isStaticInitializer();
		}
		for (Unit returnSite : icfg.getSuccsOf(callSite)) {
			if (icfg.getCalleesOfCallAt(callSite).isEmpty() || (onlyStaticInitializer && value.isStatic())) {
				out.addAll(computeNormalFlow(caller, (Stmt) callSite, value, (Stmt) returnSite));
			}
			out.addAll(getEmptyCalleeFlow(caller, (Stmt) callSite, value, (Stmt) returnSite));
		}
		return out;
	}	
	

	@Override
	public Collection<? extends State> computeSuccessor(Node<Statement, Val> node) {
		Statement stmt = node.stmt();
		Optional<Stmt> unit = stmt.getUnit();
		logger.trace("Computing successor for {} with solver {}", node, this);
		if (unit.isPresent()) {
			Stmt curr = unit.get();
			Val value = node.fact();
			SootMethod method = icfg.getMethodOf(curr);
			if(method == null)
				return Collections.emptySet();
			if (killFlow(method, curr, value)) {
				return Collections.emptySet();
			}
			if(options.isIgnoredMethod(method)){
				return Collections.emptySet();
			}
			if (curr.containsInvokeExpr() && valueUsedInStatement(curr, value) && INTERPROCEDURAL) {
				return callFlow(method, curr, curr.getInvokeExpr(), value);
			} else if (icfg.isExitStmt(curr)) {
				return returnFlow(method, curr, value);
			} else {
				return normalFlow(method, curr, value);
			}
		}
		return Collections.emptySet();
	}
	protected Collection<State> normalFlow(SootMethod method, Stmt curr, Val value) {
		Set<State> out = Sets.newHashSet();
		for (Unit succ : icfg.getSuccsOf(curr)) {
			Collection<State> flow = computeNormalFlow(method, curr, value, (Stmt) succ);
			out.addAll(flow);
		}
		return out;
	}

	protected Collection<? extends State> computeCallFlow(SootMethod caller, Statement returnSite, Statement callSite,
			InvokeExpr invokeExpr, Val fact, SootMethod callee, Stmt calleeSp) {
		if (!callee.hasActiveBody())
			return Collections.emptySet();
		if(calleeSp instanceof ThrowStmt){
			return Collections.emptySet();
		}
		Body calleeBody = callee.getActiveBody();
		Set<State> out = Sets.newHashSet();
		if (invokeExpr instanceof InstanceInvokeExpr) {
			InstanceInvokeExpr iie = (InstanceInvokeExpr) invokeExpr;
			if (iie.getBase().equals(fact.value()) && !callee.isStatic()) {
				out.add(new PushNode<Statement, Val, Statement>(new Statement(calleeSp, callee),
						new Val(calleeBody.getThisLocal(),callee), returnSite, PDSSystem.CALLS));
			}
		}
		List<Local> parameterLocals = calleeBody.getParameterLocals();
		int i = 0;
		for (Value arg : invokeExpr.getArgs()) {
			if (arg.equals(fact.value()) && parameterLocals.size() > i) {
				Local param = parameterLocals.get(i);
				out.add(new PushNode<Statement, Val, Statement>(new Statement(calleeSp, callee),
						new Val(param,callee), returnSite, PDSSystem.CALLS));
			}
			i++;
		}
		
		if(callSite.getUnit().get() instanceof AssignStmt && calleeSp instanceof ReturnStmt){
			AssignStmt as = (AssignStmt) callSite.getUnit().get();
			ReturnStmt retStmt = (ReturnStmt) calleeSp;
			if(as.getLeftOp().equals(fact.value())){
				out.add(new PushNode<Statement, Val, Statement>(new Statement(calleeSp, callee),
						new Val(retStmt.getOp(),callee), returnSite, PDSSystem.CALLS));
			}
		}
		if(fact.isStatic()){
			out.add(new PushNode<Statement, Val, Statement>(new Statement(calleeSp, callee),
					new StaticFieldVal(fact.value(),((StaticFieldVal) fact).field(), callee), returnSite, PDSSystem.CALLS));
		}
		return out;
	}

	@Override
	protected Collection<State> computeNormalFlow(SootMethod method, Stmt curr, Val fact, Stmt succ) {
//		assert !fact.equals(thisVal()) && !fact.equals(returnVal()) && !fact.equals(param(0));
		if(options.isAllocationVal(fact.value())){
			return Collections.emptySet();
		}
		Set<State> out = Sets.newHashSet();

//		if (!isFieldWriteWithBase(curr, fact)) {
//			// always maintain data-flow if not a field write // killFlow has
//			// been taken care of
//			out.add(new Node<Statement, Value>(new Statement(succ, method), fact));
//		} else {
//			out.add(new ExclusionNode<Statement, Value, Field>(new Statement((Stmt) succ, method), fact,
//					getWrittenField(curr)));
//		}
		boolean leftSideMatches = false;
		if (curr instanceof AssignStmt) {
			AssignStmt assignStmt = (AssignStmt) curr;
			Value leftOp = assignStmt.getLeftOp();
			Value rightOp = assignStmt.getRightOp();
			if (leftOp.equals(fact.value())) {
				leftSideMatches = true;
				if (rightOp instanceof InstanceFieldRef) {
					InstanceFieldRef ifr = (InstanceFieldRef) rightOp;
					out.add(new PushNode<Statement, Val, Field>(new Statement(succ, method), new Val(ifr.getBase(),method),
							new Field(ifr.getField()), PDSSystem.FIELDS));
				} else if(rightOp instanceof StaticFieldRef){
					if(options.staticFlows()){
						StaticFieldRef sfr = (StaticFieldRef) rightOp;
						out.add(new Node<Statement, Val>(new Statement(succ, method), new StaticFieldVal(leftOp,sfr.getField(),method)));
					}
				} else if(rightOp instanceof ArrayRef){
					ArrayRef ifr = (ArrayRef) rightOp;
					if(options.arrayFlows()){
						out.add(new PushNode<Statement, Val, Field>(new Statement(succ, method), new Val(ifr.getBase(),method),
								Field.array(), PDSSystem.FIELDS));
					}
//					leftSideMatches = false;
				} else if(rightOp instanceof CastExpr){
					CastExpr castExpr = (CastExpr) rightOp;
					out.add(new Node<Statement, Val>(new Statement(succ, method), new Val(castExpr.getOp(),method)));
				} else {	
					if(isFieldLoadWithBase(curr, fact)){
						out.add(new ExclusionNode<Statement, Val, Field>(new Statement(succ, method), fact,
							getLoadedField(curr)));
					} else{
						out.add(new Node<Statement, Val>(new Statement(succ, method), new Val(rightOp,method)));
					}
				}
			}
			if (leftOp instanceof InstanceFieldRef) {
				InstanceFieldRef ifr = (InstanceFieldRef) leftOp;
				Value base = ifr.getBase();
				if (base.equals(fact.value())) {
					NodeWithLocation<Statement, Val, Field> succNode = new NodeWithLocation<>(
							new Statement(succ, method), new Val(rightOp,method), new Field(ifr.getField()));
					out.add(new PopNode<NodeWithLocation<Statement, Val, Field>>(succNode, PDSSystem.FIELDS));
				}
			} else if(leftOp instanceof StaticFieldRef){
				StaticFieldRef sfr = (StaticFieldRef) leftOp;
				if (fact.isStatic() && fact.equals(new StaticFieldVal(leftOp,sfr.getField(),method))) {
					out.add(new Node<Statement, Val>(new Statement(succ, method), new Val(rightOp,method)));
				}
			} else if (leftOp instanceof ArrayRef) {
				ArrayRef ifr = (ArrayRef) leftOp;
				Value base = ifr.getBase();
				if (base.equals(fact.value())) {
					NodeWithLocation<Statement, Val, Field> succNode = new NodeWithLocation<>(
							new Statement(succ, method), new Val(rightOp,method), Field.array());
					out.add(new PopNode<NodeWithLocation<Statement, Val, Field>>(succNode, PDSSystem.FIELDS));
				}
			} 
		}
		if(!leftSideMatches)
			out.add(new Node<Statement, Val>(new Statement(succ, method), fact));
		return out;
	}
}
