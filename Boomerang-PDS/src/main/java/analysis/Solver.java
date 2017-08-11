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
import wpds.impl.PushdownSystem;
import wpds.impl.Rule;
import wpds.impl.Transition;
import wpds.impl.Weight.NoWeight;
import wpds.interfaces.Location;

public abstract class Solver<Stmt, Fact, Field extends Location, CallSite extends Location> {
	private PushdownSystem<CallSite, INode<Stmt, Fact>> callingContextPDS = new PushdownSystem<CallSite, INode<Stmt, Fact>>() {
	};
	private PushdownSystem<Field, NodeWithLocation<Stmt, Fact, Field>> fieldRefContextPDS = new PushdownSystem<Field, NodeWithLocation<Stmt, Fact, Field>>() {
	};
	private LinkedList<Node<Stmt, Fact>> worklist = Lists.newLinkedList();
	private Node<Stmt, Fact> seed;
	private Set<Node<Stmt, Fact>> reachedStates = Sets.newHashSet();
	private Set<Field> fieldOutOfSeed = Sets.newHashSet();
	private Set<CallSite> callsiteOutOfSeed = Sets.newHashSet();
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
				Rule<Field, NodeWithLocation<Stmt, Fact, Field>, NoWeight<Field>> fieldRule = computeFieldRefRule(withField(curr), withField(succ));
				fieldRefContextPDS.addRule(fieldRule);

				Rule<CallSite, INode<Stmt, Fact>, NoWeight<CallSite>> callRule = computeCallSiteRule(curr, succ);
				System.err.println(curr +" succ " + succ + callRule);
				callingContextPDS.addRule(callRule);

				if (curr.equals(seed)) {
					callsiteOutOfSeed.add(callRule.getL1());
					fieldOutOfSeed.add(fieldRule.getL1());
				}
				
				if (callRule instanceof PopRule || fieldRule instanceof PopRule) {
					checkFeasibility(succ);	
				} else {
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

		PAutomaton<CallSite, INode<Stmt, Fact>> aut2 = new PAutomaton<CallSite, INode<Stmt, Fact>>(seed, seed) {
			@Override
			public  INode<Stmt, Fact> createState(INode<Stmt, Fact> d, CallSite loc) {
				return generateState(d,loc);
			}
			
			

			@Override
			public CallSite epsilon() {
				return epsilonCallSite();
			}
		};
			aut2.addTransition(new Transition<CallSite, INode<Stmt, Fact>>(seed, emptyCallSite(), seed));
		callingContextPDS.poststar(aut2);
		System.out.println(aut2);
		System.out.println(aut2.getStates());
		boolean pds2 = aut2.getStates().contains(succ);
		if (pds1 && pds2) {
			System.out.println("IS feasable!" + succ);
			worklist.add(succ);
		}
	}

	Map<Entry<INode<Stmt,Fact>, CallSite>, INode<Stmt,Fact>> generatedState = Maps.newHashMap();
	
	protected INode<Stmt, Fact> generateState(final INode<Stmt, Fact> d, final CallSite loc) {
		Entry<INode<Stmt,Fact>, CallSite> e = new AbstractMap.SimpleEntry<>(d,loc);
		if(!generatedState.containsKey(e)){
			generatedState.put(e, new INode<Stmt, Fact>() {
				@Override
				public Stmt stmt() {
					return null;
				}

				@Override
				public Fact fact() {
					return null;
				}
				@Override
				public String toString() {
					// TODO Auto-generated method stub
					return d + " " + loc;
				}
			});
		}
			return generatedState.get(e);
	}

	private NodeWithLocation<Stmt, Fact, Field> withField(Node<Stmt,Fact> node) {
		return new NodeWithLocation<Stmt, Fact, Field>(node.stmt, node.variable, emptyField());
	}

	private NodeWithLocation<Stmt, Fact, CallSite> withCall(Node<Stmt,Fact> node) {
		return new NodeWithLocation<Stmt, Fact, CallSite>(node.stmt, node.variable, emptyCallSite());
	}
	
	

	public abstract Rule<CallSite, INode<Stmt, Fact>, NoWeight<CallSite>> computeCallSiteRule(Node<Stmt, Fact> curr,
			Node<Stmt, Fact> succ);

	public abstract Rule<Field, NodeWithLocation<Stmt, Fact, Field>, NoWeight<Field>> computeFieldRefRule(NodeWithLocation<Stmt, Fact, Field> curr,
			NodeWithLocation<Stmt, Fact, Field> succ);

	public abstract Collection<Node<Stmt, Fact>> computeSuccessor(Node<Stmt, Fact> node);

	public abstract Field epsilonField();
	
	public abstract Field emptyField();

	public abstract CallSite epsilonCallSite();

	public abstract CallSite emptyCallSite();
	
	public Set<Node<Stmt, Fact>> getReachedStates() {
		return Sets.newHashSet(reachedStates);
	}
}
