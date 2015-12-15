package wpds.impl;

import java.util.LinkedList;
import java.util.List;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import wpds.interfaces.IPushdownSystem;
import wpds.interfaces.Location;
import wpds.interfaces.State;
import wpds.interfaces.Weight;

public class PreStar<N extends Location, D extends State, W extends Weight> {
  private LinkedList<Transition<N, D>> worklist = Lists.newLinkedList();
  private IPushdownSystem<N, D, W> pds;
  private WeightedPAutomaton<N, D, W> fa;

  public WeightedPAutomaton<N, D, W> prestar(IPushdownSystem<N, D, W> pds,
      WeightedPAutomaton<N, D, W> initialAutomaton) {
    this.pds = pds;
    worklist = Lists.newLinkedList(initialAutomaton.getTransitions());
    fa = initialAutomaton;

    for (Transition<N, D> trans : Sets.newHashSet(fa.getTransitions())) {
      fa.addWeightForTransition(trans, pds.getOne());
    }
    for (PopRule<N, D, W> r : pds.getPopRules()) {
      update(new Transition<N, D>(r.getS1(), r.getL1(), r.getS2()), r.getWeight(),
          Lists.<Transition<N, D>>newLinkedList());
    }

    while (!worklist.isEmpty()) {
      Transition<N, D> t = worklist.removeFirst();

      for (NormalRule<N, D, W> r : pds.getNormalRulesEnding(t.getStart(), t.getLabel())) {
        // Normal rules
          LinkedList<Transition<N, D>> previous = Lists.<Transition<N, D>>newLinkedList();
          previous.add(t);
          update(new Transition<N, D>(r.getS1(), r.getL1(), t.getTarget()), r.getWeight(),
              previous);
      }
      for (PushRule<N, D, W> r : pds.getPushRulesEnding(t.getStart(), t.getLabel())) {
        // Push rules
        for (Transition<N, D> tdash : Sets.newHashSet(fa.getTransitions())) {
          if (tdash.getLabel().equals(r.getCallSite())) {
            LinkedList<Transition<N, D>> previous = Lists.<Transition<N, D>>newLinkedList();
            previous.add(t);
            previous.add(tdash);
            update(new Transition<N, D>(r.getS1(), r.getL1(), tdash.getTarget()), r.getWeight(),
                previous);
          } else if (r.getCallSite().equals(pds.anyTransition())) {
            LinkedList<Transition<N, D>> previous = Lists.<Transition<N, D>>newLinkedList();
            previous.add(t);
            previous.add(tdash);
            update(new Transition<N, D>(r.getS1(), tdash.getLabel(), tdash.getTarget()),
                r.getWeight(), previous);
          }

        }
      }

      for (PushRule<N, D, W> r : pds.getPushRules()) {
        if (!r.getCallSite().equals(pds.anyTransition())
            && !r.getCallSite().equals(t.getString())) {
          continue;
        }
        Transition<N, D> tdash = new Transition<N, D>(r.getS2(), r.getL2(), t.getTarget());
        if (!fa.getTransitions().contains(tdash)) {
          continue;
        }
        LinkedList<Transition<N, D>> previous = Lists.<Transition<N, D>>newLinkedList();
        previous.add(tdash);
        previous.add(t);
        N label = (r.getCallSite().equals(pds.anyTransition()) ? t.getLabel() : r.getL1());
        update(new Transition<N, D>(r.getS1(), label, t.getTarget()), r.getWeight(), previous);
      }
    }

    return fa;
  }

  private void update(Transition<N, D> trans, W weight, List<Transition<N, D>> previous) {
    if (trans.getLabel().equals(pds.anyTransition()))
      throw new RuntimeException("INVALID TRANSITION");
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
