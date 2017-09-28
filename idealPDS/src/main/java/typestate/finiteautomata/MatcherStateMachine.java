package typestate.finiteautomata;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import com.google.common.base.Joiner;

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
import soot.jimple.NewExpr;
import soot.jimple.Stmt;
import sync.pds.solver.WeightFunctions;
import sync.pds.solver.nodes.Node;
import typestate.TransitionFunction;
import typestate.finiteautomata.MatcherTransition.Parameter;
import typestate.finiteautomata.MatcherTransition.Type;

public abstract class MatcherStateMachine implements  WeightFunctions<Statement, Val, Statement, TransitionFunction> {
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
	
	public TransitionFunction pop(Node<Statement,Val> curr, Statement pops) {
		return getMatchingTransitions(curr.stmt().getMethod(), curr.fact(), Type.OnReturn);
	}

	public TransitionFunction push(Node<Statement,Val> curr, Node<Statement,Val> succ, Statement push) {
		return getMatchingTransitions(succ.stmt().getMethod(),succ.fact(), Type.OnCall);
	}
	
	@Override
	public TransitionFunction normal(Node<Statement, Val> curr, Node<Statement, Val> succ) {
		return getOne();
	}
	

//	public Set<Transition<State>> getCallToReturnTransitionsFor(AccessGraph d1, Unit callSite, AccessGraph d2,
//			Unit returnSite, AccessGraph d3) {
//		Set<Transition<State>> res = new HashSet<>();
//		if(callSite instanceof Stmt){
//			Stmt stmt = (Stmt) callSite;
//			if(stmt.containsInvokeExpr() && stmt.getInvokeExpr() instanceof InstanceInvokeExpr){
//				SootMethod method = stmt.getInvokeExpr().getMethod();
//				InstanceInvokeExpr e = (InstanceInvokeExpr)stmt.getInvokeExpr();
//				if(e.getBase().equals(d2.getBase())){
//					for (MatcherTransition<State> trans : transition) {
//						if(trans.matches(method) && trans.getType().equals(Type.OnCallToReturn)){
//							res.add(trans);
//						}
//					}	
//				}
//			}
//		}
//		return res;
//	}

	private TransitionFunction getMatchingTransitions(SootMethod method, Val node, Type type) {
		Set<ITransition> res = new HashSet<>();
//		if (node.getFieldCount() == 0) { //TODO How do we check this?
			for (MatcherTransition trans : transition) {
				if (trans.matches(method) && trans.getType().equals(type)) {
					Parameter param = trans.getParam();
					if (param.equals(Parameter.This) && isThisValue(method, node))
						res.add(new Transition(trans.from(), trans.to()));
					if (param.equals(Parameter.Param1)
							&& method.getActiveBody().getParameterLocal(0).equals(node.value()))
						res.add(new Transition(trans.from(), trans.to()));
					if (param.equals(Parameter.Param2)
							&& method.getActiveBody().getParameterLocal(1).equals(node.value()))
						res.add(new Transition(trans.from(), trans.to()));
				}
//			}
		}
			
		if(res.isEmpty())
			return getOne();
		return new TransitionFunction(res);
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

	protected Collection<Val> getLeftSideOf(SootMethod m, Unit unit) {
		if (unit instanceof AssignStmt) {
			Set<Val> out = new HashSet<>();
			AssignStmt stmt = (AssignStmt) unit;
			out.add(
					new Val(stmt.getLeftOp(), m));
			return out;
		}
		return Collections.emptySet();
	}
	
	protected Collection<Val> generateThisAtAnyCallSitesOf(SootMethod m, Unit unit,
			Collection<SootMethod> calledMethod, Set<SootMethod> hasToCall) {
		for (SootMethod callee : calledMethod) {
			if (hasToCall.contains(callee)) {
				if (unit instanceof Stmt) {
					if (((Stmt) unit).getInvokeExpr() instanceof InstanceInvokeExpr) {
						InstanceInvokeExpr iie = (InstanceInvokeExpr) ((Stmt) unit).getInvokeExpr();
						Local thisLocal = (Local) iie.getBase();
						Set<Val> out = new HashSet<>();
						out.add(new Val(thisLocal, m));
						return out;
						
					}
				}

			}
		}
		return Collections.emptySet();
	}
	

	protected Collection<Val> generateAtAllocationSiteOf(SootMethod m, Unit unit, Class allocationSuperType) {
		if(unit instanceof AssignStmt){
			AssignStmt assignStmt = (AssignStmt) unit;
			if(assignStmt.getRightOp() instanceof NewExpr){
				NewExpr newExpr = (NewExpr) assignStmt.getRightOp();
				Value leftOp = assignStmt.getLeftOp();
				soot.Type type = newExpr.getType();
				if(Scene.v().getOrMakeFastHierarchy().canStoreType(type, Scene.v().getType(allocationSuperType.getName()))){
					return Collections.singleton(new Val(leftOp,m));
				}
			}
		}
		return Collections.emptySet();
	}
	
	@Override
	public String toString() {
		return Joiner.on("\n").join(transition);
	}

	public abstract Collection<Val> generateSeed(SootMethod method, Unit stmt, Collection<SootMethod> calledMethod); 
}
	
