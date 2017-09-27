package wpds.impl;

import wpds.impl.Weight.NoWeight;
import wpds.interfaces.Location;
import wpds.interfaces.State;

public class PushdownSystem<N extends Location, D extends State>
    extends WeightedPushdownSystem<N, D, NoWeight> {

  @Override
  public boolean addRule(Rule<N, D, NoWeight> rule) {
    if (!(rule instanceof UNormalRule) && !(rule instanceof UPopRule)
        && !(rule instanceof UPushRule))
      throw new RuntimeException("Trying to add a weighted rule to an unweighted PDS!");
    return super.addRule(rule);
  }

}
