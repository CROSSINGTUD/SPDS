package boomerang.scene.jimple;

import boomerang.scene.DeclaredMethod;
import boomerang.scene.InvokeExpr;
import boomerang.scene.Method;
import boomerang.scene.Val;
import com.google.common.collect.Lists;
import java.util.ArrayList;
import java.util.List;
import soot.jimple.InstanceInvokeExpr;
import soot.jimple.SpecialInvokeExpr;
import soot.jimple.StaticInvokeExpr;

public class JimpleInvokeExpr implements InvokeExpr {

  private soot.jimple.InvokeExpr delegate;
  private Method m;
  private ArrayList<Val> cache;

  public JimpleInvokeExpr(soot.jimple.InvokeExpr ive, Method m) {
    this.delegate = ive;
    this.m = m;
  }

  public Val getArg(int index) {
    if (delegate.getArg(index) == null) {
      return Val.zero();
    }
    return new JimpleVal(delegate.getArg(index), m);
  }

  public List<Val> getArgs() {
    if (cache == null) {
      cache = Lists.newArrayList();
      for (int i = 0; i < delegate.getArgCount(); i++) {
        cache.add(getArg(i));
      }
    }
    return cache;
  }

  public boolean isInstanceInvokeExpr() {
    return delegate instanceof InstanceInvokeExpr;
  }

  public Val getBase() {
    InstanceInvokeExpr iie = (InstanceInvokeExpr) delegate;
    return new JimpleVal(iie.getBase(), m);
  }

  public DeclaredMethod getMethod() {
    return new JimpleDeclaredMethod(this, delegate.getMethod());
  }

  public boolean isSpecialInvokeExpr() {
    return delegate instanceof SpecialInvokeExpr;
  }

  public boolean isStaticInvokeExpr() {
    return delegate instanceof StaticInvokeExpr;
  }

  @Override
  public String toString() {
    return delegate.toString();
  }
}
