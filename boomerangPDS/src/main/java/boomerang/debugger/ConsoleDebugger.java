package boomerang.debugger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Joiner;

import boomerang.Query;
import soot.SootMethod;
import wpds.impl.Weight;

public class ConsoleDebugger<W extends Weight> extends Debugger<W> {
	
	private static final Logger logger = LoggerFactory.getLogger(ConsoleDebugger.class);

    public void done(java.util.Map<boomerang.Query, boomerang.solver.AbstractBoomerangSolver<W>> queryToSolvers) {
        int totalRules = 0;
        for (Query q : queryToSolvers.keySet()) {
            totalRules += queryToSolvers.get(q).getNumberOfRules();
        }
        logger.debug("Total number of rules: " + totalRules);
        for (Query q : queryToSolvers.keySet()) {
            logger.debug("========================");
            logger.debug(q.toString());
            logger.debug("========================");
            queryToSolvers.get(q).debugOutput();
            for (SootMethod m : queryToSolvers.get(q).getReachableMethods()) {
                logger.debug(m + "\n" + Joiner.on("\n\t").join(queryToSolvers.get(q).getResults(m).cellSet()));
            }
            queryToSolvers.get(q).debugOutput();
        }
    };
}
