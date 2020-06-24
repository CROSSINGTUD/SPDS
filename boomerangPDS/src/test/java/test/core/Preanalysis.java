package test.core;

import boomerang.Query;
import boomerang.scene.AnalysisScope;
import boomerang.scene.CallGraph;
import boomerang.scene.Statement;
import java.util.Collection;
import java.util.Collections;

public class Preanalysis extends AnalysisScope {

  private ValueOfInterestInUnit f;

  public Preanalysis(CallGraph cg, ValueOfInterestInUnit f) {
    super(cg);
    this.f = f;
  }

  @Override
  protected Collection<? extends Query> generate(Statement seed) {
    if (f.test(seed).isPresent()) {
      return Collections.singleton(f.test(seed).get());
    }
    return Collections.emptySet();
  }
}
