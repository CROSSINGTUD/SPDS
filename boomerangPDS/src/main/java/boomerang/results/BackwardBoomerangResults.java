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
import boomerang.WeightedBoomerang.AccessPathBackwardQuery;
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
import sync.pds.solver.nodes.AllocNode;
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

public class BackwardBoomerangResults<W extends Weight> implements PointsToSet{

	private final BackwardQuery query;
	private final DefaultValueMap<Query, AbstractBoomerangSolver<W>> queryToSolvers;
	private Map<ForwardQuery,PAutomaton<Statement, INode<Val>>> allocationSites;
	private final boolean timedout;
	private final IBoomerangStats<W> stats;
	private Stopwatch analysisWatch;
	private long maxMemory;

	public BackwardBoomerangResults(BackwardQuery query, boolean timedout, DefaultValueMap<Query, AbstractBoomerangSolver<W>> queryToSolvers, IBoomerangStats<W> stats, Stopwatch analysisWatch) {
		this.query = query;
		this.queryToSolvers = queryToSolvers;
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
			if(query instanceof AccessPathBackwardQuery) {
				SingleNode<Node<Statement, Val>> startNode = new SingleNode<Node<Statement,Val>>(query.asNode());
				for(Statement pred : fw.getValue().getPredsOf(query.stmt())){
					fw.getValue().getFieldAutomaton().registerListener(new IntersectionListener(startNode,fw.getValue().getFieldAutomaton(), new SingleNode<Node<Statement,Val>>(new Node<Statement,Val>(pred,query.asNode().fact())),((AccessPathBackwardQuery)query).getFieldAutomaton()) {
						
						@Override
						protected void intersect(Transition<Field, INode<Node<Statement, Val>>> tA,
								Transition<Field, INode<Node<Statement, Val>>> tB) {
							System.out.println("INTERSECT " + tA);
							if(tA.getLabel().equals(Field.empty())) {
								results.add((ForwardQuery) fw.getKey());
							}
						}
					});
				}
			} else {
				fw.getValue().getFieldAutomaton().registerListener(new ExtractAllocationSiteStateListener(fw.getValue().getFieldAutomaton().getInitialState(), query, (ForwardQuery) fw.getKey(), results));
			}
		}
		allocationSites = Maps.newHashMap();
		for(ForwardQuery q : results) {
			PAutomaton<Statement,INode<Val>> context = constructContextGraph(queryToSolvers.get(q));
			assert allocationSites.get(q) == null;
//			System.out.println(context.toRegEx(new SingleNode<Val>(query.var()), new SingleNode<Val>(q.asNode().fact())));
			allocationSites.put(q, context);
		}
	}
	private PAutomaton<Statement,INode<Val>> constructContextGraph(AbstractBoomerangSolver<W> solver) {
		WeightedPAutomaton<Statement, INode<Val>, W> callAutomaton = solver.getCallAutomaton();
		SingleNode<Val> initialState = new SingleNode<Val>(query.asNode().fact());
		PAutomaton<Statement,INode<Val>> aut = new PAutomaton<Statement,INode<Val>>(initialState) {

			@Override
			public INode<Val> createState(INode<Val> d, Statement loc) {
				throw new RuntimeException("Not implemented");
			}

			@Override
			public boolean isGeneratedState(INode<Val> d) {
				throw new RuntimeException("Not implemented");
			}

			@Override
			public Statement epsilon() {
				return Statement.epsilon();
			}

		};
		callAutomaton.registerListener(new CallStackExtracter(new SingleNode<Val>(query.asNode().fact()),new SingleNode<Val>(query.asNode().fact()),aut, solver));
		return aut;
	}
	
	private class CallStackExtracter extends WPAStateListener<Statement, INode<Val>, W>{

		private AbstractBoomerangSolver<W> solver;
		private INode<Val> source;
		private PAutomaton<Statement, INode<Val>> aut;

		public CallStackExtracter(INode<Val> state, INode<Val> source,PAutomaton<Statement, INode<Val>> aut, AbstractBoomerangSolver<W> solver) {
			super(state);
			this.source = source;
			this.aut = aut;
			this.solver = solver;
		}

		@Override
		public void onOutTransitionAdded(Transition<Statement, INode<Val>> t, W w,
				WeightedPAutomaton<Statement, INode<Val>, W> weightedPAutomaton) {
			if(t.getLabel().getMethod() != null) {
				if(t.getStart() instanceof GeneratedState) {
					Set<Statement> succsOf = solver.getPredsOf(t.getLabel());
					for(Statement s : succsOf) {
						aut.addTransition(new Transition<Statement,INode<Val>>(source,s,t.getTarget()));
					}
				} else {
					weightedPAutomaton.registerListener(new CallStackExtracter(t.getTarget(),source, aut, solver));
					return;
				}
			}
			weightedPAutomaton.registerListener(new CallStackExtracter(t.getTarget(),t.getTarget(), aut, solver));
		}

		@Override
		public void onInTransitionAdded(Transition<Statement, INode<Val>> t, W w,
				WeightedPAutomaton<Statement, INode<Val>, W> weightedPAutomaton) {
			
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = super.hashCode();
			result = prime * result + getOuterType().hashCode();
			result = prime * result + ((source == null) ? 0 : source.hashCode());
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
			CallStackExtracter other = (CallStackExtracter) obj;
			if (!getOuterType().equals(other.getOuterType()))
				return false;
			if (source == null) {
				if (other.source != null)
					return false;
			} else if (!source.equals(other.source))
				return false;
			return true;
		}

		private BackwardBoomerangResults getOuterType() {
			return BackwardBoomerangResults.this;
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
			types.add(t.getStart().fact().value().getType());
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
	private class ExtractAllocationSiteAccesssPathStateListener extends WPAStateListener<Field, INode<Node<Statement, Val>>, W> {
		
		private ForwardQuery query;
		private Set<ForwardQuery> results;
		private AccessPathBackwardQuery bwQuery;

		public ExtractAllocationSiteAccesssPathStateListener(INode<Node<Statement, Val>> state,  AccessPathBackwardQuery bwQuery,ForwardQuery query, Set<ForwardQuery> results) {
			super(state);
			this.bwQuery = bwQuery;
			this.query = query;
			this.results = results;
		}

		@Override
		public void onOutTransitionAdded(Transition<Field, INode<Node<Statement, Val>>> t, W w,
				WeightedPAutomaton<Field, INode<Node<Statement, Val>>, W> weightedPAutomaton) {
			
			if(t.getStart().fact().equals(bwQuery.asNode()) && t.getLabel().equals(Field.empty())){
				results.add(query);
			}
		}
		
		@Override
		public void onInTransitionAdded(Transition<Field, INode<Node<Statement, Val>>> t, W w,
				WeightedPAutomaton<Field, INode<Node<Statement, Val>>, W> weightedPAutomaton) {
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
	
	private abstract class IntersectionListener extends WPAStateListener<Field, INode<Node<Statement,Val>>, W>{
		public IntersectionListener(INode<Node<Statement, Val>> stateA,WeightedPAutomaton<Field, INode<Node<Statement, Val>>, W> autA,INode<Node<Statement, Val>> stateB,WeightedPAutomaton<Field, INode<Node<Statement, Val>>, W> autB) {
			super(stateA);
			this.autA = autA;
			this.autB = autB;
			this.stateB = stateB;
		}

		WeightedPAutomaton<Field, INode<Node<Statement, Val>>, W> autA;
		INode<Node<Statement,Val>> stateA;
		WeightedPAutomaton<Field, INode<Node<Statement, Val>>, W> autB;
		INode<Node<Statement,Val>> stateB;
		@Override
		public void onOutTransitionAdded(Transition<Field, INode<Node<Statement, Val>>> tA, W w,
				WeightedPAutomaton<Field, INode<Node<Statement, Val>>, W> weightedPAutomaton) {
			System.out.println("INTERSECT A " + tA.getLabel());
			autB.registerListener(new WPAStateListener<Field, INode<Node<Statement,Val>>, W>(stateB) {

				@Override
				public void onOutTransitionAdded(Transition<Field, INode<Node<Statement, Val>>> tB, W w,
						WeightedPAutomaton<Field, INode<Node<Statement, Val>>, W> weightedPAutomaton) {
					if(tA.getLabel().equals(tB.getLabel())) {
						intersect(tA,tB);
						System.out.println("INTERSECT REACHED " + tA);
						System.out.println("INTERSECT REACHED " + tB);
						BackwardBoomerangResults<W>.IntersectionListener outer = IntersectionListener.this;
						autA.registerListener(new IntersectionListener(tA.getTarget(),autA, tB.getTarget(), autB) {
							@Override
							protected void intersect(Transition<Field, INode<Node<Statement, Val>>> tA,
									Transition<Field, INode<Node<Statement, Val>>> tB) {
								outer.intersect(tA, tB);
							}
						});
					}
				}

				@Override
				public void onInTransitionAdded(Transition<Field, INode<Node<Statement, Val>>> tB, W w,
						WeightedPAutomaton<Field, INode<Node<Statement, Val>>, W> weightedPAutomaton) {
					System.out.println("INTERSECT B " + tB.getLabel());
				
				}
				@Override
				public int hashCode() {
					return System.identityHashCode(this);
				}
				@Override
				public boolean equals(Object obj) {
					return this == obj;
				}
			});
		}

		protected abstract void intersect(Transition<Field, INode<Node<Statement, Val>>> tA,
				Transition<Field, INode<Node<Statement, Val>>> tB);

		@Override
		public void onInTransitionAdded(Transition<Field, INode<Node<Statement, Val>>> t, W w,
				WeightedPAutomaton<Field, INode<Node<Statement, Val>>, W> weightedPAutomaton) {
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = super.hashCode();
			result = prime * result + getOuterType().hashCode();
			result = prime * result + ((autA == null) ? 0 : autA.hashCode());
			result = prime * result + ((autB == null) ? 0 : autB.hashCode());
			result = prime * result + ((stateA == null) ? 0 : stateA.hashCode());
			result = prime * result + ((stateB == null) ? 0 : stateB.hashCode());
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
			IntersectionListener other = (IntersectionListener) obj;
			if (!getOuterType().equals(other.getOuterType()))
				return false;
			if (autA == null) {
				if (other.autA != null)
					return false;
			} else if (!autA.equals(other.autA))
				return false;
			if (autB == null) {
				if (other.autB != null)
					return false;
			} else if (!autB.equals(other.autB))
				return false;
			if (stateA == null) {
				if (other.stateA != null)
					return false;
			} else if (!stateA.equals(other.stateA))
				return false;
			if (stateB == null) {
				if (other.stateB != null)
					return false;
			} else if (!stateB.equals(other.stateB))
				return false;
			return true;
		}

		private BackwardBoomerangResults getOuterType() {
			return BackwardBoomerangResults.this;
		}
		
	}
}
