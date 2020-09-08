package boomerang.controlflowgraph;

import boomerang.scene.Statement;

public abstract class PredecessorListener {

  private final Statement curr;

  public PredecessorListener(Statement curr) {
    this.curr = curr;
  }

  public Statement getCurr() {
    return curr;
  }

  public abstract void getPredecessor(Statement pred);
}
