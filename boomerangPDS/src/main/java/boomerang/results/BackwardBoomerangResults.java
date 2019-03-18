package boomerang.results;

import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import com.google.common.base.Stopwatch;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import boomerang.BackwardQuery;
import boomerang.ForwardQuery;
import boomerang.Query;
import boomerang.Util;
import boomerang.jimple.AllocVal;
import boomerang.jimple.Field;
import boomerang.jimple.Statement;
import boomerang.jimple.Val;
import boomerang.solver.AbstractBoomerangSolver;
import boomerang.stats.IBoomerangStats;
import boomerang.util.AccessPath;
import com.google.common.base.Stopwatch;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import heros.utilities.DefaultValueMap;
import soot.Local;
import soot.PointsToSet;
import soot.Type;
import soot.jimple.ClassConstant;
import soot.jimple.NewExpr;
import sync.pds.solver.nodes.GeneratedState;
import sync.pds.solver.nodes.INode;
import sync.pds.solver.nodes.Node;
import wpds.impl.Transition;
import wpds.impl.Weight;
import wpds.impl.WeightedPAutomaton;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

public class BackwardBoomerangResults<W extends Weight> extends AbstractBoomerangResults<W> implements PointsToSet {

    private final BackwardQuery query;
    private Map<ForwardQuery, AbstractBoomerangResults<W>.Context> allocationSites;
    private final boolean timedout;
    private final IBoomerangStats<W> stats;
    private Stopwatch analysisWatch;
    private long maxMemory;

    public BackwardBoomerangResults(BackwardQuery query, boolean timedout,
            DefaultValueMap<Query, AbstractBoomerangSolver<W>> queryToSolvers, IBoomerangStats<W> stats,
            Stopwatch analysisWatch) {
        super(queryToSolvers);
        this.query = query;
        this.timedout = timedout;
        this.stats = stats;
        this.analysisWatch = analysisWatch;
        stats.terminated(query, this);
        maxMemory = Util.getReallyUsedMemory();
    }

    public Map<ForwardQuery, AbstractBoomerangResults<W>.Context> getAllocationSites() {
        computeAllocations();
        return allocationSites;
    }

    public boolean isTimedout() {
        return timedout;
    }

    public IBoomerangStats<W> getStats() {
        return stats;
    }

    public Stopwatch getAnalysisWatch() {
        return analysisWatch;
    }

    private void computeAllocations() {
        if (allocationSites != null)
            return;
        final Set<ForwardQuery> results = Sets.newHashSet();
        for (final Entry<Query, AbstractBoomerangSolver<W>> fw : queryToSolvers.entrySet()) {
            if (!(fw.getKey() instanceof ForwardQuery)) {
                continue;
            }
            fw.getValue().getFieldAutomaton().registerListener(new ExtractAllocationSiteStateListener<W>(
                    fw.getValue().getFieldAutomaton().getInitialState(), query, (ForwardQuery) fw.getKey()) {

                      @Override
                      protected void allocationSiteFound(ForwardQuery allocationSite,
                          BackwardQuery query) {
                        results.add(allocationSite);
                      }
            });
        }
        allocationSites = Maps.newHashMap();
        for (ForwardQuery q : results) {
            AbstractBoomerangResults<W>.Context context = constructContextGraph(q, query.asNode());
            assert allocationSites.get(q) == null;
            allocationSites.put(q, context);
        }
    }

    public boolean aliases(Query el) {
        for (final Query fw : getAllocationSites().keySet()) {
            if (fw instanceof BackwardQuery)
                continue;

            if (queryToSolvers.getOrCreate(fw).getReachedStates().contains(el.asNode())) {
                for (Transition<Field, INode<Node<Statement, Val>>> t : queryToSolvers.getOrCreate(fw)
                        .getFieldAutomaton().getTransitions()) {
                    if (t.getStart() instanceof GeneratedState) {
                        continue;
                    }
                    if (t.getStart().fact().equals(el.asNode()) && t.getLabel().equals(Field.empty())) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    @Deprecated
    public Set<AccessPath> getAllAliases(Statement stmt) {
        final Set<AccessPath> results = Sets.newHashSet();
        for (final Query fw : getAllocationSites().keySet()) {
            if (fw instanceof BackwardQuery)
                continue;
            queryToSolvers.getOrCreate(fw)
                    .registerListener(new ExtractAllAliasListener<W>(this.queryToSolvers.get(fw), results, stmt));

        }
        return results;
    }

    @Deprecated
    public Set<AccessPath> getAllAliases() {
        return getAllAliases(query.stmt());
    }

    @Override
    public boolean isEmpty() {
        computeAllocations();
        return allocationSites.isEmpty();
    }

    @Override
    public boolean hasNonEmptyIntersection(PointsToSet other) {
        if (other == this)
            return true;
        if (!(other instanceof BackwardBoomerangResults)) {
            throw new RuntimeException("Expected a points-to set of type " + BackwardBoomerangResults.class.getName());
        }
        BackwardBoomerangResults<W> otherRes = (BackwardBoomerangResults<W>) other;
        Map<ForwardQuery, AbstractBoomerangResults<W>.Context> otherAllocs = otherRes.getAllocationSites();
        boolean intersection = false;
        for (Entry<ForwardQuery, AbstractBoomerangResults<W>.Context> a : getAllocationSites().entrySet()) {
            for (Entry<ForwardQuery, AbstractBoomerangResults<W>.Context> b : otherAllocs.entrySet()) {
                if (a.getKey().equals(b.getKey()) && contextMatch(a.getValue(), b.getValue())) {
                    intersection = true;
                }
            }
        }
        return intersection;
    }

    private boolean contextMatch(AbstractBoomerangResults<W>.Context context,
            AbstractBoomerangResults<W>.Context context2) {
        return true;
    }

    @Override
    public Set<Type> possibleTypes() {
        computeAllocations();
        Set<Type> res = Sets.newHashSet();
        for (ForwardQuery q : allocationSites.keySet()) {
            Val fact = q.asNode().fact();
            if (fact.isNewExpr()) {
                AllocVal alloc = (AllocVal) fact;
                NewExpr expr = (NewExpr) alloc.allocationValue();
                res.add(expr.getType());
            } else {
                res.add(fact.value().getType());
            }
        }
        return res;
    }

    /**
     * Returns the set of types the backward analysis for the triggered query ever propagates.
     * 
     * @return Set of types the backward analysis propagates
     */
    public Set<Type> getPropagationType() {
        AbstractBoomerangSolver<W> solver = queryToSolvers.get(query);
        Set<Type> types = Sets.newHashSet();
        for (Transition<Statement, INode<Val>> t : solver.getCallAutomaton().getTransitions()) {
            types.add(t.getStart().fact().getType());
        }
        return types;
    }

    /**
     * Computes the set of statements (and variables at these statements) relevant for data-flow propagation. A
     * statement s is relevant, if a propagated variable x is used at s. I.e., when propagting x @ y = x, the returned
     * set contains x @ y = x, whereas it will not contain a call site x @ y = foo(c), because x is not used at the
     * statement.
     * 
     * @return The set of relevant statements during data-flow propagation
     */
    public Set<Node<Statement, Val>> getDataFlowPath(ForwardQuery query) {
        Set<Node<Statement, Val>> dataFlowPath = Sets.newHashSet();
        WeightedPAutomaton<Statement, INode<Val>, W> callAut = queryToSolvers.getOrCreate(query).getCallAutomaton();
        for (Entry<Transition<Statement, INode<Val>>, W> e : callAut.getTransitionsToFinalWeights().entrySet()) {
            Transition<Statement, INode<Val>> t = e.getKey();
            if (t.getLabel().equals(Statement.epsilon()))
                continue;
            if (t.getStart().fact().value() instanceof Local
                    && !t.getLabel().getMethod().equals(t.getStart().fact().m()))
                continue;
            if (queryToSolvers.getOrCreate(query).valueUsedInStatement(t.getLabel().getUnit().get(),
                    t.getStart().fact()))
                dataFlowPath.add(new Node<Statement, Val>(t.getLabel(), t.getStart().fact()));
        }
        return dataFlowPath;
    }
    
    @Override
    public Set<String> possibleStringConstants() {
        throw new RuntimeException("Not implemented!");
    }

    @Override
    public Set<ClassConstant> possibleClassConstants() {
        throw new RuntimeException("Not implemented!");
    }

    public long getMaxMemory() {
        return maxMemory;
    }
}
