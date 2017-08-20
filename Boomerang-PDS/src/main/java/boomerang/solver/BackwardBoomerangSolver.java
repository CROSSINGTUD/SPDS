package boomerang.solver;

import java.util.Collection;
import java.util.Collections;
import java.util.Set;

import com.beust.jcommander.internal.Sets;

import boomerang.Boomerang;
import boomerang.jimple.Field;
import boomerang.jimple.ReturnSite;
import boomerang.jimple.Statement;
import soot.Body;
import soot.Local;
import soot.SootMethod;
import soot.Unit;
import soot.Value;
import soot.jimple.AssignStmt;
import soot.jimple.InstanceFieldRef;
import soot.jimple.InstanceInvokeExpr;
import soot.jimple.InvokeExpr;
import soot.jimple.NewExpr;
import soot.jimple.NullConstant;
import soot.jimple.ReturnStmt;
import soot.jimple.Stmt;
import soot.jimple.toolkits.ide.icfg.BackwardsInterproceduralCFG;
import soot.jimple.toolkits.ide.icfg.BiDiInterproceduralCFG;
import sync.pds.solver.nodes.ExclusionNode;
import sync.pds.solver.nodes.Node;
import sync.pds.solver.nodes.NodeWithLocation;
import sync.pds.solver.nodes.PopNode;
import sync.pds.solver.nodes.PushNode;
import wpds.interfaces.State;

public class BackwardBoomerangSolver extends AbstractBoomerangSolver{

	public BackwardBoomerangSolver(BiDiInterproceduralCFG<Unit, SootMethod> icfg){
		super(icfg);
	}

	@Override
	protected boolean killFlow(Stmt curr, Value value) {
		return false;
	}

	@Override
	protected Collection<? extends State> computeReturnFlow(SootMethod method, Stmt curr, Value value) {
//		if (curr instanceof ReturnStmt) {
//			Value op = ((ReturnStmt) curr).getOp();
//
//			if (op.equals(value)) {
//				return Collections.singleton(new PopNode<Value>(returnVal(), PDSSystem.CALLS));
//			}
//		}
		if (!method.isStatic()) {
			if (value.equals(method.getActiveBody().getThisLocal())) {
				return Collections.singleton(new PopNode<Value>(thisVal(), PDSSystem.CALLS));
			}
		}
		int index = 0;
		for (Local param : method.getActiveBody().getParameterLocals()) {
			if (param.equals(value)) {
				return Collections.singleton(new PopNode<Value>(param(index), PDSSystem.CALLS));
			}
			index++;
		}

		return Collections.emptySet();
	}

	@Override
	protected Collection<? extends State> computeCallFlow(SootMethod caller, ReturnSite returnSite,
			InvokeExpr invokeExpr, Value fact, SootMethod callee, Stmt calleeSp) {
		if (!callee.hasActiveBody())
			return Collections.emptySet();
		Body calleeBody = callee.getActiveBody();
		
		if (invokeExpr instanceof InstanceInvokeExpr) {
			InstanceInvokeExpr iie = (InstanceInvokeExpr) invokeExpr;
			if (iie.getBase().equals(fact) && !callee.isStatic()) {
				return Collections.singleton(new PushNode<Statement, Value, Statement>(new Statement(calleeSp, callee),
						calleeBody.getThisLocal(), returnSite, PDSSystem.CALLS));
			}
		}
		int i = 0;
		for (Value arg : invokeExpr.getArgs()) {
			if (arg.equals(fact)) {
				Local param = calleeBody.getParameterLocal(i);
				return Collections.singleton(new PushNode<Statement, Value, Statement>(new Statement(calleeSp, callee),
						param, returnSite, PDSSystem.CALLS));
			}
			i++;
		}
		
		Stmt callSite = returnSite.getCallSite();
		if(callSite instanceof AssignStmt && calleeSp instanceof ReturnStmt){
			AssignStmt as = (AssignStmt) callSite;
			ReturnStmt retStmt = (ReturnStmt) calleeSp;
			if(as.getLeftOp().equals(fact)){
				return Collections.singleton(new PushNode<Statement, Value, Statement>(new Statement(calleeSp, callee),
						retStmt.getOp(), returnSite, PDSSystem.CALLS));
			}
		}
		return Collections.emptySet();
	}

	@Override
	protected Collection<State> computeNormalFlow(SootMethod method, Stmt curr, Value fact, Stmt succ) {
		assert !fact.equals(thisVal()) && !fact.equals(returnVal()) && !fact.equals(param(0));
		if(Boomerang.isAllocationValue(fact)){
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
			if (leftOp.equals(fact)) {
				leftSideMatches = true;
				if (rightOp instanceof InstanceFieldRef) {
					InstanceFieldRef ifr = (InstanceFieldRef) rightOp;
					out.add(new PushNode<Statement, Value, Field>(new Statement(succ, method), ifr.getBase(),
							new Field(ifr.getField()), PDSSystem.FIELDS));
				} else {	
					if(isFieldLoadWithBase(curr, fact)){
						out.add(new ExclusionNode<Statement, Value, Field>(new Statement(succ, method), fact,
							getLoadedField(curr)));
					} else{
						out.add(new Node<Statement, Value>(new Statement(succ, method), rightOp));
					}
				}
			}
			if (leftOp instanceof InstanceFieldRef) {
				InstanceFieldRef ifr = (InstanceFieldRef) leftOp;
				Value base = ifr.getBase();
				if (base.equals(fact)) {
					NodeWithLocation<Statement, Value, Field> succNode = new NodeWithLocation<>(
							new Statement(succ, method), rightOp, new Field(ifr.getField()));
					out.add(new PopNode<NodeWithLocation<Statement, Value, Field>>(succNode, PDSSystem.FIELDS));
				}
			}
		}
		if(!leftSideMatches)
			out.add(new Node<Statement, Value>(new Statement(succ, method), fact));
		return out;
	}
	
}
