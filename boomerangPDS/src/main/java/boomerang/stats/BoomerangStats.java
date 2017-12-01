package boomerang.stats;

import java.util.Map;
import java.util.Set;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import boomerang.Query;
import boomerang.WeightedBoomerang;
import boomerang.jimple.Field;
import boomerang.jimple.Statement;
import boomerang.jimple.Val;
import boomerang.solver.AbstractBoomerangSolver;
import soot.SootMethod;
import sync.pds.solver.SyncPDSUpdateListener;
import sync.pds.solver.WitnessNode;
import sync.pds.solver.nodes.INode;
import sync.pds.solver.nodes.Node;
import wpds.impl.Rule;
import wpds.impl.Transition;
import wpds.impl.Weight;
import wpds.impl.WeightedPAutomaton;
import wpds.interfaces.Location;
import wpds.interfaces.State;
import wpds.interfaces.WPAUpdateListener;
import wpds.interfaces.WPDSUpdateListener;

public class BoomerangStats<W extends Weight> {

	private Map<Query, AbstractBoomerangSolver<W>> queries = Maps.newHashMap();
	private Set<WeightedTransition<Field, INode<Node<Statement, Val>>, W>> globalFieldTransitions = Sets.newHashSet();
	private int fieldTransitionCollisions;
	private Set<WeightedTransition<Statement, INode<Val>, W>> globalCallTransitions = Sets.newHashSet();
	private int callTransitionCollisions;
	private Set<Rule<Field, INode<Node<Statement, Val>>, W>> globalFieldRules = Sets.newHashSet();
	private int fieldRulesCollisions;
	private Set<Rule<Statement, INode<Val>, W>> globalCallRules = Sets.newHashSet();
	private int callRulesCollisions;
	private Set<Node<Statement,Val>> reachedNodes = Sets.newHashSet();
	private int reachedNodeCollisions;
	private Set<SootMethod> visitedMethods = Sets.newHashSet();
	private int arrayFlows;
	private int staticFlows;
	private int callSitePOIs;
	private int fieldWritePOIs;
	private int fieldReadPOIs;

	public void registerSolver(Query key, AbstractBoomerangSolver<W> solver) {
		if (queries.containsKey(key)) {
			return;
		}
		queries.put(key, solver);
		solver.getFieldAutomaton().registerListener(new WPAUpdateListener<Field, INode<Node<Statement, Val>>, W>() {
			@Override
			public void onWeightAdded(Transition<Field, INode<Node<Statement, Val>>> t, W w,
					WeightedPAutomaton<Field, INode<Node<Statement, Val>>, W> aut) {
				if (!globalFieldTransitions.add(new WeightedTransition<Field, INode<Node<Statement, Val>>, W>(t, w))) {
					fieldTransitionCollisions++;
				}
				if(t.getLabel().equals(Field.array())){
					arrayFlows++;
				}
			}
		});

		solver.getCallAutomaton().registerListener(new WPAUpdateListener<Statement, INode<Val>, W>() {
			@Override
			public void onWeightAdded(Transition<Statement, INode<Val>> t, W w,
					WeightedPAutomaton<Statement, INode<Val>, W> aut) {
				if (!globalCallTransitions.add(new WeightedTransition<Statement, INode<Val>, W>(t, w))) {
					callTransitionCollisions++;
				}
				visitedMethods.add(t.getLabel().getMethod());

				if(t.getStart().fact().isStatic()){
					staticFlows++;
				}
			}
		});

		solver.getFieldPDS().registerUpdateListener(new WPDSUpdateListener<Field, INode<Node<Statement, Val>>, W>() {
			@Override
			public void onRuleAdded(Rule<Field, INode<Node<Statement, Val>>, W> rule) {
				if (!globalFieldRules.add(rule)) {
					fieldRulesCollisions++;
				}
			}
		});
		solver.getCallPDS().registerUpdateListener(new WPDSUpdateListener<Statement, INode<Val>, W>() {

			@Override
			public void onRuleAdded(Rule<Statement, INode<Val>, W> rule) {
				if (!globalCallRules.add(rule)) {
					callRulesCollisions++;
				}
			}
		});
		
		solver.registerListener(new SyncPDSUpdateListener<Statement, Val, Field>() {
			
			@Override
			public void onReachableNodeAdded(WitnessNode<Statement, Val, Field> reachableNode) {
				if(!reachedNodes.add(reachableNode.asNode())){
					reachedNodeCollisions++;
				}
			}
		});
	}

	public void registerCallSitePOI(WeightedBoomerang<W>.ForwardCallSitePOI key) {
		callSitePOIs++;
	}

	public void registerFieldWritePOI(WeightedBoomerang<W>.FieldWritePOI key) {
		fieldWritePOIs++;
	}

	public void registerFieldReadPOI(WeightedBoomerang<W>.FieldReadPOI key) {
		fieldReadPOIs++;
	}
	
	public String toString(){
		String s = "=========== Boomerang Stats =============\n";
		s+= String.format("Queries: \t\t %s\n", queries.keySet().size());
		s+= String.format("Visited Methods: \t\t %s\n", visitedMethods.size());
		s+= String.format("Reached Nodes(Collisions): \t\t %s (%s)\n", reachedNodes.size(),reachedNodeCollisions);
		s+= String.format("Point of Indirections (Store/Load/Callsite): \t\t %s/%s/%s\n", fieldWritePOIs,fieldReadPOIs,callSitePOIs);
		s+= String.format("Global Field Rules(Collisions): \t\t %s (%s)\n", globalFieldRules.size(),fieldRulesCollisions);
		s+= String.format("Global Field Transitions(Collisions): \t\t %s (%s)\n", globalFieldTransitions.size(),fieldTransitionCollisions);
		s+= String.format("Global Call Rules(Collisions): \t\t %s (%s)\n", globalCallRules.size(),callRulesCollisions);
		s+= String.format("Global Call Transitions(Collisions): \t\t %s (%s)\n", globalCallTransitions.size(),callTransitionCollisions);
		s+= String.format("Special Flows (Static/Array): \t\t %s(%s)/%s(%s)\n", staticFlows,globalCallTransitions.size(),arrayFlows,globalFieldTransitions.size());
		s+= computeMetrics();
		s+="\n";
		return s;
	}	

	public Set<SootMethod> getVisitedMethods() {
		return Sets.newHashSet(visitedMethods);
	}
	public String computeMetrics(){
		int min = Integer.MAX_VALUE;
		int totalReached = 0;
		int max = 0;
		Query maxQuery = null;
		for (Query q : queries.keySet()) {
			int size = queries.get(q).getReachedStates().size();
			totalReached += size;
			min = Math.min(size, min);
			if(size > max){
				maxQuery = q;
			}
			max = Math.max(size, max);
			
		}
		float average = ((float) totalReached )/ queries.keySet().size();
		String s = String.format("Reachable nodes (Min/Avg/Max): \t\t%s/%s/%s\n", min, average, max); 
		s += String.format("Maximal Query: \t\t%s\n", maxQuery);
		return s;
	}

	

	private static class WeightedTransition<X extends Location, Y extends State, W> {
		final Transition<X, Y> t;
		final W w;

		public WeightedTransition(Transition<X, Y> t, W w) {
			this.t = t;
			this.w = w;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((t == null) ? 0 : t.hashCode());
			result = prime * result + ((w == null) ? 0 : w.hashCode());
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
			WeightedTransition other = (WeightedTransition) obj;
			if (t == null) {
				if (other.t != null)
					return false;
			} else if (!t.equals(other.t))
				return false;
			if (w == null) {
				if (other.w != null)
					return false;
			} else if (!w.equals(other.w))
				return false;
			return true;
		}
	}

}
