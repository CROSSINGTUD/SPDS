/*******************************************************************************
 * Copyright (c) 2018 Fraunhofer IEM, Paderborn, Germany.
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *  
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Johannes Spaeth - initial API and implementation
 *******************************************************************************/
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
