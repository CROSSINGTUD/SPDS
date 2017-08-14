package boomerang.solver;

import java.util.Collection;
import java.util.Collections;
import java.util.Set;

import com.beust.jcommander.internal.Sets;
import com.google.common.base.Optional;

import analysis.DoublePDSSolver;
import analysis.Node;
import analysis.PopNode;
import analysis.PushNode;
import boomerang.jimple.Field;
import boomerang.jimple.Statement;
import heros.InterproceduralCFG;
import soot.Body;
import soot.Local;
import soot.Scene;
import soot.SootMethod;
import soot.Unit;
import soot.Value;
import soot.jimple.AssignStmt;
import soot.jimple.InstanceFieldRef;
import soot.jimple.InstanceInvokeExpr;
import soot.jimple.InvokeExpr;
import soot.jimple.Jimple;
import soot.jimple.ReturnStmt;
import soot.jimple.Stmt;
import wpds.interfaces.State;

public class BoomerangSolver extends DoublePDSSolver<Statement, Value, Field>{

	private static Local returnVal;
	private InterproceduralCFG<Unit, SootMethod> icfg;
	
	
	
	public BoomerangSolver(InterproceduralCFG<Unit, SootMethod> icfg){
		this.icfg = icfg;
	}
	@Override
	public Field fieldWildCard() {
		return Field.wildcard();
	}

	@Override
	public Collection<? extends State> computeSuccessor(Node<Statement, Value> node) {
		Statement stmt = node.stmt();
		Optional<Stmt> unit = stmt.getUnit();
		if(unit.isPresent()){
			Stmt curr = unit.get();
			Value value = node.fact();
			if(killFlow(node, curr)){
				return Collections.emptySet();
			}
			if(curr.containsInvokeExpr() && valueUsedInStatement(curr,curr.getInvokeExpr(), value)){
				return callFlow(curr, curr.getInvokeExpr(), value);
			} else if(curr instanceof ReturnStmt){
				return computeReturnFlow((ReturnStmt)curr, value);
			} else{
				return normalFlow(curr, value);
			}
		}
		return Collections.emptySet();
	}

	private Collection<State> normalFlow(Stmt curr, Value fact) {
		Set<State> out = Sets.newHashSet();
		for(Unit succ : icfg.getSuccsOf(curr)){
			//always maitain data-flow
			out.add(new Node<Statement,Value>(new Statement((Stmt) succ),fact));
			out.addAll(computeNormalFlow(curr, fact, (Stmt) succ));
		}
		return out;
	}
	private boolean killFlow(Node<Statement, Value> node, Stmt u) {
		if(u instanceof AssignStmt){
			AssignStmt as = (AssignStmt) u;
			//Kill x at any statement x = * during propagation.
			if(as.getLeftOp().equals(node.fact())){
				//But not for a statement x = x.f
				if(as.getRightOp() instanceof InstanceFieldRef){
					InstanceFieldRef iie = (InstanceFieldRef) as.getRightOp();
					if(iie.getBase().equals(node.fact())){
						return false;
					}
				}
				return true;
			}
		}
		return false;
	}
	private boolean valueUsedInStatement(Stmt u, InvokeExpr invokeExpr, Value fact) {
		//TODO what about assignment?
		if(invokeExpr instanceof InstanceInvokeExpr){
			InstanceInvokeExpr iie = (InstanceInvokeExpr) invokeExpr;
			if(iie.getBase().equals(fact))
				return true;
		}
		for(Value arg : invokeExpr.getArgs()){
			if(arg.equals(fact)){
				return true;
			}
		}
		return false;
	}
	private Collection<? extends State> computeReturnFlow(ReturnStmt curr, Value value) {
		assert icfg.isExitStmt(curr);
		Value op = curr.getOp();
	
		if(op.equals(value))
			return Collections.singleton(new PopNode<Value>(returnVal(), PDSSystem.CALLS));
		return Collections.emptySet();
	}
	private Collection<State> callFlow(Stmt callSite, InvokeExpr invokeExpr, Value value) {
		assert icfg.isCallStmt(callSite);
		Set<State> out = Sets.newHashSet();
		for(SootMethod callee : icfg.getCalleesOfCallAt(callSite)){
			for(Unit calleeSp : icfg.getStartPointsOf(callee)){
				out.addAll(computeCallFlow(callSite, invokeExpr, value, callee, (Stmt) calleeSp));
			}
		}
		return out;
	}


	private Collection<State> computeNormalFlow(Stmt curr, Value fact, Stmt succ) {
		Set<State> out = Sets.newHashSet();
		if(curr instanceof AssignStmt){
			AssignStmt assignStmt = (AssignStmt) curr;
			Value leftOp = assignStmt.getLeftOp();
			Value rightOp = assignStmt.getRightOp();
			if(rightOp.equals(fact)){
				if(leftOp instanceof InstanceFieldRef){
					InstanceFieldRef ifr = (InstanceFieldRef) leftOp;
					out.add(new PushNode<Statement, Value, Field>(new Statement(succ),leftOp, new Field(ifr.getField()), PDSSystem.FIELDS));
				} else{
					out.add(new Node<Statement,Value>(new Statement(succ),leftOp));
				}
			}
			if(rightOp instanceof InstanceFieldRef){
				InstanceFieldRef ifr = (InstanceFieldRef) rightOp;
				Value base = ifr.getBase();
				if(base.equals(fact)){
					out.add(new PopNode<Field>(new Field(ifr.getField()), PDSSystem.FIELDS));
				}
			}
		}
		return null;
	}

	private Collection<? extends State> computeCallFlow(Stmt callSite, InvokeExpr invokeExpr, Value fact, SootMethod callee, Stmt calleeSp) {
		if(!callee.hasActiveBody())
			return Collections.emptySet();
		Body calleeBody = callee.getActiveBody();
		if(invokeExpr instanceof InstanceInvokeExpr){
			InstanceInvokeExpr iie = (InstanceInvokeExpr) invokeExpr;
			if(iie.getBase().equals(fact) && !callee.isStatic()){
				// TODO Do we need the return site? 
				return Collections.singleton(new PushNode<Statement, Value, Statement>(new Statement(calleeSp), calleeBody.getThisLocal(),new Statement(callSite), PDSSystem.CALLS));
			}
		}
		int i = 0;
		for(Value arg : invokeExpr.getArgs()){
			if(arg.equals(fact)){
				Local param = calleeBody.getParameterLocal(i);
				return Collections.singleton(new PushNode<Statement, Value, Statement>(new Statement(calleeSp), param,new Statement(callSite), PDSSystem.CALLS));
			}
			i++;
		}
		return Collections.emptySet();
	}
	@Override
	public Field epsilonField() {
		return Field.epsilon();
	}

	@Override
	public Field emptyField() {
		return Field.empty();
	}

	@Override
	public Statement epsilonStmt() {
		return Statement.epsilon();
	}
	
	private static Value returnVal(){
		if(returnVal == null)
			returnVal = Jimple.v().newLocal("RET", Scene.v().getType("java.lang.String"));
		return returnVal;
	}
}
