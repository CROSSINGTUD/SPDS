package boomerang.scene.jimple;

import boomerang.scene.Method;
import boomerang.scene.Val;
import boomerang.scene.ValWithFalseVariable;
import soot.Value;

public class JimpleDoubleVal extends JimpleVal implements ValWithFalseVariable {
  private final Val falseVariable;

  public JimpleDoubleVal(Value v, Method m, Val instanceofValue) {
    super(v, m);
    this.falseVariable = instanceofValue;
  }

  public Val getFalseVariable() {
    return falseVariable;
  }

  @Override
  public String toString() {
    return "Instanceof " + falseVariable + " " + super.toString();
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = super.hashCode();
    result = prime * result + ((falseVariable == null) ? 0 : falseVariable.hashCode());
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) return true;
    if (!super.equals(obj)) return false;
    if (getClass() != obj.getClass()) return false;
    JimpleDoubleVal other = (JimpleDoubleVal) obj;
    if (falseVariable == null) {
      if (other.falseVariable != null) return false;
    } else if (!falseVariable.equals(other.falseVariable)) return false;
    return true;
  }
}
