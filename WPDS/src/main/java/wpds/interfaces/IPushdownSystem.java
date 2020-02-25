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
package wpds.interfaces;

import java.util.Set;
import wpds.impl.NestedWeightedPAutomatons;
import wpds.impl.NormalRule;
import wpds.impl.PopRule;
import wpds.impl.PushRule;
import wpds.impl.Rule;
import wpds.impl.Weight;
import wpds.impl.WeightedPAutomaton;

public interface IPushdownSystem<N extends Location, D extends State, W extends Weight> {

  boolean addRule(Rule<N, D, W> rule);

  Set<D> getStates();

  Set<NormalRule<N, D, W>> getNormalRules();

  Set<PopRule<N, D, W>> getPopRules();

  Set<PushRule<N, D, W>> getPushRules();

  Set<Rule<N, D, W>> getAllRules();

  Set<Rule<N, D, W>> getRulesStarting(D start, N string);

  Set<NormalRule<N, D, W>> getNormalRulesEnding(D start, N string);

  Set<PushRule<N, D, W>> getPushRulesEnding(D start, N string);

  void prestar(WeightedPAutomaton<N, D, W> initialAutomaton);

  void poststar(WeightedPAutomaton<N, D, W> initialAutomaton);

  void poststar(
      WeightedPAutomaton<N, D, W> initialAutomaton, NestedWeightedPAutomatons<N, D, W> summaries);

  void registerUpdateListener(WPDSUpdateListener<N, D, W> listener);

  void unregisterAllListeners();
}
