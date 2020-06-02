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
package boomerang;

import boomerang.callgraph.BackwardsObservableICFG;
import boomerang.callgraph.CalleeListener;
import boomerang.callgraph.CallerListener;
import boomerang.callgraph.ObservableICFG;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Joiner;
import com.google.common.base.Optional;
import com.google.common.base.Stopwatch;
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;
import com.google.common.collect.Table;

import boomerang.customize.BackwardEmptyCalleeFlow;
import boomerang.customize.EmptyCalleeFlow;
import boomerang.customize.ForwardEmptyCalleeFlow;
import boomerang.debugger.Debugger;
import boomerang.jimple.*;
import boomerang.poi.AbstractPOI;
import boomerang.poi.ExecuteImportFieldStmtPOI;
import boomerang.poi.PointOfIndirection;
import boomerang.preanalysis.BoomerangPretransformer;
import boomerang.results.BackwardBoomerangResults;
import boomerang.results.ForwardBoomerangResults;
import boomerang.seedfactory.SimpleSeedFactory;
import boomerang.solver.*;
import boomerang.seedfactory.SeedFactory;
import boomerang.solver.AbstractBoomerangSolver;
import boomerang.solver.BackwardBoomerangSolver;
import boomerang.solver.ForwardBoomerangSolver;
import boomerang.solver.ReachableMethodListener;
import boomerang.solver.StatementBasedCallTransitionListener;
import boomerang.stats.IBoomerangStats;
import com.google.common.base.Joiner;
import com.google.common.base.Optional;
import com.google.common.base.Stopwatch;
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.common.collect.Table;
import heros.utilities.DefaultValueMap;

import soot.*;
import soot.jimple.*;
import sync.pds.solver.SyncPDSUpdateListener;
import sync.pds.solver.WeightFunctions;
import sync.pds.solver.nodes.*;
import wpds.impl.*;
import soot.Local;
import soot.Scene;
import soot.SootClass;
import soot.SootField;
import soot.SootMethod;
import soot.Unit;
import soot.jimple.ArrayRef;
import soot.jimple.AssignStmt;
import soot.jimple.InstanceFieldRef;
import soot.jimple.InvokeExpr;
import soot.jimple.NewMultiArrayExpr;
import soot.jimple.Stmt;
import soot.jimple.StringConstant;
import soot.jimple.toolkits.ide.icfg.BackwardsInterproceduralCFG;
import soot.jimple.toolkits.ide.icfg.BiDiInterproceduralCFG;
import sync.pds.solver.SyncPDSUpdateListener;
import sync.pds.solver.WeightFunctions;
import sync.pds.solver.nodes.GeneratedState;
import sync.pds.solver.nodes.INode;
import sync.pds.solver.nodes.Node;
import sync.pds.solver.nodes.SingleNode;
import wpds.impl.NestedWeightedPAutomatons;
import wpds.impl.Rule;
import wpds.impl.StackListener;
import wpds.impl.SummaryNestedWeightedPAutomatons;
import wpds.impl.Transition;
import wpds.impl.UnbalancedPopListener;
import wpds.impl.Weight;
import wpds.impl.WeightedPAutomaton;
import wpds.interfaces.State;
import wpds.interfaces.WPAStateListener;
import wpds.interfaces.WPAUpdateListener;

import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.TimeUnit;

public abstract class WeightedBoomerang<W extends Weight> {
    public static final boolean DEBUG = false;
    protected ObservableICFG<Unit, SootMethod> icfg;
    private static final Logger logger = LoggerFactory.getLogger(WeightedBoomerang.class);
    private Map<Entry<INode<Node<Statement, Val>>, Field>, INode<Node<Statement, Val>>> genField = new HashMap<>();
    private long lastTick;
    private IBoomerangStats<W> stats;
    private Set<SolverCreationListener<W>> solverCreationListeners = Sets.newHashSet();
    private Multimap<SolverPair, ExecuteImportFieldStmtPOI<W>> poiListeners = HashMultimap.create();
    private Multimap<SolverPair, INode<Node<Statement, Val>>> activatedPoi = HashMultimap.create();
    private final DefaultValueMap<Query, AbstractBoomerangSolver<W>> queryToSolvers = new DefaultValueMap<Query, AbstractBoomerangSolver<W>>() {

        @Override
        protected AbstractBoomerangSolver<W> createItem(final Query key) {
            final AbstractBoomerangSolver<W> solver;
            if (key instanceof BackwardQuery) {
                logger.debug("Backward solving query: " + key);
                solver = createBackwardSolver((BackwardQuery) key);
            } else {
                logger.debug("Forward solving query: " + key);
                solver = createForwardSolver((ForwardQuery) key);
            }
            solver.getCallAutomaton()
                    .registerUnbalancedPopListener(new UnbalancedPopListener<Statement, INode<Val>, W>() {

                        @Override
                        public void unbalancedPop(final INode<Val> returningFact,
                                final Transition<Statement, INode<Val>> trans, final W weight) {
                            Statement exitStmt = trans.getLabel();
                            SootMethod callee = exitStmt.getMethod();
                            if (!callee.isStaticInitializer()) {

                                UnbalancedPopHandler<W> info = new UnbalancedPopHandler<W>(returningFact, trans,
                                        weight);
                                icfg().addCallerListener(new UnbalancedPopCallerListener(callee, info, key, solver));
                            } else {
                                for (SootMethod entryPoint : Scene.v().getEntryPoints()) {
                                    for (Unit ep : WeightedBoomerang.this.icfg().getStartPointsOf(entryPoint)) {
                                        final Statement callStatement = new Statement((Stmt) ep,
                                                WeightedBoomerang.this.icfg().getMethodOf(ep));
                                        solver.submit(callStatement.getMethod(), new Runnable() {
                                            @Override
                                            public void run() {
                                                Val unbalancedFact = returningFact.fact().asUnbalanced(callStatement);
                                                SingleNode<Val> unbalancedState = new SingleNode<Val>(unbalancedFact);
                                                solver.getCallAutomaton().addUnbalancedState(unbalancedState);
                                                solver.getCallAutomaton().addWeightForTransition(
                                                        new Transition<Statement, INode<Val>>(
                                                                solver.getCallAutomaton().getInitialState(),
                                                                callStatement, unbalancedState),
                                                        solver.getCallAutomaton().getOne());

                                            }
                                        });
                                    }
                                }
                            }
                        }
                    });
            stats.registerSolver(key, solver);
            solver.getCallAutomaton().registerListener(new WPAUpdateListener<Statement, INode<Val>, W>() {
                @Override
                public void onWeightAdded(Transition<Statement, INode<Val>> t, W w,
                        WeightedPAutomaton<Statement, INode<Val>, W> aut) {
                    throwOnEarlyTermination();
                }
            });
            solver.getFieldAutomaton().registerListener(new WPAUpdateListener<Field, INode<Node<Statement, Val>>, W>() {

                @Override
                public void onWeightAdded(Transition<Field, INode<Node<Statement, Val>>> t, W w,
                        WeightedPAutomaton<Field, INode<Node<Statement, Val>>, W> aut) {
                    throwOnEarlyTermination();
                }
            });
            SeedFactory<W> seedFactory = getSeedFactory();
            if (options.onTheFlyCallGraph() && seedFactory != null) {
                for (SootMethod m : seedFactory.getMethodScope(key)) {
                    solver.addReachable(m);
                }
            }
            onCreateSubSolver(key, solver);
            return solver;
        }
    };

    private void registerUnbalancedPopListener(Node<Statement, AbstractBoomerangSolver<W>> unbalancedPopPair,
            UnbalancedPopHandler<W> unbalancedPopInfo) {
        if (unbalancedPopPairs.contains(unbalancedPopPair)) {
            unbalancedPopInfo.trigger(unbalancedPopPair.stmt(), unbalancedPopPair.fact());
        } else {
            unbalancedListeners.put(unbalancedPopPair, unbalancedPopInfo);
        }

    }

    public final void throwOnEarlyTermination() {
        checkAborted();
        checkTimeout();
    }

    public void checkAborted() {

    }

    public void checkTimeout() {
        if (options.analysisTimeoutMS() > 0) {
            long elapsed = analysisWatch.elapsed(TimeUnit.MILLISECONDS);
            if (elapsed - lastTick > 15000) {
                System.out.println("Alive " + elapsed + "  " + options.analysisTimeoutMS());
                lastTick = elapsed;
            }
            if (options.analysisTimeoutMS() < elapsed) {
                if (analysisWatch.isRunning())
                    analysisWatch.stop();
                throw new BoomerangTimeoutException(elapsed, stats);
            }
        }
    }

    Multimap<Node<Statement, AbstractBoomerangSolver<W>>, UnbalancedPopHandler<W>> unbalancedListeners = HashMultimap
            .create();
    Set<Node<Statement, AbstractBoomerangSolver<W>>> unbalancedPopPairs = Sets.newHashSet();

    private void triggerUnbalancedPop(Node<Statement, AbstractBoomerangSolver<W>> unbalancedPopPair) {
        if (unbalancedPopPairs.add(unbalancedPopPair)) {
            for (UnbalancedPopHandler<W> unbalancedPopInfo : Lists
                    .newArrayList(unbalancedListeners.get(unbalancedPopPair))) {
                unbalancedPopInfo.trigger(unbalancedPopPair.stmt(), unbalancedPopPair.fact());
            }
        }
    }

    private ObservableICFG<Unit, SootMethod> bwicfg;
    private EmptyCalleeFlow forwardEmptyCalleeFlow = new ForwardEmptyCalleeFlow();
    private EmptyCalleeFlow backwardEmptyCalleeFlow = new BackwardEmptyCalleeFlow();

    private NestedWeightedPAutomatons<Statement, INode<Val>, W> backwardCallSummaries = new SummaryNestedWeightedPAutomatons<>();
    private NestedWeightedPAutomatons<Field, INode<Node<Statement, Val>>, W> backwardFieldSummaries = new SummaryNestedWeightedPAutomatons<>();
    private NestedWeightedPAutomatons<Statement, INode<Val>, W> forwardCallSummaries = new SummaryNestedWeightedPAutomatons<>();
    private NestedWeightedPAutomatons<Field, INode<Node<Statement, Val>>, W> forwardFieldSummaries = new SummaryNestedWeightedPAutomatons<>();
    private DefaultValueMap<FieldWritePOI, FieldWritePOI> fieldWrites = new DefaultValueMap<FieldWritePOI, FieldWritePOI>() {
        @Override
        protected FieldWritePOI createItem(FieldWritePOI key) {
            stats.registerFieldWritePOI(key);
            return key;
        }
    };
    private DefaultValueMap<FieldReadPOI, FieldReadPOI> fieldReads = new DefaultValueMap<FieldReadPOI, FieldReadPOI>() {
        @Override
        protected FieldReadPOI createItem(FieldReadPOI key) {
            stats.registerFieldReadPOI(key);
            return key;
        }
    };
    protected final BoomerangOptions options;
    private Debugger<W> debugger;
    private Stopwatch analysisWatch = Stopwatch.createUnstarted();
    private Set<BackwardQuery> scopedQueries = Sets.newHashSet();

    public WeightedBoomerang(BoomerangOptions options) {
        this.options = options;
        this.stats = options.statsFactory();
        if (!BoomerangPretransformer.v().isApplied()) {
            throw new RuntimeException(
                    "Using WeightedBoomerang requires a call to BoomerangPretransformer.v().apply() prior constructing the ICFG");
        }
    }

    public WeightedBoomerang() {
        this(new DefaultBoomerangOptions());
    }

    protected AbstractBoomerangSolver<W> createBackwardSolver(final BackwardQuery backwardQuery) {
        BackwardBoomerangSolver<W> solver = new BackwardBoomerangSolver<W>(bwicfg(), backwardQuery, genField, options,
                createCallSummaries(backwardQuery, backwardCallSummaries),
                createFieldSummaries(backwardQuery, backwardFieldSummaries)) {

            @Override
            protected Collection<? extends State> computeCallFlow(SootMethod caller, Statement returnSite,
                    Statement callSite, InvokeExpr invokeExpr, Val fact, SootMethod callee, Stmt calleeSp) {
                return super.computeCallFlow(caller, returnSite, callSite, invokeExpr, fact, callee, calleeSp);
            }

            @Override
            protected Collection<? extends State> getEmptyCalleeFlow(SootMethod caller, Stmt callSite, Val value,
                    Stmt returnSite) {
                return backwardEmptyCalleeFlow.getEmptyCalleeFlow(caller, callSite, value, returnSite);
            }

            @Override
            protected WeightFunctions<Statement, Val, Field, W> getFieldWeights() {
                return WeightedBoomerang.this.getBackwardFieldWeights();
            }

            @Override
            protected WeightFunctions<Statement, Val, Statement, W> getCallWeights() {
                return WeightedBoomerang.this.getBackwardCallWeights();
            }

            @Override
            protected void onManyStateListenerRegister() {
                throwOnEarlyTermination();
            }

        };
        solver.registerListener(new SyncPDSUpdateListener<Statement, Val>() {
            @Override
            public void onReachableNodeAdded(Node<Statement, Val> node) {
                if (hasNoMethod(node)) {
                    return;
                }
                Optional<AllocVal> allocNode = isAllocationNode(node.stmt(), node.fact());
                if (allocNode.isPresent()) {
                    ForwardQuery q = new ForwardQuery(node.stmt(), allocNode.get());
                    final AbstractBoomerangSolver<W> forwardSolver = forwardSolve(q);
                    solver.registerReachableMethodListener(new ReachableMethodListener<W>() {

                        @Override
                        public void reachable(SootMethod m) {
                            forwardSolver.addReachable(m);
                        }
                    });
                }
                if (isFieldStore(node.stmt())) {
                } else if (isArrayLoad(node.stmt())) {
                    backwardHandleFieldRead(node, createArrayFieldLoad(node.stmt()), backwardQuery);
                } else if (isFieldLoad(node.stmt())) {
                    backwardHandleFieldRead(node, createFieldLoad(node.stmt()), backwardQuery);
                }
                if (isBackwardEnterCall(node.stmt())) {
                    // TODO
                }
                if (options.trackStaticFieldAtEntryPointToClinit() && node.fact().isStatic()
                        && isFirstStatementOfEntryPoint(node.stmt())) {
                    StaticFieldVal val = (StaticFieldVal) node.fact();
                    for (SootMethod m : val.field().getDeclaringClass().getMethods()) {
                        if (m.isStaticInitializer()) {
                            solver.addReachable(m);
                            for (Unit ep : icfg().getEndPointsOf(m)) {
                                StaticFieldVal newVal = new StaticFieldVal(val.value(), val.field(), m);
                                solver.addNormalCallFlow(node,
                                        new Node<Statement, Val>(new Statement((Stmt) ep, m), newVal));
                                solver.addNormalFieldFlow(node,
                                        new Node<Statement, Val>(new Statement((Stmt) ep, m), newVal));
                            }
                        }
                    }
                }
            }

        });

        return solver;
    }

    protected boolean hasNoMethod(Node<Statement, Val> node) {
        if (icfg().getMethodOf(node.stmt().getUnit().get()) == null) {
            return true;
        }
        return false;
    }

    protected boolean isFirstStatementOfEntryPoint(Statement stmt) {
        for (SootMethod m : Scene.v().getEntryPoints()) {
            if (m.hasActiveBody()) {
                if (stmt.equals(new Statement((Stmt) m.getActiveBody().getUnits().getFirst(), m))) {
                    return true;
                }
            }
        }
        return false;
    }

    protected boolean isBackwardEnterCall(Statement stmt) {
        if (!stmt.getUnit().isPresent())
            return false;
        try {
            return icfg().isExitStmt(stmt.getUnit().get());
        } catch (NullPointerException e) {
            return false;
        }
    }

    protected Optional<AllocVal> isAllocationNode(Statement s, Val fact) {
        Optional<Stmt> optUnit = s.getUnit();
        if (optUnit.isPresent()) {
            Stmt stmt = optUnit.get();
            return options.getAllocationVal(s.getMethod(), stmt, fact, icfg());
        }
        return Optional.absent();
    }

    protected ForwardBoomerangSolver<W> createForwardSolver(final ForwardQuery sourceQuery) {
        final ForwardBoomerangSolver<W> solver = new ForwardBoomerangSolver<W>(icfg(), sourceQuery, genField, options,
                createCallSummaries(sourceQuery, forwardCallSummaries),
                createFieldSummaries(sourceQuery, forwardFieldSummaries)) {

            @Override
            protected Collection<? extends State> getEmptyCalleeFlow(SootMethod caller, Stmt callSite, Val value,
                    Stmt returnSite) {
                return forwardEmptyCalleeFlow.getEmptyCalleeFlow(caller, callSite, value, returnSite);
            }

            @Override
            protected WeightFunctions<Statement, Val, Statement, W> getCallWeights() {
                return WeightedBoomerang.this.getForwardCallWeights(sourceQuery);
            }

            @Override
            protected WeightFunctions<Statement, Val, Field, W> getFieldWeights() {
                return WeightedBoomerang.this.getForwardFieldWeights();
            }

            @Override
            public void addCallRule(Rule<Statement, INode<Val>, W> rule) {
                if (preventCallRuleAdd(sourceQuery, rule)) {
                    return;
                }
                super.addCallRule(rule);
            }

            @Override
            protected void onManyStateListenerRegister() {
                throwOnEarlyTermination();
            }

        };

        solver.registerListener(new SyncPDSUpdateListener<Statement, Val>() {
            @Override
            public void onReachableNodeAdded(Node<Statement, Val> node) {
                if (hasNoMethod(node)) {
                    return;
                }
                if (isFieldStore(node.stmt())) {
                    forwardHandleFieldWrite(node, createFieldStore(node.stmt()), sourceQuery);
                } else if (isArrayStore(node.stmt())) {
                    if (options.arrayFlows()) {
                        forwardHandleFieldWrite(node, createArrayFieldStore(node.stmt()), sourceQuery);
                    }
                } else if (isFieldLoad(node.stmt())) {
                    forwardHandleFieldLoad(node, createFieldLoad(node.stmt()), sourceQuery);
                } else if (isArrayLoad(node.stmt())) {
                    forwardHandleFieldLoad(node, createArrayFieldLoad(node.stmt()), sourceQuery);
                }
                if (isBackwardEnterCall(node.stmt())) {
                    // TODO
                }
            }
        });

        return solver;
    }

    private NestedWeightedPAutomatons<Statement, INode<Val>, W> createCallSummaries(final Query sourceQuery,
            final NestedWeightedPAutomatons<Statement, INode<Val>, W> summaries) {
        return new NestedWeightedPAutomatons<Statement, INode<Val>, W>() {

            @Override
            public void putSummaryAutomaton(INode<Val> target, WeightedPAutomaton<Statement, INode<Val>, W> aut) {
                summaries.putSummaryAutomaton(target, aut);
            }

            @Override
            public WeightedPAutomaton<Statement, INode<Val>, W> getSummaryAutomaton(INode<Val> target) {
                if (target.fact().equals(sourceQuery.var())) {
                    return queryToSolvers.getOrCreate(sourceQuery).getCallAutomaton();
                }
                return summaries.getSummaryAutomaton(target);
            }
        };
    }

    private NestedWeightedPAutomatons<Field, INode<Node<Statement, Val>>, W> createFieldSummaries(final Query query,
            final NestedWeightedPAutomatons<Field, INode<Node<Statement, Val>>, W> summaries) {
        return new NestedWeightedPAutomatons<Field, INode<Node<Statement, Val>>, W>() {

            @Override
            public void putSummaryAutomaton(INode<Node<Statement, Val>> target,
                    WeightedPAutomaton<Field, INode<Node<Statement, Val>>, W> aut) {
                summaries.putSummaryAutomaton(target, aut);
            }

            @Override
            public WeightedPAutomaton<Field, INode<Node<Statement, Val>>, W> getSummaryAutomaton(
                    INode<Node<Statement, Val>> target) {
                if (target.fact().equals(query.asNode())) {
                    return queryToSolvers.getOrCreate(query).getFieldAutomaton();
                }
                return summaries.getSummaryAutomaton(target);
            }

        };
    }

    public boolean preventCallRuleAdd(ForwardQuery sourceQuery, Rule<Statement, INode<Val>, W> rule) {
        return false;
    }

    protected FieldReadPOI createFieldLoad(Statement s) {
        Stmt stmt = s.getUnit().get();
        AssignStmt as = (AssignStmt) stmt;
        InstanceFieldRef ifr = (InstanceFieldRef) as.getRightOp();
        Val base = new Val(ifr.getBase(), icfg().getMethodOf(as));
        Field field = new Field(ifr.getField());
        return fieldReads
                .getOrCreate(new FieldReadPOI(s, base, field, new Val(as.getLeftOp(), icfg().getMethodOf(as))));
    }

    protected FieldReadPOI createArrayFieldLoad(Statement s) {
        Stmt stmt = s.getUnit().get();
        AssignStmt as = (AssignStmt) stmt;
        ArrayRef ifr = (ArrayRef) as.getRightOp();
        Val base = new Val(ifr.getBase(), icfg().getMethodOf(as));
        Val stored = new Val(as.getLeftOp(), icfg().getMethodOf(as));
        return fieldReads.getOrCreate(new FieldReadPOI(s, base, Field.array(), stored));
    }

    protected FieldWritePOI createArrayFieldStore(Statement s) {
        Stmt stmt = s.getUnit().get();
        AssignStmt as = (AssignStmt) stmt;
        ArrayRef ifr = (ArrayRef) as.getLeftOp();
        Val base = new Val(ifr.getBase(), icfg().getMethodOf(as));
        Val stored = new Val(as.getRightOp(), icfg().getMethodOf(as));
        return fieldWrites.getOrCreate(new FieldWritePOI(s, base, Field.array(), stored));
    }

    protected FieldWritePOI createFieldStore(Statement s) {
        Stmt stmt = s.getUnit().get();
        AssignStmt as = (AssignStmt) stmt;
        InstanceFieldRef ifr = (InstanceFieldRef) as.getLeftOp();
        Val base = new Val(ifr.getBase(), icfg().getMethodOf(as));
        Val stored = new Val(as.getRightOp(), icfg().getMethodOf(as));
        Field field = new Field(ifr.getField());
        return fieldWrites.getOrCreate(new FieldWritePOI(s, base, field, stored));
    }

    public static boolean isFieldStore(Statement s) {
        Optional<Stmt> optUnit = s.getUnit();
        if (optUnit.isPresent()) {
            Stmt stmt = optUnit.get();
            if (stmt instanceof AssignStmt && ((AssignStmt) stmt).getLeftOp() instanceof InstanceFieldRef) {
                return true;
            }
        }
        return false;
    }

    public static boolean isArrayStore(Statement s) {
        Optional<Stmt> optUnit = s.getUnit();
        if (optUnit.isPresent()) {
            Stmt stmt = optUnit.get();
            if (stmt instanceof AssignStmt && ((AssignStmt) stmt).getLeftOp() instanceof ArrayRef) {
                return true;
            }
        }
        return false;
    }

    public static boolean isArrayLoad(Statement s) {
        Optional<Stmt> optUnit = s.getUnit();
        if (optUnit.isPresent()) {
            Stmt stmt = optUnit.get();
            if (stmt instanceof AssignStmt && ((AssignStmt) stmt).getRightOp() instanceof ArrayRef) {
                return true;
            }
        }
        return false;
    }

    public static boolean isFieldLoad(Statement s) {
        Optional<Stmt> optUnit = s.getUnit();
        if (optUnit.isPresent()) {
            Stmt stmt = optUnit.get();
            if (stmt instanceof AssignStmt && ((AssignStmt) stmt).getRightOp() instanceof InstanceFieldRef) {
                return true;
            }
        }
        return false;
    }

    protected void backwardHandleFieldRead(final Node<Statement, Val> node, FieldReadPOI fieldRead,
            final BackwardQuery sourceQuery) {
        if (node.fact().equals(fieldRead.getStoredVar())) {
            fieldRead.addFlowAllocation(sourceQuery);
        }
    }

    protected void forwardHandleFieldWrite(final Node<Statement, Val> node, final FieldWritePOI fieldWritePoi,
            final ForwardQuery sourceQuery) {
        BackwardQuery backwardQuery = new BackwardQuery(node.stmt(), fieldWritePoi.getBaseVar());
        if (node.fact().equals(fieldWritePoi.getStoredVar())) {
            backwardSolveUnderScope(backwardQuery, sourceQuery, node);
            queryToSolvers.get(sourceQuery).getFieldAutomaton()
                    .registerListener(new ForwardHandleFieldWrite(sourceQuery, fieldWritePoi, node.stmt()));
        }
        if (node.fact().equals(fieldWritePoi.getBaseVar())) {
            queryToSolvers.getOrCreate(sourceQuery).getFieldAutomaton()
                    .registerListener(new TriggerBaseAllocationAtFieldWrite(new SingleNode<Node<Statement, Val>>(node),
                            fieldWritePoi, sourceQuery));
        }
    }

    public BackwardBoomerangResults<W> backwardSolveUnderScope(BackwardQuery backwardQuery, ForwardQuery forwardQuery,
            Node<Statement, Val> node) {
        scopedQueries.add(backwardQuery);
        boolean aborted = false;
        try {
            backwardSolve(backwardQuery);
            final AbstractBoomerangSolver<W> bwSolver = queryToSolvers.getOrCreate(backwardQuery);
            AbstractBoomerangSolver<W> fwSolver = queryToSolvers.getOrCreate(forwardQuery);
            fwSolver.registerReachableMethodListener(new ReachableMethodListener<W>() {
                @Override
                public void reachable(SootMethod m) {
                    bwSolver.addReachable(m);
                }
            });

            fwSolver.getCallAutomaton().registerListener(new StackListener<Statement, INode<Val>, W>(
                    fwSolver.getCallAutomaton(), new SingleNode<>(node.fact()), node.stmt()) {

                @Override
                public void stackElement(Statement callSite) {
                    triggerUnbalancedPop(new Node<Statement, AbstractBoomerangSolver<W>>(callSite, bwSolver));
                }

                @Override
                public void anyContext(Statement end) {
                    for (Unit sP : icfg().getStartPointsOf(end.getMethod())) {
                        bwSolver.registerStatementCallTransitionListener(new CanUnbalancedReturn(end.getMethod(),
                                new Statement((Stmt) sP, end.getMethod()), bwSolver));
                    }
                }
            });
        } catch (BoomerangAbortedException e) {
            aborted = true;
            cleanup();
        }

        return new BackwardBoomerangResults<W>(backwardQuery, aborted, this.queryToSolvers, getStats(), analysisWatch);
    }

    private void cleanup() {
        for (AbstractBoomerangSolver<W> solver : queryToSolvers.values()) {
            solver.cleanup();
        }
        this.poiListeners.clear();
        this.unbalancedListeners.clear();
    }

    public BackwardBoomerangResults<W> backwardSolveUnderScope(BackwardQuery backwardQuery,
            IContextRequester requester) {
        scopedQueries.add(backwardQuery);
        boolean aborted = false;
        try {
            if (analysisWatch.isRunning()) {
                analysisWatch.stop();
            }
            analysisWatch = Stopwatch.createStarted();
            backwardSolve(backwardQuery);

            final AbstractBoomerangSolver<W> bwSolver = queryToSolvers.getOrCreate(backwardQuery);
            Collection<Context> callSiteOf = requester.getCallSiteOf(requester.initialContext(backwardQuery.stmt()));
            for (Context c : callSiteOf) {
                bwSolver.registerListener(
                        new CanUnbalancedReturnToCallSite(backwardQuery.stmt().getMethod(), c, bwSolver, requester));
            }
            if (analysisWatch.isRunning()) {
                analysisWatch.stop();
            }
        } catch (BoomerangAbortedException e) {
            aborted = true;
            cleanup();
        }

        return new BackwardBoomerangResults<W>(backwardQuery, aborted, this.queryToSolvers, getStats(), analysisWatch);
    }

    private final class UnbalancedPopCallerListener implements CallerListener<Unit, SootMethod> {
        private final SootMethod callee;
        private final UnbalancedPopHandler<W> info;
        private final Query key;
        private final AbstractBoomerangSolver<W> solver;

        private UnbalancedPopCallerListener(SootMethod callee, UnbalancedPopHandler<W> info, Query key,
                AbstractBoomerangSolver<W> solver) {
            this.callee = callee;
            this.info = info;
            this.key = key;
            this.solver = solver;
        }

        @Override
        public SootMethod getObservedCallee() {
            return callee;
        }

        @Override
        public void onCallerAdded(Unit callSite, SootMethod m) {
            if (!((Stmt) callSite).containsInvokeExpr())
                return;
            final Statement callStatement = new Statement((Stmt) callSite,
                    WeightedBoomerang.this.icfg().getMethodOf(callSite));
            Node<Statement, AbstractBoomerangSolver<W>> solverPair = new Node<>(callStatement, solver);
            registerUnbalancedPopListener(solverPair, info);
            if (callee.isStatic() || !scopedQueries.contains(key)) {
                triggerUnbalancedPop(solverPair);
            }
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + getOuterType().hashCode();
            result = prime * result + ((callee == null) ? 0 : callee.hashCode());
            result = prime * result + ((info == null) ? 0 : info.hashCode());
            result = prime * result + ((key == null) ? 0 : key.hashCode());
            result = prime * result + ((solver == null) ? 0 : solver.hashCode());
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (obj == null)
                return false;
            if (getClass() != obj.getClass())
                return false;
            UnbalancedPopCallerListener other = (UnbalancedPopCallerListener) obj;
            if (!getOuterType().equals(other.getOuterType()))
                return false;
            if (callee == null) {
                if (other.callee != null)
                    return false;
            } else if (!callee.equals(other.callee))
                return false;
            if (info == null) {
                if (other.info != null)
                    return false;
            } else if (!info.equals(other.info))
                return false;
            if (key == null) {
                if (other.key != null)
                    return false;
            } else if (!key.equals(other.key))
                return false;
            if (solver == null) {
                if (other.solver != null)
                    return false;
            } else if (!solver.equals(other.solver))
                return false;
            return true;
        }

        private WeightedBoomerang getOuterType() {
            return WeightedBoomerang.this;
        }

    }

    private class CanUnbalancedReturnToCallSite implements SyncPDSUpdateListener<Statement, Val> {

        private AbstractBoomerangSolver<W> bwSolver;
        private SootMethod method;
        private Collection<Unit> startPointsOf;
        private Context callSite;
        private IContextRequester req;

        public CanUnbalancedReturnToCallSite(SootMethod method, Context callSite, AbstractBoomerangSolver<W> bwSolver,
                IContextRequester req) {
            this.method = method;
            this.callSite = callSite;
            this.bwSolver = bwSolver;
            this.req = req;
            this.startPointsOf = icfg().getStartPointsOf(method);
        }

        @Override
        public void onReachableNodeAdded(Node<Statement, Val> reachableNode) {
            if (startPointsOf.contains(reachableNode.stmt().getUnit().get())) {
                Node<Statement, AbstractBoomerangSolver<W>> solverPair = new Node<>(callSite.getStmt(), bwSolver);
                bwSolver.addReachable(callSite.getStmt().getMethod());
                triggerUnbalancedPop(solverPair);
                for (Context parent : req.getCallSiteOf(callSite)) {
                    bwSolver.registerListener(
                            new CanUnbalancedReturnToCallSite(callSite.getStmt().getMethod(), parent, bwSolver, req));
                }
            }
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + getOuterType().hashCode();
            result = prime * result + ((bwSolver == null) ? 0 : bwSolver.hashCode());
            result = prime * result + ((method == null) ? 0 : method.hashCode());
            result = prime * result + ((callSite == null) ? 0 : callSite.hashCode());
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (obj == null)
                return false;
            if (getClass() != obj.getClass())
                return false;
            CanUnbalancedReturnToCallSite other = (CanUnbalancedReturnToCallSite) obj;
            if (!getOuterType().equals(other.getOuterType()))
                return false;
            if (bwSolver == null) {
                if (other.bwSolver != null)
                    return false;
            } else if (!bwSolver.equals(other.bwSolver))
                return false;
            if (method == null) {
                if (other.method != null)
                    return false;
            } else if (!method.equals(other.method))
                return false;
            if (callSite == null) {
                if (other.callSite != null)
                    return false;
            } else if (!callSite.equals(other.callSite))
                return false;
            return true;
        }

        private WeightedBoomerang getOuterType() {
            return WeightedBoomerang.this;
        }

    }

    private final class UnbalancedReturnCallerListener implements CallerListener<Unit, SootMethod> {
        private final AbstractBoomerangSolver<W> bwSolver;
        private final SootMethod method;

        public UnbalancedReturnCallerListener(AbstractBoomerangSolver<W> bwSolver, SootMethod method) {
            this.bwSolver = bwSolver;
            this.method = method;
        }

        @Override
        public SootMethod getObservedCallee() {
            return method;
        }

        @Override
        public void onCallerAdded(Unit callSite, SootMethod m) {
            if (!((Stmt) callSite).containsInvokeExpr())
                return;
            final Statement callStatement = new Statement((Stmt) callSite,
                    WeightedBoomerang.this.icfg().getMethodOf(callSite));
            Node<Statement, AbstractBoomerangSolver<W>> solverPair = new Node<>(callStatement, bwSolver);
            triggerUnbalancedPop(solverPair);
            for (Unit sP : icfg().getStartPointsOf(callStatement.getMethod())) {
                bwSolver.registerStatementCallTransitionListener(new CanUnbalancedReturn(callStatement.getMethod(),
                        new Statement((Stmt) sP, callStatement.getMethod()), bwSolver));
            }
        }
    }

    private class CanUnbalancedReturn extends StatementBasedCallTransitionListener<W> {

        private AbstractBoomerangSolver<W> bwSolver;
        private Statement startPoint;
        private SootMethod method;

        public CanUnbalancedReturn(SootMethod method, Statement startPoint, AbstractBoomerangSolver<W> bwSolver) {
            super(startPoint);
            this.method = method;
            this.bwSolver = bwSolver;
            this.startPoint = startPoint;
        }

        @Override
        public void onAddedTransition(Transition<Statement, INode<Val>> t, W w) {
            if (t.getLabel().equals(startPoint)) {
                WeightedBoomerang.this.icfg().addCallerListener(new UnbalancedReturnCallerListener(bwSolver, method));
            }
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + getOuterType().hashCode();
            result = prime * result + ((bwSolver == null) ? 0 : bwSolver.hashCode());
            result = prime * result + ((startPoint == null) ? 0 : startPoint.hashCode());
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (obj == null)
                return false;
            if (getClass() != obj.getClass())
                return false;
            CanUnbalancedReturn other = (CanUnbalancedReturn) obj;
            if (!getOuterType().equals(other.getOuterType()))
                return false;
            if (bwSolver == null) {
                if (other.bwSolver != null)
                    return false;
            } else if (!bwSolver.equals(other.bwSolver))
                return false;
            if (startPoint == null) {
                if (other.startPoint != null)
                    return false;
            } else if (!startPoint.equals(other.startPoint))
                return false;
            return true;
        }

        private WeightedBoomerang getOuterType() {
            return WeightedBoomerang.this;
        }

    }

    private final class ForwardHandleFieldWrite implements WPAUpdateListener<Field, INode<Node<Statement, Val>>, W> {
        private final Query sourceQuery;
        private final AbstractPOI<Statement, Val, Field> fieldWritePoi;
        private final Statement stmt;

        private ForwardHandleFieldWrite(Query sourceQuery, AbstractPOI<Statement, Val, Field> fieldWritePoi,
                Statement statement) {
            this.sourceQuery = sourceQuery;
            this.fieldWritePoi = fieldWritePoi;
            this.stmt = statement;
        }

        @Override
        public void onWeightAdded(Transition<Field, INode<Node<Statement, Val>>> t, W w,
                WeightedPAutomaton<Field, INode<Node<Statement, Val>>, W> aut) {
            if (t.getStart() instanceof GeneratedState)
                return;
            if (t.getStart().fact().stmt().equals(stmt) && t.getLabel().equals(Field.empty())) {
                fieldWritePoi.addFlowAllocation(sourceQuery);
            }
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + getOuterType().hashCode();
            result = prime * result + ((sourceQuery == null) ? 0 : sourceQuery.hashCode());
            result = prime * result + ((stmt == null) ? 0 : stmt.hashCode());
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (obj == null)
                return false;
            if (getClass() != obj.getClass())
                return false;
            ForwardHandleFieldWrite other = (ForwardHandleFieldWrite) obj;
            if (!getOuterType().equals(other.getOuterType()))
                return false;
            if (sourceQuery == null) {
                if (other.sourceQuery != null)
                    return false;
            } else if (!sourceQuery.equals(other.sourceQuery))
                return false;
            if (stmt == null) {
                if (other.stmt != null)
                    return false;
            } else if (!stmt.equals(other.stmt))
                return false;
            return true;
        }

        private WeightedBoomerang getOuterType() {
            return WeightedBoomerang.this;
        }

    }

    private class TriggerBaseAllocationAtFieldWrite extends WPAStateListener<Field, INode<Node<Statement, Val>>, W> {

        private final PointOfIndirection<Statement, Val, Field> fieldWritePoi;
        private final ForwardQuery sourceQuery;

        public TriggerBaseAllocationAtFieldWrite(INode<Node<Statement, Val>> state,
                PointOfIndirection<Statement, Val, Field> fieldWritePoi, ForwardQuery sourceQuery) {
            super(state);
            this.fieldWritePoi = fieldWritePoi;
            this.sourceQuery = sourceQuery;
        }

        @Override
        public void onOutTransitionAdded(Transition<Field, INode<Node<Statement, Val>>> t, W w,
                WeightedPAutomaton<Field, INode<Node<Statement, Val>>, W> aut) {
            if (isAllocationNode(t.getTarget().fact().fact(), sourceQuery)) {
                fieldWritePoi.addBaseAllocation(sourceQuery);
            }
        }

        @Override
        public void onInTransitionAdded(Transition<Field, INode<Node<Statement, Val>>> t, W w,
                WeightedPAutomaton<Field, INode<Node<Statement, Val>>, W> aut) {
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = super.hashCode();
            result = prime * result + getOuterType().hashCode();
            result = prime * result + ((fieldWritePoi == null) ? 0 : fieldWritePoi.hashCode());
            result = prime * result + ((sourceQuery == null) ? 0 : sourceQuery.hashCode());
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (!super.equals(obj))
                return false;
            if (getClass() != obj.getClass())
                return false;
            TriggerBaseAllocationAtFieldWrite other = (TriggerBaseAllocationAtFieldWrite) obj;
            if (!getOuterType().equals(other.getOuterType()))
                return false;
            if (fieldWritePoi == null) {
                if (other.fieldWritePoi != null)
                    return false;
            } else if (!fieldWritePoi.equals(other.fieldWritePoi))
                return false;
            if (sourceQuery == null) {
                if (other.sourceQuery != null)
                    return false;
            } else if (!sourceQuery.equals(other.sourceQuery))
                return false;
            return true;
        }

        private WeightedBoomerang getOuterType() {
            return WeightedBoomerang.this;
        }

    }

    private void forwardHandleFieldLoad(final Node<Statement, Val> node, final FieldReadPOI fieldReadPoi,
            final ForwardQuery sourceQuery) {
        if (node.fact().equals(fieldReadPoi.getBaseVar())) {
            queryToSolvers.getOrCreate(sourceQuery).getFieldAutomaton()
                    .registerListener(new TriggerBaseAllocationAtFieldWrite(new SingleNode<Node<Statement, Val>>(node),
                            fieldReadPoi, sourceQuery));
        }
    }

    private boolean isAllocationNode(Val fact, ForwardQuery sourceQuery) {
        return fact.equals(sourceQuery.var());
    }

    private ObservableICFG<Unit, SootMethod> bwicfg() {
        if (bwicfg == null)
            bwicfg = new BackwardsObservableICFG(icfg());
        return bwicfg;
    }

    public ForwardBoomerangResults<W> solve(ForwardQuery query) {
        if (!analysisWatch.isRunning()) {
            analysisWatch.start();
        }
        boolean aborted = false;
        try {
            logger.debug("Starting forward analysis of: {}", query);
            forwardSolve(query);
            logger.debug("Terminated forward analysis of: {}", query);
        } catch (BoomerangAbortedException e) {
            aborted = true;
            cleanup();
            logger.debug("Timeout of query: {}", query);
        }

        if (analysisWatch.isRunning()) {
            analysisWatch.stop();
        }
        return new ForwardBoomerangResults<W>(query, icfg(), aborted, this.queryToSolvers, getStats(), analysisWatch);
    }

    public BackwardBoomerangResults<W> solve(BackwardQuery query) {
        icfg().addUnbalancedMethod(query.stmt().getMethod());
        return solve(query, true);
    }

    public BackwardBoomerangResults<W> solve(BackwardQuery query, boolean timing) {
        if (timing && !analysisWatch.isRunning()) {
            analysisWatch.start();
        }
        boolean aborted = false;
        try {
            logger.debug("Starting backward analysis of: {}", query);
            backwardSolve(query);
            logger.debug("Terminated backward analysis of: {}", query);
        } catch (BoomerangAbortedException e) {
            aborted = true;
            cleanup();
            logger.debug("Timeout of query: {}", query);
        }
        if (timing && analysisWatch.isRunning()) {
            analysisWatch.stop();
        }

        return new BackwardBoomerangResults<W>(query, aborted, this.queryToSolvers, getStats(), analysisWatch);
    }

    protected void backwardSolve(BackwardQuery query) {
        if (!options.aliasing())
            return;
        Optional<Stmt> unit = query.asNode().stmt().getUnit();
        AbstractBoomerangSolver<W> solver = queryToSolvers.getOrCreate(query);
        if (unit.isPresent()) {
            solver.solve(query.asNode());
        }
    }

    private AbstractBoomerangSolver<W> forwardSolve(ForwardQuery query) {
        Optional<Stmt> unit = query.asNode().stmt().getUnit();
        AbstractBoomerangSolver<W> solver = queryToSolvers.getOrCreate(query);
        if (unit.isPresent()) {
            if (isMultiArrayAllocation(unit.get()) && options.arrayFlows()) {
                // TODO fix; adjust as below;
                SingleNode<Node<Statement, Val>> sourveVal = new SingleNode<Node<Statement, Val>>(query.asNode());
                GeneratedState<Node<Statement, Val>, Field> genState = new GeneratedState<Node<Statement, Val>, Field>(
                        sourveVal, Field.array());
                insertTransition(solver.getFieldAutomaton(),
                        new Transition<Field, INode<Node<Statement, Val>>>(sourveVal, Field.array(), genState));
                insertTransition(solver.getFieldAutomaton(), new Transition<Field, INode<Node<Statement, Val>>>(
                        genState, Field.empty(), solver.getFieldAutomaton().getInitialState()));
            }
            if (isStringAllocation(unit.get())) {
                // Scene.v().forceResolve("java.lang.String",
                // SootClass.BODIES);
                SootClass stringClass = Scene.v().getSootClass("java.lang.String");
                if (stringClass.declaresField("char[] value")) {
                    SootField valueField = stringClass.getFieldByName("value");
                    SingleNode<Node<Statement, Val>> s = new SingleNode<Node<Statement, Val>>(query.asNode());
                    INode<Node<Statement, Val>> irState = solver.getFieldAutomaton().createState(s,
                            new Field(valueField));
                    insertTransition(solver.getFieldAutomaton(), new Transition<Field, INode<Node<Statement, Val>>>(
                            new SingleNode<Node<Statement, Val>>(query.asNode()), new Field(valueField), irState));
                    insertTransition(solver.getFieldAutomaton(), new Transition<Field, INode<Node<Statement, Val>>>(
                            irState, Field.empty(), solver.getFieldAutomaton().getInitialState()));
                }
            }
            if (query instanceof WeightedForwardQuery) {
                WeightedForwardQuery<W> q = (WeightedForwardQuery<W>) query;
                solver.solve(q.asNode(), q.weight());
            } else {
                solver.solve(query.asNode());
            }
        }
        return solver;
    }

    private boolean isStringAllocation(Stmt stmt) {
        if (stmt instanceof AssignStmt && ((AssignStmt) stmt).getRightOp() instanceof StringConstant) {
            return true;
        }
        return false;
    }

    private boolean insertTransition(final WeightedPAutomaton<Field, INode<Node<Statement, Val>>, W> aut,
            Transition<Field, INode<Node<Statement, Val>>> transition) {
        if (!aut.nested()) {
            return aut.addTransition(transition);
        }
        INode<Node<Statement, Val>> target = transition.getTarget();
        if (!(target instanceof GeneratedState)) {
            forwardFieldSummaries.putSummaryAutomaton(target, aut);

            aut.registerListener(new WPAUpdateListener<Field, INode<Node<Statement, Val>>, W>() {

                @Override
                public void onWeightAdded(Transition<Field, INode<Node<Statement, Val>>> t, W w,
                        WeightedPAutomaton<Field, INode<Node<Statement, Val>>, W> aut) {
                    if (t.getStart() instanceof GeneratedState) {
                        WeightedPAutomaton<Field, INode<Node<Statement, Val>>, W> n = forwardFieldSummaries
                                .getSummaryAutomaton(t.getStart());
                        aut.addNestedAutomaton(n);
                    }
                }
            });
            return aut.addTransition(transition);
        }
        final WeightedPAutomaton<Field, INode<Node<Statement, Val>>, W> nested = forwardFieldSummaries
                .getSummaryAutomaton(target);
        nested.registerListener(new WPAUpdateListener<Field, INode<Node<Statement, Val>>, W>() {

            @Override
            public void onWeightAdded(Transition<Field, INode<Node<Statement, Val>>> t, W w,
                    WeightedPAutomaton<Field, INode<Node<Statement, Val>>, W> aut) {
                if (t.getStart() instanceof GeneratedState) {
                    WeightedPAutomaton<Field, INode<Node<Statement, Val>>, W> n = forwardFieldSummaries
                            .getSummaryAutomaton(t.getStart());
                    aut.addNestedAutomaton(n);
                }
            }
        });
        return nested.addTransition(transition);
    }

    private boolean isMultiArrayAllocation(Stmt stmt) {
        return (stmt instanceof AssignStmt) && ((AssignStmt) stmt).getRightOp() instanceof NewMultiArrayExpr;
    }

    public class FieldWritePOI extends AbstractPOI<Statement, Val, Field> {

        public FieldWritePOI(Statement statement, Val base, Field field, Val stored) {
            super(statement, base, field, stored);
        }

        @Override
        public void execute(final ForwardQuery baseAllocation, final Query flowAllocation) {
            if (flowAllocation instanceof BackwardQuery) {
            } else if (flowAllocation instanceof ForwardQuery) {
                AbstractBoomerangSolver<W> baseSolver = queryToSolvers.get(baseAllocation);
                AbstractBoomerangSolver<W> flowSolver = queryToSolvers.get(flowAllocation);
                ExecuteImportFieldStmtPOI<W> exec = new ExecuteImportFieldStmtPOI<W>(WeightedBoomerang.this, baseSolver,
                        flowSolver, FieldWritePOI.this, getStmt()) {
                    public void activate(INode<Node<Statement, Val>> start) {
                        activateAllPois(new SolverPair(flowSolver, baseSolver), start);
                    };
                };
                registerActivationListener(new SolverPair(flowSolver, baseSolver), exec);
                exec.solve();
            }
        }

    }

    public class FieldReadPOI extends AbstractPOI<Statement, Val, Field> {

        public FieldReadPOI(Statement statement, Val base, Field field, Val stored) {
            super(statement, base, field, stored);
        }

        @Override
        public void execute(final ForwardQuery baseAllocation, final Query flowAllocation) {
            if (WeightedBoomerang.this instanceof WholeProgramBoomerang)
                throw new RuntimeException("should not be invoked!");
            if (flowAllocation instanceof ForwardQuery) {
            } else if (flowAllocation instanceof BackwardQuery) {
                AbstractBoomerangSolver<W> baseSolver = queryToSolvers.get(baseAllocation);
                AbstractBoomerangSolver<W> flowSolver = queryToSolvers.get(flowAllocation);
                for (Statement succ : flowSolver.getSuccsOf(getStmt())) {
                    ExecuteImportFieldStmtPOI<W> exec = new ExecuteImportFieldStmtPOI<W>(WeightedBoomerang.this,
                            baseSolver, flowSolver, FieldReadPOI.this, succ) {
                        public void activate(INode<Node<Statement, Val>> start) {
                            activateAllPois(new SolverPair(flowSolver, baseSolver), start);
                        };
                    };
                    registerActivationListener(new SolverPair(flowSolver, baseSolver), exec);
                    exec.solve();
                }
            }
        }

    }

    protected void activateAllPois(SolverPair pair, INode<Node<Statement, Val>> start) {
        if (activatedPoi.put(pair, start)) {
            Collection<ExecuteImportFieldStmtPOI<W>> listeners = poiListeners.get(pair);
            for (ExecuteImportFieldStmtPOI<W> l : Lists.newArrayList(listeners)) {
                l.trigger(start);
            }
        }
    }

    public void registerActivationListener(WeightedBoomerang<W>.SolverPair solverPair,
            ExecuteImportFieldStmtPOI<W> exec) {
        Collection<INode<Node<Statement, Val>>> listeners = activatedPoi.get(solverPair);
        for (INode<Node<Statement, Val>> node : Lists.newArrayList(listeners)) {
            exec.trigger(node);
        }
        poiListeners.put(solverPair, exec);
    }

    private class SolverPair {

        private AbstractBoomerangSolver<W> flowSolver;
        private AbstractBoomerangSolver<W> baseSolver;

        public SolverPair(AbstractBoomerangSolver<W> flowSolver, AbstractBoomerangSolver<W> baseSolver) {
            this.flowSolver = flowSolver;
            this.baseSolver = baseSolver;
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + getOuterType().hashCode();
            result = prime * result + ((baseSolver == null) ? 0 : baseSolver.hashCode());
            result = prime * result + ((flowSolver == null) ? 0 : flowSolver.hashCode());
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (obj == null)
                return false;
            if (getClass() != obj.getClass())
                return false;
            SolverPair other = (SolverPair) obj;
            if (!getOuterType().equals(other.getOuterType()))
                return false;
            if (baseSolver == null) {
                if (other.baseSolver != null)
                    return false;
            } else if (!baseSolver.equals(other.baseSolver))
                return false;
            if (flowSolver == null) {
                if (other.flowSolver != null)
                    return false;
            } else if (!flowSolver.equals(other.flowSolver))
                return false;
            return true;
        }

        private WeightedBoomerang getOuterType() {
            return WeightedBoomerang.this;
        }

    }

    public void createPOI(BiDiInterproceduralCFG<Unit, SootMethod> icfg, AbstractBoomerangSolver<W> baseSolver,
            AbstractBoomerangSolver<W> flowSolver, WeightedBoomerang<W>.FieldReadPOI fieldReadPOI, Statement succ) {
        // TODO Auto-generated method stub

    }

    public abstract ObservableICFG<Unit, SootMethod> icfg();

    protected abstract WeightFunctions<Statement, Val, Field, W> getForwardFieldWeights();

    protected abstract WeightFunctions<Statement, Val, Field, W> getBackwardFieldWeights();

    protected abstract WeightFunctions<Statement, Val, Statement, W> getBackwardCallWeights();

    protected abstract WeightFunctions<Statement, Val, Statement, W> getForwardCallWeights(ForwardQuery sourceQuery);

    public DefaultValueMap<Query, AbstractBoomerangSolver<W>> getSolvers() {
        return queryToSolvers;
    }

    public abstract Debugger<W> createDebugger();

    public void debugOutput() {
        Debugger<W> debugger = getOrCreateDebugger();
        debugger.done(queryToSolvers);
    }

    public Debugger<W> getOrCreateDebugger() {
        if (this.debugger == null)
            this.debugger = createDebugger();
        return debugger;
    }

    public SeedFactory<W> getSeedFactory() {
        return null;
    }

    public IBoomerangStats<W> getStats() {
        return stats;
    }

    public void onCreateSubSolver(Query key, AbstractBoomerangSolver<W> solver) {
        for (SolverCreationListener<W> l : solverCreationListeners) {
            l.onCreatedSolver(key, solver);
        }
    }

    public void registerSolverCreationListener(SolverCreationListener<W> l) {
        if(solverCreationListeners.add(l)) {
          for(Entry<Query, AbstractBoomerangSolver<W>> e : Lists.newArrayList(queryToSolvers.entrySet())) {
            l.onCreatedSolver(e.getKey(), e.getValue());
          }
        }
    }

    public Table<Statement, Val, W> getResults(Query seed) {
        final Table<Statement, Val, W> results = HashBasedTable.create();
        WeightedPAutomaton<Statement, INode<Val>, W> fieldAut = queryToSolvers.getOrCreate(seed).getCallAutomaton();
        for (Entry<Transition<Statement, INode<Val>>, W> e : fieldAut.getTransitionsToFinalWeights().entrySet()) {
            Transition<Statement, INode<Val>> t = e.getKey();
            W w = e.getValue();
            if (t.getLabel().equals(Statement.epsilon()))
                continue;
            if (t.getStart().fact().value() instanceof Local
                    && !t.getLabel().getMethod().equals(t.getStart().fact().m()))
                continue;
            if (t.getLabel().getUnit().isPresent())
                results.put(t.getLabel(), t.getStart().fact(), w);
        }
        return results;
    }

    public BoomerangOptions getOptions() {
        return this.options;
    }

}
