package boomerang;

import boomerang.solver.AbstractBoomerangSolver;
import wpds.impl.Weight;

public interface SolverCreationListener<W extends Weight> {

	void onCreatedSolver(AbstractBoomerangSolver<W> solver);

}
