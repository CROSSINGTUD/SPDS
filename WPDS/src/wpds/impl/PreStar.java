package wpds.impl;

import java.util.LinkedList;
import java.util.List;

import wpds.interfaces.IPushdownSystem;
import wpds.interfaces.Location;
import wpds.interfaces.State;
import wpds.interfaces.Weight;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

public class PreStar<N extends Location, D extends State, W extends Weight> {
  private LinkedList<Transition<N, D, W>> worklist = Lists.newLinkedList();
  private IPushdownSystem<N, D, W> pds;
  private WeightedPAutomaton<N, D, W> fa;

  public WeightedPAutomaton<N, D, W> prestar(IPushdownSystem<N, D, W> pds,
      WeightedPAutomaton<N, D, W> initialAutomaton) {
    this.pds = pds;
    worklist = Lists.newLinkedList(initialAutomaton.getTransitions());
    fa = initialAutomaton;

    for (Transition<N, D, W> trans : Sets.newHashSet(fa.getTransitions())) {
      fa.addWeightForTransition(trans, pds.getOne());
    }
    for (PopRule<N, D, W> r : pds.getPopRules()) {
      assert r instanceof PopRule;
      update(new Transition<N, D, W>(r.getS1(), r.getL1(), r.getS2()), r.getWeight(),
          Lists.<Transition<N, D, W>>newLinkedList());
    }

    while (!worklist.isEmpty()) {
      Transition<N, D, W> t = worklist.removeFirst();

      // Normal rules
      for (NormalRule<N, D, W> r : pds.getNormalRules()) {
        if (r.getTargetConfig().equals(t.getStartConfig())) {
          LinkedList<Transition<N, D, W>> previous = Lists.<Transition<N, D, W>>newLinkedList();
          previous.add(t);
          update(new Transition<N, D, W>(r.getS1(), r.getL1(), t.getTarget()), r.getWeight(),
              previous);
        }
      }

      // Push rules
      for (PushRule<N, D, W> r : pds.getPushRules()) {
        if (r.getTargetConfig().equals(t.getStartConfig())) {
          LinkedList<Transition<N, D, W>> previous = Lists.<Transition<N, D, W>>newLinkedList();
          previous.add(t);
          for (Transition<N, D, W> tdash : Sets.newHashSet(fa.getTransitions())) {
            previous.add(tdash);
            update(new Transition<N, D, W>(r.getS1(), r.getL1(), tdash.getTarget()), r.getWeight(),
                previous);
          }
        }
      }
      for (PushRule<N, D, W> r : pds.getPushRules()) {
        if (!r.getCallSite().equals(t.getString())) {
          continue;
        }
        Transition<N, D, W> tdash = new Transition<N, D, W>(r.getS2(), r.getL2(), t.getTarget());
        if (!fa.getTransitions().contains(tdash)) {
          continue;
        }
        LinkedList<Transition<N, D, W>> previous = Lists.<Transition<N, D, W>>newLinkedList();
        previous.add(tdash);
        previous.add(t);
        update(new Transition<N, D, W>(r.getS1(), r.getL1(), t.getTarget()), r.getWeight(),
            previous);
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
