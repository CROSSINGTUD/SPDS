/**
 * ***************************************************************************** Copyright (c) 2018
 * Fraunhofer IEM, Paderborn, Germany. This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * <p>SPDX-License-Identifier: EPL-2.0
 *
 * <p>Contributors: Johannes Spaeth - initial API and implementation
 * *****************************************************************************
 */
package boomerang.scene;

import java.util.Collection;
import wpds.interfaces.Empty;
import wpds.interfaces.Location;

public abstract class Statement implements Location {
  // Wrapper for stmt so we know the method
  private static Statement epsilon;
  private final String rep;
  protected final Method method;

  protected Statement(Method method) {
    this.rep = null;
    this.method = method;
  }

  private Statement(String rep) {
    this.rep = rep;
    this.method = null;
  }

  public static Statement epsilon() {
    if (epsilon == null) {
      epsilon = new EpsStatement();
    }
    return epsilon;
  }

  private static class EpsStatement extends Statement implements Empty {

    public EpsStatement() {
      super("Eps_s");
    }

    @Override
    public Method getMethod() {
      // TODO Auto-generated method stub
      return null;
    }

    @Override
    public boolean containsStaticFieldAccess() {
      // TODO Auto-generated method stub
      return false;
    }

    @Override
    public boolean containsInvokeExpr() {
      // TODO Auto-generated method stub
      return false;
    }

    @Override
    public Field getWrittenField() {
      // TODO Auto-generated method stub
      return null;
    }

    @Override
    public boolean isFieldWriteWithBase(Val base) {
      // TODO Auto-generated method stub
      return false;
    }

    @Override
    public Field getLoadedField() {
      // TODO Auto-generated method stub
      return null;
    }

    @Override
    public boolean isFieldLoadWithBase(Val base) {
      // TODO Auto-generated method stub
      return false;
    }

    @Override
    public boolean isParameter(Val value) {
      // TODO Auto-generated method stub
      return false;
    }

    @Override
    public boolean assignsValue(Val value) {
      // TODO Auto-generated method stub
      return false;
    }

    @Override
    public boolean isReturnOperator(Val val) {
      // TODO Auto-generated method stub
      return false;
    }

    @Override
    public boolean uses(Val value) {
      // TODO Auto-generated method stub
      return false;
    }

    @Override
    public boolean isAssign() {
      // TODO Auto-generated method stub
      return false;
    }

    @Override
    public Val getLeftOp() {
      // TODO Auto-generated method stub
      return null;
    }

    @Override
    public Val getRightOp() {
      // TODO Auto-generated method stub
      return null;
    }

    @Override
    public boolean isInstanceOfStatement(Val fact) {
      // TODO Auto-generated method stub
      return false;
    }

    @Override
    public boolean isCast() {
      // TODO Auto-generated method stub
      return false;
    }

    @Override
    public InvokeExpr getInvokeExpr() {
      // TODO Auto-generated method stub
      return null;
    }

    @Override
    public boolean isReturnStmt() {
      // TODO Auto-generated method stub
      return false;
    }

    @Override
    public boolean isThrowStmt() {
      // TODO Auto-generated method stub
      return false;
    }

    @Override
    public boolean isIfStmt() {
      // TODO Auto-generated method stub
      return false;
    }

    @Override
    public IfStatement getIfStmt() {
      // TODO Auto-generated method stub
      return null;
    }

    @Override
    public Val getReturnOp() {
      // TODO Auto-generated method stub
      return null;
    }

    @Override
    public boolean isMultiArrayAllocation() {
      // TODO Auto-generated method stub
      return false;
    }

    @Override
    public boolean isStringAllocation() {
      // TODO Auto-generated method stub
      return false;
    }

    @Override
    public boolean isFieldStore() {
      // TODO Auto-generated method stub
      return false;
    }

    @Override
    public boolean isArrayStore() {
      // TODO Auto-generated method stub
      return false;
    }

    @Override
    public boolean isArrayLoad() {
      // TODO Auto-generated method stub
      return false;
    }

    @Override
    public boolean isFieldLoad() {
      // TODO Auto-generated method stub
      return false;
    }

    @Override
    public boolean isIdentityStmt() {
      // TODO Auto-generated method stub
      return false;
    }

    @Override
    public boolean killAtIfStmt(Val fact, Statement successor) {
      // TODO Auto-generated method stub
      return false;
    }

    @Override
    public Pair<Val, Field> getFieldStore() {
      // TODO Auto-generated method stub
      return null;
    }

    @Override
    public Pair<Val, Field> getFieldLoad() {
      // TODO Auto-generated method stub
      return null;
    }

    @Override
    public boolean isStaticFieldLoad() {
      // TODO Auto-generated method stub
      return false;
    }

    @Override
    public boolean isStaticFieldStore() {
      // TODO Auto-generated method stub
      return false;
    }

    @Override
    public StaticFieldVal getStaticField() {
      // TODO Auto-generated method stub
      return null;
    }

    @Override
    public boolean isPhiStatement() {
      // TODO Auto-generated method stub
      return false;
    }

    @Override
    public Collection<Val> getPhiVals() {
      return null;
    }

    @Override
    public Pair<Val, Integer> getArrayBase() {
      // TODO Auto-generated method stub
      return null;
    }

    @Override
    public int getStartLineNumber() {
      // TODO Auto-generated method stub
      return 0;
    }

    @Override
    public int getStartColumnNumber() {
      // TODO Auto-generated method stub
      return 0;
    }

    @Override
    public int getEndColumnNumber() {
      // TODO Auto-generated method stub
      return 0;
    }

    @Override
    public int getEndLineNumber() {
      // TODO Auto-generated method stub
      return 0;
    }

    @Override
    public boolean isCatchStmt() {
      // TODO Auto-generated method stub
      return false;
    }

    @Override
    public int hashCode() {
      return System.identityHashCode(this);
    }

    @Override
    public boolean equals(Object obj) {
      return obj == this;
    }
  }

  @Override
  public String toString() {
    return rep;
  }

  public Method getMethod() {
    return this.method;
  }

  public abstract boolean containsStaticFieldAccess();

  public abstract boolean containsInvokeExpr();

  public abstract Field getWrittenField();

  public abstract boolean isFieldWriteWithBase(Val base);

  public abstract Field getLoadedField();

  public abstract boolean isFieldLoadWithBase(Val base);

  public boolean isParameter(Val value) {
    if (containsInvokeExpr()) {
      InvokeExpr invokeExpr = getInvokeExpr();
      if (invokeExpr.isInstanceInvokeExpr()) {
        if (invokeExpr.getBase().equals(value)) return true;
      }
      for (Val arg : invokeExpr.getArgs()) {
        if (arg.equals(value)) {
          return true;
        }
      }
    }
    return false;
  }

  public int getParameter(Val value) {
    if (containsInvokeExpr()) {
      InvokeExpr invokeExpr = getInvokeExpr();
      if (invokeExpr.isInstanceInvokeExpr()) {
        if (invokeExpr.getBase().equals(value)) return -2;
      }
      int index = 0;
      for (Val arg : invokeExpr.getArgs()) {
        if (arg.equals(value)) {
          return index;
        }
        index++;
      }
    }
    return -1;
  }

  public boolean isReturnOperator(Val val) {
    if (isReturnStmt()) {
      return getReturnOp().equals(val);
    }
    return false;
  }

  public boolean uses(Val value) {
    if (value.isStatic()) return true;
    if (assignsValue(value)) return true;
    if (isFieldStore()) {
      if (getFieldStore().getX().equals(value)) return true;
    }
    if (isReturnOperator(value)) return true;
    if (isParameter(value)) {
      return true;
    }
    return false;
  }

  public boolean assignsValue(Val value) {
    if (isAssign()) {
      if (getLeftOp().equals(value)) return true;
    }
    return false;
  }

  public abstract boolean isAssign();

  public abstract Val getLeftOp();

  public abstract Val getRightOp();

  public abstract boolean isInstanceOfStatement(Val fact);

  public abstract boolean isCast();

  public abstract boolean isPhiStatement();

  public abstract InvokeExpr getInvokeExpr();

  public abstract boolean isReturnStmt();

  public abstract boolean isThrowStmt();

  public abstract boolean isIfStmt();

  public abstract IfStatement getIfStmt();

  public abstract Val getReturnOp();

  public abstract boolean isMultiArrayAllocation();

  public abstract boolean isStringAllocation();

  public abstract boolean isFieldStore();

  public abstract boolean isArrayStore();

  public abstract boolean isArrayLoad();

  public abstract boolean isFieldLoad();

  public abstract boolean isIdentityStmt();

  public abstract Pair<Val, Field> getFieldStore();

  public abstract Pair<Val, Field> getFieldLoad();

  public abstract boolean isStaticFieldLoad();

  public abstract boolean isStaticFieldStore();

  public abstract StaticFieldVal getStaticField();

  /**
   * This method kills a data-flow at an if-stmt, it is assumed that the propagated "allocation"
   * site is x = null and fact is the propagated aliased variable. (i.e., y after a statement y =
   * x). If the if-stmt checks for if y != null or if y == null, data-flow propagation can be killed
   * when along the true/false branch.
   *
   * @param fact The data-flow value that bypasses the if-stmt
   * @return true if the Val fact shall be killed
   */
  public abstract boolean killAtIfStmt(Val fact, Statement successor);

  public abstract Collection<Val> getPhiVals();

  public abstract Pair<Val, Integer> getArrayBase();

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((method == null) ? 0 : method.hashCode());
    result = prime * result + ((rep == null) ? 0 : rep.hashCode());
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) return true;
    if (obj == null) return false;
    if (getClass() != obj.getClass()) return false;
    Statement other = (Statement) obj;
    if (method == null) {
      if (other.method != null) return false;
    } else if (!method.equals(other.method)) return false;
    if (rep == null) {
      if (other.rep != null) return false;
    } else if (!rep.equals(other.rep)) return false;
    return true;
  }

  public abstract int getStartLineNumber();

  public abstract int getStartColumnNumber();

  public abstract int getEndLineNumber();

  public abstract int getEndColumnNumber();

  public abstract boolean isCatchStmt();

  @Override
  public boolean accepts(Location other) {
    return this.equals(other);
  }
}
