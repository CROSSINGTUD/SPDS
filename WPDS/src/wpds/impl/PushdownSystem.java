package wpds.impl;

import java.util.HashSet;
import java.util.Set;

import com.google.common.base.Joiner;
import com.google.common.collect.Sets;

import wpds.interfaces.IPushdownSystem;
import wpds.interfaces.Location;
import wpds.interfaces.State;
import wpds.interfaces.Weight;

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
    assert !string.equals(anyTransition());
    Set<Rule<N, D, W>> allRules = getAllRules();
    Set<Rule<N, D, W>> result = new HashSet<>();
    for (Rule<N, D, W> r : allRules) {
      if (r.getS1().equals(start) && r.getL1().equals(string))
        result.add(r);
      if (anyTransition() != null && r.getS1().equals(start) && r.getL1().equals(anyTransition())) {
        if (r instanceof NormalRule) {
          result.add(new NormalRule<N, D, W>(r.getS1(), string, r.getS2(), string, r.getWeight()));
        } else if (r instanceof PopRule) {
          result.add(new PopRule<N, D, W>(r.getS1(), string, r.getS2(), r.getWeight()));
        } else if (r instanceof PushRule) {
          result.add(new PushRule<N, D, W>(r.getS1(), string, r.getS2(), r.getL2(), string,
              r.getWeight()));
        }
      }
    }
    return result;
  }

  @Override
  public Set<NormalRule<N, D, W>> getNormalRulesEnding(D start, N string) {
    assert !string.equals(anyTransition());
    Set<NormalRule<N, D, W>> allRules = getNormalRules();
    Set<NormalRule<N, D, W>> result = new HashSet<>();
    for (NormalRule<N, D, W> r : allRules) {
      if (r.getS2().equals(start) && r.getL2().equals(string))
        result.add(r);
      if (anyTransition() != null && r.getS2().equals(start) && r.getL2().equals(anyTransition())) {
        result.add(new NormalRule<N, D, W>(r.getS1(), string, r.getS2(), string, r.getWeight()));
      }
    }
    return result;
  }

  @Override
  public Set<PushRule<N, D, W>> getPushRulesEnding(D start, N string) {
    assert !string.equals(anyTransition());
    Set<PushRule<N, D, W>> allRules = getPushRules();
    Set<PushRule<N, D, W>> result = new HashSet<>();
    for (PushRule<N, D, W> r : allRules) {
      if (r.getS2().equals(start) && r.getL2().equals(string))
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
  public WeightedPAutomaton<N, D, W> poststar(WeightedPAutomaton<N, D, W> initialAutomaton) {
    return new PostStar<N, D, W>().poststar(this, initialAutomaton);
  }

  @Override
  public WeightedPAutomaton<N, D, W> prestar(WeightedPAutomaton<N, D, W> initialAutomaton) {
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

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((normalRules == null) ? 0 : normalRules.hashCode());
    result = prime * result + ((popRules == null) ? 0 : popRules.hashCode());
    result = prime * result + ((pushRules == null) ? 0 : pushRules.hashCode());
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
    PushdownSystem other = (PushdownSystem) obj;
    if (normalRules == null) {
      if (other.normalRules != null)
        return false;
    } else if (!normalRules.equals(other.normalRules))
      return false;
    if (popRules == null) {
      if (other.popRules != null)
        return false;
    } else if (!popRules.equals(other.popRules))
      return false;
    if (pushRules == null) {
      if (other.pushRules != null)
        return false;
    } else if (!pushRules.equals(other.pushRules))
      return false;
    return true;
  }


}
