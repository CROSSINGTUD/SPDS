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
package wpds.wildcard;

import java.util.HashSet;
import java.util.Set;
import wpds.impl.NormalRule;
import wpds.impl.PopRule;
import wpds.impl.PushRule;
import wpds.impl.PushdownSystem;
import wpds.impl.Rule;
import wpds.impl.UNormalRule;
import wpds.impl.UPopRule;
import wpds.impl.UPushRule;
import wpds.impl.Weight.NoWeight;
import wpds.interfaces.Location;
import wpds.interfaces.State;

public abstract class WildcardPushdownSystem<N extends Location, D extends State>
    extends PushdownSystem<N, D> {

  @Override
  public Set<Rule<N, D, NoWeight>> getRulesStarting(D start, N string) {
    assert !string.equals(anyTransition());
    Set<Rule<N, D, NoWeight>> allRules = getAllRules();
    Set<Rule<N, D, NoWeight>> result = new HashSet<>();
    for (Rule<N, D, NoWeight> r : allRules) {
      if (r.getS1().equals(start) && r.getL1().equals(string)) result.add(r);
      if (anyTransition() != null && r.getS1().equals(start) && r.getL1().equals(anyTransition())) {
        if (r instanceof NormalRule) {
          result.add(new UNormalRule<N, D>(r.getS1(), string, r.getS2(), string));
        } else if (r instanceof PopRule) {
          result.add(new UPopRule<N, D>(r.getS1(), string, r.getS2()));
        } else if (r instanceof PushRule) {
          result.add(new UPushRule<N, D>(r.getS1(), string, r.getS2(), r.getL2(), string));
        }
      }
    }
    return result;
  }

  @Override
  public Set<NormalRule<N, D, NoWeight>> getNormalRulesEnding(D start, N string) {
    assert !string.equals(anyTransition());
    Set<NormalRule<N, D, NoWeight>> allRules = getNormalRules();
    Set<NormalRule<N, D, NoWeight>> result = new HashSet<>();
    for (NormalRule<N, D, NoWeight> r : allRules) {
      if (r.getS2().equals(start) && r.getL2().equals(string)) result.add(r);
      if (r.getS2().equals(start) && r.getL2().equals(anyTransition())) {
        result.add(new UNormalRule<N, D>(r.getS1(), string, r.getS2(), string));
      }
    }
    return result;
  }

  @Override
  public Set<PushRule<N, D, NoWeight>> getPushRulesEnding(D start, N string) {
    assert !string.equals(anyTransition());
    Set<PushRule<N, D, NoWeight>> allRules = getPushRules();
    Set<PushRule<N, D, NoWeight>> result = new HashSet<>();
    for (PushRule<N, D, NoWeight> r : allRules) {
      if (r.getS2().equals(start) && r.getL2().equals(string)) result.add(r);
    }
    return result;
  }

  public abstract Wildcard anyTransition();
}
