package test;

import pathexpression.Edge;


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

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((label == null) ? 0 : label.hashCode());
    result = prime * result + start;
    result = prime * result + target;
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj)
      return true;
    if (obj == null)
      return false;
    if (getClass() != obj.getClass())
      return false;
    IntEdge other = (IntEdge) obj;
    if (label == null) {
      if (other.label != null)
        return false;
    } else if (!label.equals(other.label))
      return false;
    if (start != other.start)
      return false;
    if (target != other.target)
      return false;
    return true;
  }

}
