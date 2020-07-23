package boomerang.scene;

import java.util.Collection;

public class CallSiteStatement extends Statement {

  private Statement delegate;

  public CallSiteStatement(Statement delegate) {
    super(delegate.getMethod());
    this.delegate = delegate;
  }

  public ReturnSiteStatement getReturnSiteStatement() {
    return new ReturnSiteStatement(delegate);
  }

  public Method getMethod() {
    return delegate.getMethod();
  }

  public boolean containsStaticFieldAccess() {
    return delegate.containsStaticFieldAccess();
  }

  public boolean containsInvokeExpr() {
    return true;
  }

  public Field getWrittenField() {
    return delegate.getWrittenField();
  }

  public boolean isFieldWriteWithBase(Val base) {
    return delegate.isFieldWriteWithBase(base);
  }

  public Field getLoadedField() {
    return delegate.getLoadedField();
  }

  public boolean isFieldLoadWithBase(Val base) {
    return delegate.isFieldLoadWithBase(base);
  }

  public boolean isParameter(Val value) {
    return delegate.isParameter(value);
  }

  public boolean isReturnOperator(Val val) {
    return delegate.isReturnOperator(val);
  }

  public boolean uses(Val value) {
    return delegate.uses(value);
  }

  public boolean assignsValue(Val value) {
    return delegate.assignsValue(value);
  }

  public boolean isAssign() {
    return delegate.isAssign();
  }

  public Val getLeftOp() {
    return delegate.getLeftOp();
  }

  public Val getRightOp() {
    return delegate.getRightOp();
  }

  public boolean isInstanceOfStatement(Val fact) {
    return delegate.isInstanceOfStatement(fact);
  }

  public boolean isCast() {
    return delegate.isCast();
  }

  public boolean isPhiStatement() {
    return delegate.isPhiStatement();
  }

  public InvokeExpr getInvokeExpr() {
    return delegate.getInvokeExpr();
  }

  public boolean isReturnStmt() {
    return delegate.isReturnStmt();
  }

  public boolean isThrowStmt() {
    return delegate.isThrowStmt();
  }

  public boolean isIfStmt() {
    return delegate.isIfStmt();
  }

  public IfStatement getIfStmt() {
    return delegate.getIfStmt();
  }

  public Val getReturnOp() {
    return delegate.getReturnOp();
  }

  public boolean isMultiArrayAllocation() {
    return delegate.isMultiArrayAllocation();
  }

  public boolean isStringAllocation() {
    return delegate.isStringAllocation();
  }

  public boolean isFieldStore() {
    return delegate.isFieldStore();
  }

  public boolean isArrayStore() {
    return delegate.isArrayStore();
  }

  public boolean isArrayLoad() {
    return delegate.isArrayLoad();
  }

  public boolean isFieldLoad() {
    return delegate.isFieldLoad();
  }

  public boolean isIdentityStmt() {
    return delegate.isIdentityStmt();
  }

  public Pair<Val, Field> getFieldStore() {
    return delegate.getFieldStore();
  }

  public Pair<Val, Field> getFieldLoad() {
    return delegate.getFieldLoad();
  }

  public boolean isStaticFieldLoad() {
    return delegate.isStaticFieldLoad();
  }

  public boolean isStaticFieldStore() {
    return delegate.isStaticFieldStore();
  }

  public StaticFieldVal getStaticField() {
    return delegate.getStaticField();
  }

  public boolean killAtIfStmt(Val fact, Statement successor) {
    return delegate.killAtIfStmt(fact, successor);
  }

  public Collection<Val> getPhiVals() {
    return delegate.getPhiVals();
  }

  public Pair<Val, Integer> getArrayBase() {
    return delegate.getArrayBase();
  }

  public int getStartLineNumber() {
    return delegate.getStartLineNumber();
  }

  public int getStartColumnNumber() {
    return delegate.getStartColumnNumber();
  }

  public int getEndLineNumber() {
    return delegate.getEndLineNumber();
  }

  public int getEndColumnNumber() {
    return delegate.getEndColumnNumber();
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = super.hashCode();
    result = prime * result + ((delegate == null) ? 0 : delegate.hashCode());
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) return true;
    if (!super.equals(obj)) return false;
    if (getClass() != obj.getClass()) return false;
    CallSiteStatement other = (CallSiteStatement) obj;
    if (delegate == null) {
      if (other.delegate != null) return false;
    } else if (!delegate.equals(other.delegate)) return false;
    return true;
  }

  @Override
  public String toString() {
    return "CS: " + delegate.toString();
  }

  public Statement getDelegate() {
    return delegate;
  }

  @Override
  public boolean isCatchStmt() {
    return delegate.isCatchStmt();
  }
}
