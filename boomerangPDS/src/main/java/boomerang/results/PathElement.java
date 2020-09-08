package boomerang.results;

import boomerang.scene.ControlFlowGraph.Edge;
import boomerang.scene.Val;
import java.util.Objects;

public class PathElement {

  private final Edge s;
  private final Val val;
  private final int stepIndex;

  public PathElement(Edge s, Val val, int stepIndex) {
    this.s = s;
    this.val = val;
    this.stepIndex = stepIndex;
  }

  public Edge getEdge() {
    return s;
  }

  public Val getVariable() {
    return val;
  }

  public int stepIndex() {
    return stepIndex;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    PathElement that = (PathElement) o;
    return stepIndex == that.stepIndex
        && Objects.equals(s, that.s)
        && Objects.equals(val, that.val);
  }

  @Override
  public int hashCode() {
    return Objects.hash(s, val, stepIndex);
  }

  @Override
  public String toString() {
    return String.format("'%d: %s'", stepIndex(), getEdge());
  }
}
