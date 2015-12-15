import heros.demandide.pathexpression.Edge;


public class IntEdge implements Edge<Integer, String> {
  private int start;
  private String label;
  private int target;

  public IntEdge(int start, String label, int target) {
    this.label = label;
    this.start = start;
    this.target = target;
  }

  @Override
  public Integer getStart() {
    return start;
  }

  @Override
  public Integer getTarget() {
    return target;
  }

  @Override
  public String getLabel() {
    return label;
  }

}
