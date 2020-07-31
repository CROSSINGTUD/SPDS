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
import boomerang.scene.Method;
import boomerang.scene.Pair;
import boomerang.scene.Statement;
import boomerang.scene.StaticFieldVal;
import boomerang.scene.Type;
import boomerang.scene.Val;
import com.ibm.wala.analysis.typeInference.TypeAbstraction;
import java.util.Collection;

public class WALADummyNullStatement extends WALAStatement {

  private Val leftOp;
  private WALAVal rightOp;

  public WALADummyNullStatement(Val a, Method method) {
    super(a + " = null", method);
    this.leftOp = a;
    this.rightOp =
        new WALAVal(-1, (WALAMethod) method) {
          @Override
          public boolean isNull() {
            return true;
          }

          @Override
          public Type getType() {
            return new WALAType(TypeAbstraction.TOP);
          }

          @Override
          public int hashCode() {
            return System.identityHashCode(this);
          }

          @Override
          public boolean equals(Object obj) {
            return this == obj;
          }
        };
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
    throw new RuntimeException("Illegal");
  }

  @Override
  public boolean isFieldWriteWithBase(Val base) {
    return false;
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
  public Val getLeftOp() {
    return leftOp;
  }

  @Override
  public Val getRightOp() {
    return rightOp;
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
    throw new RuntimeException("Illegal");
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
    return false;
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
    throw new RuntimeException("Illegal");
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
    throw new RuntimeException("Illegal");
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

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = super.hashCode();
    result = prime * result + ((leftOp == null) ? 0 : leftOp.hashCode());
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) return true;
    if (!super.equals(obj)) return false;
    if (getClass() != obj.getClass()) return false;
    WALADummyNullStatement other = (WALADummyNullStatement) obj;
    if (leftOp == null) {
      if (other.leftOp != null) return false;
    } else if (!leftOp.equals(other.leftOp)) return false;
    return true;
  }

  @Override
  public String toString() {
    return leftOp + " = null";
  }
}
