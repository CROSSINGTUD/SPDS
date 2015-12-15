package wpds.interfaces;

import java.util.Set;

import wpds.impl.NormalRule;
import wpds.impl.PAutomaton;
import wpds.impl.PopRule;
import wpds.impl.PushRule;
import wpds.impl.Rule;
import wpds.impl.WeightedPAutomaton;

public interface IPushdownSystem<N extends Location, D extends State, W extends Weight> {

  public void addRule(Rule<N, D, W> rule);

  public W getZero();

  public W getOne();

  public Set<D> getStates();

  public Set<NormalRule<N, D, W>> getNormalRules();

  public Set<PopRule<N, D, W>> getPopRules();

  public Set<PushRule<N, D, W>> getPushRules();

  public Set<Rule<N, D, W>> getAllRules();

  public Set<Rule<N, D, W>> getRulesStarting(D start, N string);

  public Set<Rule<N, D, W>> getRulesEnding(D start, N string);

  public PAutomaton<N, D, W> prestar(WeightedPAutomaton<N, D, W> initialAutomaton);

  public PAutomaton<N, D, W> poststar(WeightedPAutomaton<N, D, W> initialAutomaton);


}
