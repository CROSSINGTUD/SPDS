package test.core;

import boomerang.Query;
import boomerang.scene.Statement;
import java.util.Optional;

public interface ValueOfInterestInUnit {
  Optional<? extends Query> test(Statement unit);
}
