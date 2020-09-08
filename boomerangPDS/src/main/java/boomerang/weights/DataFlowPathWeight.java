package boomerang.weights;

import boomerang.scene.ControlFlowGraph.Edge;
import boomerang.scene.Method;
import boomerang.scene.Statement;
import boomerang.scene.Val;
import boomerang.weights.PathConditionWeight.ConditionDomain;
import com.google.common.base.Objects;
import java.util.List;
import java.util.Map;
import sync.pds.solver.nodes.Node;
import wpds.impl.Weight;

public class DataFlowPathWeight extends Weight {

  private static DataFlowPathWeight one;

  private PathTrackingWeight path;
  private PathConditionWeight condition;

  private DataFlowPathWeight() {
    path = PathTrackingWeight.one();
    condition = PathConditionWeight.one();
  }

  public DataFlowPathWeight(Node<Edge, Val> path) {
    this.path = new PathTrackingWeight(path);
    this.condition = PathConditionWeight.one();
  }

  public DataFlowPathWeight(Node<Edge, Val> path, Statement callSite, Method callee) {
    this.path = new PathTrackingWeight(path);
    this.condition = new PathConditionWeight(callSite, callee);
  }

  public DataFlowPathWeight(Statement callSite, Method callee) {
    this.path = PathTrackingWeight.one();
    this.condition = new PathConditionWeight(callSite, callee);
  }

  public DataFlowPathWeight(Statement ifStatement, Boolean condition) {
    this.path = PathTrackingWeight.one();
    this.condition = new PathConditionWeight(ifStatement, condition);
  }

  private DataFlowPathWeight(PathTrackingWeight path, PathConditionWeight condition) {
    this.path = path;
    this.condition = condition;
  }

  public DataFlowPathWeight(Val leftOp, ConditionDomain conditionVal) {
    this.path = PathTrackingWeight.one();
    this.condition = new PathConditionWeight(leftOp, conditionVal);
  }

  public DataFlowPathWeight(Val returnVal) {
    this.path = PathTrackingWeight.one();
    this.condition = new PathConditionWeight(returnVal);
  }

  public static DataFlowPathWeight one() {
    if (one == null) one = new DataFlowPathWeight();
    return one;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    DataFlowPathWeight that = (DataFlowPathWeight) o;
    return Objects.equal(path, that.path) && Objects.equal(condition, that.condition);
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(path, condition);
  }

  public List<Node<Edge, Val>> getAllStatements() {
    return path.getShortestPathWitness();
  }

  public Map<Statement, ConditionDomain> getConditions() {
    return condition.getConditions();
  }

  public Map<Val, ConditionDomain> getEvaluationMap() {
    return condition.getEvaluationMap();
  }

  @Override
  public String toString() {
    return /*"PATH" + path +*/ " COND: " + condition;
  }

  public Weight extendWith(Weight other) {
    return new DataFlowPathWeight(
        (PathTrackingWeight) path.extendWith(((DataFlowPathWeight) other).path),
        (PathConditionWeight) condition.extendWith(((DataFlowPathWeight) other).condition));
  }

  @Override
  public Weight combineWith(Weight other) {
    return new DataFlowPathWeight(
        (PathTrackingWeight) path.combineWith(((DataFlowPathWeight) other).path),
        (PathConditionWeight) condition.combineWith(((DataFlowPathWeight) other).condition));
  }
}
