package analysis;

import java.util.Collection;
import java.util.LinkedList;
import java.util.Set;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Lists;
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
	private PushdownSystem<CallSite, Node<Stmt, Fact>> callingContextPDS = new PushdownSystem<CallSite, Node<Stmt, Fact>>() {
	};
	private PushdownSystem<Field, Node<Stmt, Fact>> fieldRefContextPDS = new PushdownSystem<Field, Node<Stmt, Fact>>() {
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
				Rule<Field, Node<Stmt, Fact>, NoWeight<Field>> fieldRule = computeFieldRefRule(curr, succ);
				fieldRefContextPDS.addRule(fieldRule);

				Rule<CallSite, Node<Stmt, Fact>, NoWeight<CallSite>> callRule = computeCallSiteRule(curr, succ);
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
		System.out.println("IS feasable?" + succ);
		PAutomaton<Field, Node<Stmt, Fact>> aut1 = new PAutomaton<Field, Node<Stmt, Fact>>(seed, seed) {
			@Override
			public Node<Stmt, Fact> createState(Node<Stmt, Fact> d, Field loc) {
				System.out.println("CREATE STATE"+ d);
				return d;
			}

			@Override
			public Field epsilon() {
				return epsilonField();
			}
		};
		for (Field f : fieldOutOfSeed)
			aut1.addTransition(new Transition<Field, Node<Stmt, Fact>>(seed, f, seed));
		fieldRefContextPDS.poststar(aut1);
		System.out.println(aut1);
		System.out.println(aut1.getStates());
		boolean pds1 = aut1.getStates().contains(succ);

		PAutomaton<CallSite, Node<Stmt, Fact>> aut2 = new PAutomaton<CallSite, Node<Stmt, Fact>>(seed, seed) {
			@Override
			public Node<Stmt, Fact> createState(Node<Stmt, Fact> d, CallSite loc) {
				System.out.println("CREATE STATE" +d);
				return d;
			}

			@Override
			public CallSite epsilon() {
				return epsilonCallSite();
			}
		};
		for (CallSite c : callsiteOutOfSeed)
			aut2.addTransition(new Transition<CallSite, Node<Stmt, Fact>>(seed, c, seed));
		callingContextPDS.poststar(aut2);
		System.out.println(aut2);
		System.out.println(aut2.getStates());
		boolean pds2 = aut2.getStates().contains(succ);
		if (pds1 && pds2) {
			System.out.println("IS feasable!" + succ);
			worklist.add(succ);
		}
	}

	public abstract Rule<CallSite, Node<Stmt, Fact>, NoWeight<CallSite>> computeCallSiteRule(Node<Stmt, Fact> curr,
			Node<Stmt, Fact> succ);

	public abstract Rule<Field, Node<Stmt, Fact>, NoWeight<Field>> computeFieldRefRule(Node<Stmt, Fact> curr,
			Node<Stmt, Fact> succ);

	public abstract Collection<Node<Stmt, Fact>> computeSuccessor(Node<Stmt, Fact> node);

	public abstract Field epsilonField();

	public abstract CallSite epsilonCallSite();

	public Set<Node<Stmt, Fact>> getReachedStates() {
		return Sets.newHashSet(reachedStates);
	}
}
