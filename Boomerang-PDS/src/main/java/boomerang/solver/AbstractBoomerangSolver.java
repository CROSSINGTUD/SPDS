package boomerang.solver;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

import com.beust.jcommander.internal.Sets;
import com.google.common.base.Optional;
import com.google.common.collect.Maps;

import boomerang.jimple.Field;
import boomerang.jimple.ReturnSite;
import boomerang.jimple.Statement;
import heros.InterproceduralCFG;
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
import soot.jimple.Stmt;
import soot.jimple.internal.JimpleLocal;
import sync.pds.solver.SyncPDSSolver;
import sync.pds.solver.nodes.INode;
import sync.pds.solver.nodes.Node;
import wpds.impl.Rule;
import wpds.impl.Weight;
import wpds.impl.WeightedPAutomaton;
import wpds.interfaces.State;
import wpds.interfaces.WPAUpdateListener;

public abstract class AbstractBoomerangSolver extends SyncPDSSolver<Statement, Value, Field>{

	private static Local returnVal;
	private static Value thisVal;
	private static Map<Integer,Value> parameterVals = Maps.newHashMap();
	protected final InterproceduralCFG<Unit, SootMethod> icfg;
	private boolean INTERPROCEDURAL = true;
	
	public AbstractBoomerangSolver(InterproceduralCFG<Unit, SootMethod> icfg){
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
			if(curr.containsInvokeExpr() && valueUsedInStatement(method,curr,curr.getInvokeExpr(), value) && INTERPROCEDURAL){
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
			out.addAll(computeNormalFlow(method,curr, fact, (Stmt) succ));
		}
		return out;
	}
	protected Field getWrittenField(Stmt curr) {
		AssignStmt as = (AssignStmt) curr;
		InstanceFieldRef ifr = (InstanceFieldRef) as.getLeftOp();
		return new Field(ifr.getField());
	}
	protected boolean isFieldWriteWithBase(Stmt curr, Value base) {
		if(curr instanceof AssignStmt){
			AssignStmt as = (AssignStmt) curr;
			if(as.getLeftOp() instanceof InstanceFieldRef){
				InstanceFieldRef ifr = (InstanceFieldRef) as.getLeftOp();
				return ifr.getBase().equals(base);
			}
		}
		return false;
	}
	
	protected Field getLoadedField(Stmt curr) {
		AssignStmt as = (AssignStmt) curr;
		InstanceFieldRef ifr = (InstanceFieldRef) as.getRightOp();
		return new Field(ifr.getField());
	}
	protected boolean isFieldLoadWithBase(Stmt curr, Value base) {
		if(curr instanceof AssignStmt){
			AssignStmt as = (AssignStmt) curr;
			if(as.getRightOp() instanceof InstanceFieldRef){
				InstanceFieldRef ifr = (InstanceFieldRef) as.getRightOp();
				return ifr.getBase().equals(base);
			}
		}
		return false;
	}
	protected abstract boolean killFlow(Stmt curr, Value value);
	
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
	protected abstract Collection<? extends State> computeReturnFlow(SootMethod method, Stmt curr, Value value);
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


	protected abstract Collection<? extends State> computeCallFlow(SootMethod caller, ReturnSite returnSite, InvokeExpr invokeExpr,
			Value value, SootMethod callee, Stmt calleeSp);
	protected abstract Collection<State> computeNormalFlow(SootMethod method, Stmt curr, Value fact, Stmt succ);

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
	
	protected static Value returnVal(){
		if(returnVal == null)
			returnVal = Jimple.v().newLocal("RET", Scene.v().getType("java.lang.String"));
		return returnVal;
	}
	
	protected static Value thisVal(){
		if(thisVal == null)
			thisVal = Jimple.v().newLocal("THIS", Scene.v().getType("java.lang.String"));
		return thisVal;
	}
	
	protected static Value param(int index){
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
	
	public WeightedPAutomaton<Field, INode<Node<Statement,Value>>, Weight<Field>> getFieldAutomaton(){
		return fieldAutomaton;
	}

	public void injectFieldRule(Node<Statement,Value> source, Field field, Node<Statement,Value> target){
		processPush(source, field, target, PDSSystem.FIELDS);
	}
	public void injectFieldRule(Rule<Field, INode<Node<Statement,Value>>, Weight<Field>> rule){
		fieldPDS.addRule(rule);
	}

	public void addFieldAutomatonListener(WPAUpdateListener<Field, INode<Node<Statement, Value>>, Weight<Field>> listener) {
		fieldAutomaton.registerListener(listener);
	}
	public void addCallAutomatonListener(WPAUpdateListener<Statement, INode<Value>, Weight<Statement>> listener) {
		callAutomaton.registerListener(listener);
	}
}
