package test.core;

import boomerang.Query;
import soot.jimple.Stmt;

import java.util.Optional;

public interface ValueOfInterestInUnit {
    Optional<? extends Query> test(Stmt unit);
}
