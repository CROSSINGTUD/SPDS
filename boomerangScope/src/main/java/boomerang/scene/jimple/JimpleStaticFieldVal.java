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
package boomerang.scene.jimple;

import boomerang.scene.ControlFlowGraph.Edge;
import boomerang.scene.Field;
import boomerang.scene.Method;
import boomerang.scene.Pair;
import boomerang.scene.StaticFieldVal;
import boomerang.scene.Type;
import boomerang.scene.Val;

public class JimpleStaticFieldVal extends StaticFieldVal {

  private final JimpleField field;

  public JimpleStaticFieldVal(JimpleField field, Method m) {
    this(field, m, null);
  }

  private JimpleStaticFieldVal(JimpleField field, Method m, Edge unbalanced) {
    super(m, unbalanced);
    this.field = field;
  }

  @Override
  public boolean isStatic() {
    return true;
  }

  public String toString() {
    return "StaticField: " + field + m;
  }

  public Field field() {
    return field;
  };

  @Override
  public Val asUnbalanced(Edge stmt) {
    return new JimpleStaticFieldVal(field, m, stmt);
  }

  @Override
  public Type getType() {
    return new JimpleType(field.getSootField().getType());
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
    return new JimpleStaticFieldVal(field, callee);
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = super.hashCode();
    result = prime * result + ((field == null) ? 0 : field.hashCode());
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) return true;
    if (!super.equals(obj)) return false;
    if (getClass() != obj.getClass()) return false;
    JimpleStaticFieldVal other = (JimpleStaticFieldVal) obj;
    if (field == null) {
      if (other.field != null) return false;
    } else if (!field.equals(other.field)) return false;
    return true;
  }

  @Override
  public boolean isLongConstant() {
    return false;
  }

  @Override
  public int getIntValue() {
    return -1;
  }

  @Override
  public long getLongValue() {
    return -1;
  }

  @Override
  public Pair<Val, Integer> getArrayBase() {
    return null;
  }

  @Override
  public String getVariableName() {
    return toString();
  }
}
