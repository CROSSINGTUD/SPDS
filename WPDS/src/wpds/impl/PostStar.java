package wpds.impl;

import java.util.LinkedList;
import java.util.List;

import com.google.common.collect.Lists;

import wpds.interfaces.BackwardDFSEpsilonVisitor;
import wpds.interfaces.ForwardDFSEpsilonVisitor;
import wpds.interfaces.IPushdownSystem;
import wpds.interfaces.Location;
import wpds.interfaces.ReachabilityListener;
import wpds.interfaces.State;
import wpds.interfaces.WPAStateListener;
import wpds.interfaces.WPDSUpdateListener;
import wpds.wildcard.ExclusionWildcard;
import wpds.wildcard.Wildcard;

public class PostStar<N extends Location, D extends State, W extends Weight<N>> {
	private IPushdownSystem<N, D, W> pds;
	private WeightedPAutomaton<N, D, W> fa;

	public void poststar(IPushdownSystem<N, D, W> pds, WeightedPAutomaton<N, D, W> initialAutomaton) {
		this.pds = pds;
		this.fa = initialAutomaton;
		this.pds.registerUpdateListener(new WPDSUpdateListener<N, D, W>() {

			@Override
			public void onRuleAdded(final Rule<N, D, W> rule) {
				fa.registerListener(new ForwardDFSEpsilonVisitor<N, D, W>(fa, rule.getS1(),new ReachabilityListener<N, D>() {
					@Override
					public void reachable(Transition<N, D> t) {
						if(rule instanceof PopRule){
							fa.registerListener(new HandlePopListener((PopRule)rule, t.getStart()));
						} else if(rule instanceof NormalRule){
							fa.registerListener(new HandleNormalListener((NormalRule)rule, t.getStart()));
						} else if(rule instanceof PushRule){
							fa.registerListener(new HandlePushListener((PushRule)rule, t.getStart()));
						}
					}
				}));
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

	
	private class HandlePushListener extends WPAStateListener<N, D, W> {
		private PushRule<N, D, W> rule;
		public HandlePushListener(PushRule<N, D, W> rule, D state) {
			super(state);
			this.rule = rule;
		}


		@Override
		public void onOutTransitionAdded(final Transition<N, D> t) {
			if(t.getLabel().equals(rule.getL1()) || rule.getL1() instanceof Wildcard){
				W currWeight = getOrCreateWeight(new Transition<N,D>(state,t.getLabel(),t.getTarget()));
				final W newWeight = (W) currWeight.extendWithIn(rule.getWeight());
				final D p = rule.getS2();
				N gammaPrime = rule.getL2();
				if (gammaPrime instanceof Wildcard){
					gammaPrime = t.getString();
				}
				D irState = fa.createState(p, gammaPrime);
				LinkedList<Transition<N, D>> previous = Lists.<Transition<N, D>>newLinkedList();
				previous.add(t);
				update(rule, new Transition<N, D>(p, gammaPrime, irState),
						(W) currWeight.extendWithIn(rule.getWeight()), previous);
				final N transitionLabel = (rule.getCallSite() instanceof Wildcard ? t.getLabel() : rule.getCallSite());
				update(rule, new Transition<N, D>(irState, transitionLabel, t.getTarget()), (W) currWeight,
						previous);
				fa.registerListener(new BackwardDFSEpsilonVisitor<N, D, W>(fa, irState, new ReachabilityListener<N, D>() {

					@Override
					public void reachable(Transition<N, D> ts) {
						if (ts.getString().equals(fa.epsilon())) {
							LinkedList<Transition<N, D>> prev = Lists.<Transition<N, D>>newLinkedList();
							prev.add(t);
							prev.add(ts);
							update(rule, new Transition<N, D>(ts.getStart(), transitionLabel, t.getTarget()),
									(W) getOrCreateWeight(ts).extendWithIn(newWeight), prev);
						}
					}
				}));
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
			HandlePushListener other = (HandlePushListener) obj;
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
	
	private void update(Rule<N, D, W> triggeringRule, Transition<N, D> trans, W weight,
			List<Transition<N, D>> previous) {
		fa.addTransition(trans);
		W lt = getOrCreateWeight(trans);
		W newLt = (W) lt.combineWithIn(weight);
		fa.addWeightForTransition(trans, newLt);
	}


	private W getOrCreateWeight(Transition<N, D> trans) {
		W w = fa.getWeightFor(trans);
		if (w != null)
			return w;
		return pds.getZero();
	}

}
