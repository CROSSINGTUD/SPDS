package boomerang.controlflowgraph;

import boomerang.scene.Statement;

public interface ObservableControlFlowGraph {

  void addPredsOfListener(PredecessorListener l);

  void addSuccsOfListener(SuccessorListener l);

  void step(Statement curr, Statement succ);

  void unregisterAllListeners();
}
