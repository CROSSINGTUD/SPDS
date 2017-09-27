package ideal;

import boomerang.ForwardQuery;
import boomerang.solver.AbstractBoomerangSolver;
import wpds.impl.Weight;

public interface ResultReporter<W extends Weight> {
	public void onSeedFinished(ForwardQuery seed, AbstractBoomerangSolver<W> seedSolver);
}
