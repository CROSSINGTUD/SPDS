package boomerang.controlflowgraph;

import boomerang.scene.Statement;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import java.util.Collection;

public class DynamicCFG implements ObservableControlFlowGraph {

  Multimap<Statement, PredecessorListener> statementToPredecessorListener = HashMultimap.create();
  Multimap<Statement, SuccessorListener> statementToSuccessorListener = HashMultimap.create();

  Multimap<Statement, Statement> succToPred = HashMultimap.create();
  Multimap<Statement, Statement> predToSucc = HashMultimap.create();

  @Override
  public void addPredsOfListener(PredecessorListener l) {
    if (statementToPredecessorListener.put(l.getCurr(), l)) {
      Collection<Statement> preds = Lists.newArrayList(succToPred.get(l.getCurr()));
      for (Statement pred : preds) {
        l.getPredecessor(pred);
      }
    }
  }

  @Override
  public void addSuccsOfListener(SuccessorListener l) {
    if (statementToSuccessorListener.put(l.getCurr(), l)) {
      Collection<Statement> succs = Lists.newArrayList(predToSucc.get(l.getCurr()));
      for (Statement succ : succs) {
        l.getSuccessor(succ);
      }
    }
  }

  public void step(Statement curr, Statement succ) {
    predToSucc.put(curr, succ);
    succToPred.put(succ, curr);
    Collection<SuccessorListener> lsnr = Lists.newArrayList(statementToSuccessorListener.get(curr));
    for (SuccessorListener l : lsnr) {
      l.getSuccessor(succ);
    }
    Collection<PredecessorListener> lisnr =
        Lists.newArrayList(statementToPredecessorListener.get(succ));
    for (PredecessorListener l : lisnr) {
      l.getPredecessor(curr);
    }
  }

  @Override
  public void unregisterAllListeners() {
    statementToPredecessorListener.clear();
    statementToSuccessorListener.clear();
  }
}
