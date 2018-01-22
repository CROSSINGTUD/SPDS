/*******************************************************************************
 * Copyright (c) 2018 Fraunhofer IEM, Paderborn, Germany.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Johannes Spaeth - initial API and implementation
 *******************************************************************************/
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
