package ideal;

import boomerang.BoomerangTimeoutException;
import boomerang.WeightedBoomerang;
import wpds.impl.Weight;

/**
 * Created by johannesspath on 01.12.17.
 */
public class IDEALSeedTimeout extends RuntimeException {
    private final IDEALSeedSolver<? extends Weight> solver;
    private WeightedBoomerang<? extends Weight> timedoutSolver;
    private final BoomerangTimeoutException boomerangTimeoutException;

    public <W extends Weight> IDEALSeedTimeout(IDEALSeedSolver<W> solver, WeightedBoomerang<W> timedoutSolver, BoomerangTimeoutException boomerangTimeoutException) {
        this.solver = solver;
        this.timedoutSolver = timedoutSolver;
        this.boomerangTimeoutException = boomerangTimeoutException;
    }

    public IDEALSeedSolver<? extends Weight> getSolver() {
        return solver;
    }

    public WeightedBoomerang<? extends Weight> getTimedoutSolver() {
        return timedoutSolver;
    }

    @Override
    public String toString() {
        return "IDEAL Seed TimeoutException \n"+boomerangTimeoutException.toString();
    }
}
