package boomerang.solver;

import boomerang.BoomerangOptions;
import boomerang.WeightedBoomerang;
import boomerang.arrays.ArrayHandlingStrategy;
import boomerang.arrays.ArrayIndexInsensitiveStrategy;
import boomerang.arrays.ArrayIndexSensitiveStrategy;
import boomerang.arrays.IgnoreArrayStrategy;
import boomerang.staticfields.FlowSensitiveStaticFieldStrategy;
import boomerang.staticfields.IgnoreStaticFieldStrategy;
import boomerang.staticfields.SingletonStaticFieldStrategy;
import boomerang.staticfields.StaticFieldStrategy;
import wpds.impl.Weight;

public class Strategies<W extends Weight> {
  private final StaticFieldStrategy<W> staticFieldStrategy;
  private final ArrayHandlingStrategy<W> arrayHandlingStrategy;

  public Strategies(BoomerangOptions opts, WeightedBoomerang<W> boomerang) {
    switch (opts.getStaticFieldStrategy()) {
      case IGNORE:
        staticFieldStrategy = new IgnoreStaticFieldStrategy();
        break;
      case SINGLETON:
        staticFieldStrategy = new SingletonStaticFieldStrategy<W>(boomerang);
        break;
      case FLOW_SENSITIVE:
      default:
        staticFieldStrategy = new FlowSensitiveStaticFieldStrategy();
        break;
    }
    switch (opts.getArrayStrategy()) {
      case DISABLED:
        arrayHandlingStrategy = new IgnoreArrayStrategy();
        break;
      case INDEX_INSENSITIVE:
        arrayHandlingStrategy = new ArrayIndexInsensitiveStrategy();
        break;
      case INDEX_SENSITIVE:
      default:
        arrayHandlingStrategy = new ArrayIndexSensitiveStrategy();
        break;
    }
  }

  public StaticFieldStrategy<W> getStaticFieldStrategy() {
    return staticFieldStrategy;
  }

  public ArrayHandlingStrategy<W> getArrayHandlingStrategy() {
    return arrayHandlingStrategy;
  }
}
