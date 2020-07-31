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
import boomerang.scene.Val;
import boomerang.scene.wala.WALAVal.OP;
import com.google.common.collect.Lists;
import com.ibm.wala.cast.loader.AstMethod;
import com.ibm.wala.classLoader.IBytecodeMethod;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.shrikeCT.InvalidClassFileException;
import com.ibm.wala.ssa.SSAAbstractInvokeInstruction;
import com.ibm.wala.ssa.SSAArrayLoadInstruction;
import com.ibm.wala.ssa.SSAArrayReferenceInstruction;
import com.ibm.wala.ssa.SSAArrayStoreInstruction;
import com.ibm.wala.ssa.SSACheckCastInstruction;
import com.ibm.wala.ssa.SSAConditionalBranchInstruction;
import com.ibm.wala.ssa.SSAFieldAccessInstruction;
import com.ibm.wala.ssa.SSAGetInstruction;
import com.ibm.wala.ssa.SSAInstanceofInstruction;
import com.ibm.wala.ssa.SSAInstruction;
import com.ibm.wala.ssa.SSANewInstruction;
import com.ibm.wala.ssa.SSAPhiInstruction;
import com.ibm.wala.ssa.SSAPutInstruction;
import com.ibm.wala.ssa.SSAReturnInstruction;
import com.ibm.wala.ssa.SSAThrowInstruction;
import java.util.Collection;
import java.util.List;

public class WALAStatement extends Statement {

  private final SSAInstruction delegate;
  private final String rep;
  private List<Val> phiUseCache;

  protected WALAStatement(SSAInstruction stmt, Method method) {
    super(method);
    this.delegate = stmt;
    this.rep = null;
  }

  public WALAStatement(String string, Method method) {
    super(method);
    this.rep = string;
    this.delegate = null;
  }

  @Override
  public boolean containsStaticFieldAccess() {
    return isStaticFieldLoad() || isStaticFieldStore();
  }

  @Override
  public boolean containsInvokeExpr() {
    return delegate instanceof SSAAbstractInvokeInstruction;
  }

  @Override
  public Field getWrittenField() {
    SSAPutInstruction ins = (SSAPutInstruction) delegate;
    return new WALAField(ins.getDeclaredField());
  }

  @Override
  public boolean isFieldWriteWithBase(Val base) {
    if (delegate instanceof SSAPutInstruction) {
      SSAPutInstruction ins = (SSAPutInstruction) delegate;
      return base.equals(new WALAVal(ins.getRef(), (WALAMethod) method));
    }
    return false;
  }

  @Override
  public Field getLoadedField() {
    SSAGetInstruction ins = (SSAGetInstruction) delegate;
    return new WALAField(ins.getDeclaredField());
  }

  @Override
  public boolean isFieldLoadWithBase(Val base) {
    if (delegate instanceof SSAGetInstruction) {
      SSAGetInstruction ins = (SSAGetInstruction) delegate;
      return base.equals(new WALAVal(ins.getRef(), (WALAMethod) method));
    }
    return false;
  }

  @Override
  public boolean isReturnOperator(Val val) {
    if (isReturnStmt()) {
      SSAReturnInstruction ins = (SSAReturnInstruction) delegate;
      return ins.getResult() == -1
          ? false
          : new WALAVal(ins.getResult(), (WALAMethod) method).equals(val);
    }
    return false;
  }

  public boolean isPhiStatement() {
    return delegate instanceof SSAPhiInstruction;
  }

  public Collection<Val> getPhiVals() {
    SSAPhiInstruction ins = (SSAPhiInstruction) delegate;
    if (phiUseCache == null) {
      phiUseCache = Lists.newArrayList();
      for (int i = 0; i < ins.getNumberOfUses(); i++) {
        phiUseCache.add(new WALAVal(ins.getUse(i), (WALAMethod) method));
      }
    }

    return phiUseCache;
  }

  @Override
  public boolean isAssign() {
    return isFieldLoad()
        || isFieldStore()
        || isAllocationStatement()
        || isPhiStatement()
        || isAssigningCall()
        || isCast()
        || isArrayLoad()
        || isArrayStore()
        || isStaticFieldLoad()
        || isStaticFieldStore();
  }

  private boolean isAssigningCall() {
    if (containsInvokeExpr()) {
      if (((SSAAbstractInvokeInstruction) delegate).getNumberOfReturnValues() > 0) {
        return true;
      }
    }
    return false;
  }

  private boolean isAllocationStatement() {
    return delegate instanceof SSANewInstruction;
  }

  @Override
  public Val getLeftOp() {
    if (isFieldLoad() || isStaticFieldLoad()) {
      return new WALAVal(((SSAGetInstruction) delegate).getDef(), (WALAMethod) method);
    }
    if (isFieldStore()) {
      // The left op of a statement x.f = y must be the complete term x.f
      return new WALAVal(delegate, OP.LEFT, (WALAMethod) method);
    }
    if (isStaticFieldStore()) {
      return new WALAStaticFieldVal(
          new WALAField(((SSAFieldAccessInstruction) delegate).getDeclaredField()),
          (WALAMethod) method);
    }
    if (isAllocationStatement()) {
      return new WALAVal(((SSANewInstruction) delegate).getDef(), (WALAMethod) method);
    }
    if (isPhiStatement()) {
      return new WALAVal(((SSAPhiInstruction) delegate).getDef(), (WALAMethod) method);
    }
    if (isAssigningCall()) {
      return new WALAVal(
          ((SSAAbstractInvokeInstruction) delegate).getReturnValue(0), (WALAMethod) method);
    }
    if (isCast()) {
      return new WALAVal(((SSACheckCastInstruction) delegate).getDef(), (WALAMethod) method);
    }

    if (isArrayLoad()) {
      return new WALAVal(((SSAArrayLoadInstruction) delegate).getDef(), (WALAMethod) method);
    }

    if (isArrayStore()) {
      return new WALAVal(delegate, OP.LEFT, (WALAMethod) method);
    }
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public Val getRightOp() {
    if (isFieldLoad()) {
      // The right op of a statement x = y.f must be the complete term y.f
      return new WALAVal(delegate, OP.RIGHT, (WALAMethod) method);
    }
    if (isFieldStore() || isStaticFieldStore()) {
      return new WALAVal(((SSAPutInstruction) delegate).getVal(), (WALAMethod) method);
    }
    if (isAllocationStatement()) {
      return new WALAVal(((SSANewInstruction) delegate).getNewSite(), (WALAMethod) method);
    }
    if (isPhiStatement() || isAssigningCall()) {
      return new WALAVal(delegate, OP.RIGHT, (WALAMethod) method);
    }
    if (isCast()) {
      return new WALAVal(((SSACheckCastInstruction) delegate).getVal(), (WALAMethod) method);
    }
    if (isArrayLoad()) {
      return new WALAVal(delegate, OP.RIGHT, (WALAMethod) method);
    }
    if (isArrayStore()) {
      return new WALAVal(((SSAArrayStoreInstruction) delegate).getValue(), (WALAMethod) method);
    }
    if (isStaticFieldLoad()) {
      return new WALAStaticFieldVal(
          new WALAField(((SSAFieldAccessInstruction) delegate).getDeclaredField()),
          (WALAMethod) method);
    }
    return null;
  }

  @Override
  public boolean isInstanceOfStatement(Val fact) {
    return delegate instanceof SSAInstanceofInstruction;
  }

  @Override
  public boolean isCast() {
    return delegate instanceof SSACheckCastInstruction;
  }

  @Override
  public InvokeExpr getInvokeExpr() {
    SSAAbstractInvokeInstruction inv = (SSAAbstractInvokeInstruction) delegate;
    return new WALAInvokeExpr(inv, (WALAMethod) method);
  }

  @Override
  public boolean isReturnStmt() {
    if (delegate instanceof SSAReturnInstruction) {
      SSAReturnInstruction ret = (SSAReturnInstruction) delegate;
      return ret.getResult() != -1;
    }
    return false;
  }

  @Override
  public Val getReturnOp() {
    SSAReturnInstruction ret = (SSAReturnInstruction) delegate;
    return new WALAVal(ret.getResult(), (WALAMethod) method);
  }

  @Override
  public boolean isThrowStmt() {
    return delegate instanceof SSAThrowInstruction;
  }

  @Override
  public boolean isIfStmt() {
    return delegate instanceof SSAConditionalBranchInstruction;
  }

  @Override
  public IfStatement getIfStmt() {
    return new WALAIfStatement((SSAConditionalBranchInstruction) delegate, (WALAMethod) method);
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
    return delegate instanceof SSAPutInstruction && !((SSAPutInstruction) delegate).isStatic();
  }

  @Override
  public boolean isArrayStore() {
    return delegate instanceof SSAArrayStoreInstruction;
  }

  @Override
  public boolean isArrayLoad() {
    return delegate instanceof SSAArrayLoadInstruction;
  }

  @Override
  public boolean isFieldLoad() {
    return delegate instanceof SSAGetInstruction && !((SSAGetInstruction) delegate).isStatic();
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
  public String toString() {
    return (delegate == null ? rep : delegate.iIndex() + ":" + delegate + " in " + method);
  }

  @Override
  public Pair<Val, Field> getFieldStore() {
    SSAPutInstruction ins = (SSAPutInstruction) delegate;
    return new Pair<>(
        new WALAVal(ins.getRef(), (WALAMethod) method), new WALAField(ins.getDeclaredField()));
  }

  @Override
  public Pair<Val, Field> getFieldLoad() {
    SSAGetInstruction ins = (SSAGetInstruction) delegate;
    return new Pair<>(
        new WALAVal(ins.getRef(), (WALAMethod) method), new WALAField(ins.getDeclaredField()));
  }

  @Override
  public boolean isStaticFieldLoad() {
    return delegate instanceof SSAGetInstruction && ((SSAGetInstruction) delegate).isStatic();
  }

  @Override
  public boolean isStaticFieldStore() {
    return delegate instanceof SSAPutInstruction && ((SSAPutInstruction) delegate).isStatic();
  }

  @Override
  public StaticFieldVal getStaticField() {
    SSAFieldAccessInstruction stmt = (SSAFieldAccessInstruction) delegate;
    if (!stmt.isStatic()) throw new RuntimeException("Not a static field access statement");
    return new WALAStaticFieldVal(new WALAField(stmt.getDeclaredField()), method);
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = super.hashCode();
    if (delegate instanceof SSAPhiInstruction) {
      result = prime * result + ((SSAPhiInstruction) delegate).getDef();
    }
    result = prime * result + ((delegate == null) ? 0 : delegate.hashCode());
    result = prime * result + ((rep == null) ? 0 : rep.hashCode());
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) return true;
    if (obj == null) return false;
    if (getClass() != obj.getClass()) return false;
    if (!super.equals(obj)) {
      return false;
    }
    WALAStatement other = (WALAStatement) obj;
    if (delegate == null) {
      if (other.delegate != null) return false;
    } else if (!delegate.equals(other.delegate)) return false;
    if (rep == null) {
      if (other.rep != null) return false;
    } else if (!rep.equals(other.rep)) return false;
    if (delegate instanceof SSAPhiInstruction && other.delegate instanceof SSAPhiInstruction) {
      if (!phiEquals((SSAPhiInstruction) delegate, (SSAPhiInstruction) other.delegate)) {
        return false;
      }
    }
    return true;
  }

  /**
   * WALA computes ins1.equals(ins2) == true for two different phi instructions ins1 and ins2.
   *
   * @param phi1
   * @param phi2
   * @return
   */
  private boolean phiEquals(SSAPhiInstruction phi1, SSAPhiInstruction phi2) {
    return phi1.getDef() == phi2.getDef();
  }

  @Override
  public Pair<Val, Integer> getArrayBase() {
    if (delegate instanceof SSAArrayReferenceInstruction) {
      SSAArrayReferenceInstruction arrayRefIns = (SSAArrayReferenceInstruction) delegate;
      return new Pair<>(
          new WALAVal(arrayRefIns.getArrayRef(), (WALAMethod) method), arrayRefIns.getIndex());
    }
    throw new RuntimeException("Dead code");
  }

  @Override
  public int getStartLineNumber() {
    IMethod m = ((WALAMethod) method).getIR().getMethod();
    if (m instanceof AstMethod) {
      AstMethod c = (AstMethod) m;
      return c.getLineNumber(delegate.iIndex());
    }
    IBytecodeMethod method = (IBytecodeMethod) m;
    int bytecodeIndex;
    try {
      bytecodeIndex = method.getBytecodeIndex(delegate.iIndex());
      return method.getLineNumber(bytecodeIndex);
    } catch (InvalidClassFileException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
    return -1;
  }

  @Override
  public int getStartColumnNumber() {
    return -1;
  }

  @Override
  public int getEndColumnNumber() {
    return -1;
  }

  @Override
  public int getEndLineNumber() {
    return -1;
  }

  @Override
  public boolean isCatchStmt() {
    return false;
  }

  public SSAInstruction getSSAInstruction() {
    return delegate;
  }
}
