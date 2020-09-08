package boomerang.scene.jimple;

import boomerang.scene.IfStatement;
import boomerang.scene.Method;
import boomerang.scene.Statement;
import boomerang.scene.Val;
import soot.Value;
import soot.jimple.ConditionExpr;
import soot.jimple.EqExpr;
import soot.jimple.IfStmt;
import soot.jimple.IntConstant;
import soot.jimple.NeExpr;
import soot.jimple.NullConstant;

public class JimpleIfStatement implements IfStatement {

  private IfStmt delegate;
  private Method method;

  public JimpleIfStatement(IfStmt delegate, Method method) {
    this.delegate = delegate;
    this.method = method;
  }

  @Override
  public Statement getTarget() {
    return JimpleStatement.create(delegate.getTarget(), (JimpleMethod) method);
  }

  @Override
  public Evaluation evaluate(Val val) {
    if (delegate.getCondition() instanceof EqExpr) {
      EqExpr eqExpr = (EqExpr) delegate.getCondition();
      Value op1 = eqExpr.getOp1();
      Value op2 = eqExpr.getOp2();
      if ((val.equals(new JimpleVal(op1, method)) && op2.equals(NullConstant.v())
          || (val.equals(new JimpleVal(op2, method)) && op2.equals(NullConstant.v())))) {
        return Evaluation.TRUE;
      }
      if ((val.equals(new JimpleVal(IntConstant.v(0), method)) && op2.equals(IntConstant.v(0))
          || (val.equals(new JimpleVal(IntConstant.v(1), method))
              && op2.equals(IntConstant.v(1))))) {
        return Evaluation.TRUE;
      }
      if ((val.equals(new JimpleVal(IntConstant.v(1), method)) && op2.equals(IntConstant.v(0))
          || (val.equals(new JimpleVal(IntConstant.v(0), method))
              && op2.equals(IntConstant.v(1))))) {
        return Evaluation.FALSE;
      }
    }

    if (delegate.getCondition() instanceof NeExpr) {
      NeExpr eqExpr = (NeExpr) delegate.getCondition();
      Value op1 = eqExpr.getOp1();
      Value op2 = eqExpr.getOp2();
      if ((val.equals(new JimpleVal(op1, method)) && op2.equals(NullConstant.v())
          || (val.equals(new JimpleVal(op2, method)) && op2.equals(NullConstant.v())))) {
        return Evaluation.FALSE;
      }
      if ((val.equals(new JimpleVal(IntConstant.v(0), method)) && op2.equals(IntConstant.v(0))
          || (val.equals(new JimpleVal(IntConstant.v(1), method))
              && op2.equals(IntConstant.v(1))))) {
        return Evaluation.FALSE;
      }
      if ((val.equals(new JimpleVal(IntConstant.v(1), method)) && op2.equals(IntConstant.v(0))
          || (val.equals(new JimpleVal(IntConstant.v(0), method))
              && op2.equals(IntConstant.v(1))))) {
        return Evaluation.TRUE;
      }
    }
    return Evaluation.UNKOWN;
  }

  @Override
  public boolean uses(Val val) {
    if (delegate.getCondition() instanceof ConditionExpr) {
      ConditionExpr c = ((ConditionExpr) delegate.getCondition());
      Value op1 = c.getOp1();
      Value op2 = c.getOp2();
      return val.equals(new JimpleVal(op1, method)) || val.equals(new JimpleVal(op2, method));
    }
    return false;
  }
}
