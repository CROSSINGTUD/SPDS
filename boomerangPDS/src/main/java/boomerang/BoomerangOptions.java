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

import boomerang.callgraph.BoomerangResolver;
import boomerang.callgraph.ICallerCalleeResolutionStrategy.Factory;
import boomerang.callgraph.ObservableICFG;
import boomerang.scene.AllocVal;
import boomerang.scene.Method;
import boomerang.scene.Statement;
import boomerang.scene.Val;
import boomerang.stats.IBoomerangStats;
import java.util.Optional;

public interface BoomerangOptions {

  default Factory getResolutionStrategy() {
    return BoomerangResolver.FACTORY;
  }

  void checkValid();

  boolean trackImplicitFlows();

  boolean handleMaps();

  enum StaticFieldStrategy {
    FLOW_SENSITIVE,
    SINGLETON,
    IGNORE
  }

  StaticFieldStrategy getStaticFieldStrategy();

  enum ArrayStrategy {
    DISABLED,
    INDEX_SENSITIVE,
    INDEX_INSENSITIVE
  }

  ArrayStrategy getArrayStrategy();

  boolean typeCheck();

  boolean onTheFlyCallGraph();

  boolean throwFlows();

  boolean callSummaries();

  boolean fieldSummaries();

  int analysisTimeoutMS();

  // TODO remove icfg here.
  Optional<AllocVal> getAllocationVal(
      Method m, Statement stmt, Val fact, ObservableICFG<Statement, Method> icfg);

  IBoomerangStats statsFactory();

  boolean aliasing();

  /**
   * Assume we propagate an object of soot.NullType in variable y and the propagation reaches a
   * statement x = (Object) y.
   *
   * @return If set to true, the propagation will NOT continue in x. This does not match the runtime
   *     semantics. At runtime, null can be cast to any RefType! Though a check (null instanceof
   *     Object) returns false.
   */
  boolean killNullAtCast();

  boolean trackReturnOfInstanceOf();

  boolean trackStaticFieldAtEntryPointToClinit();

  boolean trackFields();

  int maxFieldDepth();

  int maxCallDepth();

  int maxUnbalancedCallDepth();

  boolean onTheFlyControlFlow();

  boolean ignoreInnerClassFields();

  boolean trackPathConditions();

  boolean prunePathConditions();

  boolean trackDataFlowPath();

  boolean allowMultipleQueries();
}
