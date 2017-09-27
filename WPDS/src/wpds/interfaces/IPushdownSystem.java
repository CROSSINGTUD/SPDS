package wpds.interfaces;

import java.util.Map;
import java.util.Set;

import wpds.impl.NormalRule;
import wpds.impl.PopRule;
import wpds.impl.PushRule;
import wpds.impl.Rule;
import wpds.impl.Transition;
import wpds.impl.Weight;
import wpds.impl.WeightedPAutomaton;

public interface IPushdownSystem<N extends Location, D extends State, W extends Weight> {

  public boolean addRule(Rule<N, D, W> rule);

  public W getZero();

  public W getOne();

  public Set<D> getStates();

  public Set<NormalRule<N, D, W>> getNormalRules();

  public Set<PopRule<N, D, W>> getPopRules();

  public Set<PushRule<N, D, W>> getPushRules();

  public Set<Rule<N, D, W>> getAllRules();

  public Set<Rule<N, D, W>> getRulesStarting(D start, N string);

  public Set<NormalRule<N, D, W>> getNormalRulesEnding(D start, N string);

  public Set<PushRule<N, D, W>> getPushRulesEnding(D start, N string);

  public void prestar(WeightedPAutomaton<N, D, W> initialAutomaton);

  public void poststar(WeightedPAutomaton<N, D, W> initialAutomaton);
  
  public void poststar(WeightedPAutomaton<N, D, W> initialAutomaton, Map<Transition<N,D>,WeightedPAutomaton<N, D, W>> summaries);
  
  public void registerUpdateListener(WPDSUpdateListener<N, D, W> listener);

}
