/**
 * ***************************************************************************** Copyright (c) 2018
 * Fraunhofer IEM, Paderborn, Germany. This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * <p>SPDX-License-Identifier: EPL-2.0
 *
 * <p>Contributors: Johannes Spaeth - initial API and implementation
 * *****************************************************************************
 */
package wpds.impl;

import com.google.common.base.Joiner;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import java.util.HashSet;
import java.util.Set;
import wpds.interfaces.IPushdownSystem;
import wpds.interfaces.Location;
import wpds.interfaces.State;
import wpds.interfaces.WPDSUpdateListener;
import wpds.wildcard.Wildcard;

public class WeightedPushdownSystem<N extends Location, D extends State, W extends Weight>
    implements IPushdownSystem<N, D, W> {
  protected final Set<PushRule<N, D, W>> pushRules = Sets.newHashSet();
  protected final Set<PopRule<N, D, W>> popRules = Sets.newHashSet();
  protected final Set<NormalRule<N, D, W>> normalRules = Sets.newHashSet();
  protected final Set<WPDSUpdateListener<N, D, W>> listeners = Sets.newHashSet();

  @Override
  public boolean addRule(Rule<N, D, W> rule) {
    if (addRuleInternal(rule)) {
      for (WPDSUpdateListener<N, D, W> l : Lists.newArrayList(listeners)) {
        l.onRuleAdded(rule);
      }
      return true;
    }
    return false;
  }

  private boolean addRuleInternal(Rule<N, D, W> rule) {
    if (rule instanceof PushRule) return pushRules.add((PushRule) rule);
    else if (rule instanceof PopRule) return popRules.add((PopRule) rule);
    else if (rule instanceof NormalRule) return normalRules.add((NormalRule) rule);
    throw new RuntimeException("Try to add a rule of wrong type");
  }

  public void registerUpdateListener(WPDSUpdateListener<N, D, W> listener) {
    if (!listeners.add(listener)) {
      return;
    }
    for (Rule<N, D, W> r : getAllRules()) {
      listener.onRuleAdded(r);
    }
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
    Set<Rule<N, D, W>> result = new HashSet<>();
    getRulesStartingWithinSet(start, string, popRules, result);
    getRulesStartingWithinSet(start, string, normalRules, result);
    getRulesStartingWithinSet(start, string, pushRules, result);
    return result;
  }

  private void getRulesStartingWithinSet(
      D start, N string, Set<? extends Rule<N, D, W>> rules, Set<Rule<N, D, W>> res) {
    for (Rule<N, D, W> r : rules) {
      if (r.getS1().equals(start) && (r.getL1().equals(string) || r.getL1() instanceof Wildcard))
        res.add(r);
      if (string instanceof Wildcard && r.getS1().equals(start)) {
        res.add(r);
      }
    }
  }

  @Override
  public Set<NormalRule<N, D, W>> getNormalRulesEnding(D start, N string) {
    Set<NormalRule<N, D, W>> allRules = getNormalRules();
    Set<NormalRule<N, D, W>> result = new HashSet<>();
    for (NormalRule<N, D, W> r : allRules) {
      if (r.getS2().equals(start) && r.getL2().equals(string)) result.add(r);
    }
    return result;
  }

  @Override
  public Set<PushRule<N, D, W>> getPushRulesEnding(D start, N string) {
    Set<PushRule<N, D, W>> allRules = getPushRules();
    Set<PushRule<N, D, W>> result = new HashSet<>();
    for (PushRule<N, D, W> r : allRules) {
      if (r.getS2().equals(start) && r.getL2().equals(string)) result.add(r);
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
  public void poststar(
      WeightedPAutomaton<N, D, W> initialAutomaton,
      final NestedWeightedPAutomatons<N, D, W> summaries) {
    new PostStar<N, D, W>() {

      @Override
      public void putSummaryAutomaton(D target, WeightedPAutomaton<N, D, W> aut) {
        summaries.putSummaryAutomaton(target, aut);
      }

      @Override
      public WeightedPAutomaton<N, D, W> getSummaryAutomaton(D target) {
        return summaries.getSummaryAutomaton(target);
      };
    }.poststar(this, initialAutomaton);
  }

  @Override
  public void poststar(final WeightedPAutomaton<N, D, W> initialAutomaton) {
    new PostStar<N, D, W>() {

      @Override
      public void putSummaryAutomaton(D target, WeightedPAutomaton<N, D, W> aut) {}

      @Override
      public WeightedPAutomaton<N, D, W> getSummaryAutomaton(D target) {
        return initialAutomaton;
      }
    }.poststar(this, initialAutomaton);
  }

  @Override
  public void prestar(WeightedPAutomaton<N, D, W> initialAutomaton) {
    new PreStar<N, D, W>().prestar(this, initialAutomaton);
  }

  public String toString() {
    String s = "WPDS (#Rules: " + getAllRules().size() + ")\n";
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

  // @Override
  // public int hashCode() {
  // final int prime = 31;
  // int result = 1;
  // result = prime * result + ((normalRules == null) ? 0 : normalRules.hashCode());
  // result = prime * result + ((popRules == null) ? 0 : popRules.hashCode());
  // result = prime * result + ((pushRules == null) ? 0 : pushRules.hashCode());
  // return result;
  // }
  //
  // @Override
  // public boolean equals(Object obj) {
  // if (this == obj)
  // return true;
  // if (obj == null)
  // return false;
  // if (getClass() != obj.getClass())
  // return false;
  // PushdownSystem other = (PushdownSystem) obj;
  // if (normalRules == null) {
  // if (other.normalRules != null)
  // return false;
  // } else if (!normalRules.equals(other.normalRules))
  // return false;
  // if (popRules == null) {
  // if (other.popRules != null)
  // return false;
  // } else if (!popRules.equals(other.popRules))
  // return false;
  // if (pushRules == null) {
  // if (other.pushRules != null)
  // return false;
  // } else if (!pushRules.equals(other.pushRules))
  // return false;
  // return true;
  // }

  @Override
  public void unregisterAllListeners() {
    listeners.clear();
  }
}
