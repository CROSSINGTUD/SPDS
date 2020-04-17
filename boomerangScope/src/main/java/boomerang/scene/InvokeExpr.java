package boomerang.scene;

import java.util.List;

public interface InvokeExpr {

  public Val getArg(int index);

  public List<Val> getArgs();

  public boolean isInstanceInvokeExpr();

  public Val getBase();

  public DeclaredMethod getMethod();

  public boolean isSpecialInvokeExpr();

  public boolean isStaticInvokeExpr();
}
