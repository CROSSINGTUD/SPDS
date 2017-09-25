package wpds.impl;

import java.util.Map;

import com.google.common.collect.Maps;

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
	private Map<Transition<N,D>, WeightedPAutomaton<N, D, W>> summaries = Maps.newHashMap();
	public static boolean SUMMARIES = false;

	public void poststar(IPushdownSystem<N, D, W> pds, WeightedPAutomaton<N, D, W> initialAutomaton) {
		this.pds = pds;
		this.fa = initialAutomaton;
		this.pds.registerUpdateListener(new PostStarUpdateListener(fa));
	}
	
	
	private class PostStarUpdateListener implements WPDSUpdateListener<N, D, W> {

		private WeightedPAutomaton<N, D, W> aut;

		public PostStarUpdateListener(WeightedPAutomaton<N, D, W> fa) {
			aut = fa;
		}
		
		@Override
		public void onRuleAdded(final Rule<N, D, W> rule) {
			if(rule instanceof NormalRule){
				aut.registerListener(new HandleNormalListener((NormalRule)rule));
			} else if(rule instanceof PushRule){
				aut.registerListener(new HandlePushListener((PushRule)rule));
			} else if(rule instanceof PopRule){
				aut.registerDFSEpsilonListener(rule.getS1(),new ReachabilityListener<N, D>() {
					@Override
					public void reachable(Transition<N, D> t) {
						aut.registerListener(new HandlePopListener((PopRule)rule, t.getStart()));
					}
				});
			}
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((aut == null) ? 0 : aut.hashCode());
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
			PostStarUpdateListener other = (PostStarUpdateListener) obj;
			if (aut == null) {
				if (other.aut != null)
					return false;
			} else if (!aut.equals(other.aut))
				return false;
			return true;
		}

	}
	private class HandlePopListener extends WPAStateListener<N, D, W> {
		private PopRule<N, D, W> rule;
		public HandlePopListener(PopRule<N, D, W> rule, D state) {
			super(state);
			this.rule = rule;
		}


		@Override
		public void onOutTransitionAdded(final Transition<N, D> t, W weight) {
			if(t.getLabel().equals(rule.getL1())){
				final W newWeight = (W) weight.extendWithIn(rule.getWeight());
				final D p = rule.getS2();
				update(new Transition<N, D>(p, fa.epsilon(), t.getTarget()), newWeight);
			}
		}

		@Override
		public void onInTransitionAdded(Transition<N, D> t, W weight) {
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
		public HandleNormalListener(NormalRule<N, D, W> rule) {
			super(rule.getS1());
			this.rule = rule;
		}


		@Override
		public void onOutTransitionAdded(final Transition<N, D> t, W weight) {
			if(t.getLabel().equals(rule.getL1()) || rule.getL1() instanceof Wildcard){
				W newWeight = (W) weight.extendWithIn(rule.getWeight());
				D p = rule.getS2();
				N l2 = rule.getL2();
				if (l2 instanceof ExclusionWildcard) {
					ExclusionWildcard<N> ex = (ExclusionWildcard<N>) l2;
					if (t.getString().equals(ex.excludes()))
						return;
				}
				if (l2 instanceof Wildcard) {
					l2 = t.getString();
				}
				update(new Transition<N, D>(p, l2, t.getTarget()), newWeight);
			}
		}

		@Override
		public void onInTransitionAdded(Transition<N, D> t, W weight) {
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
		
		public HandlePushListener(PushRule<N, D, W> rule) {
			super(rule.getS1());
			this.rule = rule;
		}


		@Override
		public void onOutTransitionAdded(final Transition<N, D> t, W weight) {
			if(t.getLabel().equals(rule.getL1()) || rule.getL1() instanceof Wildcard){
				final D p = rule.getS2();
				final N gammaPrime = rule.getL2();
				final D irState = fa.createState(p, gammaPrime);
				if(!SUMMARIES){
					update(new Transition<N, D>(p, gammaPrime, irState), rule.getWeight());
				} else{
					if(!fa.isGeneratedState(irState))
						throw new RuntimeException("State must be generated");
					final WeightedPAutomaton<N, D, W> summary = getOrCreateSummaryAutomaton(new Transition<N, D>(p, gammaPrime, irState), rule.getWeight());
					summary.registerListener(new WPAUpdateListener<N, D, W>() {

						@Override
						public void onWeightAdded(Transition<N, D> t, Weight<N> w) {
							if((t.getLabel().equals(fa.epsilon()) && t.getTarget().equals(irState))){
								update(t, (W) w);
							}
						}
					});
				}
				final N transitionLabel = (rule.getCallSite() instanceof Wildcard ? t.getLabel() : rule.getCallSite());
				update(new Transition<N, D>(irState, transitionLabel, t.getTarget()), weight);
				fa.registerListener(new UpdateEpsilonOnPushListener(new Transition<N, D>(irState, transitionLabel, t.getTarget())));
			}
		}

		@Override
		public void onInTransitionAdded(Transition<N, D> t, W weight) {
			
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
	
	private class UpdateEpsilonOnPushListener extends WPAStateListener<N, D, W>{
		


		private Transition<N, D> transition;

		public UpdateEpsilonOnPushListener(Transition<N, D> transition){
			super(transition.getStart());
			this.transition = transition;
		}

		@Override
		public void onOutTransitionAdded(Transition<N, D> t, W weight) {
		}

		@Override
		public void onInTransitionAdded(Transition<N, D> t, W weight) {
			if (t.getString().equals(fa.epsilon())) {
				W newWeight = fa.getWeightFor(transition);
				update(new Transition<N, D>(t.getStart(), transition.getLabel(), transition.getTarget()),
						(W) weight.extendWith(newWeight));
			}	
		};

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = super.hashCode();
			result = prime * result + getOuterType().hashCode();
			result = prime * result + ((transition == null) ? 0 : transition.hashCode());
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
			UpdateEpsilonOnPushListener other = (UpdateEpsilonOnPushListener) obj;
			if (!getOuterType().equals(other.getOuterType()))
				return false;
			if (transition == null) {
				if (other.transition != null)
					return false;
			} else if (!transition.equals(other.transition))
				return false;
			return true;
		}

		private PostStar getOuterType() {
			return PostStar.this;
		}

		
	}
	
	private void update(Transition<N, D> trans, W weight) {
		fa.addWeightForTransition(trans, weight);
	}


	private WeightedPAutomaton<N, D, W> getOrCreateSummaryAutomaton(Transition<N, D> transition, W weight) {
		WeightedPAutomaton<N, D, W> aut = getSummaries().get(transition);
		if(aut == null){
			aut = fa.createNestedAutomaton();
			getSummaries().put(transition, aut);
		}
		new PostStar<N, D, W>(){
			protected Map<Transition<N,D>,WeightedPAutomaton<N,D,W>> getSummaries() {
				return PostStar.this.getSummaries();
			};
		}.poststar(pds, aut);
		aut.addWeightForTransition(transition, weight);
		return aut;
	}
	

	protected Map<Transition<N, D>, WeightedPAutomaton<N, D, W>> getSummaries(){
		return summaries;
	}
}
