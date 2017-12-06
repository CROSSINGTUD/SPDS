package boomerang.stats;

import boomerang.Query;
import boomerang.WeightedBoomerang;
import boomerang.solver.AbstractBoomerangSolver;
import soot.SootMethod;
import wpds.impl.Weight;

import java.util.Set;

/**
 * Created by johannesspath on 06.12.17.
 */
public interface IBoomerangStats<W extends Weight> {
    void registerSolver(Query key, AbstractBoomerangSolver<W> solver);

    void registerCallSitePOI(WeightedBoomerang<W>.ForwardCallSitePOI key);

    void registerFieldWritePOI(WeightedBoomerang<W>.FieldWritePOI key);

    void registerFieldReadPOI(WeightedBoomerang<W>.FieldReadPOI key);

    Set<SootMethod> getCallVisitedMethods();
}
