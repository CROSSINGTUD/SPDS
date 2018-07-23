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
import com.google.common.base.Optional;
import com.google.common.collect.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import pathexpression.IRegEx;
import soot.*;
import soot.jimple.*;
import sync.pds.solver.SyncPDSSolver;
import sync.pds.solver.WitnessNode;
import sync.pds.solver.nodes.*;
import wpds.impl.*;
import wpds.interfaces.State;
import wpds.interfaces.WPAUpdateListener;

import java.util.*;
import java.util.Map.Entry;

public abstract class AbstractBoomerangSolver<W extends Weight> extends SyncPDSSolver<Statement, Val, Field, W> {

	private static final Logger logger = LogManager.getLogger();
	protected final ObservableICFG<Unit, SootMethod> icfg;
	protected final Query query;
	private boolean INTERPROCEDURAL = true;
	private final Map<Entry<INode<Node<Statement, Val>>, Field>, INode<Node<Statement, Val>>> generatedFieldState;
	private Multimap<SootMethod, Transition<Field, INode<Node<Statement, Val>>>> perMethodFieldTransitions = HashMultimap
			.create();
	private Multimap<SootMethod, MethodBasedFieldTransitionListener<W>> perMethodFieldTransitionsListener = HashMultimap
			.create();
	private Multimap<Statement, Transition<Field, INode<Node<Statement, Val>>>> perStatementFieldTransitions = HashMultimap
			.create();
	private Multimap<Statement, StatementBasedFieldTransitionListener<W>> perStatementFieldTransitionsListener = HashMultimap
			.create();
	private Set<ReachableMethodListener<W>> reachableMethodListeners = Sets.newHashSet();
	private Multimap<SootMethod, Runnable> queuedReachableMethod = HashMultimap.create();
	private Collection<SootMethod> reachableMethods = Sets.newHashSet();
	private Collection<SootMethod> methodsWithCallFlow = Sets.newHashSet();
	private Collection<SootMethod> scopeOpeningReachableMethods = Sets.newHashSet();
	protected final BoomerangOptions options;
	public AbstractBoomerangSolver(ObservableICFG<Unit, SootMethod> icfg,
			Query query, Map<Entry<INode<Node<Statement, Val>>, Field>, INode<Node<Statement, Val>>> genField,
			BoomerangOptions options, NestedWeightedPAutomatons<Statement, INode<Val>, W> callSummaries,
			 NestedWeightedPAutomatons<Field, INode<Node<Statement, Val>>, W> fieldSummaries) {
		super(new SingleNode<Val>(query.asNode().fact()), new AllocNode<Node<Statement, Val>>(query.asNode()),
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

		// TODO recap, I assume we can implement this more easily.
		this.generatedFieldState = genField;
		addReachable(query.asNode().stmt().getMethod());
		scopeOpeningReachableMethods.add(query.asNode().stmt().getMethod());
	}
	
	@Override
	protected boolean preventCallTransitionAdd(Transition<Statement, INode<Val>> t, W weight) {
		if (t.getStart() instanceof GeneratedState)
			return false;
		Val fact = t.getStart().fact();
		if(fact.isStatic())
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

	public INode<Node<Statement, Val>> generateFieldState(final INode<Node<Statement, Val>> d, final Field loc) {
		Entry<INode<Node<Statement, Val>>, Field> e = new AbstractMap.SimpleEntry<>(d, loc);
		if (!generatedFieldState.containsKey(e)) {
			generatedFieldState.put(e, new GeneratedState<Node<Statement, Val>, Field>(d, loc));
		}
		return generatedFieldState.get(e);
	}

	@Override
	public Collection<? extends State> computeSuccessor(Node<Statement, Val> node) {
		Statement stmt = node.stmt();
		Optional<Stmt> unit = stmt.getUnit();
		if (unit.isPresent()) {
			Stmt curr = unit.get();
			Val value = node.fact();
			SootMethod method = icfg.getMethodOf(curr);
			if(method == null)
				return Collections.emptySet();
			if (killFlow(method, curr, value)) {
				return Collections.emptySet();
			}
			if(options.isIgnoredMethod(method)){
				return Collections.emptySet();
			}
			if (curr.containsInvokeExpr() && valueUsedInStatement(curr, value) && INTERPROCEDURAL) {
				return callFlow(method, curr, curr.getInvokeExpr(), value);
			} else if (icfg.isExitStmt(curr)) {
				return returnFlow(method, curr, value);
			} else {
				return normalFlow(method, curr, value);
			}
		}
		return Collections.emptySet();
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

	private Collection<? extends State> returnFlow(SootMethod method, Stmt curr, Val value) {
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
			if (methodsWithCallFlow.contains(method)){
				icfg.addCallerListener(new ReturnFlowCallerListener(method, curr, value, out));
			} else {
				//Unbalanced call which we did not flow
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
		Stmt curr;
		Val value;
		Set<State> out;

		ReturnFlowCallerListener(SootMethod method, Stmt curr, Val value, Set<State> out){
			this.method = method;
			this.curr = curr;
			this.value = value;
			this.out = out;
		}


		@Override
		public SootMethod getObservedCallee() {
			return method;
		}

		@Override
		public void onCallerAdded(Unit unit, SootMethod sootMethod) {
			if (((Stmt) unit).containsInvokeExpr()){
				for (Unit returnSite : icfg.getSuccsOf(unit)) {
					Collection<? extends State> outFlow = computeReturnFlow(method, curr, value, (Stmt) unit,
							(Stmt) returnSite);
					out.addAll(outFlow);
				}
			}
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || getClass() != o.getClass()) return false;
			ReturnFlowCallerListener that = (ReturnFlowCallerListener) o;
			return Objects.equals(method, that.method) &&
					Objects.equals(curr, that.curr) &&
					Objects.equals(value, that.value) &&
					Objects.equals(out, that.out);
		}

		@Override
		public int hashCode() {

			return Objects.hash(method, curr, value, out);
		}
	}

	private Collection<State> callFlow(SootMethod caller, Stmt callSite, InvokeExpr invokeExpr, Val value) {
		assert icfg.isCallStmt(callSite);
		Set<State> out = Sets.newHashSet();
		if (!value.isStatic()){
			icfg.addCalleeListener(new CallFlowCalleeListener(callSite, caller, invokeExpr, out, value));
		}
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
		Stmt callSite;
		SootMethod caller;
		InvokeExpr invokeExpr;
		Set<State> out;
		Val value;

		CallFlowCalleeListener(Stmt callSite, SootMethod caller, InvokeExpr invokeExpr, Set<State> out, Val value){
			this.callSite = callSite;
			this.caller = caller;
			this.invokeExpr = invokeExpr;
			this.out = out;
			this.value = value;
		}

		@Override
		public Unit getObservedCaller() {
			return callSite;
		}

		@Override
		public void onCalleeAdded(Unit unit, SootMethod sootMethod) {
			for (Unit calleeSp : icfg.getStartPointsOf(sootMethod)) {
				for (Unit returnSite : icfg.getSuccsOf(callSite)) {
					Collection<? extends State> res = computeCallFlow(caller, new Statement((Stmt) returnSite, caller),
							new Statement(callSite, caller), invokeExpr, value, sootMethod, (Stmt) calleeSp);
					onCallFlow(sootMethod, callSite, value, res);
					out.addAll(res);
				}
			}
			addReachable(sootMethod);
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || getClass() != o.getClass()) return false;
			CallFlowCalleeListener that = (CallFlowCalleeListener) o;
			return Objects.equals(callSite, that.callSite) &&
					Objects.equals(caller, that.caller) &&
					Objects.equals(invokeExpr, that.invokeExpr) &&
					Objects.equals(out, that.out) &&
					Objects.equals(value, that.value);
		}

		@Override
		public int hashCode() {

			return Objects.hash(callSite, caller, invokeExpr, out, value);
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
	protected void processNode(final WitnessNode<Statement, Val, Field> witnessNode) {
		submit(witnessNode.stmt().getMethod(), new Runnable() {
			@Override
			public void run() {
				AbstractBoomerangSolver.super.processNode(witnessNode);
			}
		});

	}

	protected void onCallFlow(SootMethod callee, Stmt callSite, Val value, Collection<? extends State> res){
		methodsWithCallFlow.add(callee);
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
		Value sourceVal = source.value();
		Value targetVal = target.value();
		if(sourceVal.getType().equals(targetVal.getType())){
			return false;
		}
		if(source.isStatic()){
			return false;
		}
		if(!(targetVal.getType() instanceof RefType) || !(sourceVal.getType() instanceof RefType)){
			return false;//!allocVal.value().getType().equals(varVal.value().getType());
		}

		RefType targetType = (RefType) targetVal.getType(); 
		RefType sourceType = (RefType) sourceVal.getType(); 
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
					System.out.println(t.getStart().fact().fact() +" " + regEx);
				}
			}
		});
	}

	public Collection<SootMethod> getReachableMethods() {
		return reachableMethods;
	}

}
