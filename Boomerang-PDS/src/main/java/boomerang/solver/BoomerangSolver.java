package boomerang.solver;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

import com.beust.jcommander.internal.Sets;
import com.google.common.base.Optional;
import com.google.common.collect.Maps;

import analysis.DoublePDSSolver;
import analysis.ExclusionNode;
import analysis.Node;
import analysis.NodeWithLocation;
import analysis.PopNode;
import analysis.PushNode;
import boomerang.jimple.Field;
import boomerang.jimple.ReturnSite;
import boomerang.jimple.Statement;
import heros.InterproceduralCFG;
import soot.Body;
import soot.Local;
import soot.Scene;
import soot.SootMethod;
import soot.Type;
import soot.Unit;
import soot.Value;
import soot.jimple.AssignStmt;
import soot.jimple.InstanceFieldRef;
import soot.jimple.InstanceInvokeExpr;
import soot.jimple.InvokeExpr;
import soot.jimple.Jimple;
import soot.jimple.ReturnStmt;
import soot.jimple.Stmt;
import soot.jimple.internal.JimpleLocal;
import wpds.interfaces.State;

public class BoomerangSolver extends DoublePDSSolver<Statement, Value, Field>{

	private static Local returnVal;
	private static Value thisVal;
	private static Map<Integer,Value> parameterVals = Maps.newHashMap();
	private InterproceduralCFG<Unit, SootMethod> icfg;
	
	public BoomerangSolver(InterproceduralCFG<Unit, SootMethod> icfg){
		this.icfg = icfg;
	}
	@Override
	public Collection<? extends State> computeSuccessor(Node<Statement, Value> node) {
		Statement stmt = node.stmt();
		Optional<Stmt> unit = stmt.getUnit();
		if(unit.isPresent()){
			Stmt curr = unit.get();
			Value value = node.fact();
			SootMethod method = icfg.getMethodOf(curr);
			if(node.stmt() instanceof ReturnSite){
				ReturnSite returnSite = (ReturnSite) node.stmt();
				return mapValuesToCaller(method,returnSite.getCallSite(),value, curr);
			}
			if(killFlow(curr, value)){
				return Collections.emptySet();
			}
			if(curr.containsInvokeExpr() && valueUsedInStatement(method,curr,curr.getInvokeExpr(), value)){
				return callFlow(method, curr, curr.getInvokeExpr(), value);
			} else if(icfg.isExitStmt(curr)){
				return computeReturnFlow(method,curr, value);
			} else{
				return normalFlow(method, curr, value);
			}
		}
		return Collections.emptySet();
	}

	private Collection<? extends State> mapValuesToCaller(SootMethod method, Stmt callSite, Value value, Stmt curr) {
		if(value.equals(thisVal())){
			InvokeExpr invokeExpr = callSite.getInvokeExpr();
			if(invokeExpr instanceof InstanceInvokeExpr){
				InstanceInvokeExpr iie = (InstanceInvokeExpr) invokeExpr;
				return Collections.singleton(new Node<Statement,Value>(new Statement(curr, method),iie.getBase()));
			}
		}
		if(value.equals(returnVal())){
			if(callSite instanceof AssignStmt){
				AssignStmt as = (AssignStmt) callSite;
				return Collections.singleton(new Node<Statement,Value>(new Statement(curr, method), as.getLeftOp()));
			}
		}
		if(value instanceof ParameterValue){
			ParameterValue paramVal = (ParameterValue) value;
			int index = paramVal.getIndex();
			InvokeExpr invokeExpr = callSite.getInvokeExpr();
			return Collections.singleton(new Node<Statement,Value>(new Statement(curr, method),invokeExpr.getArg(index)));
		}
		return Collections.emptySet();
	}
	private Collection<State> normalFlow(SootMethod method, Stmt curr, Value fact) {
		Set<State> out = Sets.newHashSet();
		for(Unit succ : icfg.getSuccsOf(curr)){
			//always maitain data-flow // killFlow has been taken care of
			if(isFieldWriteWithBase(curr,fact)){
				out.add(new ExclusionNode<Statement,Value,Field>(new Statement((Stmt) succ, method),fact,getWrittenField(curr)));
			}
			else{
				out.add(new Node<Statement,Value>(new Statement((Stmt) succ, method),fact));
			}
			out.addAll(computeNormalFlow(method,curr, fact, (Stmt) succ));
		}
		return out;
	}
	private Field getWrittenField(Stmt curr) {
		AssignStmt as = (AssignStmt) curr;
		InstanceFieldRef ifr = (InstanceFieldRef) as.getLeftOp();
		return new Field(ifr.getField());
	}
	private boolean isFieldWriteWithBase(Stmt curr, Value base) {
		if(curr instanceof AssignStmt){
			AssignStmt as = (AssignStmt) curr;
			if(as.getLeftOp() instanceof InstanceFieldRef){
				InstanceFieldRef ifr = (InstanceFieldRef) as.getLeftOp();
				return ifr.getBase().equals(base);
			}
		}
		return false;
	}
	private boolean killFlow(Stmt curr, Value value) {
		if(curr instanceof AssignStmt){
			AssignStmt as = (AssignStmt) curr;
			//Kill x at any statement x = * during propagation.
			if(as.getLeftOp().equals(value)){
				//But not for a statement x = x.f
				if(as.getRightOp() instanceof InstanceFieldRef){
					InstanceFieldRef iie = (InstanceFieldRef) as.getRightOp();
					if(iie.getBase().equals(value)){
						return false;
					}
				}
				return true;
			}
		}
		return false;
	}
	private boolean valueUsedInStatement(SootMethod method, Stmt u, InvokeExpr invokeExpr, Value fact) {
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
	private Collection<? extends State> computeReturnFlow(SootMethod method, Stmt curr, Value value) {
		if(curr instanceof ReturnStmt){
			Value op = ((ReturnStmt) curr).getOp();
	
			if(op.equals(value)){
				return Collections.singleton(new PopNode<Value>(returnVal(), PDSSystem.CALLS));
			}
		}
		if(!method.isStatic()){
			if(value.equals(method.getActiveBody().getThisLocal())){
				return Collections.singleton(new PopNode<Value>(thisVal(), PDSSystem.CALLS));
			}
		}
		int index = 0;
		for(Local param : method.getActiveBody().getParameterLocals()){
			if(param.equals(value)){
				return Collections.singleton(new PopNode<Value>(param(index), PDSSystem.CALLS));	
			}
			index++;
		}
		
		return Collections.emptySet();
	}
	private Collection<State> callFlow(SootMethod caller, Stmt callSite, InvokeExpr invokeExpr, Value value) {
		assert icfg.isCallStmt(callSite);
		Set<State> out = Sets.newHashSet();
		for(SootMethod callee : icfg.getCalleesOfCallAt(callSite)){
			for(Unit calleeSp : icfg.getStartPointsOf(callee)){
				for(Unit returnSite : icfg.getSuccsOf(callSite)){
					out.addAll(computeCallFlow(caller,new ReturnSite((Stmt) returnSite,caller, callSite), invokeExpr, value, callee, (Stmt) calleeSp));
				}
			}
		}
		return out;
	}


	private Collection<State> computeNormalFlow(SootMethod method, Stmt curr, Value fact, Stmt succ) {
		Set<State> out = Sets.newHashSet();
		if(curr instanceof AssignStmt){
			AssignStmt assignStmt = (AssignStmt) curr;
			Value leftOp = assignStmt.getLeftOp();
			Value rightOp = assignStmt.getRightOp();
			if(rightOp.equals(fact)){
				if(leftOp instanceof InstanceFieldRef){
					InstanceFieldRef ifr = (InstanceFieldRef) leftOp;
					out.add(new PushNode<Statement, Value, Field>(new Statement(succ, method),ifr.getBase(), new Field(ifr.getField()), PDSSystem.FIELDS));
				} else{
					out.add(new Node<Statement,Value>(new Statement(succ, method),leftOp));
				}
			}
			if(rightOp instanceof InstanceFieldRef){
				InstanceFieldRef ifr = (InstanceFieldRef) rightOp;
				Value base = ifr.getBase();
				if(base.equals(fact)){
					NodeWithLocation<Statement, Value, Field> succNode = new NodeWithLocation<>(new Statement(succ, method), leftOp, new Field(ifr.getField()));
					out.add(new PopNode<NodeWithLocation<Statement, Value, Field>>(succNode, PDSSystem.FIELDS));
				}
			}
		}
		return out;
	}

	private Collection<? extends State> computeCallFlow(SootMethod caller, ReturnSite returnSite, InvokeExpr invokeExpr, Value fact, SootMethod callee, Stmt calleeSp) {
		if(!callee.hasActiveBody())
			return Collections.emptySet();
		Body calleeBody = callee.getActiveBody();
		if(invokeExpr instanceof InstanceInvokeExpr){
			InstanceInvokeExpr iie = (InstanceInvokeExpr) invokeExpr;
			if(iie.getBase().equals(fact) && !callee.isStatic()){
				return Collections.singleton(new PushNode<Statement, Value, Statement>(new Statement(calleeSp,callee), calleeBody.getThisLocal(),returnSite, PDSSystem.CALLS));
			}
		}
		int i = 0;
		for(Value arg : invokeExpr.getArgs()){
			if(arg.equals(fact)){
				Local param = calleeBody.getParameterLocal(i);
				return Collections.singleton(new PushNode<Statement, Value, Statement>(new Statement(calleeSp,callee), param,returnSite, PDSSystem.CALLS));
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
	
	private static Value thisVal(){
		if(thisVal == null)
			thisVal = Jimple.v().newLocal("THIS", Scene.v().getType("java.lang.String"));
		return thisVal;
	}
	
	private static Value param(int index){
		Value val = null;
		val = parameterVals.get(index);
		
		if(val == null){
			Local paramVal = new ParameterValue("PARAM" + index, Scene.v().getType("java.lang.String"), index);
			parameterVals.put(index,paramVal);
		}
		return parameterVals.get(index);
	}
	
	private static class ParameterValue extends JimpleLocal{
		private static final long serialVersionUID = 1L;
		private int index;

		public ParameterValue(String name, Type type, int index) {
			super(name, type);
			this.index = index;
		}
		
		public int getIndex(){
			return index;
		}
		
	}

	@Override
	public Field fieldWildCard() {
		return Field.wildcard();
	}

	@Override
	public Field exclusionFieldWildCard(Field exclusion) {
		return Field.exclusionWildcard(exclusion);
	}
}
