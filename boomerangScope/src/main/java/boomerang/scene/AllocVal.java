package boomerang.scene;

import boomerang.scene.ControlFlowGraph.Edge;

public class AllocVal extends Val {

  private Val delegate;
  private Val allocationVal;
  private Statement allocStatement;

  public AllocVal(Val delegate, Statement allocStatement, Val allocationVal) {
    super();
    this.delegate = delegate;
    this.allocationVal = allocationVal;
    this.allocStatement = allocStatement;
  }

  public Type getType() {
    return delegate.getType();
  }

  public Method m() {
    return delegate.m();
  }

  public String toString() {
    if (allocStatement.isAssign()) {
      if (allocStatement.getRightOp().isIntConstant()) {
        return delegate + " Value (int): " + allocStatement.getRightOp().getIntValue();
      }
      if (allocStatement.getRightOp().isStringConstant()) {
        return delegate + " Value (String): " + allocStatement.getRightOp().getStringValue();
      }
    }
    if (delegate == null) return "";
    return delegate.toString();
  }

  public boolean isStatic() {
    return delegate.isStatic();
  }

  public boolean isNewExpr() {
    return delegate.isNewExpr();
  }

  public Type getNewExprType() {
    return delegate.getNewExprType();
  }

  public boolean isUnbalanced() {
    return delegate.isUnbalanced();
  }

  public Val asUnbalanced(Edge stmt) {
    return delegate.asUnbalanced(stmt);
  }

  public boolean isLocal() {
    return delegate.isLocal();
  }

  public boolean isArrayAllocationVal() {
    return delegate.isArrayAllocationVal();
  }

  public boolean isNull() {
    return allocationVal.isNull();
  }

  public boolean isStringConstant() {
    return delegate.isStringConstant();
  }

  public String getStringValue() {
    return delegate.getStringValue();
  }

  public boolean isStringBufferOrBuilder() {
    return delegate.isStringBufferOrBuilder();
  }

  public boolean isThrowableAllocationType() {
    return delegate.isThrowableAllocationType();
  }

  public boolean isCast() {
    return delegate.isCast();
  }

  public Val getCastOp() {
    return delegate.getCastOp();
  }

  public boolean isArrayRef() {
    return delegate.isArrayRef();
  }

  public boolean isInstanceOfExpr() {
    return delegate.isInstanceOfExpr();
  }

  public Val getInstanceOfOp() {
    return delegate.getInstanceOfOp();
  }

  public boolean isLengthExpr() {
    return delegate.isLengthExpr();
  }

  public Val getLengthOp() {
    return delegate.getLengthOp();
  }

  public boolean isIntConstant() {
    return delegate.isIntConstant();
  }

  public boolean isClassConstant() {
    return delegate.isClassConstant();
  }

  public Type getClassConstantType() {
    return delegate.getClassConstantType();
  }

  public Val withNewMethod(Method callee) {
    return delegate.withNewMethod(callee);
  }

  public Val withSecondVal(Val leftOp) {
    return delegate.withSecondVal(leftOp);
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = super.hashCode();
    result = prime * result + ((allocStatement == null) ? 0 : allocStatement.hashCode());
    result = prime * result + ((allocationVal == null) ? 0 : allocationVal.hashCode());
    result = prime * result + ((delegate == null) ? 0 : delegate.hashCode());
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) return true;
    if (!super.equals(obj)) return false;
    if (getClass() != obj.getClass()) return false;
    AllocVal other = (AllocVal) obj;
    if (allocStatement == null) {
      if (other.allocStatement != null) return false;
    } else if (!allocStatement.equals(other.allocStatement)) return false;
    if (allocationVal == null) {
      if (other.allocationVal != null) return false;
    } else if (!allocationVal.equals(other.allocationVal)) return false;
    if (delegate == null) {
      if (other.delegate != null) return false;
    } else if (!delegate.equals(other.delegate)) return false;
    return true;
  }

  public Val getAllocVal() {
    return allocationVal;
  }

  public Val getDelegate() {
    return delegate;
  }

  @Override
  public boolean isLongConstant() {
    return false;
  }

  @Override
  public int getIntValue() {
    return delegate.getIntValue();
  }

  @Override
  public long getLongValue() {
    return delegate.getLongValue();
  }

  @Override
  public Pair<Val, Integer> getArrayBase() {
    return delegate.getArrayBase();
  }

  @Override
  public String getVariableName() {
    return delegate.getVariableName();
  }
}
