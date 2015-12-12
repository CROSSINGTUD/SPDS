package wpds.impl;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import wpds.interfaces.IPushdownSystem;
import wpds.interfaces.Location;
import wpds.interfaces.State;
import wpds.interfaces.Weight;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

public class PostStar<N extends Location, D extends State, W extends Weight> {
  private Map<Transition<N, D, W>, W> transitionToWeight = Maps.newHashMap();
  private LinkedList<Transition<N, D, W>> worklist = Lists.newLinkedList();
  private IPushdownSystem<N, D, W> pds;
  private WeightedPAutomaton<N, D, W> fa;

  public PAutomaton<N, D, W> poststar(IPushdownSystem<N, D, W> pds,
      WeightedPAutomaton<N, D, W> initialAutomaton) {
    this.pds = pds;
    worklist = Lists.newLinkedList(initialAutomaton.getTransitions());
    fa = initialAutomaton;
    System.out.println(worklist);

    for (Transition<N, D, W> trans : worklist)
      fa.addWeightForTransition(trans, pds.getOne());

    // PHASE 1: Is done automatically

    while (!worklist.isEmpty()) {
      Transition<N, D, W> t = worklist.removeFirst();
      Set<Rule<N, D, W>> rules = pds.getRulesStarting(t.getStart(), t.getString());

      W currWeight = getOrCreateWeight(t);
      for (Rule<N, D, W> rule : rules) {
        W newWeight = (W) currWeight.extendWith(rule.getWeight());
        if (newWeight.equals(pds.getZero()))
          continue;
        D p = rule.getS2();
        if (rule instanceof PopRule) {
          PopRule<N, D, W> popRule = (PopRule<N, D, W>) rule;
          LinkedList<Transition<N, D, W>> previous = Lists.<Transition<N, D, W>>newLinkedList();
          previous.add(t);
          update(new Transition<N, D, W>(p, pds.epsilon(), t.getTarget()), newWeight, previous);

          Collection<Transition<N, D, W>> trans = fa.getTransitionsOutOf(t.getTarget());
          for (Transition<N, D, W> tq : trans) {
            LinkedList<Transition<N, D, W>> prev = Lists.<Transition<N, D, W>>newLinkedList();
            prev.add(t);
            prev.add(tq);
            update(new Transition<N, D, W>(p, tq.getString(), tq.getTarget()),
                (W) newWeight.extendWith(getOrCreateWeight(tq)), prev);
          }
        } else if (rule instanceof NormalRule) {
          NormalRule<N, D, W> normalRule = (NormalRule<N, D, W>) rule;
          LinkedList<Transition<N, D, W>> previous = Lists.<Transition<N, D, W>>newLinkedList();
          previous.add(t);
          update(new Transition<N, D, W>(p, normalRule.getL2(), t.getTarget()), newWeight, previous);
        } else if (rule instanceof PushRule) {
          PushRule<N, D, W> pushRule = (PushRule<N, D, W>) rule;
          D irState = fa.createState(p, pushRule.getL2());
          LinkedList<Transition<N, D, W>> previous = Lists.<Transition<N, D, W>>newLinkedList();
          previous.add(t);
          update(new Transition<N, D, W>(p, pushRule.l2, irState),
              (W) currWeight.extendWith(pushRule.w), previous);

          Collection<Transition<N, D, W>> into = fa.getTransitionsInto(irState);
          for (Transition<N, D, W> ts : into) {
            if (ts.getString().equals(pds.epsilon())) {
              LinkedList<Transition<N, D, W>> prev = Lists.<Transition<N, D, W>>newLinkedList();
              prev.add(t);
              prev.add(ts);
              update(new Transition<N, D, W>(ts.getStart(), pushRule.getCallSite(), t.getTarget()),
                  (W) transitionToWeight.get(ts).extendWith(newWeight), prev);
            }
          }
        }
      }

    }

    return fa;
  }

  private void update(Transition<N, D, W> trans, W weight, List<Transition<N, D, W>> previous) {
    fa.addTransition(trans);
    W lt = getOrCreateWeight(trans);
    W fr = weight;
    for (Transition<N, D, W> prev : previous) {
      fr = (W) fr.extendWith(getOrCreateWeight(prev));
    }
    W newLt = (W) lt.combineWith(fr);
    fa.addWeightForTransition(trans, newLt);
    if (!lt.equals(newLt)) {
      worklist.add(trans);
    }
  }

  private W getOrCreateWeight(Transition<N, D, W> trans) {
    W w = fa.getWeightFor(trans);
    if (w != null)
      return w;
    return pds.getZero();
  }

}
