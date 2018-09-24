/*******************************************************************************
 * Copyright (c) 2018 Fraunhofer IEM, Paderborn, Germany.
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *  
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Johannes Spaeth - initial API and implementation
 *******************************************************************************/
package boomerang.solver;

import java.util.AbstractMap;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.common.base.Optional;
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;
import com.google.common.collect.Table;

import boomerang.BoomerangOptions;
import boomerang.Query;
import boomerang.callgraph.CalleeListener;
import boomerang.callgraph.CallerListener;
import boomerang.callgraph.ObservableICFG;
import boomerang.jimple.AllocVal;
import boomerang.jimple.Field;
import boomerang.jimple.Statement;
import boomerang.jimple.Val;
import boomerang.util.RegExAccessPath;
import pathexpression.IRegEx;
import soot.NullType;
import soot.RefType;
import soot.Scene;
import soot.SootMethod;
import soot.Type;
import soot.Unit;
import soot.Value;
import soot.jimple.AssignStmt;
import soot.jimple.CastExpr;
import soot.jimple.InstanceFieldRef;
import soot.jimple.InstanceInvokeExpr;
import soot.jimple.InvokeExpr;
import soot.jimple.NewExpr;
import soot.jimple.StaticFieldRef;
import soot.jimple.Stmt;
import sync.pds.solver.SyncPDSSolver;
import sync.pds.solver.nodes.GeneratedState;
import sync.pds.solver.nodes.INode;
import sync.pds.solver.nodes.Node;
import sync.pds.solver.nodes.SingleNode;
import wpds.impl.NestedWeightedPAutomatons;
import wpds.impl.NormalRule;
import wpds.impl.PopRule;
import wpds.impl.Rule;
import wpds.impl.Transition;
import wpds.impl.Weight;
import wpds.impl.WeightedPAutomaton;
import wpds.impl.WeightedPushdownSystem;
import wpds.interfaces.State;
import wpds.interfaces.WPAUpdateListener;

public abstract class AbstractBoomerangSolver<W extends Weight> extends SyncPDSSolver<Statement, Val, Field, W> {

	private static final Logger logger = LogManager.getLogger();
	protected final ObservableICFG<Unit, SootMethod> icfg;
	protected final Query query;
	private boolean INTERPROCEDURAL = true;
	protected final Map<Entry<INode<Node<Statement, Val>>, Field>, INode<Node<Statement, Val>>> generatedFieldState;
	private Multimap<SootMethod, Transition<Field, INode<Node<Statement, Val>>>> perMethodFieldTransitions = HashMultimap
			.create();
	private Multimap<SootMethod, MethodBasedFieldTransitionListener<W>> perMethodFieldTransitionsListener = HashMultimap
			.create();
	private Multimap<Statement, Transition<Field, INode<Node<Statement, Val>>>> perStatementFieldTransitions = HashMultimap
			.create();
	private Multimap<Statement, StatementBasedFieldTransitionListener<W>> perStatementFieldTransitionsListener = HashMultimap
			.create();
	private HashBasedTable<Statement, Transition<Statement, INode<Val>>,W> perStatementCallTransitions = HashBasedTable.create();
	private Multimap<Statement, StatementBasedCallTransitionListener<W>> perStatementCallTransitionsListener = HashMultimap
			.create();
	private Set<ReachableMethodListener<W>> reachableMethodListeners = Sets.newHashSet();
	private Multimap<SootMethod, Runnable> queuedReachableMethod = HashMultimap.create();
	private Collection<SootMethod> reachableMethods = Sets.newHashSet();
	protected final BoomerangOptions options;

	public AbstractBoomerangSolver(ObservableICFG<Unit, SootMethod> icfg,
			Query query, Map<Entry<INode<Node<Statement, Val>>, Field>, INode<Node<Statement, Val>>> genField,
			BoomerangOptions options, NestedWeightedPAutomatons<Statement, INode<Val>, W> callSummaries,
			 NestedWeightedPAutomatons<Field, INode<Node<Statement, Val>>, W> fieldSummaries) {
		super(new SingleNode<Val>(query.asNode().fact()), new SingleNode<Node<Statement, Val>>(query.asNode()),
				options.callSummaries(), callSummaries, options.fieldSummaries(), fieldSummaries);
		this.options = options;
		this.icfg = icfg;
		this.query = query;
		this.fieldAutomaton.registerListener(new WPAUpdateListener<Field, INode<Node<Statement, Val>>, W>() {

			@Override
			public void onWeightAdded(Transition<Field, INode<Node<Statement, Val>>> t, W w,
					WeightedPAutomaton<Field, INode<Node<Statement, Val>>, W> aut) {
				addTransitionToMethod(t.getStart().fact().stmt().getMethod(), t);
				addTransitionToMethod(t.getTarget().fact().stmt().getMethod(), t);
				addTransitionToStatement(t.getStart().fact().stmt(), t);
			}
		});
		this.callAutomaton.registerListener(new WPAUpdateListener<Statement, INode<Val>, W>() {
			@Override
			public void onWeightAdded(Transition<Statement, INode<Val>> t, W w,
					WeightedPAutomaton<Statement, INode<Val>, W> aut) {
				addCallTransitionToStatement(t.getLabel(),t, w);
			}
		});
		// TODO recap, I assume we can implement this more easily.
		this.generatedFieldState = genField;
		addReachable(query.asNode().stmt().getMethod());
	}
	

	@Override
	protected boolean preventCallTransitionAdd(Transition<Statement, INode<Val>> t, W weight) {
		if (t.getStart() instanceof GeneratedState)
			return false;
		Val fact = t.getStart().fact();
		
		if(fact.isStatic())
			return false;
		if (callAutomaton.isUnbalancedState(t.getStart()) && callAutomaton.isUnbalancedState(t.getTarget()))
			return false;
		SootMethod m = fact.m();
		SootMethod method = t.getLabel().getMethod();
		if (m == null || method == null)
			return false;
		if (!m.equals(method))
			return true;
		return false;
	}

	@Override
	public void addCallRule(final Rule<Statement, INode<Val>, W> rule) {
		if(rule instanceof NormalRule){
			if(rule.getL1().equals(rule.getL2()) && rule.getS1().equals(rule.getS2()))
				return;
		}
		if(rule instanceof PopRule)
			super.addCallRule(rule);
		else
			submit(rule.getS2().fact().m(), new Runnable() {
				@Override
				public void run() {
					AbstractBoomerangSolver.super.addCallRule(rule);
				}
			});
		}

	@Override
	public void addFieldRule(final Rule<Field, INode<Node<Statement, Val>>, W> rule) {
		if(rule instanceof NormalRule){
			if(rule.getL1().equals(rule.getL2()) && rule.getS1().equals(rule.getS2()))
				return;
		}
		submit(rule.getS2().fact().stmt().getMethod(), new Runnable() {
				@Override
				public void run() {
					AbstractBoomerangSolver.super.addFieldRule(rule);
		}
		});
	}

	private void addTransitionToMethod(SootMethod method, Transition<Field, INode<Node<Statement, Val>>> t) {
		if (perMethodFieldTransitions.put(method, t)) {
			for (MethodBasedFieldTransitionListener<W> l : Lists
					.newArrayList(perMethodFieldTransitionsListener.get(method))) {
				l.onAddedTransition(t);
			}
		}
	}

	public void registerFieldTransitionListener(MethodBasedFieldTransitionListener<W> l) {
		if (perMethodFieldTransitionsListener.put(l.getMethod(), l)) {
			for (Transition<Field, INode<Node<Statement, Val>>> t : Lists
					.newArrayList(perMethodFieldTransitions.get(l.getMethod()))) {
				l.onAddedTransition(t);
			}
		}
	}

	private void addTransitionToStatement(Statement s, Transition<Field, INode<Node<Statement, Val>>> t) {
		if (perStatementFieldTransitions.put(s, t)) {
			for (StatementBasedFieldTransitionListener<W> l : Lists
					.newArrayList(perStatementFieldTransitionsListener.get(s))) {
				l.onAddedTransition(t);
			}
		}
	}

	public void registerStatementFieldTransitionListener(StatementBasedFieldTransitionListener<W> l) {
		if (perStatementFieldTransitionsListener.put(l.getStmt(), l)) {
			for (Transition<Field, INode<Node<Statement, Val>>> t : Lists
					.newArrayList(perStatementFieldTransitions.get(l.getStmt()))) {
				l.onAddedTransition(t);
			}
		}
	}
	
	private void addCallTransitionToStatement(Statement s, Transition<Statement, INode<Val>> t, W w) {
		W put = perStatementCallTransitions.get(s, t);
		if(put != null) {
			W combineWith = (W) put.combineWith(w);
			if(!combineWith.equals(put)) {
				perStatementCallTransitions.put(s, t,combineWith);
				for (StatementBasedCallTransitionListener<W> l : Lists
						.newArrayList(perStatementCallTransitionsListener.get(s))) {
					l.onAddedTransition(t,w);
				}
			}
		} else {
			perStatementCallTransitions.put(s, t,w);
			for (StatementBasedCallTransitionListener<W> l : Lists
					.newArrayList(perStatementCallTransitionsListener.get(s))) {
				l.onAddedTransition(t,w);
			}
		}
	}

	public void registerStatementCallTransitionListener(StatementBasedCallTransitionListener<W> l) {
		if (perStatementCallTransitionsListener.put(l.getStmt(), l)) {
			Map<Transition<Statement, INode<Val>>, W> row = perStatementCallTransitions.row(l.getStmt());
			for (Entry<Transition<Statement, INode<Val>>, W> t : Lists
					.newArrayList(row.entrySet())) {
				l.onAddedTransition(t.getKey(),t.getValue());
			}
		}
	}
	public INode<Node<Statement, Val>> generateFieldState(final INode<Node<Statement, Val>> d, final Field loc) {
		Entry<INode<Node<Statement, Val>>, Field> e = new AbstractMap.SimpleEntry<>(d, loc);
		if (!generatedFieldState.containsKey(e)) {
			generatedFieldState.put(e, new GeneratedState<Node<Statement, Val>, Field>(d, loc));
		}
		return generatedFieldState.get(e);
	}

	@Override
	public void computeSuccessor(Node<Statement, Val> node) {
		Statement stmt = node.stmt();
		Optional<Stmt> unit = stmt.getUnit();
		logger.trace("Computing successor for {} with solver {}", node, this);
		if (unit.isPresent()) {
			Stmt curr = unit.get();
			Val value = node.fact();
			SootMethod method = icfg.getMethodOf(curr);
			if(method == null)
				return;
			if (killFlow(method, curr, value)) {
				return;
			}
			if(options.isIgnoredMethod(method)){
				return;
			}
			Collection<? extends State> out;
			if (curr.containsInvokeExpr() && valueUsedInStatement(curr, value) && INTERPROCEDURAL) {
				out = callFlow(method, node);
			} else if (icfg.isExitStmt(curr)) {
				out =  returnFlow(method, node);
			} else {
				out =  normalFlow(method, curr, value);
			}
			for(State s : out) {
				propagate(node, s);
			}
		}
	}

	protected abstract void callBypass(Statement callSite, Statement returnSite, Val value);

	private Collection<State> normalFlow(SootMethod method, Stmt curr, Val value) {
		Set<State> out = Sets.newHashSet();
		for (Unit succ : icfg.getSuccsOf(curr)) {
			if (curr.containsInvokeExpr()) {
				callBypass(new Statement(curr, method), new Statement((Stmt) succ, method), value);
			}
			Collection<State> flow = computeNormalFlow(method, curr, value, (Stmt) succ);
			if(options.fastForwardFlows() && isIdentityFlow(value,  (Stmt) succ,method, flow)){
				flow = dfs( value,  (Stmt) succ,method);
			}
			out.addAll(flow);
		}
		return out;
	}

	private Collection<State> dfs(Val value, Stmt succ, SootMethod method) {
		LinkedList<Unit> worklist = Lists.newLinkedList();
		worklist.add(succ);
		Set<Unit> visited = Sets.newHashSet();
		Collection<State> out = Sets.newHashSet(); 
		while(!worklist.isEmpty()){
			Unit curr = worklist.poll();
			if(!visited.add(curr))
				continue;
			for(Unit s : icfg.getSuccsOf(curr)){
				Collection<State> flow = computeNormalFlow(method, (Stmt) curr, value, (Stmt) s);
				if(!isIdentityFlow( value,  (Stmt) s,method, flow)){
					out.add(new Node<Statement, Val>(new Statement((Stmt) curr, method), value));
				} else{
					worklist.add(s);
				}
			}
		}
		return out;
	}

	private boolean isIdentityFlow(Val value, Stmt succ, SootMethod method, Collection<State> out){
		if(out.size() != 1 || succ.containsInvokeExpr() || icfg.isExitStmt(succ))
			return false;
		if(value.isStatic()){
			if(containsStaticFieldAccess(succ)){
				return false;
			}
		} else if(succ.containsFieldRef()){
			return false;
		}
		List<State> l = Lists.newArrayList(out);
		State state = l.get(0);
		return state.equals(new Node<Statement, Val>(new Statement((Stmt) succ, method), value));
	}
	private boolean containsStaticFieldAccess(Stmt succ) {
		if(succ instanceof AssignStmt){
			AssignStmt assignStmt = (AssignStmt) succ;
			return assignStmt.getLeftOp() instanceof StaticFieldRef || assignStmt.getRightOp() instanceof StaticFieldRef;
		}
		return false;
	}

	protected Field getWrittenField(Stmt curr) {
		AssignStmt as = (AssignStmt) curr;
		if (as.getLeftOp() instanceof StaticFieldRef) {
			StaticFieldRef staticFieldRef = (StaticFieldRef) as.getLeftOp();
			return new Field(staticFieldRef.getField());
		}
		InstanceFieldRef ifr = (InstanceFieldRef) as.getLeftOp();
		return new Field(ifr.getField());
	}

	protected boolean isFieldWriteWithBase(Stmt curr, Val base) {
		if (curr instanceof AssignStmt) {
			AssignStmt as = (AssignStmt) curr;
			if (as.getLeftOp() instanceof InstanceFieldRef) {
				InstanceFieldRef ifr = (InstanceFieldRef) as.getLeftOp();
				return ifr.getBase().equals(base.value());
			}
		}
		return false;
	}

	protected Field getLoadedField(Stmt curr) {
		AssignStmt as = (AssignStmt) curr;
		InstanceFieldRef ifr = (InstanceFieldRef) as.getRightOp();
		return new Field(ifr.getField());
	}

	protected boolean isFieldLoadWithBase(Stmt curr, Val base) {
		if (curr instanceof AssignStmt) {
			AssignStmt as = (AssignStmt) curr;
			if (as.getRightOp() instanceof InstanceFieldRef) {
				InstanceFieldRef ifr = (InstanceFieldRef) as.getRightOp();
				return ifr.getBase().equals(base);
			}
		}
		return false;
	}

	protected abstract boolean killFlow(SootMethod method, Stmt curr, Val value);

	public boolean valueUsedInStatement(Stmt u, Val value) {
		if (value.isStatic())
			return true;
		if (isBackward()) {
			if (assignsValue(u, value))
				return true;
		}
		if (u.containsInvokeExpr()) {
			InvokeExpr invokeExpr = u.getInvokeExpr();
			if (invokeExpr instanceof InstanceInvokeExpr) {
				InstanceInvokeExpr iie = (InstanceInvokeExpr) invokeExpr;
				if (iie.getBase().equals(value.value()))
					return true;
			}
			for (Value arg : invokeExpr.getArgs()) {
				if (arg.equals(value.value())) {
					return true;
				}
			}
		}
		return false;
	}
	public static boolean assignsValue(Stmt u, Val value){
		if(u instanceof AssignStmt){
			AssignStmt assignStmt = (AssignStmt) u;
			if (assignStmt.getLeftOp().equals(value.value()))
				return true;
		}
		return false;
	}
	private boolean isBackward() {
		return this instanceof BackwardBoomerangSolver;
	}

	protected abstract Collection<? extends State> computeReturnFlow(SootMethod method, Stmt curr, Val value,
			Stmt callSite, Stmt returnSite);

	private Collection<? extends State> returnFlow(SootMethod method, Node<Statement, Val> currNode) {
		Val value = currNode.fact();
		Stmt curr = currNode.stmt().getUnit().get();
		Set<State> out = Sets.newHashSet();
		if(method.isStaticInitializer() && value.isStatic()){
			for(SootMethod entryPoint : Scene.v().getEntryPoints()){
				for(Unit sp : icfg.getStartPointsOf(entryPoint)){
					Collection<? extends State> outFlow = computeReturnFlow(method, curr, value, (Stmt) sp,
							(Stmt) sp);
					out.addAll(outFlow);
				}
			}
		} else{
			if (icfg.isMethodsWithCallFlow(method)){
				icfg.addCallerListener(new ReturnFlowCallerListener(method, currNode));
			} else {
				//Unbalanced call which we did not observe a flow to previously
				for (Unit unit : icfg.getAllPrecomputedCallers(method)){
					if (((Stmt) unit).containsInvokeExpr()){
						for (Unit returnSite : icfg.getSuccsOf(unit)) {
							Collection<? extends State> outFlow = computeReturnFlow(method, curr, value, (Stmt) unit,
									(Stmt) returnSite);
							out.addAll(outFlow);
						}
					}
				}
			}
		}
		return out;
	}

	private class ReturnFlowCallerListener implements CallerListener<Unit, SootMethod>{
		SootMethod method;
		private Node<Statement, Val> curr;

		ReturnFlowCallerListener(SootMethod method, Node<Statement,Val> curr){
			this.method = method;
			this.curr = curr;
		}


		@Override
		public SootMethod getObservedCallee() {
			return method;
		}

		@Override
		public void onCallerAdded(Unit unit, SootMethod sootMethod) {
			if (((Stmt) unit).containsInvokeExpr()){
				for (Unit returnSite : icfg.getSuccsOf(unit)) {
					Collection<? extends State> outFlow = computeReturnFlow(method, curr.stmt().getUnit().get(), curr.fact(), (Stmt) unit,
							(Stmt) returnSite);
					for(State s : outFlow) {
						AbstractBoomerangSolver.this.propagate(curr, s);
					}
				}
			}
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || getClass() != o.getClass()) return false;
			ReturnFlowCallerListener that = (ReturnFlowCallerListener) o;
			return Objects.equals(method, that.method) &&
					Objects.equals(curr, that.curr);
		}

		@Override
		public int hashCode() {
			return Objects.hash(method, curr);
		}
	}

	private Collection<? extends State> callFlow(SootMethod caller, Node<Statement,Val> curr) {
		assert icfg.isCallStmt(curr.stmt().getUnit().get());
		Val value = curr.fact();
		Stmt callSite = curr.stmt().getUnit().get();
		if (!curr.fact().isStatic()){
			icfg.addCalleeListener(new CallFlowCalleeListener(curr, caller));
		}
		Set<State> out = Sets.newHashSet();
		//TODO Melanie: Revisited and check if the callFlow also needs to be added for static values
		for (Unit returnSite : icfg.getSuccsOf(callSite)) {
			out.addAll(getEmptyCalleeFlow(caller, callSite, value, (Stmt) returnSite));
		}
		//If there is no flow yet, add a normal flow. Note that another call flow might be added later
		//TODO Melanie: Check if we need to get rid of the extra normal flow later.
		if (out.isEmpty()){
			for (Unit returnSite : icfg.getSuccsOf(callSite)) {
				out.addAll(computeNormalFlow(caller,callSite, value, (Stmt) returnSite));
			}
		}
		return out;
	}

	private class CallFlowCalleeListener implements CalleeListener<Unit, SootMethod>{
		private final SootMethod caller;
		private final Node<Statement, Val> curr;

		CallFlowCalleeListener(Node<Statement,Val> curr, SootMethod caller){
			this.curr = curr;
			this.caller = caller;
		}

		@Override
		public Unit getObservedCaller() {
			return curr.stmt().getUnit().get();
		}

		@Override
		public void onCalleeAdded(Unit unit, SootMethod sootMethod) {
			Stmt callSite = curr.stmt().getUnit().get();
			Val value = curr.fact();
			InvokeExpr invokeExpr = callSite.getInvokeExpr();
			for (Unit calleeSp : icfg.getStartPointsOf(sootMethod)) {
				for (Unit returnSite : icfg.getSuccsOf(callSite)) {
					Collection<? extends State> res = computeCallFlow(caller, new Statement((Stmt) returnSite, caller),
							new Statement(callSite, caller), invokeExpr, value, sootMethod, (Stmt) calleeSp);
					onCallFlow(sootMethod, callSite, value, res);
					for(State s : res) {
						propagate(curr, s);
					}
				}
			}
			addReachable(sootMethod);
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || getClass() != o.getClass()) return false;
			CallFlowCalleeListener that = (CallFlowCalleeListener) o;
				return	Objects.equals(caller, that.caller);
					
		}

		@Override
		public int hashCode() {
			return Objects.hash(caller, curr);
		}
	}

	protected abstract Collection<? extends State> getEmptyCalleeFlow(SootMethod caller, Stmt callSite, Val value,
			Stmt returnSite);

	protected abstract Collection<? extends State> computeCallFlow(SootMethod caller, Statement returnSite,
			Statement callSite, InvokeExpr invokeExpr, Val value, SootMethod callee, Stmt calleeSp);

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

	public WeightedPAutomaton<Field, INode<Node<Statement, Val>>, W> getFieldAutomaton() {
		return fieldAutomaton;
	}

	public WeightedPAutomaton<Statement, INode<Val>, W> getCallAutomaton() {
		return callAutomaton;
	}
	
	public WeightedPushdownSystem<Statement, INode<Val>, W> getCallPDS(){
		return callingPDS;
	}
	
	public WeightedPushdownSystem<Field, INode<Node<Statement, Val>>, W> getFieldPDS(){
		return fieldPDS;
	}

	@Override
	protected void processNode(final Node<Statement, Val> witnessNode) {
		submit(witnessNode.stmt().getMethod(), new Runnable() {
			@Override
			public void run() {
				AbstractBoomerangSolver.super.processNode(witnessNode);
			}
		});

	}

	protected void onCallFlow(SootMethod callee, Stmt callSite, Val value, Collection<? extends State> res){
		icfg.addMethodWithCallFlow(callee);
	}

	public Set<Statement> getSuccsOf(Statement stmt) {
		Set<Statement> res = Sets.newHashSet();
		if (!stmt.getUnit().isPresent())
			return res;
		Stmt curr = stmt.getUnit().get();
		for (Unit succ : icfg.getSuccsOf(curr)) {
			res.add(new Statement((Stmt) succ, icfg.getMethodOf(succ)));
		}
		return res;
	}
	public Set<Statement> getPredsOf(Statement stmt) {
		Set<Statement> res = Sets.newHashSet();
		if (!stmt.getUnit().isPresent())
			return res;
		Stmt curr = stmt.getUnit().get();
		for (Unit succ : icfg.getPredsOf(curr)) {
			res.add(new Statement((Stmt) succ, icfg.getMethodOf(succ)));
		}
		return res;
	}
	@Override
	public String toString() {
		return "Solver for: " + query.toString();
	}



	public Map<Transition<Statement, INode<Val>>, W> getTransitionsToFinalWeights() {
		return callAutomaton.getTransitionsToFinalWeights();
	}

	public int getNumberOfRules() {
		return callingPDS.getAllRules().size() + fieldPDS.getAllRules().size();
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
		Type sourceVal = source.getType();
		Type targetVal = target.getType();
		if(sourceVal.equals(targetVal)){
			return false;
		}
		if(source.isStatic()){
			return false;
		}
		if(!(targetVal instanceof RefType) || !(sourceVal instanceof RefType)){
			if(options.killNullAtCast() && targetVal instanceof NullType && isCastNode(t.getStart().fact())) {
				//A null pointer cannot be cast to any object 
				return true;
			}
			return false;//!allocVal.value().getType().equals(varVal.value().getType());
		}

		RefType targetType = (RefType) targetVal; 
		RefType sourceType = (RefType) sourceVal; 
		if(targetType.getSootClass().isPhantom() || sourceType.getSootClass().isPhantom())
			return false;
		if(target instanceof AllocVal && ((AllocVal) target).allocationValue() instanceof NewExpr){
			boolean castFails = Scene.v().getOrMakeFastHierarchy().canStoreType(targetType,sourceType);
			return !castFails;
		}
		//TODO this line is necessary as canStoreType does not properly work for interfaces, see Java doc. 
		if(targetType.getSootClass().isInterface()) {
			return false;
		}
		boolean castFails = Scene.v().getOrMakeFastHierarchy().canStoreType(targetType,sourceType) || Scene.v().getOrMakeFastHierarchy().canStoreType(sourceType,targetType);
		return !castFails;
	}
	

	private boolean isCastNode(Node<Statement, Val> node) {
		Stmt stmt = node.stmt().getUnit().get();
		AssignStmt x;
		if(stmt instanceof AssignStmt && (x = (AssignStmt) stmt).getRightOp() instanceof CastExpr) {
			CastExpr c = (CastExpr) x.getRightOp();
			if(c.getOp().equals(node.fact().value())) {
				return true;
			}
		}
		return false;
	}


	public void addReachable(SootMethod m) {
		if (reachableMethods.add(m)) {
			Collection<Runnable> collection = queuedReachableMethod.get(m);
			for (Runnable runnable : collection) {
				runnable.run();
			}
			for (ReachableMethodListener<W> l : Lists.newArrayList(reachableMethodListeners)) {
				l.reachable(m);
			}
		}
	}
	public void submit(SootMethod method, Runnable runnable) {
		if (reachableMethods.contains(method) || !options.onTheFlyCallGraph()) {
			runnable.run();
		} else {
			queuedReachableMethod.put(method, runnable);
		}
	}

	public void registerReachableMethodListener(ReachableMethodListener<W> reachableMethodListener) {
		if (reachableMethodListeners.add(reachableMethodListener)) {
			for (SootMethod m : Lists.newArrayList(reachableMethods)) {
				reachableMethodListener.reachable(m);
			}
		}
	}
	public Map<RegExAccessPath, W> getResultsAt(final Statement stmt){
		final Map<RegExAccessPath, W> results = Maps.newHashMap();
		fieldAutomaton.registerListener(new WPAUpdateListener<Field, INode<Node<Statement,Val>>, W>() {

			@Override
			public void onWeightAdded(Transition<Field, INode<Node<Statement, Val>>> t, W w,
					WeightedPAutomaton<Field, INode<Node<Statement, Val>>, W> aut) {
				if(t.getStart() instanceof GeneratedState) {
					return;
				}
				if(t.getStart().fact().stmt().equals(stmt)) {
					IRegEx<Field> regEx = fieldAutomaton.toRegEx(t.getStart(), fieldAutomaton.getInitialState());
					results.put(new RegExAccessPath(t.getStart().fact().fact(), regEx),w);
				}
			}
		});
		return results;
	}
	
	public Table<Statement, RegExAccessPath, W> getResults(SootMethod m){
		final Table<Statement, RegExAccessPath, W> results = HashBasedTable.create();
		logger.debug("Start extracting results from {}", this);
		fieldAutomaton.registerListener(new WPAUpdateListener<Field, INode<Node<Statement,Val>>, W>() {

			@Override
			public void onWeightAdded(Transition<Field, INode<Node<Statement, Val>>> t, W w,
					WeightedPAutomaton<Field, INode<Node<Statement, Val>>, W> aut) {
				if(t.getStart() instanceof GeneratedState) {
					return;
				}
				if(t.getStart().fact().stmt().getMethod().equals(m)) {
					IRegEx<Field> regEx = fieldAutomaton.toRegEx(t.getStart(), fieldAutomaton.getInitialState());
					AbstractBoomerangSolver.this.callAutomaton.registerListener(new WPAUpdateListener<Statement, INode<Val>, W>() {

						@Override
						public void onWeightAdded(Transition<Statement, INode<Val>> callT, W w,
								WeightedPAutomaton<Statement, INode<Val>, W> aut) {
							if(callT.getStart().fact().equals(t.getStart().fact().fact()) && callT.getLabel().equals(t.getStart().fact().stmt())) {
								results.put(t.getStart().fact().stmt(), new RegExAccessPath(t.getStart().fact().fact(),regEx),w);
							}
						}
					});
				}
			}
		});
		logger.debug("End extracted results from {}", this);
		return results;
	}
	
	public void debugFieldAutomaton(final Statement stmt) {
		fieldAutomaton.registerListener(new WPAUpdateListener<Field, INode<Node<Statement,Val>>, W>() {

			@Override
			public void onWeightAdded(Transition<Field, INode<Node<Statement, Val>>> t, W w,
					WeightedPAutomaton<Field, INode<Node<Statement, Val>>, W> aut) {
				if(t.getStart() instanceof GeneratedState) {
					return;
				}
				if(t.getStart().fact().stmt().equals(stmt)) {
					IRegEx<Field> regEx = fieldAutomaton.toRegEx(t.getStart(), fieldAutomaton.getInitialState());
					logger.debug(t.getStart().fact().fact() +" " + regEx);
				}
			}
		});
	}

	public Collection<SootMethod> getReachableMethods() {
		return reachableMethods;
	}
	
}
