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
import boomerang.scene.Method;
import boomerang.scene.Pair;
import boomerang.scene.StaticFieldVal;
import boomerang.scene.Type;
import boomerang.scene.Val;
import soot.Local;
import soot.NullType;
import soot.Scene;
import soot.Value;
import soot.jimple.ArrayRef;
import soot.jimple.CastExpr;
import soot.jimple.ClassConstant;
import soot.jimple.InstanceOfExpr;
import soot.jimple.IntConstant;
import soot.jimple.LengthExpr;
import soot.jimple.LongConstant;
import soot.jimple.NewArrayExpr;
import soot.jimple.NewExpr;
import soot.jimple.NewMultiArrayExpr;
import soot.jimple.NullConstant;
import soot.jimple.StaticFieldRef;
import soot.jimple.StringConstant;

public class JimpleVal extends Val {
  private final Value v;

  public JimpleVal(Value v, Method m) {
    this(v, m, null);
  }

  protected JimpleVal(Value v, Method m, Edge unbalanced) {
    super(m, unbalanced);
    if (v == null) throw new RuntimeException("Value must not be null!");
    this.v = v;
  }

  public JimpleType getType() {
    return v == null ? new JimpleType(NullType.v()) : new JimpleType(v.getType());
  }

  public Method m() {
    return m;
  }

  @Override
  public String toString() {
    return v.toString()
        + " ("
        + m.getDeclaringClass()
        + "."
        + m
        + ")"
        + (isUnbalanced() ? " unbalanaced " + unbalancedStmt : "");
  }

  public boolean isStatic() {
    return false;
  }

  public boolean isNewExpr() {
    return v instanceof NewExpr;
  }

  public Type getNewExprType() {
    return new JimpleType(((NewExpr) v).getType());
  }

  public Val asUnbalanced(Edge stmt) {
    return new JimpleVal(v, m, stmt);
  }

  public boolean isLocal() {
    return v instanceof Local;
  }

  public boolean isArrayAllocationVal() {
    if (v instanceof NewArrayExpr) {
      NewArrayExpr expr = (NewArrayExpr) v;
      // TODO Performance issue?!
      //            return expr.getBaseType() instanceof RefType;
      return true;
    } else if (v instanceof NewMultiArrayExpr) {
      return true;
    }
    return false;
  }

  public boolean isNull() {
    return v instanceof NullConstant;
  }

  public boolean isStringConstant() {
    return v instanceof StringConstant;
  }

  public String getStringValue() {
    return ((StringConstant) v).value;
  }

  public boolean isStringBufferOrBuilder() {
    Type type = getType();
    return type.toString().equals("java.lang.String")
        || type.toString().equals("java.lang.StringBuilder")
        || type.toString().equals("java.lang.StringBuffer");
  }

  public boolean isThrowableAllocationType() {
    return Scene.v()
        .getOrMakeFastHierarchy()
        .canStoreType(getType().getDelegate(), Scene.v().getType("java.lang.Throwable"));
  }

  public boolean isCast() {
    return v instanceof CastExpr;
  }

  public Val getCastOp() {
    CastExpr cast = (CastExpr) v;
    return new JimpleVal(cast.getOp(), m);
  }

  public boolean isInstanceFieldRef() {
    return v instanceof soot.jimple.InstanceFieldRef;
  }

  public boolean isStaticFieldRef() {
    return v instanceof StaticFieldRef;
  }

  public StaticFieldVal getStaticField() {
    StaticFieldRef val = (StaticFieldRef) v;
    return new JimpleStaticFieldVal(new JimpleField(val.getField()), m);
  }

  public boolean isArrayRef() {
    return v instanceof ArrayRef;
  }

  @Override
  public Pair<Val, Integer> getArrayBase() {
    return new Pair<>(
        new JimpleVal(((ArrayRef) v).getBase(), m),
        ((ArrayRef) v).getIndex() instanceof IntConstant
            ? ((IntConstant) ((ArrayRef) v).getIndex()).value
            : -1);
  }

  public boolean isInstanceOfExpr() {
    return v instanceof InstanceOfExpr;
  }

  public Val getInstanceOfOp() {
    InstanceOfExpr val = (InstanceOfExpr) v;
    return new JimpleVal(val.getOp(), m);
  }

  public boolean isLengthExpr() {
    return v instanceof LengthExpr;
  }

  public Val getLengthOp() {
    LengthExpr val = (LengthExpr) v;
    return new JimpleVal(val.getOp(), m);
  }

  public boolean isIntConstant() {
    return v instanceof IntConstant;
  }

  public int getIntValue() {
    return ((IntConstant) v).value;
  }

  public boolean isLongConstant() {
    return v instanceof LongConstant;
  }

  public long getLongValue() {
    return ((LongConstant) v).value;
  }

  public boolean isClassConstant() {
    return v instanceof ClassConstant;
  }

  public Type getClassConstantType() {
    return new JimpleType(((ClassConstant) v).toSootType());
  }

  @Override
  public Val withNewMethod(Method callee) {
    throw new RuntimeException("Only allowed for static fields");
  }

  @Override
  public Val withSecondVal(Val leftOp) {
    return new JimpleDoubleVal(v, m, leftOp);
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = super.hashCode();
    result = prime * result + ((v == null) ? 0 : v.hashCode());
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) return true;
    if (!super.equals(obj)) return false;
    if (getClass() != obj.getClass()) return false;
    JimpleVal other = (JimpleVal) obj;
    if (v == null) {
      if (other.v != null) return false;
    } else if (!v.equals(other.v)) return false;
    return true;
  }

  public Value getDelegate() {
    return v;
  }

  @Override
  public String getVariableName() {
    return v.toString();
  }
}
