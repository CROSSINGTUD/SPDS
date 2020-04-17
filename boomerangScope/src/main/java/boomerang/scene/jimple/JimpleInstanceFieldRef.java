package boomerang.scene.jimple;

import boomerang.scene.Field;
import boomerang.scene.InstanceFieldRef;
import boomerang.scene.Val;

public class JimpleInstanceFieldRef implements InstanceFieldRef {

  private soot.jimple.InstanceFieldRef delegate;
  private JimpleMethod m;

  public JimpleInstanceFieldRef(soot.jimple.InstanceFieldRef ifr, JimpleMethod m) {
    this.delegate = ifr;
    this.m = m;
  }

  public Val getBase() {
    return new JimpleVal(delegate.getBase(), m);
  }

  public Field getField() {
    return new JimpleField(delegate.getField());
  }
}
