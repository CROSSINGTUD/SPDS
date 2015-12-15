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
  private Map<Transition<N, D>, W> transitionToWeight = Maps.newHashMap();
  private LinkedList<Transition<N, D>> worklist = Lists.newLinkedList();
  private IPushdownSystem<N, D, W> pds;
  private WeightedPAutomaton<N, D, W> fa;
  private int iterationCount;

  public WeightedPAutomaton<N, D, W> poststar(IPushdownSystem<N, D, W> pds,
      WeightedPAutomaton<N, D, W> initialAutomaton) {
    long before = System.currentTimeMillis();
    this.pds = pds;
    worklist = Lists.newLinkedList(initialAutomaton.getTransitions());
    fa = initialAutomaton;

    for (Transition<N, D> trans : worklist)
      fa.addWeightForTransition(trans, pds.getOne());

    saturate();

    long after = System.currentTimeMillis();
    System.out.println("POSTSTAR TOOK: " + (after - before) + "ms/" + iterationCount + " Iter.");
    return fa;
  }

  private void saturate() {
    // PHASE 1: Is done automatically

    while (!worklist.isEmpty()) {
      iterationCount++;
      Transition<N, D> t = worklist.removeFirst();
      Set<Rule<N, D, W>> rules = pds.getRulesStarting(t.getStart(), t.getString());

      W currWeight = getOrCreateWeight(t);
      for (Rule<N, D, W> rule : rules) {
        W newWeight = (W) currWeight.extendWith(rule.getWeight());
        if (newWeight.equals(pds.getZero()))
          continue;
        D p = rule.getS2();
        if (rule instanceof PopRule) {
          PopRule<N, D, W> popRule = (PopRule<N, D, W>) rule;
          LinkedList<Transition<N, D>> previous = Lists.<Transition<N, D>>newLinkedList();
          previous.add(t);
          update(new Transition<N, D>(p, fa.epsilon(), t.getTarget()), newWeight, previous);

          Collection<Transition<N, D>> trans = fa.getTransitionsOutOf(t.getTarget());
          for (Transition<N, D> tq : trans) {
            LinkedList<Transition<N, D>> prev = Lists.<Transition<N, D>>newLinkedList();
            prev.add(t);
            prev.add(tq);
            update(new Transition<N, D>(p, tq.getString(), tq.getTarget()),
                (W) newWeight.extendWith(getOrCreateWeight(tq)), prev);
          }
        } else if (rule instanceof NormalRule) {
          NormalRule<N, D, W> normalRule = (NormalRule<N, D, W>) rule;
          LinkedList<Transition<N, D>> previous = Lists.<Transition<N, D>>newLinkedList();
          previous.add(t);
          update(new Transition<N, D>(p, normalRule.getL2(), t.getTarget()), newWeight, previous);
        } else if (rule instanceof PushRule) {
          PushRule<N, D, W> pushRule = (PushRule<N, D, W>) rule;
          D irState = fa.createState(p, pushRule.getL2());
          if (irState.toString().equals("<7,b>")) {
            System.out.println("AA");
          }
          LinkedList<Transition<N, D>> previous = Lists.<Transition<N, D>>newLinkedList();
          previous.add(t);
          update(new Transition<N, D>(p, pushRule.l2, irState),
              (W) currWeight.extendWith(pushRule.w), previous);

          Collection<Transition<N, D>> into = fa.getTransitionsInto(irState);
          for (Transition<N, D> ts : into) {
            if (ts.getString().equals(fa.epsilon())) {
              LinkedList<Transition<N, D>> prev = Lists.<Transition<N, D>>newLinkedList();
              prev.add(t);
              prev.add(ts);
              update(new Transition<N, D>(ts.getStart(), pushRule.getCallSite(), t.getTarget()),
                  (W) transitionToWeight.get(ts).extendWith(newWeight), prev);
            }
          }
          update(new Transition<N, D>(irState, pushRule.getCallSite(), t.getTarget()),
              (W) currWeight, previous);
        }
      }

    }
  }

  private void update(Transition<N, D> trans, W weight, List<Transition<N, D>> previous) {

    fa.addTransition(trans);
    W lt = getOrCreateWeight(trans);
    W fr = weight;
    for (Transition<N, D> prev : previous) {
      fr = (W) fr.extendWith(getOrCreateWeight(prev));
    }
    W newLt = (W) lt.combineWith(fr);
    fa.addWeightForTransition(trans, newLt);
    if (!lt.equals(newLt)) {
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
