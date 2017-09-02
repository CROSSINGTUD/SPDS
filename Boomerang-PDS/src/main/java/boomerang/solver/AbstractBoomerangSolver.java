package boomerang.solver;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import com.beust.jcommander.internal.Lists;
import com.beust.jcommander.internal.Sets;
import com.google.common.base.Optional;

import boomerang.Query;
import boomerang.jimple.Field;
import boomerang.jimple.ReturnSite;
import boomerang.jimple.Statement;
import boomerang.jimple.Val;
import heros.InterproceduralCFG;
import soot.SootMethod;
import soot.Unit;
import soot.Value;
import soot.jimple.AssignStmt;
import soot.jimple.InstanceFieldRef;
import soot.jimple.InstanceInvokeExpr;
import soot.jimple.InvokeExpr;
import soot.jimple.Stmt;
import sync.pds.solver.SyncPDSSolver;
import sync.pds.solver.nodes.INode;
import sync.pds.solver.nodes.Node;
import wpds.impl.Rule;
import wpds.impl.Weight;
import wpds.impl.WeightedPAutomaton;
import wpds.interfaces.State;
import wpds.interfaces.WPAUpdateListener;

public abstract class AbstractBoomerangSolver extends SyncPDSSolver<Statement, Val, Field>{

	protected final InterproceduralCFG<Unit, SootMethod> icfg;
	protected final Query query;
	private boolean INTERPROCEDURAL = true;
	
	
	public AbstractBoomerangSolver(InterproceduralCFG<Unit, SootMethod> icfg, Query query){
		this.icfg = icfg;
		this.query = query;
	}
	@Override
	public Collection<? extends State> computeSuccessor(Node<Statement, Val> node) {
		Statement stmt = node.stmt();
		Optional<Stmt> unit = stmt.getUnit();
		if(unit.isPresent()){
			Stmt curr = unit.get();
			Val value = node.fact();
			SootMethod method = icfg.getMethodOf(curr);
			if(killFlow(method, curr, value)){
				return Collections.emptySet();
			}
			if(curr.containsInvokeExpr() && valueUsedInStatement(method,curr,curr.getInvokeExpr(), value) && INTERPROCEDURAL){
				return callFlow(method, curr, curr.getInvokeExpr(), value);
			} else if(icfg.isExitStmt(curr)){
				return returnFlow(method,curr, value);
			} else{
				return normalFlow(method, curr, value);
			}
		}
		return Collections.emptySet();
	}

	private Collection<State> normalFlow(SootMethod method, Stmt curr, Val value) {
		Set<State> out = Sets.newHashSet();
		for(Unit succ : icfg.getSuccsOf(curr)){
			out.addAll(computeNormalFlow(method,curr, value, (Stmt) succ));
		}
		List<Unit> succsOf = icfg.getSuccsOf(curr);
		while(out.size() == 1 && succsOf.size() == 1){
			List<State> l = Lists.newArrayList(out);
			State state = l.get(0);
			Unit succ = succsOf.get(0);
			if(!state.equals(new Node<Statement,Val>(new Statement((Stmt)succ,method),value)))
				break;
			out.clear();
			out.addAll(computeNormalFlow(method,curr, value, (Stmt) succ));
			succsOf = icfg.getSuccsOf(succ);
			curr = (Stmt) succ;
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
	protected abstract boolean killFlow(SootMethod method, Stmt curr, Val value);
	
	private boolean valueUsedInStatement(SootMethod method, Stmt u, InvokeExpr invokeExpr, Val value) {
		if(u instanceof AssignStmt && isBackward()){
			AssignStmt assignStmt = (AssignStmt) u;
			if(assignStmt.getLeftOp().equals(value.value()))
				return true;
		}
		if(invokeExpr instanceof InstanceInvokeExpr){
			InstanceInvokeExpr iie = (InstanceInvokeExpr) invokeExpr;
			if(iie.getBase().equals(value.value()))
				return true;
		}
		for(Value arg : invokeExpr.getArgs()){
			if(arg.equals(value.value())){
				return true;
			}
		}
		return false;
	}
	
	private boolean isBackward(){
		return this instanceof BackwardBoomerangSolver;
	}
	
	protected abstract Collection<? extends State> computeReturnFlow(SootMethod method, Stmt curr, Val value, Stmt callSite, Stmt returnSite);

	private Collection<? extends State> returnFlow(SootMethod method, Stmt curr, Val value) {
		Set<State> out = Sets.newHashSet();
		for(Unit callSite : icfg.getCallersOf(method)){
			for(Unit returnSite : icfg.getSuccsOf(callSite)){
				out.addAll(computeReturnFlow(method, curr, value, (Stmt) callSite, (Stmt) returnSite));
			}
		}
		return out;
	}
	
	private Collection<State> callFlow(SootMethod caller, Stmt callSite, InvokeExpr invokeExpr, Val value) {
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
			Val value, SootMethod callee, Stmt calleeSp);
	protected abstract Collection<State> computeNormalFlow(SootMethod method, Stmt curr, Val value, Stmt succ);

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
	
	@Override
	public Field fieldWildCard() {
		return Field.wildcard();
	}

	@Override
	public Field exclusionFieldWildCard(Field exclusion) {
		return Field.exclusionWildcard(exclusion);
	}
	
	public WeightedPAutomaton<Field, INode<Node<Statement,Val>>, Weight<Field>> getFieldAutomaton(){
		return fieldAutomaton;
	}

	public void injectFieldRule(Node<Statement,Val> source, Field field, Node<Statement,Val> target){
		processPush(source, field, target, PDSSystem.FIELDS);
	}
	public void injectFieldRule(Rule<Field, INode<Node<Statement,Val>>, Weight<Field>> rule){
		fieldPDS.addRule(rule);
	}

	public void addFieldAutomatonListener(WPAUpdateListener<Field, INode<Node<Statement, Val>>, Weight<Field>> listener) {
		fieldAutomaton.registerListener(listener);
	}
	public void addCallAutomatonListener(WPAUpdateListener<Statement, INode<Val>, Weight<Statement>> listener) {
		callAutomaton.registerListener(listener);
	}
}
