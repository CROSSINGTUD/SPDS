/**
 * ***************************************************************************** Copyright (c) 2020
 * CodeShield GmbH, Paderborn, Germany. This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * <p>SPDX-License-Identifier: EPL-2.0
 *
 * <p>Contributors: Johannes Spaeth - initial API and implementation
 * *****************************************************************************
 */
package boomerang.scene.wala;

import boomerang.scene.Field;
import boomerang.scene.IfStatement;
import boomerang.scene.InvokeExpr;
import boomerang.scene.Pair;
import boomerang.scene.Statement;
import boomerang.scene.StaticFieldVal;
import boomerang.scene.Val;
import java.util.Collection;

public class WALAUnitializedFieldStatement extends WALAStatement {

  private WALAField field;
  private WALAMethod method;
  private Val thisLocal;
  private Val rightOp;

  public WALAUnitializedFieldStatement(
      WALAField field, WALAMethod method, Val thisLocal, Val rightOp) {
    super("this." + field + " = " + rightOp, method);
    this.field = field;
    this.method = method;
    this.thisLocal = thisLocal;
    this.rightOp = rightOp;
  }

  @Override
  public boolean containsStaticFieldAccess() {
    return false;
  }

  @Override
  public boolean containsInvokeExpr() {
    return false;
  }

  @Override
  public Field getWrittenField() {
    return field;
  }

  @Override
  public Val getRightOp() {
    return rightOp;
  }

  @Override
  public boolean isFieldWriteWithBase(Val base) {
    return thisLocal.equals(base);
  }

  @Override
  public Field getLoadedField() {
    throw new RuntimeException("Illegal");
  }

  @Override
  public boolean isFieldLoadWithBase(Val base) {
    return false;
  }

  @Override
  public boolean isAssign() {
    return true;
  }

  @Override
  public boolean isInstanceOfStatement(Val fact) {
    return false;
  }

  @Override
  public boolean isCast() {
    return false;
  }

  @Override
  public boolean isPhiStatement() {
    return false;
  }

  @Override
  public InvokeExpr getInvokeExpr() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public boolean isReturnStmt() {
    return false;
  }

  @Override
  public boolean isThrowStmt() {
    return false;
  }

  @Override
  public boolean isIfStmt() {
    return false;
  }

  @Override
  public IfStatement getIfStmt() {
    throw new RuntimeException("Illegal");
  }

  @Override
  public Val getReturnOp() {
    throw new RuntimeException("Illegal");
  }

  @Override
  public boolean isMultiArrayAllocation() {
    return false;
  }

  @Override
  public boolean isStringAllocation() {
    return false;
  }

  @Override
  public boolean isFieldStore() {
    return true;
  }

  @Override
  public boolean isArrayStore() {
    return false;
  }

  @Override
  public boolean isArrayLoad() {
    return false;
  }

  @Override
  public boolean isFieldLoad() {
    return false;
  }

  @Override
  public boolean isIdentityStmt() {
    return false;
  }

  @Override
  public Pair<Val, Field> getFieldStore() {
    return new Pair<Val, Field>(thisLocal, field);
  }

  @Override
  public Pair<Val, Field> getFieldLoad() {
    throw new RuntimeException("Illegal");
  }

  @Override
  public boolean isStaticFieldLoad() {
    return false;
  }

  @Override
  public boolean isStaticFieldStore() {
    return false;
  }

  @Override
  public StaticFieldVal getStaticField() {
    throw new RuntimeException("Illegal");
  }

  @Override
  public boolean killAtIfStmt(Val fact, Statement successor) {
    return false;
  }

  @Override
  public Collection<Val> getPhiVals() {
    return null;
  }

  @Override
  public Pair<Val, Integer> getArrayBase() {
    throw new RuntimeException("Illegal");
  }

  @Override
  public int getStartLineNumber() {
    return 0;
  }

  @Override
  public boolean isCatchStmt() {
    return false;
  }
}
