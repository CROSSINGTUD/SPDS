package boomerang.solver;

import java.util.AbstractMap;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import com.beust.jcommander.internal.Lists;
import com.beust.jcommander.internal.Sets;
import com.google.common.base.Optional;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;

import boomerang.Boomerang;
import boomerang.Query;
import boomerang.jimple.Field;
import boomerang.jimple.Statement;
import boomerang.jimple.Val;
import heros.InterproceduralCFG;
import soot.RefType;
import soot.SootMethod;
import soot.Unit;
import soot.Value;
import soot.jimple.AssignStmt;
import soot.jimple.InstanceFieldRef;
import soot.jimple.InstanceInvokeExpr;
import soot.jimple.InvokeExpr;
import soot.jimple.StaticFieldRef;
import soot.jimple.Stmt;
import sync.pds.solver.SyncPDSSolver;
import sync.pds.solver.SyncPDSUpdateListener;
import sync.pds.solver.WitnessNode;
import sync.pds.solver.nodes.CallPopNode;
import sync.pds.solver.nodes.GeneratedState;
import sync.pds.solver.nodes.INode;
import sync.pds.solver.nodes.Node;
import wpds.impl.Transition;
import wpds.impl.Weight;
import wpds.impl.WeightedPAutomaton;
import wpds.interfaces.ReachabilityListener;
import wpds.interfaces.State;
import wpds.interfaces.WPAUpdateListener;

public abstract class AbstractBoomerangSolver<W extends Weight> extends SyncPDSSolver<Statement, Val, Field, W>{

	protected final InterproceduralCFG<Unit, SootMethod> icfg;
	protected final Query query;
	private boolean INTERPROCEDURAL = true;
	private Collection<Node<Statement, Val>> fieldFlows = Sets.newHashSet();
	private Collection<RefType> allocationTypes = Sets.newHashSet();
	private Collection<AllocationTypeListener> allocationTypeListeners = Sets.newHashSet();
	private Collection<SootMethod> reachableMethods = Sets.newHashSet();
	private Collection<ReachableMethodListener<W>> reachableMethodListeners = Sets.newHashSet();
	private Collection<SootMethod> unbalancedMethod = Sets.newHashSet();
	private final Map<Entry<INode<Node<Statement,Val>>, Field>, INode<Node<Statement,Val>>> generatedFieldState;
	private Multimap<SootMethod,Transition<Field,INode<Node<Statement,Val>>>> perMethodFieldTransitions = HashMultimap.create();
	private Multimap<SootMethod, MethodBasedFieldTransitionListener<W>> perMethodFieldTransitionsListener = HashMultimap.create();
	private Multimap<Statement,Transition<Field,INode<Node<Statement,Val>>>> perStatementFieldTransitions = HashMultimap.create();
	private Multimap<Statement, StatementBasedFieldTransitionListener<W>> perStatementFieldTransitionsListener = HashMultimap.create();
	
	
	public AbstractBoomerangSolver(InterproceduralCFG<Unit, SootMethod> icfg, Query query, Map<Entry<INode<Node<Statement,Val>>, Field>, INode<Node<Statement,Val>>> genField, Map<Transition<Statement, INode<Val>>, WeightedPAutomaton<Statement, INode<Val>, W>> callSummaries, Map<Transition<Field, INode<Node<Statement, Val>>>, WeightedPAutomaton<Field, INode<Node<Statement, Val>>, W>> fieldSummaries){
		super(callSummaries, fieldSummaries);

		this.icfg = icfg;
		this.query = query;
		this.unbalancedMethod.add(query.asNode().stmt().getMethod());
		this.fieldAutomaton.registerListener(new WPAUpdateListener<Field, INode<Node<Statement,Val>>, W>() {

			@Override
			public void onWeightAdded(Transition<Field, INode<Node<Statement, Val>>> t, W w) {
				addTransitionToMethod(t.getStart().fact().stmt().getMethod(), t);
				addTransitionToMethod(t.getTarget().fact().stmt().getMethod(), t);
				addTransitionToStatement(t.getStart().fact().stmt(), t);
			}
		});
		
		//TODO recap, I assume we can implement this more easily.
		this.generatedFieldState = genField;
	}
	
	@Override
	protected boolean preventCallTransitionAdd(Transition<Statement, INode<Val>> t, W weight) {
		if(t.getStart() instanceof GeneratedState)
			return  false;
		SootMethod m = t.getStart().fact().m();
		SootMethod method = t.getLabel().getMethod();
		if(m == null || method == null)
			return false;
		if(!m.equals(method))
			return true;
		return false;
	}
	
	private void addTransitionToMethod(SootMethod method, Transition<Field, INode<Node<Statement, Val>>> t) {
		if(perMethodFieldTransitions.put(method, t)){
			for(MethodBasedFieldTransitionListener<W> l : Lists.newArrayList(perMethodFieldTransitionsListener.get(method))){
				l.onAddedTransition(t);
			}
		}
	}
	public void registerFieldTransitionListener(MethodBasedFieldTransitionListener<W> l) {
		if(perMethodFieldTransitionsListener.put(l.getMethod(),l)){
			for(Transition<Field, INode<Node<Statement, Val>>> t : Lists.newArrayList(perMethodFieldTransitions.get(l.getMethod()))){
				l.onAddedTransition(t);
			}
		}
	}
	
	private void addTransitionToStatement(Statement s, Transition<Field, INode<Node<Statement, Val>>> t) {
		if(perStatementFieldTransitions.put(s, t)){
			for(StatementBasedFieldTransitionListener<W> l : Lists.newArrayList(perStatementFieldTransitionsListener.get(s))){
				l.onAddedTransition(t);
			}
		}
	}
	public void registerStatementFieldTransitionListener(
			StatementBasedFieldTransitionListener<W> l) {
		if(perStatementFieldTransitionsListener.put(l.getStmt(),l)){
			for(Transition<Field, INode<Node<Statement, Val>>> t : Lists.newArrayList(perStatementFieldTransitions.get(l.getStmt()))){
				l.onAddedTransition(t);
			}
		}	
	}
	public INode<Node<Statement,Val>> generateFieldState(final INode<Node<Statement,Val>> d, final Field loc) {
		Entry<INode<Node<Statement,Val>>, Field> e = new AbstractMap.SimpleEntry<>(d, loc);
		if (!generatedFieldState.containsKey(e)) {
			generatedFieldState.put(e, new GeneratedState<Node<Statement,Val>,Field>(d,loc));
		}
		return generatedFieldState.get(e);
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
			if(curr.containsInvokeExpr() && valueUsedInStatement(curr, value) && INTERPROCEDURAL){
				return callFlow(method, curr, curr.getInvokeExpr(), value);
			} else if(icfg.isExitStmt(curr)){
				return returnFlow(method,curr, value);
			} else{
				return normalFlow(method, curr, value);
			}
		}
		return Collections.emptySet();
	}

	protected abstract void callBypass(Statement callSite, Statement returnSite,  Val value);

	private Collection<State> normalFlow(SootMethod method, Stmt curr, Val value) {
		Set<State> out = Sets.newHashSet();
		for(Unit succ : icfg.getSuccsOf(curr)){
			if(curr.containsInvokeExpr()){
				callBypass(new Statement(curr,method), new Statement((Stmt) succ,method), value);
			}
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
		if(as.getLeftOp() instanceof StaticFieldRef){
			StaticFieldRef staticFieldRef = (StaticFieldRef) as.getLeftOp();
			return new Field(staticFieldRef.getField());
		}
		InstanceFieldRef ifr = (InstanceFieldRef) as.getLeftOp();
		return new Field(ifr.getField());
	}
	protected boolean isFieldWriteWithBase(Stmt curr, Val base) {
		if(curr instanceof AssignStmt){
			AssignStmt as = (AssignStmt) curr;
			if(base.equals(Val.statics())){
				if(as.getLeftOp() instanceof StaticFieldRef){
					return true;
				}
				return false;
			}
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
	
	public boolean valueUsedInStatement(Stmt u, Val value) {
		if(value.equals(Val.statics()))
			return true;
		if(u instanceof AssignStmt && isBackward()){
			AssignStmt assignStmt = (AssignStmt) u;
			if(assignStmt.getLeftOp().equals(value.value()))
				return true;
		}
		if(u.containsInvokeExpr()){
			InvokeExpr invokeExpr = u.getInvokeExpr();
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
				Collection<? extends State> outFlow = computeReturnFlow(method, curr, value, (Stmt) callSite, (Stmt) returnSite);
				onReturnFlow(callSite, returnSite, method, curr, value, outFlow);
				out.addAll(outFlow);
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
					Collection<? extends State> res = computeCallFlow(caller, new Statement((Stmt)returnSite, caller), new Statement((Stmt)callSite, caller), invokeExpr, value, callee, (Stmt) calleeSp);
					onCallFlow(callee, callSite, value, res);
					out.addAll(res);
				}
			}
		}
		for(Unit returnSite : icfg.getSuccsOf(callSite)){
			if(icfg.getCalleesOfCallAt(callSite).isEmpty()){
				out.addAll(computeNormalFlow(caller, (Stmt)callSite, value,(Stmt) returnSite));
			}
			out.addAll(getEmptyCalleeFlow(caller, (Stmt)callSite, value,(Stmt) returnSite));
		}
		return out;
	}


	protected abstract Collection<? extends State> getEmptyCalleeFlow(SootMethod caller, Stmt callSite, Val value,
			Stmt returnSite);

	protected abstract Collection<? extends State> computeCallFlow(SootMethod caller, Statement returnSite, Statement callSite, InvokeExpr invokeExpr,
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
	
	public WeightedPAutomaton<Field, INode<Node<Statement,Val>>, W> getFieldAutomaton(){
		return fieldAutomaton;
	}

	public WeightedPAutomaton<Statement, INode<Val>, W> getCallAutomaton(){
		return callAutomaton;
	}

	public void addFieldAutomatonListener(WPAUpdateListener<Field, INode<Node<Statement, Val>>, W> listener) {
		fieldAutomaton.registerListener(listener);
	}
	public void addCallAutomatonListener(WPAUpdateListener<Statement, INode<Val>, W> listener) {
		callAutomaton.registerListener(listener);
	}
	public void addUnbalancedFlow(SootMethod m, Collection<? extends State> outFlow) {
	}
	public boolean addFieldFlow(Node<Statement, Val> fieldFlow) {
		return fieldFlows.add(fieldFlow);
	}
	
	@Override
	protected void processNode(final WitnessNode<Statement, Val, Field> witnessNode) {
//		if(reachableMethods.contains(witnessNode.stmt().getMethod()) || witnessNode.stmt().getMethod().isStatic()){
			AbstractBoomerangSolver.super.processNode(witnessNode);
//		}
//		else{
//			registerAllocationTypeListener(new AllocationTypeListener() {
//				@Override
//				public void allocationType(RefType type) {
//					for(SootClass c:Scene.v().getActiveHierarchy().getSuperclassesOfIncluding(type.getSootClass())){
//						if(c.getMethods().contains(witnessNode.stmt().getMethod())){
//							AbstractBoomerangSolver.super.processNode(witnessNode);
//						}
//					}
//					
//				}
//			});
//		}
	}
	
	public void registerAllocationTypeListener(AllocationTypeListener listener){
		if(allocationTypeListeners.add(listener)){
			for(RefType type : Lists.newArrayList(allocationTypes)){
				listener.allocationType(type);
			}
		}
	}
	
	public void addAllocatedType(RefType type){
		if(allocationTypes.add(type)){
			for(AllocationTypeListener listener : Lists.newArrayList(allocationTypeListeners)){
				listener.allocationType(type);
			}
		}
	}
	
	protected void onCallFlow(SootMethod callee, Stmt callSite, Val value, Collection<? extends State> res) {
		if(!res.isEmpty()){
			addReachableMethod(callee);
		}
	}

	protected void handleUnbalancedFlow(Unit callSite, Unit returnSite, SootMethod method, Stmt curr, Val value,
			Collection<? extends State> outFlow) {
		if(unbalancedMethod.contains(method)){
			SootMethod caller = icfg.getMethodOf(callSite);
			unbalancedMethod.add(caller);
			addUnbalancedFlow(method, outFlow);
		}
	}
	
	protected void addReachableMethod(SootMethod m){
		if(reachableMethods.add(m)){
			for(ReachableMethodListener<W> l : Lists.newArrayList(reachableMethodListeners)){
				l.reachable(this, m);
			}
		}
	}
	public void registerReachableMethodListener(ReachableMethodListener<W> listener){
		if(reachableMethodListeners.add(listener)){
			for(SootMethod m : Lists.newArrayList(reachableMethods)){
				listener.reachable(this, m);
			}
		}
	}
	public Set<Statement> getSuccsOf(Statement stmt) {
		Set<Statement> res = Sets.newHashSet();
		if(!stmt.getUnit().isPresent())
			return res;
		Stmt curr = stmt.getUnit().get();
		for(Unit succ : icfg.getSuccsOf(curr)){
			res.add(new Statement((Stmt) succ, icfg.getMethodOf(succ)));
		}
		return res;	
	}
	
	@Override
	public String toString() {
		return "Solver for: " + query.toString();
	}
	

	protected void onReturnFlow(final Unit callSite, Unit returnSite, final SootMethod method, final Stmt returnStmt, final Val value,
			Collection<? extends State> outFlow) {
		handleUnbalancedFlow(callSite, returnSite, method, returnStmt, value, outFlow);
		for(State r : outFlow){
			if(r instanceof CallPopNode){
				final CallPopNode<Val,Statement> callPopNode = (CallPopNode) r;
				this.registerListener(new SyncPDSUpdateListener<Statement, Val, Field>() {
					
					@Override
					public void onReachableNodeAdded(WitnessNode<Statement, Val, Field> reachableNode) {
						if(reachableNode.asNode().equals(new Node<Statement,Val>(callPopNode.getReturnSite(),callPopNode.location()))){
							if(!valueUsedInStatement((Stmt)callSite,  callPopNode.location())){
								//TODO why do we need this?
								return;
							}
							onReturnFromCall(new Statement((Stmt) callSite, icfg.getMethodOf(callSite)), callPopNode.getReturnSite(), reachableNode.asNode(),unbalancedMethod.contains(method));
						}
					}
				});
			}
		}
	}

	protected abstract void onReturnFromCall(Statement statement, Statement returnSite,  Node<Statement, Val> node, boolean unbalanced);
	
	
	

	public void debugFieldAutomaton(final Statement statement) {
		if(!Boomerang.DEBUG)
			return;
		final WeightedPAutomaton<Field, INode<Node<Statement,Val>>, Weight> weightedPAutomaton = new WeightedPAutomaton<Field, INode<Node<Statement,Val>>, Weight>(){

			@Override
			public Field epsilon() {
				return Field.epsilon();
			}

			@Override
			public INode<Node<Statement, Val>> createState(INode<Node<Statement, Val>> d, Field loc) {
				// TODO Auto-generated method stub
				return null;
			}

			@Override
			public Weight getZero() {
				return getCallWeights().getZero();
			}

			@Override
			public Weight getOne() {
				return getCallWeights().getOne();
			}

			@Override
			public boolean isGeneratedState(INode<Node<Statement, Val>> d) {
				return d instanceof GeneratedState;
			}};
		fieldAutomaton.registerListener(new WPAUpdateListener<Field, INode<Node<Statement,Val>>, W>() {
			
			@Override
			public void onWeightAdded(Transition<Field, INode<Node<Statement, Val>>> t, W w) {
				if(t.getStart().fact().stmt().equals(statement) && !(t.getStart() instanceof GeneratedState)){
					fieldAutomaton.registerDFSListener(t.getStart(),new ReachabilityListener<Field, INode<Node<Statement,Val>>>() {
						@Override
						public void reachable(Transition<Field, INode<Node<Statement,Val>>> t) {
							weightedPAutomaton.addTransition(t);
						}
					});
				}
			}
			
		});
		if(!weightedPAutomaton.getTransitions().isEmpty()){
			System.out.println(statement);
			System.out.println(weightedPAutomaton.toDotString());
			}
		}

	public Map<Transition<Statement, INode<Val>>, W> getTransitionsToFinalWeights() {
		return callAutomaton.getTransitionsToFinalWeights();
	}


}
