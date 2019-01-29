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

import boomerang.BoomerangOptions;
import boomerang.ForwardQuery;
import boomerang.callgraph.CalleeListener;
import boomerang.callgraph.ObservableICFG;
import boomerang.jimple.Field;
import boomerang.jimple.Statement;
import boomerang.jimple.StaticFieldVal;
import boomerang.jimple.Val;
import boomerang.jimple.ValWithFalseVariable;
import soot.Body;
import soot.Local;
import soot.NullType;
import soot.Scene;
import soot.SootMethod;
import soot.Unit;
import soot.Value;
import soot.jimple.ArrayRef;
import soot.jimple.AssignStmt;
import soot.jimple.CastExpr;
import soot.jimple.IfStmt;
import soot.jimple.InstanceFieldRef;
import soot.jimple.InstanceInvokeExpr;
import soot.jimple.InstanceOfExpr;
import soot.jimple.IntConstant;
import soot.jimple.InvokeExpr;
import soot.jimple.NullConstant;
import soot.jimple.ReturnStmt;
import soot.jimple.StaticFieldRef;
import soot.jimple.Stmt;
import soot.jimple.ThrowStmt;
import soot.jimple.internal.JEqExpr;
import soot.jimple.internal.JNeExpr;
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

public abstract class ForwardBoomerangSolver<W extends Weight> extends AbstractBoomerangSolver<W> {
	private final class CallSiteCalleeListener implements CalleeListener<Unit, SootMethod> {
		private final SootMethod caller;
		private final Stmt callSite;
		private final Node<Statement, Val> currNode;
		private final InvokeExpr invokeExpr;

		private CallSiteCalleeListener(SootMethod caller, Stmt callSite, Node<Statement, Val> currNode,
				InvokeExpr invokeExpr) {
			this.caller = caller;
			this.callSite = callSite;
			this.currNode = currNode;
			this.invokeExpr = invokeExpr;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + getOuterType().hashCode();
			result = prime * result + ((callSite == null) ? 0 : callSite.hashCode());
			result = prime * result + ((caller == null) ? 0 : caller.hashCode());
			result = prime * result + ((currNode == null) ? 0 : currNode.hashCode());
			result = prime * result + ((invokeExpr == null) ? 0 : invokeExpr.hashCode());
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
			CallSiteCalleeListener other = (CallSiteCalleeListener) obj;
			if (!getOuterType().equals(other.getOuterType()))
				return false;
			if (callSite == null) {
				if (other.callSite != null)
					return false;
			} else if (!callSite.equals(other.callSite))
				return false;
			if (caller == null) {
				if (other.caller != null)
					return false;
			} else if (!caller.equals(other.caller))
				return false;
			if (currNode == null) {
				if (other.currNode != null)
					return false;
			} else if (!currNode.equals(other.currNode))
				return false;
			if (invokeExpr == null) {
				if (other.invokeExpr != null)
					return false;
			} else if (!invokeExpr.equals(other.invokeExpr))
				return false;
			return true;
		}

		@Override
		public void onCalleeAdded(Unit callSite, SootMethod callee) {
			if(callee.isStaticInitializer()) {
				return;
			}
//				onlyStaticInitializer = false;
			icfg.addMethodWithCallFlow(callee);
			for (Unit calleeSp : icfg.getStartPointsOf(callee)) {
				Set<State> out = Sets.newHashSet();
				Collection<? extends State> res = computeCallFlow(caller,
						new Statement((Stmt) callSite, caller), invokeExpr, currNode.fact(), callee, (Stmt) calleeSp);
				out.addAll(res);
				for(State s : out) {
					propagate(currNode, s);
				}
			}
			addReachable(callee);

//				if(Scene.v().isExcluded(callee.getDeclaringClass())) {
//					calleeExcluded = true;
//				}
		}

		@Override
		public Unit getObservedCaller() {
			return callSite;
		}

		private ForwardBoomerangSolver getOuterType() {
			return ForwardBoomerangSolver.this;
		}
	}

	public ForwardBoomerangSolver(ObservableICFG<Unit, SootMethod> icfg, ForwardQuery query, Map<Entry<INode<Node<Statement, Val>>, Field>, INode<Node<Statement, Val>>> genField, BoomerangOptions options, NestedWeightedPAutomatons<Statement, INode<Val>, W> callSummaries, NestedWeightedPAutomatons<Field, INode<Node<Statement, Val>>,W> fieldSummaries) {
		super(icfg, query, genField, options, callSummaries, fieldSummaries);
	}
	
	public Collection<? extends State> computeCallFlow(SootMethod caller, Statement callSite, InvokeExpr invokeExpr,
			Val fact, SootMethod callee, Stmt calleeSp) {
		if (!callee.hasActiveBody() || callee.isStaticInitializer()){
			return Collections.emptySet();
		}
		Body calleeBody = callee.getActiveBody();
		Set<State> out = Sets.newHashSet();
		if (invokeExpr instanceof InstanceInvokeExpr) {
			InstanceInvokeExpr iie = (InstanceInvokeExpr) invokeExpr;
			if (iie.getBase().equals(fact.value()) && !callee.isStatic()) {
				out.add(new PushNode<Statement, Val, Statement>(new Statement(calleeSp, callee),
						new Val(calleeBody.getThisLocal(),callee), callSite, PDSSystem.CALLS));
			}
		}
		int i = 0;
		List<Local> parameterLocals = calleeBody.getParameterLocals();
		for (Value arg : invokeExpr.getArgs()) {
			if (arg.equals(fact.value()) && parameterLocals.size() > i) {
				Local param = parameterLocals.get(i);
				out.add(new PushNode<Statement,  Val, Statement>(new Statement(calleeSp, callee),
						new Val(param,callee), callSite, PDSSystem.CALLS));
			}
			i++;
		}
		if(fact.isStatic()){
			out.add(new PushNode<Statement, Val, Statement>(new Statement(calleeSp, callee),
					new StaticFieldVal(fact.value(),((StaticFieldVal) fact).field(), callee), callSite, PDSSystem.CALLS));
		}
		return out;
	}
	

	public INode<Node<Statement,Val>> generateFieldState(final INode<Node<Statement, Val>> d, final Field loc) {
		Entry<INode<Node<Statement,Val>>, Field> e = new AbstractMap.SimpleEntry<>(d, loc);
		if (!generatedFieldState.containsKey(e)) {
			generatedFieldState.put(e, new GeneratedState<Node<Statement,Val>,Field>(fieldAutomaton.getInitialState(),loc));
		}
		return generatedFieldState.get(e);
	}
	

	@Override
	protected boolean killFlow(SootMethod m, Stmt curr, Val value) {
		if (!m.getActiveBody().getLocals().contains(value.value()) && !value.isStatic())
			return true;
		if (curr instanceof AssignStmt) {
			AssignStmt as = (AssignStmt) curr;
			// Kill x at any statement x = * during propagation.
			if (as.getLeftOp().equals(value.value())) {
				// But not for a statement x = x.f
				if (as.getRightOp() instanceof InstanceFieldRef) {
					InstanceFieldRef iie = (InstanceFieldRef) as.getRightOp();
					if (iie.getBase().equals(value.value())) {
						return false;
					}
				}
				return true;
			}
			if(as.getLeftOp() instanceof StaticFieldRef){
				StaticFieldRef sfr = (StaticFieldRef) as.getLeftOp();
				if(value.isStatic() && value.equals(new StaticFieldVal(as.getLeftOp(), sfr.getField(), m))){
					return true;
				}
			}
		}
		return false;
	}

	@Override
	public void computeSuccessor(Node<Statement, Val> node) {
		Statement stmt = node.stmt();
		Optional<Stmt> unit = stmt.getUnit();
		if (unit.isPresent()) {
			Stmt curr = unit.get();
			Val value = node.fact();
			SootMethod method = icfg.getMethodOf(curr);
			if(method == null)
				return;
			if (icfg.isExitStmt(curr)) {
				returnFlow(method, node);
				return;
			}
			for(Unit next : icfg.getSuccsOf(curr)){
				Stmt nextStmt = (Stmt) next; 
				if(query.getType() instanceof NullType && curr instanceof IfStmt && killAtIfStmt((IfStmt) curr, value, next)) {
					continue;
				}
				if (nextStmt.containsInvokeExpr() && valueUsedInStatement(nextStmt, value)) {
					callFlow(method, node, nextStmt, nextStmt.getInvokeExpr());
				} else if (!killFlow(method, nextStmt, value)) {
					Collection<State> out = computeNormalFlow(method, curr, value, nextStmt);
					for(State s : out) {
						propagate(node, s);
					}
				}
			}
		}
	}
	
	/**
	 * This method kills a data-flow at an if-stmt, it is assumed that the propagated "allocation" site
	 * is x = null and fact is the propagated aliased variable. (i.e., y after a statement y = x). 
	 * If the if-stmt checks for if y != null or if y == null, data-flow propagation can be killed when along the true/false
	 * branch. 
	 * @param ifStmt The if-stmt the data-flow value fact bypasses
	 * @param fact The data-flow value that bypasses the if-stmt
	 * @param succ The successor statement of the if-stmt
	 * @return true if the Val fact shall be killed
	 */
	private boolean killAtIfStmt(IfStmt ifStmt, Val fact, Unit succ) {
		Stmt target = ifStmt.getTarget();
		Value condition = ifStmt.getCondition();
		if(condition instanceof JEqExpr) {
			JEqExpr eqExpr = (JEqExpr) condition;
			Value op1 = eqExpr.getOp1();
			Value op2 = eqExpr.getOp2();
			if(fact instanceof ValWithFalseVariable) {
				ValWithFalseVariable valWithFalseVar = (ValWithFalseVariable) fact;
				if(op1.equals(valWithFalseVar.getFalseVariable())){
					if(op2.equals(IntConstant.v(0))) {
						if(!succ.equals(target)) {
							return true;
						}
					}
				}
				if(op2.equals(valWithFalseVar.getFalseVariable())){
					if(op1.equals(IntConstant.v(0))) {
						if(!succ.equals(target)) {
							return true;
						}
					}
				}
			}
			if(op1 instanceof NullConstant) {
				if(op2.equals(fact.value())) {
					if(!succ.equals(target)) {
						return true;
					}
				}
			} else if(op2 instanceof NullConstant) {
				if(op1.equals(fact.value())) {
					if(!succ.equals(target)) {
						return true;
					}
				}
			} 
		}		
		if(condition instanceof JNeExpr) {
			JNeExpr eqExpr = (JNeExpr) condition;
			Value op1 = eqExpr.getOp1();
			Value op2 = eqExpr.getOp2();
			if(op1 instanceof NullConstant) {
				if(op2.equals(fact.value())) {
					if(succ.equals(target)) {
						return true;
					}
				}
			} else if(op2 instanceof NullConstant) {
				if(op1.equals(fact.value())) {
					if(succ.equals(target)) {
						return true;
					}
				}
			}
		}
		return false;
	}

	protected Collection<State> normalFlow(SootMethod method, Stmt curr, Val value) {
		Set<State> out = Sets.newHashSet();
		for (Unit succ : icfg.getSuccsOf(curr)) {
			Collection<State> flow = computeNormalFlow(method, curr, value, (Stmt) succ);
			out.addAll(flow);
		}
		return out;
	}
	
	@Override
	public Collection<State> computeNormalFlow(SootMethod method, Stmt curr, Val fact, Stmt succ) {
		Set<State> out = Sets.newHashSet();
		if (!isFieldWriteWithBase(succ, fact)) {
			// always maintain data-flow if not a field write // killFlow has
			// been taken care of
			if(!options.trackReturnOfInstanceOf() || !isInstanceOfStatement(succ,fact)) {
				out.add(new Node<Statement, Val>(new Statement((Stmt) succ, method), fact));
			}
		} else {
			out.add(new ExclusionNode<Statement, Val, Field>(new Statement(succ, method), fact,
					getWrittenField(succ)));
		}
		if (succ instanceof AssignStmt) {
			AssignStmt assignStmt = (AssignStmt) succ;
			Value leftOp = assignStmt.getLeftOp();
			Value rightOp = assignStmt.getRightOp();
			if (rightOp.equals(fact.value())) {
				if (leftOp instanceof InstanceFieldRef) {
					InstanceFieldRef ifr = (InstanceFieldRef) leftOp;
					out.add(new PushNode<Statement, Val, Field>(new Statement(succ, method), new Val(ifr.getBase(),method),
							new Field(ifr.getField()), PDSSystem.FIELDS));
				} else if(leftOp instanceof StaticFieldRef){
					StaticFieldRef sfr = (StaticFieldRef) leftOp;
					if(options.staticFlows()){
						out.add(new Node<Statement, Val>(new Statement(succ, method), new StaticFieldVal(leftOp,sfr.getField(),method)));
					}
				} else if(leftOp instanceof ArrayRef){
					ArrayRef arrayRef = (ArrayRef) leftOp;
					if(options.arrayFlows()){
						out.add(new PushNode<Statement, Val, Field>(new Statement(succ, method), new Val(arrayRef.getBase(),method),
								Field.array(), PDSSystem.FIELDS));
					}
				} else{
					out.add(new Node<Statement, Val>(new Statement(succ, method), new Val(leftOp,method)));
				}
			}
			if (rightOp instanceof InstanceFieldRef) {
				InstanceFieldRef ifr = (InstanceFieldRef) rightOp;
				Value base = ifr.getBase();
				if (base.equals(fact.value())) {
					NodeWithLocation<Statement, Val, Field> succNode = new NodeWithLocation<>(
							new Statement(succ, method), new Val(leftOp,method), new Field(ifr.getField()));
					out.add(new PopNode<NodeWithLocation<Statement, Val, Field>>(succNode, PDSSystem.FIELDS));
				}
			} else if(rightOp instanceof StaticFieldRef){
				StaticFieldRef sfr = (StaticFieldRef) rightOp;
				if (fact.isStatic() && fact.equals(new StaticFieldVal(rightOp,sfr.getField(),method))) {
					out.add(new Node<Statement, Val>(new Statement(succ, method), new Val(leftOp,method)));
				}
			} else if(rightOp instanceof ArrayRef){
				ArrayRef arrayRef = (ArrayRef) rightOp;
				Value base = arrayRef.getBase();
				if (base.equals(fact.value())) {
					NodeWithLocation<Statement, Val, Field> succNode = new NodeWithLocation<>(
							new Statement(succ, method), new Val(leftOp,method), Field.array());
					out.add(new PopNode<NodeWithLocation<Statement, Val, Field>>(succNode, PDSSystem.FIELDS));
				}
			} else if(rightOp instanceof CastExpr){
				CastExpr castExpr = (CastExpr) rightOp;
				if (castExpr.getOp().equals(fact.value())) {
					out.add(new Node<Statement,Val>(new Statement(succ, method), new Val(leftOp,method)));
				}
			} else if(rightOp instanceof InstanceOfExpr && query.getType() instanceof NullType && options.trackReturnOfInstanceOf()) {
				InstanceOfExpr instanceOfExpr = (InstanceOfExpr) rightOp;
				if (instanceOfExpr.getOp().equals(fact.value()) ) {
					out.add(new Node<Statement,Val>(new Statement(succ, method), new ValWithFalseVariable(fact.value(),method,leftOp)));
				}
			}
		}

		return out;
	}

	private boolean isInstanceOfStatement(Stmt curr, Val fact) {
		if(curr instanceof AssignStmt) {
			AssignStmt as = (AssignStmt) curr;
			if(as.getRightOp() instanceof InstanceOfExpr  && query.getType() instanceof NullType) {
				InstanceOfExpr instanceOfExpr = (InstanceOfExpr) as.getRightOp();
				if (instanceOfExpr.getOp().equals(fact.value())) {
					return true;
				}
			}
		}
		return false;
	}


	protected void callFlow(SootMethod caller, Node<Statement,Val> currNode, Stmt callSite, InvokeExpr invokeExpr) {
		assert icfg.isCallStmt(callSite);
		if (Scene.v().isExcluded(invokeExpr.getMethod().getDeclaringClass()) || invokeExpr.getMethod().isNative()) {
			for(State s:  computeNormalFlow(caller, currNode.stmt().getUnit().get(), currNode.fact(), (Stmt) callSite)) {
				propagate(currNode, s);
			}
		}
			
		icfg.addCalleeListener(new CallSiteCalleeListener(caller, callSite, currNode, invokeExpr));
	}
	
	@Override
	public Collection<? extends State> computeReturnFlow(SootMethod method, Stmt curr, Val value, Stmt callSite,
			Stmt returnSite) {
		Statement returnSiteStatement = new Statement(callSite,icfg.getMethodOf(callSite));
		if(curr instanceof ThrowStmt && !options.throwFlows()){
			return Collections.emptySet();
		}
		Set<State> out = Sets.newHashSet();
		if (curr instanceof ReturnStmt) {
			Value op = ((ReturnStmt) curr).getOp();
			if (op.equals(value.value())) {
				if(callSite instanceof AssignStmt){
					out.add(new CallPopNode<Val,Statement>(new Val(((AssignStmt)callSite).getLeftOp(), icfg.getMethodOf(callSite)), PDSSystem.CALLS,returnSiteStatement));
				}
			}
		}
		if (!method.isStatic()) {
			if (method.getActiveBody().getThisLocal().equals(value.value())) {
				if (callSite.containsInvokeExpr()) {
					if (callSite.getInvokeExpr() instanceof InstanceInvokeExpr) {
						InstanceInvokeExpr iie = (InstanceInvokeExpr) callSite.getInvokeExpr();
						out.add(new CallPopNode<Val,Statement>(new Val(iie.getBase(), icfg.getMethodOf(callSite)), PDSSystem.CALLS,returnSiteStatement));
					}
				}
			}
		}
		int index = 0;
		for (Local param : method.getActiveBody().getParameterLocals()) {
			if (param.equals(value.value())) {
				if (callSite.containsInvokeExpr()) {
					InvokeExpr iie = (InvokeExpr) callSite.getInvokeExpr();
					out.add(new CallPopNode<Val,Statement>(new Val(iie.getArg(index),icfg.getMethodOf(callSite)), PDSSystem.CALLS,returnSiteStatement));
				}
			}
			index++;
		}
		if(value.isStatic()){
			out.add(new CallPopNode<Val,Statement>(new StaticFieldVal(value.value(),((StaticFieldVal) value).field(), icfg.getMethodOf(callSite)), PDSSystem.CALLS,returnSiteStatement));
		}
		return out;
	}
	
}