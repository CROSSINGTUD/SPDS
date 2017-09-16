package wpds.impl;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.SynchronousQueue;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import wpds.interfaces.ForwardDFSEpsilonVisitor;
import wpds.interfaces.ForwardDFSVisitor;
import wpds.interfaces.IPushdownSystem;
import wpds.interfaces.Location;
import wpds.interfaces.ReachabilityListener;
import wpds.interfaces.State;
import wpds.interfaces.WPAStateListener;
import wpds.interfaces.WPAUpdateListener;
import wpds.interfaces.WPDSUpdateListener;
import wpds.wildcard.ExclusionWildcard;
import wpds.wildcard.Wildcard;

public class PostStar<N extends Location, D extends State, W extends Weight<N>> {
	private IPushdownSystem<N, D, W> pds;
	private WeightedPAutomaton<N, D, W> fa;
	private LinkedList<Transition<N,D>> worklist = Lists.newLinkedList();

	public void poststar(IPushdownSystem<N, D, W> pds, WeightedPAutomaton<N, D, W> initialAutomaton) {
		this.pds = pds;
		this.fa = initialAutomaton;
		this.pds.registerUpdateListener(new WPDSUpdateListener<N, D, W>() {

			@Override
			public void onRuleAdded(final Rule<N, D, W> rule) {
				if(rule instanceof PopRule){
					onPopRuleAdded((PopRule)rule);
					return;
				} else if(rule instanceof NormalRule){
					onNormalRuleAdded((NormalRule) rule);
					return;
				}
				Collection<Transition<N, D>> trans = fa.getTransitionsOutOf(rule.getS1());
				for(Transition<N, D> t : Lists.newLinkedList(trans)){
					if(t.getLabel().equals(rule.getL1()) || rule.getL1() instanceof Wildcard){
						update(t, rule);
					}
				}
				await();
			}

			private void onNormalRuleAdded(final NormalRule<N, D, W> rule) {
				fa.registerListener(new ForwardDFSEpsilonVisitor<N, D, W>(fa, rule.getS1(),new ReachabilityListener<N, D>() {
					@Override
					public void reachable(Transition<N, D> t) {
						fa.registerListener(new HandleNormalListener(rule, t.getStart()));
					}
				}));
				
			}

			private void onPopRuleAdded(final PopRule<N, D, W> rule) {
				fa.registerListener(new ForwardDFSEpsilonVisitor<N, D, W>(fa, rule.getS1(),new ReachabilityListener<N, D>() {
					@Override
					public void reachable(Transition<N, D> t) {
						fa.registerListener(new HandlePopListener(rule, t.getStart()));
					}
				}));
			}
		});
		fa.registerListener(new WPAUpdateListener<N, D, W>() {
			@Override
			public void onAddedTransition(Transition<N, D> t) {
				addToWorklist(t);
				await();
			}
			@Override
			public void onWeightAdded(Transition<N, D> t, Weight<N> w) {
			}
		});
	}
	
	private class HandlePopListener extends WPAStateListener<N, D, W> {
		private PopRule<N, D, W> rule;
		public HandlePopListener(PopRule<N, D, W> rule, D state) {
			super(state);
			this.rule = rule;
		}


		@Override
		public void onOutTransitionAdded(final Transition<N, D> t) {
			if(t.getLabel().equals(rule.getL1())){
				W currWeight = getOrCreateWeight(new Transition<N,D>(state,t.getLabel(),t.getTarget()));
				final W newWeight = (W) currWeight.extendWithIn(rule.getWeight());
				final D p = rule.getS2();
				LinkedList<Transition<N, D>> previous = Lists.<Transition<N, D>>newLinkedList();
				previous.add(t);
				update(rule, new Transition<N, D>(p, fa.epsilon(), t.getTarget()), newWeight, previous);
				fa.registerListener(new WPAStateListener<N, D, W>(t.getTarget()) {

					@Override
					public void onOutTransitionAdded(Transition<N, D> tq) {
						LinkedList<Transition<N, D>> prev = Lists.<Transition<N, D>>newLinkedList();
						prev.add(t);
						prev.add(tq);
						update(rule, new Transition<N, D>(p, tq.getString(), tq.getTarget()), (W) newWeight, prev);
					}

					@Override
					public void onInTransitionAdded(Transition<N, D> t) {
						
					}
				});
			}
		}

		@Override
		public void onInTransitionAdded(Transition<N, D> t) {
			
		}


		@Override
		public int hashCode() {
			final int prime = 31;
			int result = super.hashCode();
			result = prime * result + getOuterType().hashCode();
			result = prime * result + ((rule == null) ? 0 : rule.hashCode());
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
			HandlePopListener other = (HandlePopListener) obj;
			if (!getOuterType().equals(other.getOuterType()))
				return false;
			if (rule == null) {
				if (other.rule != null)
					return false;
			} else if (!rule.equals(other.rule))
				return false;
			return true;
		}



		private PostStar getOuterType() {
			return PostStar.this;
		}
	}
	
	private class HandleNormalListener extends WPAStateListener<N, D, W> {
		private NormalRule<N, D, W> rule;
		public HandleNormalListener(NormalRule<N, D, W> rule, D state) {
			super(state);
			this.rule = rule;
		}


		@Override
		public void onOutTransitionAdded(final Transition<N, D> t) {
			if(t.getLabel().equals(rule.getL1()) || rule.getL1() instanceof Wildcard){
				W currWeight = getOrCreateWeight(new Transition<N,D>(state,t.getLabel(),t.getTarget()));
				W newWeight = (W) currWeight.extendWithIn(rule.getWeight());
				D p = rule.getS2();
				LinkedList<Transition<N, D>> previous = Lists.<Transition<N, D>>newLinkedList();
				previous.add(t);
				N l2 = rule.getL2();
				if (l2 instanceof Wildcard) {
					if (l2 instanceof ExclusionWildcard) {
						ExclusionWildcard<N> ex = (ExclusionWildcard<N>) l2;
						if (t.getString().equals(ex.excludes()))
							return;
					}
					l2 = t.getString();
				}
				update(rule, new Transition<N, D>(p, l2, t.getTarget()), newWeight, previous);
			}
		}

		@Override
		public void onInTransitionAdded(Transition<N, D> t) {
			
		}


		@Override
		public int hashCode() {
			final int prime = 31;
			int result = super.hashCode();
			result = prime * result + getOuterType().hashCode();
			result = prime * result + ((rule == null) ? 0 : rule.hashCode());
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
			HandleNormalListener other = (HandleNormalListener) obj;
			if (!getOuterType().equals(other.getOuterType()))
				return false;
			if (rule == null) {
				if (other.rule != null)
					return false;
			} else if (!rule.equals(other.rule))
				return false;
			return true;
		}



		private PostStar getOuterType() {
			return PostStar.this;
		}
	}

	private void update(Transition<N, D> t, Rule<N, D, W> rule) {
		W currWeight = getOrCreateWeight(t);
		W newWeight = (W) currWeight.extendWithIn(rule.getWeight());
		// if (newWeight.equals(pds.getZero()))
		// continue;
		D p = rule.getS2();
		if (rule instanceof PopRule) {
		} else if (rule instanceof NormalRule) {
//			NormalRule<N, D, W> normalRule = (NormalRule<N, D, W>) rule;
//			LinkedList<Transition<N, D>> previous = Lists.<Transition<N, D>>newLinkedList();
//			previous.add(t);
//			N l2 = normalRule.getL2();
//			if (l2 instanceof Wildcard) {
//				if (l2 instanceof ExclusionWildcard) {
//					ExclusionWildcard<N> ex = (ExclusionWildcard<N>) l2;
//					if (t.getString().equals(ex.excludes()))
//						return;
//				}
//				l2 = t.getString();
//			}
//			update(rule, new Transition<N, D>(p, l2, t.getTarget()), newWeight, previous);
		} else if (rule instanceof PushRule) {
			PushRule<N, D, W> pushRule = (PushRule<N, D, W>) rule;
			N gammaPrime = pushRule.getL2();
			if (gammaPrime instanceof Wildcard){
				gammaPrime = t.getString();
			}
			D irState = fa.createState(p, gammaPrime);
			LinkedList<Transition<N, D>> previous = Lists.<Transition<N, D>>newLinkedList();
			previous.add(t);
			update(rule, new Transition<N, D>(p, gammaPrime, irState),
					(W) currWeight.extendWithIn(pushRule.getWeight()), previous);
			N callSite = pushRule.getCallSite();
			N transitionLabel;
			if (callSite instanceof Wildcard) {
				transitionLabel = t.getString();
			} else {
				transitionLabel = callSite;
			}
			Collection<Transition<N, D>> into = fa.getTransitionsInto(irState);
			for (Transition<N, D> ts : into) {
				if (ts.getString().equals(fa.epsilon())) {
					LinkedList<Transition<N, D>> prev = Lists.<Transition<N, D>>newLinkedList();
					prev.add(t);
					prev.add(ts);
					update(rule, new Transition<N, D>(ts.getStart(), transitionLabel, t.getTarget()),
							(W) getOrCreateWeight(ts).extendWithIn(newWeight), prev);
				}
			}
		
			update(rule, new Transition<N, D>(irState, transitionLabel, t.getTarget()), (W) currWeight,
					previous);
		}
	}

	private void update(Rule<N, D, W> triggeringRule, Transition<N, D> trans, W weight,
			List<Transition<N, D>> previous) {
		fa.addTransition(trans);
		W lt = getOrCreateWeight(trans);
		W newLt = (W) lt.combineWithIn(weight);
		fa.addWeightForTransition(trans, newLt);
		if (!lt.equals(newLt)) {
			addToWorklist(trans);
		}
	}


	private void addToWorklist(Transition<N, D> t) {
		worklist.add(t);
	}
	private void await(){
		while(!worklist.isEmpty()){
			Transition<N, D> pop = worklist.pop();
			update(pop);
		}
	}
	private void update(Transition<N, D> t) {
		Set<Rule<N, D, W>> rules = pds.getRulesStarting(t.getStart(), t.getString());
		for(Rule<N, D, W> rule : rules){
			update(t,rule);
		}
	}

	private W getOrCreateWeight(Transition<N, D> trans) {
		W w = fa.getWeightFor(trans);
		if (w != null)
			return w;
		return pds.getZero();
	}

}
