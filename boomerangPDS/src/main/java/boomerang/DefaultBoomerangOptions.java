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
package boomerang;

import boomerang.callgraph.ObservableICFG;
import boomerang.scene.AllocVal;
import boomerang.scene.Method;
import boomerang.scene.Statement;
import boomerang.scene.Val;
import boomerang.stats.IBoomerangStats;
import boomerang.stats.SimpleBoomerangStats;
import com.google.common.base.Joiner;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class DefaultBoomerangOptions implements BoomerangOptions {

  public boolean isAllocationVal(Val val) {
    if (!trackStrings() && val.isStringBufferOrBuilder()) {
      return false;
    }
    if (trackNullAssignments() && val.isNull()) {
      return true;
    }
    if (getArrayStrategy() != ArrayStrategy.DISABLED && val.isArrayAllocationVal()) {
      return true;
    }
    if (trackStrings() && val.isStringConstant()) {
      return true;
    }
    if (!trackAnySubclassOfThrowable() && val.isThrowableAllocationType()) {
      return false;
    }

    return val.isNewExpr();
  }

  @Override
  public StaticFieldStrategy getStaticFieldStrategy() {
    return StaticFieldStrategy.SINGLETON;
  }

  @Override
  public ArrayStrategy getArrayStrategy() {
    return ArrayStrategy.INDEX_SENSITIVE;
  }

  @Override
  public boolean typeCheck() {
    return true;
  }

  @Override
  public boolean trackReturnOfInstanceOf() {
    return false;
  }

  @Override
  public boolean onTheFlyCallGraph() {
    return false;
  }

  @Override
  public boolean throwFlows() {
    return false;
  }

  @Override
  public boolean callSummaries() {
    return false;
  }

  @Override
  public boolean fieldSummaries() {
    return false;
  }

  public boolean trackAnySubclassOfThrowable() {
    return false;
  }

  public boolean trackStrings() {
    return true;
  }

  public boolean trackNullAssignments() {
    return true;
  }

  @Override
  public Optional<AllocVal> getAllocationVal(
      Method m, Statement stmt, Val fact, ObservableICFG<Statement, Method> icfg) {
    if (!stmt.isAssign()) {
      return Optional.empty();
    }
    if (!stmt.getLeftOp().equals(fact)) {
      return Optional.empty();
    }
    if (isAllocationVal(stmt.getRightOp())) {
      return Optional.of(new AllocVal(stmt.getLeftOp(), stmt, stmt.getRightOp()));
    }
    return Optional.empty();
  }

  @Override
  public int analysisTimeoutMS() {
    return 10000;
  }

  @Override
  public IBoomerangStats statsFactory() {
    return new SimpleBoomerangStats();
  }

  @Override
  public boolean aliasing() {
    return true;
  }

  @Override
  public boolean killNullAtCast() {
    return false;
  }

  @Override
  public boolean trackStaticFieldAtEntryPointToClinit() {
    return false;
  }

  @Override
  public boolean trackFields() {
    return true;
  }

  @Override
  public int maxCallDepth() {
    return -1;
  }

  @Override
  public int maxUnbalancedCallDepth() {
    return -1;
  }

  @Override
  public int maxFieldDepth() {
    return -1;
  }

  @Override
  public boolean onTheFlyControlFlow() {
    return false;
  }

  @Override
  public String toString() {
    Class<? extends DefaultBoomerangOptions> cls = this.getClass();
    List<String> methodToVal = new ArrayList<>();
    String s = cls.getName();
    for (java.lang.reflect.Method m : cls.getMethods()) {
      String name = m.getName();
      if (name.contains("toString")) continue;

      if (m.getParameterCount() == 0) {
        try {
          Object val = m.invoke(this);
          methodToVal.add(name + "=" + val);
        } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
        }
      }
    }
    String joined = Joiner.on(",").join(methodToVal);
    return "[" + s + "{" + joined + "}]";
  }

  @Override
  public boolean ignoreInnerClassFields() {
    return false;
  }

  @Override
  public boolean trackPathConditions() {
    return false;
  }

  @Override
  public boolean prunePathConditions() {
    return false;
  }

  @Override
  public boolean trackDataFlowPath() {
    return true;
  }

  @Override
  public boolean trackImplicitFlows() {
    return false;
  }

  @Override
  public boolean allowMultipleQueries() {
    return false;
  }

  public void checkValid() {
    if (trackPathConditions() == false && prunePathConditions()) {
      throw new RuntimeException(
          "InvalidCombinations of Options, Path Conditions must be ables when pruning path conditions");
    }
  }

  @Override
  public boolean handleMaps() {
    return true;
  }
}
