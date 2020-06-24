package ideal;

import boomerang.WeightedForwardQuery;
import boomerang.results.ForwardBoomerangResults;
import com.google.common.collect.Maps;
import java.util.Map;
import wpds.impl.Weight;

public class StoreIDEALResultHandler<W extends Weight> extends IDEALResultHandler<W> {
  Map<WeightedForwardQuery<W>, ForwardBoomerangResults<W>> seedToSolver = Maps.newHashMap();

  @Override
  public void report(WeightedForwardQuery<W> seed, ForwardBoomerangResults<W> res) {
    seedToSolver.put(seed, res);
  }

  public Map<WeightedForwardQuery<W>, ForwardBoomerangResults<W>> getResults() {
    return seedToSolver;
  }
}
