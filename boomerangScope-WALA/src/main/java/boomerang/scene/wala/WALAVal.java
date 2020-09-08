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
import boomerang.scene.Method;
import boomerang.scene.Pair;
import boomerang.scene.Type;
import boomerang.scene.Val;
import com.ibm.wala.analysis.typeInference.PointType;
import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.classLoader.NewSiteReference;
import com.ibm.wala.ipa.cha.IClassHierarchy;
import com.ibm.wala.ssa.SSAArrayReferenceInstruction;
import com.ibm.wala.ssa.SSAInstruction;
import com.ibm.wala.ssa.SSAInvokeInstruction;
import com.ibm.wala.ssa.SymbolTable;

public class WALAVal extends Val {

  private final int programCounter;
  protected final WALAMethod method;
  private final SymbolTable symbolTable;
  private final NewSiteReference newSite;
  private final SSAInstruction ssaInstruction;
  private final OP op;

  public static enum OP {
    LEFT,
    RIGHT
  }

  public WALAVal(int programCounter, WALAMethod method) {
    this(programCounter, method, null);
  }

  public WALAVal(int programCounter, WALAMethod method, Edge unbalanced) {
    super(method, unbalanced);
    this.programCounter = programCounter;
    this.method = method;
    this.symbolTable = method.getIR().getSymbolTable();
    this.newSite = null;
    this.ssaInstruction = null;
    this.op = null;
  }

  /**
   * WALAVal representing a new allocation site statement
   *
   * @param newSite
   * @param method
   */
  public WALAVal(NewSiteReference newSite, WALAMethod method, Edge unbalanced) {
    super(method, unbalanced);
    this.newSite = newSite;
    this.method = method;
    this.symbolTable = null;
    this.programCounter = -1;
    this.ssaInstruction = null;
    this.op = null;
  }

  public WALAVal(NewSiteReference newSite, WALAMethod method) {
    this(newSite, method, null);
  }

  /**
   * Dummy WALAVal to represent right hand side of any instruction for instance of a phi val or of
   * an assining call statement.
   *
   * @param delegate
   * @param method
   */
  public WALAVal(SSAInstruction delegate, OP op, WALAMethod method, Edge unbalanced) {
    super(method, unbalanced);
    this.ssaInstruction = delegate;
    this.op = op;
    this.method = method;
    this.newSite = null;
    this.symbolTable = null;
    this.programCounter = -1;
  }

  public WALAVal(SSAInstruction delegate, OP op, WALAMethod method) {
    this(delegate, op, method, null);
  }

  @Override
  public Type getType() {
    if (newSite != null) {
      return getNewExprType();
    }
    if (ssaInstruction instanceof SSAInvokeInstruction) {
      SSAInvokeInstruction ssaInvokeInstruction = (SSAInvokeInstruction) ssaInstruction;
      return new WALAType(method.getTypeInference().getType(ssaInvokeInstruction.getDef()));
    }
    return new WALAType(method.getTypeInference().getType(programCounter));
  }

  @Override
  public boolean isStatic() {
    // TODO Auto-generated method stub
    return false;
  }

  @Override
  public boolean isNewExpr() {
    return newSite != null;
  }

  @Override
  public Type getNewExprType() {
    IClassHierarchy cha = method.getIR().getMethod().getDeclaringClass().getClassHierarchy();
    IClass klass = cha.lookupClass(newSite.getDeclaredType());
    return new WALAType(new PointType(klass));
  }

  @Override
  public Val asUnbalanced(Edge stmt) {
    if (newSite != null) {
      return new WALAVal(newSite, method, stmt);
    }
    if (ssaInstruction != null) {
      return new WALAVal(ssaInstruction, op, method, stmt);
    }
    return new WALAVal(programCounter, method, stmt);
  }

  @Override
  public boolean isLocal() {
    return true;
  }

  @Override
  public boolean isArrayAllocationVal() {
    // TODO Auto-generated method stub
    return false;
  }

  @Override
  public boolean isNull() {
    return symbolTable != null
        && symbolTable.getValue(programCounter) != null
        && symbolTable.getValue(programCounter).isNullConstant();
  }

  @Override
  public boolean isStringConstant() {
    // TODO Auto-generated method stub
    return false;
  }

  @Override
  public String getStringValue() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public boolean isStringBufferOrBuilder() {
    // TODO Auto-generated method stub
    return false;
  }

  @Override
  public boolean isThrowableAllocationType() {
    // TODO Auto-generated method stub
    return false;
  }

  @Override
  public boolean isCast() {
    // TODO Auto-generated method stub
    return false;
  }

  @Override
  public Val getCastOp() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public boolean isArrayRef() {
    return ssaInstruction instanceof SSAArrayReferenceInstruction;
  }

  @Override
  public boolean isInstanceOfExpr() {
    // TODO Auto-generated method stub
    return false;
  }

  @Override
  public Val getInstanceOfOp() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public boolean isLengthExpr() {
    // TODO Auto-generated method stub
    return false;
  }

  @Override
  public Val getLengthOp() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public boolean isIntConstant() {
    if (symbolTable == null) return false;
    if (programCounter == -1) {
      return false;
    }
    return symbolTable.isIntegerConstant(programCounter);
  }

  @Override
  public boolean isClassConstant() {
    return false;
  }

  @Override
  public Type getClassConstantType() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public Val withNewMethod(Method callee) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public String toString() {
    return Integer.valueOf(programCounter).toString() + " in " + method.toString();
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = super.hashCode();
    result = prime * result + ((method == null) ? 0 : method.hashCode());
    result = prime * result + ((newSite == null) ? 0 : newSite.hashCode());
    result = prime * result + ((op == null) ? 0 : op.hashCode());
    result = prime * result + ((ssaInstruction == null) ? 0 : ssaInstruction.hashCode());
    result = prime * result + programCounter;
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) return true;
    if (!super.equals(obj)) return false;
    if (getClass() != obj.getClass()) return false;
    WALAVal other = (WALAVal) obj;
    if (method == null) {
      if (other.method != null) return false;
    } else if (!method.equals(other.method)) return false;
    if (newSite == null) {
      if (other.newSite != null) return false;
    } else if (!newSite.equals(other.newSite)) return false;
    if (op == null) {
      if (other.op != null) return false;
    } else if (!op.equals(other.op)) return false;
    if (ssaInstruction == null) {
      if (other.ssaInstruction != null) return false;
    } else if (!ssaInstruction.equals(other.ssaInstruction)) return false;
    if (programCounter != other.programCounter) return false;
    return true;
  }

  @Override
  public boolean isLongConstant() {
    // TODO Auto-generated method stub
    return false;
  }

  @Override
  public int getIntValue() {
    return symbolTable.getIntValue(programCounter);
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
