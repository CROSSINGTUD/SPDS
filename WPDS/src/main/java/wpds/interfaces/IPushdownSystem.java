/*******************************************************************************
 * Copyright (c) 2018 Fraunhofer IEM, Paderborn, Germany.
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *  
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Johannes Spaeth - initial API and implementation
 *******************************************************************************/
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

    public boolean addRule(Rule<N, D, W> rule);

    public Set<D> getStates();

    public Set<NormalRule<N, D, W>> getNormalRules();

    public Set<PopRule<N, D, W>> getPopRules();

    public Set<PushRule<N, D, W>> getPushRules();

    public Set<Rule<N, D, W>> getAllRules();

    public Set<Rule<N, D, W>> getRulesStarting(D start, N string);

    public Set<NormalRule<N, D, W>> getNormalRulesEnding(D start, N string);

    public Set<PushRule<N, D, W>> getPushRulesEnding(D start, N string);

    public void prestar(WeightedPAutomaton<N, D, W> initialAutomaton);

    public void poststar(WeightedPAutomaton<N, D, W> initialAutomaton);

    public void poststar(WeightedPAutomaton<N, D, W> initialAutomaton, NestedWeightedPAutomatons<N, D, W> summaries);

    public void registerUpdateListener(WPDSUpdateListener<N, D, W> listener);

}
