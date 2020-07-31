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

import boomerang.scene.ControlFlowGraph;
import boomerang.scene.Method;
import boomerang.scene.Statement;
import boomerang.scene.Val;
import boomerang.scene.WrappedClass;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.ibm.wala.analysis.typeInference.TypeInference;
import com.ibm.wala.cast.ir.ssa.AstIRFactory.AstIR;
import com.ibm.wala.cast.java.analysis.typeInference.AstJavaTypeInference;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.ipa.cha.IClassHierarchy;
import com.ibm.wala.ssa.IR;
import java.util.List;
import java.util.Set;

public class WALAMethod extends Method {

  private IMethod delegate;
  private WALAControlFlowGraph cfg;
  private IR ir;
  private List<Val> paramLocalCache;
  private Set<Val> valueCache;
  private TypeInference typeInference;
  private IClassHierarchy cha;

  public WALAMethod(IMethod delegate, IR ir, IClassHierarchy cha) {
    this.delegate = delegate;
    this.ir = ir;
    this.cha = cha;
    if (ir instanceof AstIR) {
      this.typeInference = new AstJavaTypeInference(ir, true);
    } else {
      this.typeInference = TypeInference.make(ir, true);
    }
    this.cfg = new WALAControlFlowGraph(this, cha);
  }

  @Override
  public boolean isStaticInitializer() {
    return delegate.isClinit();
  }

  @Override
  public boolean isParameterLocal(Val val) {
    return getParameterLocals().contains(val);
  }

  @Override
  public boolean isThisLocal(Val val) {
    return getThisLocal().equals(val);
  }

  @Override
  public Set<Val> getLocals() {
    if (valueCache == null) {
      valueCache = Sets.newHashSet();
      for (int i = 0; i <= ir.getSymbolTable().getMaxValueNumber(); i++) {
        valueCache.add(new WALAVal(i, this));
      }
    }
    return valueCache;
  }

  @Override
  public Val getThisLocal() {
    return new WALAVal(ir.getSymbolTable().getParameter(0), this);
  }

  @Override
  public List<Val> getParameterLocals() {
    if (paramLocalCache == null) {
      paramLocalCache = Lists.newArrayList();
      for (int i = (isStatic() ? 0 : 1); i < ir.getSymbolTable().getNumberOfParameters(); i++) {
        paramLocalCache.add(new WALAVal(ir.getParameter(i), this));
      }
    }
    return paramLocalCache;
  }

  @Override
  public boolean isStatic() {
    return delegate.isStatic();
  }

  @Override
  public boolean isNative() {
    return delegate.isNative();
  }

  @Override
  public List<Statement> getStatements() {
    return getControlFlowGraph().getStatements();
  }

  @Override
  public WrappedClass getDeclaringClass() {
    return new WALAClass(delegate.getDeclaringClass().getReference());
  }

  @Override
  public ControlFlowGraph getControlFlowGraph() {
    return cfg;
  }

  @Override
  public String getSubSignature() {
    return delegate.getSelector().toString();
  }

  @Override
  public String getName() {
    return delegate.getName().toString();
  }

  public IMethod getDelegate() {
    return delegate;
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
    if (this == obj) return true;
    if (obj == null) return false;
    if (getClass() != obj.getClass()) return false;
    WALAMethod other = (WALAMethod) obj;
    if (delegate == null) {
      if (other.delegate != null) return false;
    } else if (!delegate.equals(other.delegate)) return false;
    return true;
  }

  IR getIR() {
    return ir;
  }

  @Override
  public String toString() {
    return delegate.getSignature();
  }

  public TypeInference getTypeInference() {
    return typeInference;
  }

  @Override
  public boolean isConstructor() {
    return delegate.isInit();
  }

  @Override
  public boolean isPublic() {
    return delegate.isPublic();
  }

  public Statement getBranchTarget(int target) {
    return cfg.getBranchTarget(target);
  }
}
