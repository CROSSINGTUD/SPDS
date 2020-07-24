package boomerang.scene.wala;

import boomerang.scene.Field;
import com.ibm.wala.types.FieldReference;

public class WALAField extends Field {

  private FieldReference fieldRef;

  public WALAField(FieldReference fieldRef) {
    this.fieldRef = fieldRef;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = super.hashCode();
    result = prime * result + ((fieldRef == null) ? 0 : fieldRef.hashCode());
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) return true;
    if (!super.equals(obj)) return false;
    if (getClass() != obj.getClass()) return false;
    WALAField other = (WALAField) obj;
    if (fieldRef == null) {
      if (other.fieldRef != null) return false;
    } else if (!fieldRef.equals(other.fieldRef)) return false;
    return true;
  }

  @Override
  public String toString() {
    return fieldRef.toString();
  }

  @Override
  public boolean isInnerClassField() {
    throw new RuntimeException("Not yet implemented");
  }
}
