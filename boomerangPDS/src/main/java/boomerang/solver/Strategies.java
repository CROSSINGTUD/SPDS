package boomerang.solver;

import boomerang.BoomerangOptions;
import boomerang.WeightedBoomerang;
import boomerang.staticfields.FlowSensitiveStaticFieldStrategy;
import boomerang.staticfields.IgnoreStaticFieldStrategy;
import boomerang.staticfields.SingletonStaticFieldStrategy;
import boomerang.staticfields.StaticFieldStrategy;
import wpds.impl.Weight;

public class Strategies<W extends Weight> {
  private final StaticFieldStrategy<W> staticFieldStrategy;

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
  }

  public StaticFieldStrategy<W> getStaticFieldStrategy() {
    return staticFieldStrategy;
  }
}
