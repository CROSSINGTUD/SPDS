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

import boomerang.scene.ControlFlowGraph.Edge;
import boomerang.scene.Field;
import boomerang.scene.Method;
import boomerang.scene.Pair;
import boomerang.scene.StaticFieldVal;
import boomerang.scene.Type;
import boomerang.scene.Val;

public class WALAStaticFieldVal extends StaticFieldVal {

  private Field declaredField;

  public WALAStaticFieldVal(Field declaredField, Method method) {
    this(declaredField, method, null);
  }

  public WALAStaticFieldVal(Field declaredField, Method method, Edge unbalanced) {
    super(method);
    this.declaredField = declaredField;
  }

  @Override
  public Field field() {
    return declaredField;
  }

  @Override
  public Val asUnbalanced(Edge stmt) {
    return new WALAStaticFieldVal(declaredField, m, stmt);
  }

  @Override
  public Type getType() {
    throw new RuntimeException("Fault!");
  }

  @Override
  public boolean isStatic() {
    return true;
  }

  @Override
  public boolean isNewExpr() {
    return false;
  }

  @Override
  public Type getNewExprType() {
    throw new RuntimeException("Fault!");
  }

  @Override
  public boolean isLocal() {
    return false;
  }

  @Override
  public boolean isArrayAllocationVal() {
    return false;
  }

  @Override
  public boolean isNull() {
    return false;
  }

  @Override
  public boolean isStringConstant() {
    return false;
  }

  @Override
  public String getStringValue() {
    throw new RuntimeException("Fault!");
  }

  @Override
  public boolean isStringBufferOrBuilder() {
    return false;
  }

  @Override
  public boolean isThrowableAllocationType() {
    return false;
  }

  @Override
  public boolean isCast() {
    return false;
  }

  @Override
  public Val getCastOp() {
    throw new RuntimeException("Fault!");
  }

  @Override
  public boolean isArrayRef() {
    return false;
  }

  @Override
  public boolean isInstanceOfExpr() {
    return false;
  }

  @Override
  public Val getInstanceOfOp() {
    throw new RuntimeException("Fault!");
  }

  @Override
  public boolean isLengthExpr() {
    return false;
  }

  @Override
  public Val getLengthOp() {
    throw new RuntimeException("Fault!");
  }

  @Override
  public boolean isIntConstant() {
    return false;
  }

  @Override
  public boolean isClassConstant() {
    return false;
  }

  @Override
  public Type getClassConstantType() {
    throw new RuntimeException("Fault!");
  }

  @Override
  public Val withNewMethod(Method callee) {
    return new WALAStaticFieldVal(declaredField, callee, unbalancedStmt);
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = super.hashCode();
    result = prime * result + ((declaredField == null) ? 0 : declaredField.hashCode());
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) return true;
    if (!super.equals(obj)) return false;
    if (getClass() != obj.getClass()) return false;
    WALAStaticFieldVal other = (WALAStaticFieldVal) obj;
    if (declaredField == null) {
      if (other.declaredField != null) return false;
    } else if (!declaredField.equals(other.declaredField)) return false;
    return true;
  }

  @Override
  public boolean isLongConstant() {
    // TODO Auto-generated method stub
    return false;
  }

  @Override
  public int getIntValue() {
    // TODO Auto-generated method stub
    return 0;
  }

  @Override
  public long getLongValue() {
    // TODO Auto-generated method stub
    return 0;
  }

  @Override
  public Pair<Val, Integer> getArrayBase() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public String getVariableName() {
    return toString();
  }
}
