package wpds.impl;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import com.google.common.collect.Lists;

import wpds.interfaces.IPushdownSystem;
import wpds.interfaces.Location;
import wpds.interfaces.State;

public class PostStar<N extends Location, D extends State, W extends Weight<N>> {
  private LinkedList<Transition<N, D>> worklist = Lists.newLinkedList();
  private IPushdownSystem<N, D, W> pds;
  private WeightedPAutomaton<N, D, W> fa;
  private int iterationCount;

  public void poststar(IPushdownSystem<N, D, W> pds,
      WeightedPAutomaton<N, D, W> initialAutomaton) {
    this.pds = pds;
    worklist = Lists.newLinkedList(initialAutomaton.getTransitions());
    fa = initialAutomaton;

    for (Transition<N, D> trans : worklist)
      fa.addWeightForTransition(trans, pds.getOne());

    saturate();

  }

  private void saturate() {
    // PHASE 1: Is done automatically

    while (!worklist.isEmpty()) {
      iterationCount++;
      Transition<N, D> t = worklist.removeFirst();
      Set<Rule<N, D, W>> rules = pds.getRulesStarting(t.getStart(), t.getString());

      W currWeight = getOrCreateWeight(t);
      for (Rule<N, D, W> rule : rules) {
        W newWeight = (W) currWeight.extendWithIn(rule.getWeight());
        // if (newWeight.equals(pds.getZero()))
        // continue;
        D p = rule.getS2();
        if (rule instanceof PopRule) {
          LinkedList<Transition<N, D>> previous = Lists.<Transition<N, D>>newLinkedList();
          previous.add(t);
          System.out.println("EPS");
          update(rule, new Transition<N, D>(p, fa.epsilon(), t.getTarget()), newWeight,
              previous);

          Collection<Transition<N, D>> trans = fa.getTransitionsOutOf(t.getTarget());
          for (Transition<N, D> tq : trans) {
            LinkedList<Transition<N, D>> prev = Lists.<Transition<N, D>>newLinkedList();
            prev.add(t);
            prev.add(tq);
            update(rule, new Transition<N, D>(p, tq.getString(), tq.getTarget()),
 (W) newWeight,
                prev);
          }
        } else if (rule instanceof NormalRule) {
          NormalRule<N, D, W> normalRule = (NormalRule<N, D, W>) rule;
          LinkedList<Transition<N, D>> previous = Lists.<Transition<N, D>>newLinkedList();
          previous.add(t);
          update(rule, new Transition<N, D>(p, normalRule.getL2(), t.getTarget()), newWeight,
              previous);
        } else if (rule instanceof PushRule) {
          PushRule<N, D, W> pushRule = (PushRule<N, D, W>) rule;
          D irState = fa.createState(p, pushRule.getL2());
          LinkedList<Transition<N, D>> previous = Lists.<Transition<N, D>>newLinkedList();
          previous.add(t);
          update(rule, new Transition<N, D>(p, pushRule.l2, irState),
              (W) currWeight.extendWithIn(pushRule.getWeight()), previous);

          Collection<Transition<N, D>> into = fa.getTransitionsInto(irState);
          for (Transition<N, D> ts : into) {
            if (ts.getString().equals(fa.epsilon())) {
              LinkedList<Transition<N, D>> prev = Lists.<Transition<N, D>>newLinkedList();
              prev.add(t);
              prev.add(ts);
              update(rule,
                  new Transition<N, D>(ts.getStart(), pushRule.getCallSite(), t.getTarget()),
                  (W) getOrCreateWeight(ts).extendWithIn(newWeight), prev);
            }
          }
          System.out.println("PUSHRULE weight " + currWeight);
          update(rule, new Transition<N, D>(irState, pushRule.getCallSite(), t.getTarget()),
              (W) currWeight, previous);
        }
      }

    }
  }

  private void update(Rule<N, D, W> triggeringRule, Transition<N, D> trans, W weight,
      List<Transition<N, D>> previous) {
    fa.addTransition(trans);
    W lt = getOrCreateWeight(trans);
    W newLt = (W) lt.combineWithIn(weight);
    fa.addWeightForTransition(trans, newLt);
    if (!lt.equals(newLt)) {
      System.out.println(
          trans + "\t as of \t" + triggeringRule + " \t and " + newLt);
      worklist.add(trans);
    }
  }

  private W getOrCreateWeight(Transition<N, D> trans) {
    W w = fa.getWeightFor(trans);
    if (w != null)
      return w;
    return pds.getZero();
  }

}
