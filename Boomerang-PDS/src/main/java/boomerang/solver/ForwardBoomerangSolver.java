package boomerang.solver;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import com.beust.jcommander.internal.Sets;

import boomerang.ForwardQuery;
import boomerang.jimple.Field;
import boomerang.jimple.ReturnSite;
import boomerang.jimple.Statement;
import boomerang.jimple.Val;
import heros.InterproceduralCFG;
import soot.Body;
import soot.Local;
import soot.SootMethod;
import soot.Unit;
import soot.Value;
import soot.jimple.AssignStmt;
import soot.jimple.CastExpr;
import soot.jimple.InstanceFieldRef;
import soot.jimple.InstanceInvokeExpr;
import soot.jimple.InvokeExpr;
import soot.jimple.ReturnStmt;
import soot.jimple.Stmt;
import sync.pds.solver.SyncPDSUpdateListener;
import sync.pds.solver.WitnessNode;
import sync.pds.solver.nodes.CallPopNode;
import sync.pds.solver.nodes.ExclusionNode;
import sync.pds.solver.nodes.INode;
import sync.pds.solver.nodes.Node;
import sync.pds.solver.nodes.NodeWithLocation;
import sync.pds.solver.nodes.PopNode;
import sync.pds.solver.nodes.PushNode;
import wpds.interfaces.State;

public abstract class ForwardBoomerangSolver extends AbstractBoomerangSolver {
	public ForwardBoomerangSolver(InterproceduralCFG<Unit, SootMethod> icfg, ForwardQuery query, Map<Entry<INode<Node<Statement, Val>>, Field>, INode<Node<Statement, Val>>> genField) {
		super(icfg, query, genField);
	}

	@Override
	public Collection<? extends State> computeCallFlow(SootMethod caller, ReturnSite returnSite, InvokeExpr invokeExpr,
			Val fact, SootMethod callee, Stmt calleeSp) {
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
		return Collections.emptySet();
	}
	
	

	@Override
	protected boolean killFlow(SootMethod m, Stmt curr, Val value) {
		if (!m.getActiveBody().getLocals().contains(value.value()))
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
		}
		return false;
	}

	@Override
	public Collection<State> computeNormalFlow(SootMethod method, Stmt curr, Val fact, Stmt succ) {
		Set<State> out = Sets.newHashSet();
		if (!isFieldWriteWithBase(curr, fact.value())) {
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
				} else {
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
			}
			if(rightOp instanceof CastExpr){
				CastExpr castExpr = (CastExpr) rightOp;
				if (castExpr.getOp().equals(fact.value())) {
					out.add(new Node<Statement, Val>(new Statement(succ, method), new Val(leftOp,method)));
				}
				
			}
		}
		return out;
	}

	@Override
	public Collection<? extends State> computeReturnFlow(SootMethod method, Stmt curr, Val value, Stmt callSite,
			Stmt returnSite) {
		Statement returnSiteStatement = new Statement(returnSite,icfg.getMethodOf(returnSite));
		if (curr instanceof ReturnStmt) {
			Value op = ((ReturnStmt) curr).getOp();
			if (op.equals(value.value())) {
				if(callSite instanceof AssignStmt){
					return Collections.singleton(new CallPopNode<Val,Statement>(new Val(((AssignStmt)callSite).getLeftOp(), icfg.getMethodOf(callSite)), PDSSystem.CALLS,returnSiteStatement));
				}
			}
		}
		if (!method.isStatic()) {
			if (value.value().equals(method.getActiveBody().getThisLocal())) {
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

		return Collections.emptySet();
	}
	
	@Override
	public void addUnbalancedFlow(SootMethod m) {
		for (Statement succ : getSuccsOf(query.asNode().stmt())) {
			Node<Statement, Val> curr = new Node<Statement, Val>(succ, query.asNode().fact());
			for(Unit callSite : icfg.getCallersOf(m)){
				for(Unit returnSite : icfg.getSuccsOf(callSite)){
					this.processPush(curr, new Statement((Stmt) returnSite, icfg.getMethodOf(returnSite)), curr, PDSSystem.CALLS);
				}
			}
		}
	}
	
	@Override
	protected void onReturnFlow(final Unit callSite, Unit returnSite, final SootMethod method, final Stmt returnStmt, final Val value,
			Collection<? extends State> outFlow) {
		for(State r : outFlow){
			if(r instanceof CallPopNode){
				final CallPopNode<Val,Statement> callPopNode = (CallPopNode) r;
				this.registerListener(new SyncPDSUpdateListener<Statement, Val, Field>() {
					
					@Override
					public void onReachableNodeAdded(WitnessNode<Statement, Val, Field> reachableNode) {
						if(reachableNode.asNode().equals(new Node<Statement,Val>(callPopNode.getReturnSite(),callPopNode.location()))){
							if(!valueUsedInStatement(icfg.getMethodOf(callSite), (Stmt)callSite, ((Stmt) callSite).getInvokeExpr(), callPopNode.location())){
								//TODO why do we need this?
								return;
							}
							onReturnFromCall(new Statement((Stmt) callSite, icfg.getMethodOf(callSite)), callPopNode.getReturnSite(),new Node<Statement,Val>(new Statement((Stmt)returnStmt, method),value), reachableNode.asNode());
						}
					}
				});
			}
		}
		super.onReturnFlow(callSite, returnSite, method, returnStmt, value, outFlow);
	}

	protected abstract void onReturnFromCall(Statement statement, Statement returnSite, Node<Statement, Val> asNode, Node<Statement, Val> node);
	
}
