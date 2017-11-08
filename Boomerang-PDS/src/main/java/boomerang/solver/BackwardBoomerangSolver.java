package boomerang.solver;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import com.google.common.collect.Sets;

import boomerang.BackwardQuery;
import boomerang.Boomerang;
import boomerang.MethodReachableQueue;
import boomerang.jimple.Field;
import boomerang.jimple.Statement;
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
import soot.jimple.toolkits.ide.icfg.BiDiInterproceduralCFG;
import sync.pds.solver.nodes.CallPopNode;
import sync.pds.solver.nodes.ExclusionNode;
import sync.pds.solver.nodes.INode;
import sync.pds.solver.nodes.Node;
import sync.pds.solver.nodes.NodeWithLocation;
import sync.pds.solver.nodes.PopNode;
import sync.pds.solver.nodes.PushNode;
import wpds.impl.NestedWeightedPAutomatons;
import wpds.impl.Weight;
import wpds.impl.WeightedPAutomaton;
import wpds.interfaces.State;

public abstract class BackwardBoomerangSolver<W extends Weight> extends AbstractBoomerangSolver<W>{

	public BackwardBoomerangSolver(MethodReachableQueue queue, BiDiInterproceduralCFG<Unit, SootMethod> icfg, BackwardQuery query, Map<Entry<INode<Node<Statement,Val>>, Field>, INode<Node<Statement,Val>>> genField, boolean useCallSummaries, NestedWeightedPAutomatons<Statement, INode<Val>, W> callSummaries, boolean useFieldSummaries, NestedWeightedPAutomatons<Field, INode<Node<Statement, Val>>, W> fieldSummaries){
		super(queue, icfg, query, genField, useCallSummaries, callSummaries, useFieldSummaries, fieldSummaries);
	}

	@Override
	protected boolean killFlow(SootMethod m, Stmt curr, Val value) {	
		if(value.equals(Val.statics()))
			return false;
		if (!m.getActiveBody().getLocals().contains(value.value()))
			return true;
		return false;
	}

	@Override
	protected Collection<? extends State> computeReturnFlow(SootMethod method, Stmt curr, Val value, Stmt callSite, Stmt returnSite) {
		Statement returnSiteStatement = new Statement(returnSite,icfg.getMethodOf(returnSite));
		if (!method.isStatic()) {
			if (method.getActiveBody().getThisLocal().equals(value.value())) {
				if(callSite.containsInvokeExpr()){
					if(callSite.getInvokeExpr() instanceof InstanceInvokeExpr){
						InstanceInvokeExpr iie = (InstanceInvokeExpr) callSite.getInvokeExpr();
						return Collections.singleton(new CallPopNode<Val,Statement>(new Val(iie.getBase(),icfg.getMethodOf(callSite)), PDSSystem.CALLS,returnSiteStatement));
					}
				}
			}
		}
		int index = 0;
		for (Local param : method.getActiveBody().getParameterLocals()) {
			if (param.equals(value.value())) {
				if(callSite.containsInvokeExpr()){
					InvokeExpr ie = callSite.getInvokeExpr();
					return Collections.singleton(new CallPopNode<Val,Statement>(new Val(ie.getArg(index),icfg.getMethodOf(callSite)), PDSSystem.CALLS,returnSiteStatement));
				}
			}
			index++;
		}
		if(value.equals(Val.statics())){
			return Collections.singleton(new CallPopNode<Val,Statement>(value, PDSSystem.CALLS,returnSiteStatement));
		}
		return Collections.emptySet();
	}

	@Override
	protected Collection<? extends State> computeCallFlow(SootMethod caller, Statement returnSite, Statement callSite,
			InvokeExpr invokeExpr, Val fact, SootMethod callee, Stmt calleeSp) {
		if (!callee.hasActiveBody())
			return Collections.emptySet();
		Body calleeBody = callee.getActiveBody();
		
		if (invokeExpr instanceof InstanceInvokeExpr) {
			InstanceInvokeExpr iie = (InstanceInvokeExpr) invokeExpr;
			if (iie.getBase().equals(fact.value()) && !callee.isStatic()) {
				return Collections.singleton(new PushNode<Statement, Val, Statement>(new Statement(calleeSp, callee),
						new Val(calleeBody.getThisLocal(),callee), returnSite, PDSSystem.CALLS));
			}
		}
		List<Local> parameterLocals = calleeBody.getParameterLocals();
		int i = 0;
		for (Value arg : invokeExpr.getArgs()) {
			if (arg.equals(fact.value()) && parameterLocals.size() > i) {
				Local param = parameterLocals.get(i);
				return Collections.singleton(new PushNode<Statement, Val, Statement>(new Statement(calleeSp, callee),
						new Val(param,callee), returnSite, PDSSystem.CALLS));
			}
			i++;
		}
		
		if(callSite.getUnit().get() instanceof AssignStmt && calleeSp instanceof ReturnStmt){
			AssignStmt as = (AssignStmt) callSite.getUnit().get();
			ReturnStmt retStmt = (ReturnStmt) calleeSp;
			if(as.getLeftOp().equals(fact.value())){
				return Collections.singleton(new PushNode<Statement, Val, Statement>(new Statement(calleeSp, callee),
						new Val(retStmt.getOp(),callee), returnSite, PDSSystem.CALLS));
			}
		}
		if(fact.equals(Val.statics())){
			return Collections.singleton(new PushNode<Statement, Val, Statement>(new Statement(calleeSp, callee),
					fact, returnSite, PDSSystem.CALLS));
		}
		return Collections.emptySet();
	}

	@Override
	protected Collection<State> computeNormalFlow(SootMethod method, Stmt curr, Val fact, Stmt succ) {
//		assert !fact.equals(thisVal()) && !fact.equals(returnVal()) && !fact.equals(param(0));
		if(Boomerang.isAllocationVal(fact.value())){
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
					StaticFieldRef ifr = (StaticFieldRef) rightOp;
					if(Boomerang.TRACK_STATIC){
						out.add(new PushNode<Statement, Val, Field>(new Statement(succ, method), Val.statics(),
								new Field(ifr.getField()), PDSSystem.FIELDS));
					}
				} else if(rightOp instanceof ArrayRef){
					ArrayRef ifr = (ArrayRef) rightOp;
					if(Boomerang.TRACK_ARRAYS){
						out.add(new PushNode<Statement, Val, Field>(new Statement(succ, method), new Val(ifr.getBase(),method),
								Field.array(), PDSSystem.FIELDS));
					}
					leftSideMatches = false;
				} else if(rightOp instanceof CastExpr){
					CastExpr castExpr = (CastExpr) rightOp;
					out.add(new Node<Statement, Val>(new Statement(succ, method), new Val(castExpr.getOp(),method)));
				} else {	
					if(isFieldLoadWithBase(curr, fact.value())){
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
				StaticFieldRef ifr = (StaticFieldRef) leftOp;
				if (fact.equals(Val.statics())) {
					NodeWithLocation<Statement, Val, Field> succNode = new NodeWithLocation<>(
							new Statement(succ, method), new Val(rightOp,method), new Field(ifr.getField()));
					out.add(new PopNode<NodeWithLocation<Statement, Val, Field>>(succNode, PDSSystem.FIELDS));
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
