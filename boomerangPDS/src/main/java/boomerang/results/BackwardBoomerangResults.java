package boomerang.results;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import com.google.common.base.Stopwatch;
import com.google.common.collect.Lists;
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
import heros.utilities.DefaultValueMap;
import soot.PointsToSet;
import soot.Type;
import soot.jimple.ClassConstant;
import soot.jimple.NewExpr;
import sync.pds.solver.SyncPDSUpdateListener;
import sync.pds.solver.nodes.GeneratedState;
import sync.pds.solver.nodes.INode;
import sync.pds.solver.nodes.Node;
import sync.pds.solver.nodes.SingleNode;
import wpds.impl.PAutomaton;
import wpds.impl.Transition;
import wpds.impl.Weight;
import wpds.impl.WeightedPAutomaton;
import wpds.interfaces.Empty;
import wpds.interfaces.WPAStateListener;
import wpds.interfaces.WPAUpdateListener;

public class BackwardBoomerangResults<W extends Weight> extends AbstractBoomerangResults<W> implements PointsToSet{

	private final BackwardQuery query;
	private Map<ForwardQuery,PAutomaton<Statement, INode<Val>>> allocationSites;
	private final boolean timedout;
	private final IBoomerangStats<W> stats;
	private Stopwatch analysisWatch;
	private long maxMemory;

	public BackwardBoomerangResults(BackwardQuery query, boolean timedout, DefaultValueMap<Query, AbstractBoomerangSolver<W>> queryToSolvers, IBoomerangStats<W> stats, Stopwatch analysisWatch) {
		super(queryToSolvers);
		this.query = query;
		this.timedout = timedout;
		this.stats = stats;
		this.analysisWatch = analysisWatch;
		stats.terminated(query, this);
		maxMemory = Util.getReallyUsedMemory();
	}
	public Map<ForwardQuery,PAutomaton<Statement, INode<Val>>> getAllocationSites(){
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
		if(allocationSites != null)
			return;
		final Set<ForwardQuery> results = Sets.newHashSet();
		for (final Entry<Query, AbstractBoomerangSolver<W>> fw : queryToSolvers.entrySet()) {
			if(!(fw.getKey() instanceof ForwardQuery)) {
				continue;
			}
			fw.getValue().getFieldAutomaton().registerListener(new ExtractAllocationSiteStateListener(fw.getValue().getFieldAutomaton().getInitialState(), query, (ForwardQuery) fw.getKey(), results));
		}
		allocationSites = Maps.newHashMap();
		for(ForwardQuery q : results) {
			PAutomaton<Statement,INode<Val>> context = constructContextGraph(q,query.var());
			assert allocationSites.get(q) == null;
//			System.out.println(context.toRegEx(new SingleNode<Val>(query.var()), new SingleNode<Val>(q.asNode().fact())));
			allocationSites.put(q, context);
		}
	}
	
	

	
	private class ExtractAllocationSiteStateListener extends WPAStateListener<Field, INode<Node<Statement, Val>>, W> {
		
		private ForwardQuery query;
		private Set<ForwardQuery> results;
		private BackwardQuery bwQuery;

		public ExtractAllocationSiteStateListener(INode<Node<Statement, Val>> state,  BackwardQuery bwQuery,ForwardQuery query, Set<ForwardQuery> results) {
			super(state);
			this.bwQuery = bwQuery;
			this.query = query;
			this.results = results;
		}

		@Override
		public void onOutTransitionAdded(Transition<Field, INode<Node<Statement, Val>>> t, W w,
				WeightedPAutomaton<Field, INode<Node<Statement, Val>>, W> weightedPAutomaton) {
		}
		
		@Override
		public void onInTransitionAdded(Transition<Field, INode<Node<Statement, Val>>> t, W w,
				WeightedPAutomaton<Field, INode<Node<Statement, Val>>, W> weightedPAutomaton) {
			if(t.getStart().fact().equals(bwQuery.asNode()) && t.getLabel().equals(Field.empty())){
				results.add(query);
			}
		}

		@Override
		public int hashCode() {
			//Otherwise we cannot register this listener twice.
			return System.identityHashCode(this);
		}

		@Override
		public boolean equals(Object obj) {
			//Otherwise we cannot register this listener twice.
			return this == obj;
		}
	}
	
	public boolean aliases(Query el) {
		for (final Query fw : getAllocationSites().keySet()) {
			if(fw instanceof BackwardQuery)
				continue;
			
			if(queryToSolvers.getOrCreate(fw).getReachedStates().contains(el.asNode())) {
				for(Transition<Field, INode<Node<Statement, Val>>> t :queryToSolvers.getOrCreate(fw).getFieldAutomaton().getTransitions()){
					if(t.getStart() instanceof GeneratedState){
						continue;
					}
					if(t.getStart().fact().equals(el.asNode()) && t.getLabel().equals(Field.empty())){
						return true;
					}
				}
			}
		}
		return false;
	}
	
	@Deprecated
	public Set<AccessPath> getAllAliases() {
		final Set<AccessPath> results = Sets.newHashSet();
		for (final Query fw : getAllocationSites().keySet()) {
			if(fw instanceof BackwardQuery)
				continue;
			queryToSolvers.getOrCreate(fw).registerListener(new SyncPDSUpdateListener<Statement, Val>() {
				
				@Override
				public void onReachableNodeAdded(Node<Statement, Val> reachableNode) {
					if(reachableNode.stmt().equals(query.stmt())){
						Val base = reachableNode.fact();
						final INode<Node<Statement, Val>> allocNode = queryToSolvers.getOrCreate(fw).getFieldAutomaton().getInitialState();			
						queryToSolvers.getOrCreate(fw).getFieldAutomaton().registerListener(new WPAUpdateListener<Field, INode<Node<Statement, Val>>, W>() {
							@Override
							public void onWeightAdded(Transition<Field, INode<Node<Statement, Val>>> t, W w,
									WeightedPAutomaton<Field, INode<Node<Statement, Val>>, W> aut) {
								if(t.getStart().fact().stmt().equals(query.stmt()) && !(t.getStart() instanceof GeneratedState) && t.getStart().fact().fact().equals(base)){
									if (t.getLabel().equals(Field.empty())) {
										if (t.getTarget().equals(allocNode)) {
											results.add(new AccessPath(base));
										}
									}
									List<Transition<Field, INode<Node<Statement, Val>>>> fields = Lists.newArrayList();
									if (!(t.getLabel() instanceof Empty)) {
										fields.add(t);
									}
									queryToSolvers.getOrCreate(fw).getFieldAutomaton().registerListener(new ExtractAccessPathStateListener(t.getTarget(),allocNode,base, fields, results));
								}
							}
						});
					}
				}
			});
			
		}
		return results;
	}
	
	private class ExtractAccessPathStateListener extends WPAStateListener<Field, INode<Node<Statement, Val>>, W> {

		private INode<Node<Statement, Val>> allocNode;
		private Collection<Transition<Field, INode<Node<Statement, Val>>>> fields;
		private Set<AccessPath> results;
		private Val base;

		public ExtractAccessPathStateListener(INode<Node<Statement, Val>> state, INode<Node<Statement, Val>> allocNode,
				Val base, Collection<Transition<Field, INode<Node<Statement, Val>>>> fields, Set<AccessPath> results) {
			super(state);
			this.allocNode = allocNode;
			this.base = base;
			this.fields = fields;
			this.results = results;
		}

		@Override
		public void onOutTransitionAdded(Transition<Field, INode<Node<Statement, Val>>> t, W w,
				WeightedPAutomaton<Field, INode<Node<Statement, Val>>, W> weightedPAutomaton) {
			if(t.getLabel().equals(Field.epsilon()))
				return;
			Collection<Transition<Field, INode<Node<Statement, Val>>>> copiedFields = (fields instanceof Set ? Sets.newHashSet(fields) : Lists.newArrayList(fields));
			if (!t.getLabel().equals(Field.empty())) {
				if(copiedFields.contains(t)){
					copiedFields = Sets.newHashSet(fields);
				}
				if(!(t.getLabel() instanceof Empty))
					copiedFields.add(t);
			}
			if (t.getTarget().equals(allocNode)) {
				
				results.add(new AccessPath(base, convert(copiedFields)));
			} 
			weightedPAutomaton.registerListener(
							new ExtractAccessPathStateListener(t.getTarget(), allocNode, base, copiedFields, results));
		}

		private Collection<Field> convert(Collection<Transition<Field, INode<Node<Statement, Val>>>> fields) {
			Collection<Field> res;
			if(fields instanceof List) {
				res = Lists.newArrayList();
			} else {
				res = Sets.newHashSet();
			}
			for(Transition<Field, INode<Node<Statement, Val>>> f : fields) {
				res.add(f.getLabel());
			}
			return res;
		}

		@Override
		public void onInTransitionAdded(Transition<Field, INode<Node<Statement, Val>>> t, W w,
				WeightedPAutomaton<Field, INode<Node<Statement, Val>>, W> weightedPAutomaton) {
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = super.hashCode();
			result = prime * result + getOuterType().hashCode();
			result = prime * result + ((allocNode == null) ? 0 : allocNode.hashCode());
			result = prime * result + ((base == null) ? 0 : base.hashCode());
			result = prime * result + ((fields == null) ? 0 : fields.hashCode());
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
			ExtractAccessPathStateListener other = (ExtractAccessPathStateListener) obj;
			if (!getOuterType().equals(other.getOuterType()))
				return false;
			if (allocNode == null) {
				if (other.allocNode != null)
					return false;
			} else if (!allocNode.equals(other.allocNode))
				return false;
			if (base == null) {
				if (other.base != null)
					return false;
			} else if (!base.equals(other.base))
				return false;
			if (fields == null) {
				if (other.fields != null)
					return false;
			} else if (!fields.equals(other.fields))
				return false;
			return true;
		}

		private BackwardBoomerangResults getOuterType() {
			return BackwardBoomerangResults.this;
		}

	}

	
	@Override
	public boolean isEmpty() {
		computeAllocations();
		return allocationSites.isEmpty();
	}
	@Override
	public boolean hasNonEmptyIntersection(PointsToSet other) {
		if(other == this)
			return true;
		if(!(other instanceof BackwardBoomerangResults)) {
			throw new RuntimeException("Expected a points-to set of type " + BackwardBoomerangResults.class.getName());
		}
		BackwardBoomerangResults<W> otherRes = (BackwardBoomerangResults<W>) other;
		Map<ForwardQuery, PAutomaton<Statement, INode<Val>>> otherAllocs = otherRes.getAllocationSites();
		boolean intersection = false;
		for(Entry<ForwardQuery, PAutomaton<Statement, INode<Val>>> a : getAllocationSites().entrySet()) {
			for(Entry<ForwardQuery, PAutomaton<Statement, INode<Val>>> b : otherAllocs.entrySet()) {
				if(a.getKey().equals(b.getKey()) && contextMatch(a.getValue(),b.getValue())) {
					intersection = true;
				}
			}	
		}
		return intersection;
	}
	
	private boolean contextMatch(PAutomaton<Statement, INode<Val>> pAutomaton, PAutomaton<Statement, INode<Val>> pAutomaton2) {
		return true;
	}
	
	@Override
	public Set<Type> possibleTypes() {
		computeAllocations();
		Set<Type> res = Sets.newHashSet();
		for(ForwardQuery q : allocationSites.keySet()) {
			Val fact = q.asNode().fact();
			if(fact.isNewExpr()) {
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
	 * @return Set of types the backward analysis propagates
	 */
	public Set<Type> getPropagationType(){
		AbstractBoomerangSolver<W> solver = queryToSolvers.get(query);
		Set<Type> types = Sets.newHashSet();
		for(Transition<Statement, INode<Val>> t :solver.getCallAutomaton().getTransitions()) {
			types.add(t.getStart().fact().getType());
		}
		return types;
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
