package boomerang.controlflowgraph;

import boomerang.scene.Statement;

public abstract class SuccessorListener {
  private final Statement curr;

  public SuccessorListener(Statement curr) {
    this.curr = curr;
  }

  public Statement getCurr() {
    return curr;
  }

  public abstract void getSuccessor(Statement succ);
}
