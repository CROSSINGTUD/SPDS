package wpds.impl;

import java.util.HashSet;
import java.util.Set;

import wpds.interfaces.IPushdownSystem;
import wpds.interfaces.Location;
import wpds.interfaces.State;
import wpds.interfaces.Weight;

import com.google.common.base.Joiner;
import com.google.common.collect.Sets;

public abstract class PushdownSystem<N extends Location, D extends State, W extends Weight>
    implements IPushdownSystem<N, D, W> {
  Set<PushRule<N, D, W>> pushRules = Sets.newHashSet();
  Set<PopRule<N, D, W>> popRules = Sets.newHashSet();
  Set<NormalRule<N, D, W>> normalRules = Sets.newHashSet();

  @Override
  public void addRule(Rule<N, D, W> rule) {
    if (rule instanceof PushRule)
      pushRules.add((PushRule) rule);
    else if (rule instanceof PopRule)
      popRules.add((PopRule) rule);
    else if (rule instanceof NormalRule)
      normalRules.add((NormalRule) rule);
  }

  @Override
  public abstract W getZero();

  @Override
  public abstract W getOne();

  @Override
  public Set<NormalRule<N, D, W>> getNormalRules() {
    return normalRules;
  }

  @Override
  public Set<PopRule<N, D, W>> getPopRules() {
    return popRules;
  }

  @Override
  public Set<PushRule<N, D, W>> getPushRules() {
    return pushRules;
  }

  @Override
  public Set<Rule<N, D, W>> getAllRules() {
    Set<Rule<N, D, W>> rules = Sets.newHashSet();
    rules.addAll(normalRules);
    rules.addAll(popRules);
    rules.addAll(pushRules);
    return rules;
  }

  @Override
  public Set<Rule<N, D, W>> getRulesStarting(D start, N string) {
    Set<Rule<N, D, W>> allRules = getAllRules();
    Set<Rule<N, D, W>> result = new HashSet<>();
    for (Rule<N, D, W> r : allRules) {
      if (r.getS1().equals(start) && r.getL1().equals(string))
        result.add(r);
    }
    return result;
  }

  @Override
  public Set<D> getStates() {
    Set<D> states = Sets.newHashSet();
    for (Rule<N, D, W> r : getAllRules()) {
      states.add(r.getS1());
      states.add(r.getS2());
    }
    return states;
  }


  @Override
  public PAutomaton<N, D, W> poststar(WeightedPAutomaton<N, D, W> initialAutomaton) {
    return new PostStar<N, D, W>().poststar(this, initialAutomaton);
  }

  @Override
  public PAutomaton<N, D, W> prestar(WeightedPAutomaton<N, D, W> initialAutomaton) {
    return new PreStar<N, D, W>().prestar(this, initialAutomaton);
  }

  public String toString() {
    String s = "WPDS\n";
    s += "\tNormalRules:\n\t\t";
    s += Joiner.on("\n\t\t").join(normalRules);
    s += "\n";
    s += "\tPopRules:\n\t\t";
    s += Joiner.on("\n\t\t").join(popRules);
    s += "\n";

    s += "\tPushRules:\n\t\t";
    s += Joiner.on("\n\t\t").join(pushRules);
    return s;
  }

}
