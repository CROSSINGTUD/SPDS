package analysis;

import java.util.AbstractMap;
import java.util.Collection;
import java.util.LinkedList;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;

import wpds.impl.PAutomaton;
import wpds.impl.PopRule;
import wpds.impl.PushRule;
import wpds.impl.PushdownSystem;
import wpds.impl.Rule;
import wpds.impl.Transition;
import wpds.impl.Weight.NoWeight;
import wpds.interfaces.Location;

public abstract class Solver<Stmt extends Location, Fact, Field extends Location> {
	private PushdownSystem<Stmt, INode<Fact>> callingContextPDS = new PushdownSystem<Stmt, INode<Fact>>() {
	};
	private PushdownSystem<Field, NodeWithLocation<Stmt, Fact, Field>> fieldRefContextPDS = new PushdownSystem<Field, NodeWithLocation<Stmt, Fact, Field>>() {
	};
	private LinkedList<Node<Stmt, Fact>> worklist = Lists.newLinkedList();
	private Node<Stmt, Fact> seed;
	private Set<Node<Stmt, Fact>> reachedStates = Sets.newHashSet();
	private Multimap<Node<Stmt,Fact>,Node<Stmt,Fact>> solvedEdges = HashMultimap.create();

	public void solve(Node<Stmt, Fact> curr) {
		this.seed = curr;
		worklist.add(curr);
		awaitEmptyWorklist();
	}

	private void awaitEmptyWorklist() {
		while (!worklist.isEmpty()) {
			Node<Stmt, Fact> curr = worklist.poll();
			reachedStates.add(curr);
			
			Collection<Node<Stmt, Fact>> successors = computeSuccessor(curr);
			System.err.println(curr + " FLOWS TO " + successors);
			for (Node<Stmt, Fact> succ : successors) {
				if(!addEdge(curr,succ))
					continue;
				
				Collection<Rule<Field, NodeWithLocation<Stmt, Fact, Field>, NoWeight<Field>>> fieldRules = computeFieldRefRule(withField(curr), withField(succ));
				boolean check = false;
				for(Rule<Field, NodeWithLocation<Stmt, Fact, Field>, NoWeight<Field>> fieldRule : fieldRules){
					fieldRefContextPDS.addRule(fieldRule);
					if (fieldRule instanceof PopRule) {
						checkFeasibility(succ);	
						check = true;
					}
				}

				Collection<Rule<Stmt, INode<Fact>, NoWeight<Stmt>>> callRules = computeCallSiteRule(curr, succ);
				for(Rule<Stmt, INode<Fact>, NoWeight<Stmt>> callRule : callRules){
					System.err.println(curr +" succ " + succ + callRule);
					callingContextPDS.addRule(callRule);
					if (callRule instanceof PopRule) {
						check = true;	
					}
					if(callRule instanceof PushRule){
						PushRule pushRule = (PushRule) callRule;
						worklist.add(new Node<Stmt,Fact>((Stmt) ((PushRule) callRule).getL2(), callRule.getS2().fact()));
					}
				}
				
				if(check){
					checkFeasibility(succ);
				} else{
					worklist.add(succ);
				}
			}
		}
	}

	private boolean addEdge(Node<Stmt, Fact> curr, Node<Stmt, Fact> succ) {
		return solvedEdges.put(curr,succ);
	}

	private void checkFeasibility(Node<Stmt, Fact> succ) {
		PAutomaton<Field, NodeWithLocation<Stmt, Fact,Field>> aut1 = new PAutomaton<Field, NodeWithLocation<Stmt, Fact,Field>>(withField(seed), withField(seed)) {
			@Override
			public  NodeWithLocation<Stmt, Fact,Field> createState(NodeWithLocation<Stmt, Fact,Field> d, Field loc) {
				return new NodeWithLocation<Stmt, Fact, Field>(d.stmt, d.variable, loc);
			}
			@Override
			public Field epsilon() {
				return epsilonField();
			}
		};
			aut1.addTransition(new Transition<Field, NodeWithLocation<Stmt, Fact,Field>>(withField(seed), emptyField(), withField(seed)));
		fieldRefContextPDS.poststar(aut1);
		boolean pds1 = aut1.getStates().contains(withField(succ));

		PAutomaton<Stmt, INode<Fact>> aut2 = new PAutomaton<Stmt, INode<Fact>>(wrap(seed.variable), wrap(seed.variable)) {
			@Override
			public  INode<Fact> createState(INode<Fact> d, Stmt loc) {
				return generateState(d,loc);
			}
			
			

			@Override
			public Stmt epsilon() {
				return epsilonCallSite();
			}
		};
			aut2.addTransition(new Transition<Stmt, INode<Fact>>(wrap(seed.variable),seed.stmt(), wrap(seed.variable)));
		callingContextPDS.poststar(aut2);
		System.out.println(aut2);
		System.out.println(aut2.getStates());
		boolean pds2 = aut2.getStates().contains(new SingleNode<Fact>(succ.variable));
		if (pds1 && pds2) {
			System.out.println("IS feasable!" + succ);
			worklist.add(succ);
		} 
	}

	private INode<Fact> wrap(Fact variable) {
		return new SingleNode<Fact>(variable);
	}
	
	

	Map<Entry<INode<Fact>, Stmt>, INode<Fact>> generatedState = Maps.newHashMap();
	
	protected INode<Fact> generateState(final INode<Fact> d, final Stmt loc) {
		Entry<INode<Fact>, Stmt> e = new AbstractMap.SimpleEntry<>(d,loc);
		if(!generatedState.containsKey(e)){
			generatedState.put(e, new INode<Fact>() {
				@Override
				public Fact fact() {
					return null;
				}
				@Override
				public String toString() {
					return d + " " + loc;
				}
			});
		}
			return generatedState.get(e);
	}

	private NodeWithLocation<Stmt, Fact, Field> withField(Node<Stmt,Fact> node) {
		return new NodeWithLocation<Stmt, Fact, Field>(node.stmt, node.variable, emptyField());
	}

	public abstract Collection<Rule<Stmt, INode<Fact>, NoWeight<Stmt>>> computeCallSiteRule(Node<Stmt, Fact> curr,
			Node<Stmt, Fact> succ);

	public abstract Collection<Rule<Field, NodeWithLocation<Stmt, Fact, Field>, NoWeight<Field>>> computeFieldRefRule(NodeWithLocation<Stmt, Fact, Field> curr,
			NodeWithLocation<Stmt, Fact, Field> succ);

	public abstract Collection<Node<Stmt, Fact>> computeSuccessor(Node<Stmt, Fact> node);

	public abstract Field epsilonField();
	
	public abstract Field emptyField();

	public abstract Stmt epsilonCallSite();

	public abstract Stmt emptyCallSite();
	
	public Set<Node<Stmt, Fact>> getReachedStates() {
		return Sets.newHashSet(reachedStates);
	}
}
