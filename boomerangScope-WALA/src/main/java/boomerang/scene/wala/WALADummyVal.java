package boomerang.scene.wala;

import boomerang.scene.Type;
import com.ibm.wala.analysis.typeInference.TypeAbstraction;

public class WALADummyVal extends WALAVal {

  public WALADummyVal(WALAMethod method) {
    super(-1, method);
  }

  @Override
  public boolean isNull() {
    return false;
  }

  @Override
  public Type getType() {
    return new WALAType(TypeAbstraction.TOP);
  }

  @Override
  public int hashCode() {
    return System.identityHashCode(this);
  }

  @Override
  public boolean equals(Object obj) {
    return this == obj;
  }

  @Override
  public String toString() {
    return "dummy in " + method;
  }
}
