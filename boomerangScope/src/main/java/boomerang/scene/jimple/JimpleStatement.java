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

import boomerang.scene.CallSiteStatement;
import boomerang.scene.Field;
import boomerang.scene.IfStatement;
import boomerang.scene.InvokeExpr;
import boomerang.scene.Method;
import boomerang.scene.Pair;
import boomerang.scene.ReturnSiteStatement;
import boomerang.scene.Statement;
import boomerang.scene.StaticFieldVal;
import boomerang.scene.Val;
import com.google.common.base.Joiner;
import java.util.Collection;
import soot.jimple.ArrayRef;
import soot.jimple.AssignStmt;
import soot.jimple.CastExpr;
import soot.jimple.CaughtExceptionRef;
import soot.jimple.IdentityStmt;
import soot.jimple.IfStmt;
import soot.jimple.InstanceFieldRef;
import soot.jimple.InstanceInvokeExpr;
import soot.jimple.NewExpr;
import soot.jimple.NewMultiArrayExpr;
import soot.jimple.ReturnStmt;
import soot.jimple.StaticFieldRef;
import soot.jimple.Stmt;
import soot.jimple.StringConstant;
import soot.jimple.ThrowStmt;
import soot.tagkit.SourceLnPosTag;

public class JimpleStatement extends Statement {

  // Wrapper for stmt so we know the method
  private final Stmt delegate;
  private final Method method;

  private JimpleStatement(Stmt delegate, Method m) {
    super(m);
    if (delegate == null) {
      throw new RuntimeException("Invalid, parameter may not be null");
    }
    this.delegate = delegate;
    this.method = m;
  }

  public static Statement[] create(Stmt delegate, Method m) {
    JimpleStatement jimpleStatement = new JimpleStatement(delegate, m);
    if (delegate.containsInvokeExpr()) {
      return new Statement[] {
        new CallSiteStatement(jimpleStatement), new ReturnSiteStatement(jimpleStatement)
      };
    } else {
      return new Statement[] {jimpleStatement};
    }
  }

  @Override
  public String toString() {
    return shortName(delegate);
  }

  private String shortName(Stmt s) {
    if (s.containsInvokeExpr()) {
      String base = "";
      if (s.getInvokeExpr() instanceof InstanceInvokeExpr) {
        InstanceInvokeExpr iie = (InstanceInvokeExpr) s.getInvokeExpr();
        base = iie.getBase().toString() + ".";
      }
      String assign = "";
      if (s instanceof AssignStmt) {
        assign = ((AssignStmt) s).getLeftOp() + " = ";
      }
      return assign
          + base
          + s.getInvokeExpr().getMethod().getName()
          + "("
          + Joiner.on(",").join(s.getInvokeExpr().getArgs())
          + ")";
    }
    if (s instanceof IdentityStmt) {
      return s.toString();
    }
    if (s instanceof AssignStmt) {
      AssignStmt assignStmt = (AssignStmt) s;
      if (assignStmt.getLeftOp() instanceof InstanceFieldRef) {
        InstanceFieldRef ifr = (InstanceFieldRef) assignStmt.getLeftOp();
        return ifr.getBase() + "." + ifr.getField().getName() + " = " + assignStmt.getRightOp();
      }
      if (assignStmt.getRightOp() instanceof InstanceFieldRef) {
        InstanceFieldRef ifr = (InstanceFieldRef) assignStmt.getRightOp();
        return assignStmt.getLeftOp() + " = " + ifr.getBase() + "." + ifr.getField().getName();
      }
      if (assignStmt.getRightOp() instanceof NewExpr) {
        NewExpr newExpr = (NewExpr) assignStmt.getRightOp();
        return assignStmt.getLeftOp()
            + " = new "
            + newExpr.getBaseType().getSootClass().getShortName();
      }
    }
    return s.toString();
  }

  public boolean containsStaticFieldAccess() {
    if (delegate instanceof AssignStmt) {
      AssignStmt assignStmt = (AssignStmt) delegate;
      return assignStmt.getLeftOp() instanceof StaticFieldRef
          || assignStmt.getRightOp() instanceof StaticFieldRef;
    }
    return false;
  }

  public boolean containsInvokeExpr() {
    return delegate.containsInvokeExpr();
  }

  public Field getWrittenField() {
    AssignStmt as = (AssignStmt) delegate;
    if (as.getLeftOp() instanceof StaticFieldRef) {
      StaticFieldRef staticFieldRef = (StaticFieldRef) as.getLeftOp();
      return new JimpleField(staticFieldRef.getField());
    }

    if (as.getLeftOp() instanceof ArrayRef) {
      return Field.array(getArrayBase().getY());
    }
    InstanceFieldRef ifr = (InstanceFieldRef) as.getLeftOp();
    return new JimpleField(ifr.getField());
  }

  public boolean isFieldWriteWithBase(Val base) {
    if (isAssign() && isFieldStore()) {
      Pair<Val, Field> instanceFieldRef = getFieldStore();
      return instanceFieldRef.getX().equals(base);
    }
    if (isAssign() && isArrayStore()) {
      Pair<Val, Integer> arrayBase = getArrayBase();
      return arrayBase.getX().equals(base);
    }
    return false;
  }

  public Field getLoadedField() {
    AssignStmt as = (AssignStmt) delegate;
    InstanceFieldRef ifr = (InstanceFieldRef) as.getRightOp();
    return new JimpleField(ifr.getField());
  }

  public boolean isFieldLoadWithBase(Val base) {
    if (isAssign() && isFieldLoad()) {
      return getFieldLoad().getX().equals(base);
    }
    return false;
  }

  @Override
  public boolean isAssign() {
    return delegate instanceof AssignStmt;
  }

  public Val getLeftOp() {
    assert isAssign();
    AssignStmt assignStmt = (AssignStmt) delegate;
    return new JimpleVal(assignStmt.getLeftOp(), method);
  }

  public Val getRightOp() {
    assert isAssign();
    AssignStmt assignStmt = (AssignStmt) delegate;
    return new JimpleVal(assignStmt.getRightOp(), method);
  }

  public boolean isInstanceOfStatement(Val fact) {
    if (isAssign()) {
      if (getRightOp().isInstanceOfExpr()) {
        Val instanceOfOp = getRightOp().getInstanceOfOp();
        return instanceOfOp.equals(fact);
      }
    }
    return false;
  }

  public boolean isCast() {
    return delegate instanceof AssignStmt
        && ((AssignStmt) delegate).getRightOp() instanceof CastExpr;
  }

  public InvokeExpr getInvokeExpr() {
    return new JimpleInvokeExpr(delegate.getInvokeExpr(), method);
  }

  public boolean isReturnStmt() {
    return delegate instanceof ReturnStmt;
  }

  public boolean isThrowStmt() {
    return delegate instanceof ThrowStmt;
  }

  public boolean isIfStmt() {
    return delegate instanceof IfStmt;
  }

  public IfStatement getIfStmt() {
    return new JimpleIfStatement((IfStmt) delegate, method);
  }

  // TODO Rename to getReturnOp();
  public Val getReturnOp() {
    assert isReturnStmt();
    ReturnStmt assignStmt = (ReturnStmt) delegate;
    return new JimpleVal(assignStmt.getOp(), method);
  }

  public boolean isMultiArrayAllocation() {
    return (delegate instanceof AssignStmt)
        && ((AssignStmt) delegate).getRightOp() instanceof NewMultiArrayExpr;
  }

  public boolean isStringAllocation() {
    return delegate instanceof AssignStmt
        && ((AssignStmt) delegate).getRightOp() instanceof StringConstant;
  }

  public boolean isFieldStore() {
    return delegate instanceof AssignStmt
        && ((AssignStmt) delegate).getLeftOp() instanceof InstanceFieldRef;
  }

  public boolean isArrayStore() {
    return delegate instanceof AssignStmt
        && ((AssignStmt) delegate).getLeftOp() instanceof ArrayRef;
  }

  public boolean isArrayLoad() {
    return delegate instanceof AssignStmt
        && ((AssignStmt) delegate).getRightOp() instanceof ArrayRef;
  }

  public boolean isFieldLoad() {
    return delegate instanceof AssignStmt
        && ((AssignStmt) delegate).getRightOp() instanceof InstanceFieldRef;
  }

  public boolean isIdentityStmt() {
    return delegate instanceof IdentityStmt;
  }

  public Stmt getDelegate() {
    return delegate;
  }

  public String getShortLabel() {
    if (delegate instanceof AssignStmt) {
      AssignStmt assignStmt = (AssignStmt) delegate;
      if (assignStmt.getRightOp() instanceof InstanceFieldRef) {
        InstanceFieldRef fr = (InstanceFieldRef) assignStmt.getRightOp();
        return assignStmt.getLeftOp() + " = " + fr.getBase() + "." + fr.getField().getName();
      }
      if (assignStmt.getLeftOp() instanceof InstanceFieldRef) {
        InstanceFieldRef fr = (InstanceFieldRef) assignStmt.getLeftOp();
        return fr.getBase() + "." + fr.getField().getName() + " = " + assignStmt.getRightOp();
      }
    }
    if (containsInvokeExpr()) {
      InvokeExpr invokeExpr = getInvokeExpr();
      if (invokeExpr.isStaticInvokeExpr()) {
        return (isAssign() ? getLeftOp() + " = " : "")
            + invokeExpr.getMethod()
            + "("
            + invokeExpr.getArgs().toString().replace("[", "").replace("]", "")
            + ")";
      }
      if (invokeExpr.isInstanceInvokeExpr()) {
        return (isAssign() ? getLeftOp() + " = " : "")
            + invokeExpr.getBase()
            + "."
            + invokeExpr.getMethod()
            + "("
            + invokeExpr.getArgs().toString().replace("[", "").replace("]", "")
            + ")";
      }
    }
    return delegate.toString();
  }

  /**
   * This method kills a data-flow at an if-stmt, it is assumed that the propagated "allocation"
   * site is x = null and fact is the propagated aliased variable. (i.e., y after a statement y =
   * x). If the if-stmt checks for if y != null or if y == null, data-flow propagation can be killed
   * when along the true/false branch.
   *
   * @param fact The data-flow value that bypasses the if-stmt
   * @param successor The successor statement of the if-stmt
   * @return true if the Val fact shall be killed
   */
  @Deprecated
  public boolean killAtIfStmt(Val fact, Statement successor) {
    //		IfStmt ifStmt = this.getIfStmt();
    //		if(successor instanceof CallSiteStatement) {
    //          successor = ((CallSiteStatement) successor).getDelegate();
    //		} else if(successor instanceof ReturnSiteStatement) {
    //          successor = ((ReturnSiteStatement) successor).getDelegate();
    //        }
    //		Stmt succ = ((JimpleStatement)successor).getDelegate();
    //		Stmt target = ifStmt.getTarget();
    //
    //		Value condition = ifStmt.getCondition();
    //		if (condition instanceof JEqExpr) {
    //			JEqExpr eqExpr = (JEqExpr) condition;
    //			Value op1 = eqExpr.getOp1();
    //			Value op2 = eqExpr.getOp2();
    //			Val jop1 = new JimpleVal(eqExpr.getOp1(), successor.getMethod());
    //			Val jop2 = new JimpleVal(eqExpr.getOp2(), successor.getMethod());
    //			if (fact instanceof JimpleDoubleVal) {
    //				JimpleDoubleVal valWithFalseVar = (JimpleDoubleVal) fact;
    //				if (jop1.equals(valWithFalseVar.getFalseVariable())) {
    //					if (op2.equals(IntConstant.v(0))) {
    //						if (!succ.equals(target)) {
    //							return true;
    //						}
    //					}
    //				}
    //				if (jop2.equals(valWithFalseVar.getFalseVariable())) {
    //					if (op1.equals(IntConstant.v(0))) {
    //						if (!succ.equals(target)) {
    //							return true;
    //						}
    //					}
    //				}
    //			}
    //			if (op1 instanceof NullConstant) {
    //				if (new JimpleVal(op2,successor.getMethod()).equals(fact)) {
    //					if (!succ.equals(target)) {
    //						return true;
    //					}
    //				}
    //			} else if (op2 instanceof NullConstant) {
    //				if (new JimpleVal(op1,successor.getMethod()).equals(fact)) {
    //					if (!succ.equals(target)) {
    //						return true;
    //					}
    //				}
    //			}
    //		}
    //		if (condition instanceof JNeExpr) {
    //			JNeExpr eqExpr = (JNeExpr) condition;
    //			Value op1 = eqExpr.getOp1();
    //			Value op2 = eqExpr.getOp2();
    //			if (op1 instanceof NullConstant) {
    //				if (new JimpleVal(op2,successor.getMethod()).equals(fact)) {
    //					if (succ.equals(target)) {
    //						return true;
    //					}
    //				}
    //			} else if (op2 instanceof NullConstant) {
    //				if (new JimpleVal(op1,successor.getMethod()).equals(fact)) {
    //					if (succ.equals(target)) {
    //						return true;
    //					}
    //				}
    //			}
    //		}
    return false;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((delegate == null) ? 0 : delegate.hashCode());
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null) {
      return false;
    }
    if (getClass() != obj.getClass()) {
      return false;
    }
    JimpleStatement other = (JimpleStatement) obj;
    if (delegate == null) {
      return other.delegate == null;
    } else {
      return delegate.equals(other.delegate);
    }
  }

  @Override
  public Pair<Val, Field> getFieldStore() {
    AssignStmt ins = (AssignStmt) delegate;
    soot.jimple.InstanceFieldRef val = (soot.jimple.InstanceFieldRef) ins.getLeftOp();
    return new Pair<Val, Field>(
        new JimpleVal(val.getBase(), method), new JimpleField(val.getField()));
  }

  @Override
  public Pair<Val, Field> getFieldLoad() {
    AssignStmt ins = (AssignStmt) delegate;
    soot.jimple.InstanceFieldRef val = (soot.jimple.InstanceFieldRef) ins.getRightOp();
    return new Pair<Val, Field>(
        new JimpleVal(val.getBase(), method), new JimpleField(val.getField()));
  }

  @Override
  public boolean isStaticFieldLoad() {
    return delegate instanceof AssignStmt
        && ((AssignStmt) delegate).getRightOp() instanceof StaticFieldRef;
  }

  @Override
  public boolean isStaticFieldStore() {
    return delegate instanceof AssignStmt
        && ((AssignStmt) delegate).getLeftOp() instanceof StaticFieldRef;
  }

  @Override
  public StaticFieldVal getStaticField() {
    StaticFieldRef v;
    if (isStaticFieldLoad()) {
      v = (StaticFieldRef) ((AssignStmt) delegate).getRightOp();
    } else if (isStaticFieldStore()) {
      v = (StaticFieldRef) ((AssignStmt) delegate).getLeftOp();
    } else {
      throw new RuntimeException("Error");
    }
    return new JimpleStaticFieldVal(new JimpleField(v.getField()), method);
  }

  @Override
  public boolean isPhiStatement() {
    return false;
  }

  @Override
  public Collection<Val> getPhiVals() {
    throw new RuntimeException("Not supported!");
  }

  @Override
  public Pair<Val, Integer> getArrayBase() {
    if (isArrayLoad()) {
      Val rightOp = getRightOp();
      return rightOp.getArrayBase();
    }
    if (isArrayStore()) {
      Val rightOp = getLeftOp();
      return rightOp.getArrayBase();
    }
    throw new RuntimeException("Dead code");
  }

  @Override
  public int getStartLineNumber() {
    return delegate.getJavaSourceStartLineNumber();
  }

  @Override
  public int getStartColumnNumber() {
    return delegate.getJavaSourceStartColumnNumber();
  }

  @Override
  public int getEndColumnNumber() {
    // TODO move to Soot
    SourceLnPosTag tag = (SourceLnPosTag) delegate.getTag("SourceLnPosTag");
    if (tag != null) {
      return tag.endPos();
    }
    return -1;
  }

  @Override
  public int getEndLineNumber() {
    // TODO move to Soot
    SourceLnPosTag tag = (SourceLnPosTag) delegate.getTag("SourceLnPosTag");
    if (tag != null) {
      return tag.endLn();
    }
    return -1;
  }

  @Override
  public boolean isCatchStmt() {
    return delegate instanceof IdentityStmt
        && ((IdentityStmt) delegate).getRightOp() instanceof CaughtExceptionRef;
  }

  public boolean isUnitializedFieldStatement() {
    return delegate.hasTag(BoomerangPretransformer.UNITIALIZED_FIELD_TAG_NAME);
  }
}
