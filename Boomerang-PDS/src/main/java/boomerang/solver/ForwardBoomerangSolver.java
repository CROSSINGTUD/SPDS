package boomerang.solver;

import java.util.Collection;
import java.util.Collections;
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
import soot.jimple.InstanceFieldRef;
import soot.jimple.InstanceInvokeExpr;
import soot.jimple.InvokeExpr;
import soot.jimple.ReturnStmt;
import soot.jimple.Stmt;
import sync.pds.solver.nodes.CallPopNode;
import sync.pds.solver.nodes.ExclusionNode;
import sync.pds.solver.nodes.Node;
import sync.pds.solver.nodes.NodeWithLocation;
import sync.pds.solver.nodes.PopNode;
import sync.pds.solver.nodes.PushNode;
import wpds.interfaces.State;

public class ForwardBoomerangSolver extends AbstractBoomerangSolver {
	public ForwardBoomerangSolver(InterproceduralCFG<Unit, SootMethod> icfg, ForwardQuery query) {
		super(icfg, query);
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
		for (Value arg : invokeExpr.getArgs()) {
			if (arg.equals(fact.value())) {
				Local param = calleeBody.getParameterLocal(i);
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
					out.clear();
					NodeWithLocation<Statement, Val, Field> succNode = new NodeWithLocation<>(
							new Statement(succ, method), new Val(leftOp,method), new Field(ifr.getField()));
					out.add(new PopNode<NodeWithLocation<Statement, Val, Field>>(succNode, PDSSystem.FIELDS));
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
					InstanceInvokeExpr iie = (InstanceInvokeExpr) callSite.getInvokeExpr();
					return Collections.singleton(new CallPopNode<Val,Statement>(new Val(iie.getArg(index),icfg.getMethodOf(callSite)), PDSSystem.CALLS,returnSiteStatement));
				}
			}
			index++;
		}

		return Collections.emptySet();
	}

}
