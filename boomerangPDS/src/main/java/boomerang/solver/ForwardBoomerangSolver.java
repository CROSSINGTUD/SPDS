package boomerang.solver;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import com.google.common.collect.Sets;

import boomerang.BoomerangOptions;
import boomerang.ForwardQuery;
import boomerang.MethodReachableQueue;
import boomerang.jimple.AllocVal;
import boomerang.jimple.Field;
import boomerang.jimple.Statement;
import boomerang.jimple.StaticFieldVal;
import boomerang.jimple.Val;
import heros.InterproceduralCFG;
import soot.Body;
import soot.Local;
import soot.RefType;
import soot.Scene;
import soot.SootMethod;
import soot.Type;
import soot.Unit;
import soot.Value;
import soot.jimple.ArrayRef;
import soot.jimple.AssignStmt;
import soot.jimple.CastExpr;
import soot.jimple.InstanceFieldRef;
import soot.jimple.InstanceInvokeExpr;
import soot.jimple.InvokeExpr;
import soot.jimple.NewExpr;
import soot.jimple.ReturnStmt;
import soot.jimple.StaticFieldRef;
import soot.jimple.Stmt;
import soot.jimple.ThrowStmt;
import sync.pds.solver.nodes.CallPopNode;
import sync.pds.solver.nodes.CastNode;
import sync.pds.solver.nodes.ExclusionNode;
import sync.pds.solver.nodes.GeneratedState;
import sync.pds.solver.nodes.INode;
import sync.pds.solver.nodes.Node;
import sync.pds.solver.nodes.NodeWithLocation;
import sync.pds.solver.nodes.PopNode;
import sync.pds.solver.nodes.PushNode;
import wpds.impl.NestedWeightedPAutomatons;
import wpds.impl.Transition;
import wpds.impl.Weight;
import wpds.interfaces.State;

public abstract class ForwardBoomerangSolver<W extends Weight> extends AbstractBoomerangSolver<W> {
	public ForwardBoomerangSolver(MethodReachableQueue queue, InterproceduralCFG<Unit, SootMethod> icfg, ForwardQuery query, Map<Entry<INode<Node<Statement, Val>>, Field>, INode<Node<Statement, Val>>> genField, BoomerangOptions options, NestedWeightedPAutomatons<Statement, INode<Val>, W> callSummaries, NestedWeightedPAutomatons<Field, INode<Node<Statement, Val>>,W> fieldSummaries) {
		super(queue, icfg, query, genField, options, callSummaries, fieldSummaries);
	}
	
	@Override
	public Collection<? extends State> computeCallFlow(SootMethod caller, Statement returnSite, Statement callSite, InvokeExpr invokeExpr,
			Val fact, SootMethod callee, Stmt calleeSp) {
		if (!callee.hasActiveBody() || callee.isStaticInitializer()){
			return Collections.emptySet();
		}
		Body calleeBody = callee.getActiveBody();
		if (invokeExpr instanceof InstanceInvokeExpr) {
			InstanceInvokeExpr iie = (InstanceInvokeExpr) invokeExpr;
			if (iie.getBase().equals(fact.value()) && !callee.isStatic()) {
				return Collections.singleton(new PushNode<Statement, Val, Statement>(new Statement(calleeSp, callee),
						new Val(calleeBody.getThisLocal(),callee), returnSite, PDSSystem.CALLS));
			}
		}
		int i = 0;
		List<Local> parameterLocals = calleeBody.getParameterLocals();
		for (Value arg : invokeExpr.getArgs()) {
			if (arg.equals(fact.value()) && parameterLocals.size() > i) {
				Local param = parameterLocals.get(i);
				return Collections.singleton(new PushNode<Statement,  Val, Statement>(new Statement(calleeSp, callee),
						new Val(param,callee), returnSite, PDSSystem.CALLS));
			}
			i++;
		}
		if(fact.isStatic()){
			return Collections.singleton(new PushNode<Statement, Val, Statement>(new Statement(calleeSp, callee),
					new StaticFieldVal(fact.value(),((StaticFieldVal) fact).field(), callee), returnSite, PDSSystem.CALLS));
		}
		return Collections.emptySet();
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
	public Collection<State> computeNormalFlow(SootMethod method, Stmt curr, Val fact, Stmt succ) {
		Set<State> out = Sets.newHashSet();
		if (!isFieldWriteWithBase(curr, fact)) {
			// always maintain data-flow if not a field write // killFlow has
			// been taken care of
			out.add(new Node<Statement, Val>(new Statement((Stmt) succ, method), fact));
		} else {
			out.add(new ExclusionNode<Statement, Val, Field>(new Statement(succ, method), fact,
					getWrittenField(curr)));
		}
		if (curr instanceof AssignStmt) {
			AssignStmt assignStmt = (AssignStmt) curr;
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
				if (fact.isStatic() && fact.equals( new StaticFieldVal(rightOp,sfr.getField(),method))) {
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
					out.add(new CastNode<Statement,Val, Type>(new Statement(succ, method), new Val(leftOp,method),castExpr.getCastType()));
				}
				
			}
		}
		return out;
	}

	@Override
	public Collection<? extends State> computeReturnFlow(SootMethod method, Stmt curr, Val value, Stmt callSite,
			Stmt returnSite) {
		Statement returnSiteStatement = new Statement(returnSite,icfg.getMethodOf(returnSite));
		if(curr instanceof ThrowStmt && !options.throwFlows()){
			return Collections.emptySet();
		}
		if (curr instanceof ReturnStmt) {
			Value op = ((ReturnStmt) curr).getOp();
			if (op.equals(value.value())) {
				if(callSite instanceof AssignStmt){
					return Collections.singleton(new CallPopNode<Val,Statement>(new Val(((AssignStmt)callSite).getLeftOp(), icfg.getMethodOf(callSite)), PDSSystem.CALLS,returnSiteStatement));
				}
			}
		}
		if (!method.isStatic()) {
			if (method.getActiveBody().getThisLocal().equals(value.value())) {
				if (callSite.containsInvokeExpr()) {
					if (callSite.getInvokeExpr() instanceof InstanceInvokeExpr) {
						InstanceInvokeExpr iie = (InstanceInvokeExpr) callSite.getInvokeExpr();
						return Collections.singleton(new CallPopNode<Val,Statement>(new Val(iie.getBase(), icfg.getMethodOf(callSite)), PDSSystem.CALLS,returnSiteStatement));
					}
				}
			}
		}
		int index = 0;
		for (Local param : method.getActiveBody().getParameterLocals()) {
			if (param.equals(value.value())) {
				if (callSite.containsInvokeExpr()) {
					InvokeExpr iie = (InvokeExpr) callSite.getInvokeExpr();
					return Collections.singleton(new CallPopNode<Val,Statement>(new Val(iie.getArg(index),icfg.getMethodOf(callSite)), PDSSystem.CALLS,returnSiteStatement));
				}
			}
			index++;
		}
		if(value.isStatic()){
			return Collections.singleton(new CallPopNode<Val,Statement>(new StaticFieldVal(value.value(),((StaticFieldVal) value).field(), icfg.getMethodOf(callSite)), PDSSystem.CALLS,returnSiteStatement));
		}
		return Collections.emptySet();
	}
	
	@Override
	protected boolean preventFieldTransitionAdd(Transition<Field, INode<Node<Statement, Val>>> t, W weight) {
		if(!t.getLabel().equals(Field.empty()) || !options.typeCheck()){
			return false;
		}
		if(t.getTarget() instanceof GeneratedState || t.getStart() instanceof GeneratedState){
			return false;
		}
		Val target = t.getTarget().fact().fact();
		Val source = t.getStart().fact().fact();
		Value sourceVal = source.value();
		Value targetVal = target.value();
		
		if(source.isStatic()){
			return false;
		}
		if(!(targetVal.getType() instanceof RefType) || !(sourceVal.getType() instanceof RefType)){
			return false;//!allocVal.value().getType().equals(varVal.value().getType());
		}

		RefType targetType = (RefType) targetVal.getType(); 
		RefType sourceType = (RefType) sourceVal.getType(); 
		if(targetType.getSootClass().isPhantom() || sourceType.getSootClass().isPhantom())
			return false;
		if(target instanceof AllocVal && ((AllocVal) target).allocationValue() instanceof NewExpr){
			boolean castFails = Scene.v().getOrMakeFastHierarchy().canStoreType(targetType,sourceType);
			return !castFails;
		}
		boolean castFails = Scene.v().getOrMakeFastHierarchy().canStoreType(targetType,sourceType) || Scene.v().getOrMakeFastHierarchy().canStoreType(sourceType,targetType);
		return !castFails;
	}
}
