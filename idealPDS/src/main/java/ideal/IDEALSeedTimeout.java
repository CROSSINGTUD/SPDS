package ideal;

import boomerang.BoomerangTimeoutException;
import boomerang.WeightedBoomerang;
import wpds.impl.Weight;

/**
 * Created by johannesspath on 01.12.17.
 */
public class IDEALSeedTimeout extends RuntimeException {
    private final WeightedBoomerang<? extends Weight> solver;
    private final BoomerangTimeoutException boomerangTimeoutException;

    public <W extends Weight> IDEALSeedTimeout(WeightedBoomerang<W> solver, BoomerangTimeoutException boomerangTimeoutException) {
        this.solver = solver;
        this.boomerangTimeoutException = boomerangTimeoutException;
    }

    public WeightedBoomerang<? extends Weight> getSolver() {
        return solver;
    }

    @Override
    public String toString() {
        return "IDEAL Seed TimeoutException \n"+boomerangTimeoutException.toString();
    }
}
