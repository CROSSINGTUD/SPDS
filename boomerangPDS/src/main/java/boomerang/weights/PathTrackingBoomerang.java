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
package boomerang.weights;

import boomerang.BoomerangOptions;
import boomerang.ForwardQuery;
import boomerang.WeightedBoomerang;
import boomerang.scene.CallGraph;
import boomerang.scene.DataFlowScope;
import boomerang.scene.Field;
import boomerang.scene.Statement;
import boomerang.scene.Val;
import sync.pds.solver.OneWeightFunctions;
import sync.pds.solver.WeightFunctions;

public abstract class PathTrackingBoomerang extends WeightedBoomerang<DataFlowPathWeight> {

  private OneWeightFunctions<Statement, Val, Field, DataFlowPathWeight> fieldWeights;
  private PathTrackingWeightFunctions callWeights;

  public PathTrackingBoomerang(CallGraph cg, DataFlowScope scope) {
    super(cg, scope);
  }

  public PathTrackingBoomerang(CallGraph cg, DataFlowScope scope, BoomerangOptions opt) {
    super(cg, scope, opt);
  }

  @Override
  protected WeightFunctions<Statement, Val, Field, DataFlowPathWeight> getForwardFieldWeights() {
    return getOrCreateFieldWeights();
  }

  @Override
  protected WeightFunctions<Statement, Val, Field, DataFlowPathWeight> getBackwardFieldWeights() {
    return getOrCreateFieldWeights();
  }

  @Override
  protected WeightFunctions<Statement, Val, Statement, DataFlowPathWeight>
      getBackwardCallWeights() {
    return getOrCreateCallWeights();
  }

  @Override
  protected WeightFunctions<Statement, Val, Statement, DataFlowPathWeight> getForwardCallWeights(
      ForwardQuery sourceQuery) {
    return getOrCreateCallWeights();
  }

  private WeightFunctions<Statement, Val, Field, DataFlowPathWeight> getOrCreateFieldWeights() {
    if (fieldWeights == null) {
      fieldWeights = new OneWeightFunctions<>(DataFlowPathWeight.one());
    }
    return fieldWeights;
  }

  private WeightFunctions<Statement, Val, Statement, DataFlowPathWeight> getOrCreateCallWeights() {
    if (callWeights == null) {
      callWeights =
          new PathTrackingWeightFunctions(
              options.trackDataFlowPath(),
              options.trackPathConditions(),
              options.trackImplicitFlows());
    }
    return callWeights;
  }
}
