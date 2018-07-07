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

import java.util.Collection;
import java.util.Set;

import boomerang.BackwardQuery;
import boomerang.ForwardQuery;
import boomerang.Query;
import boomerang.WeightedBoomerang;
import boomerang.jimple.Statement;
import boomerang.jimple.Val;
import boomerang.results.BackwardBoomerangResults;
import boomerang.results.ForwardBoomerangResults;
import boomerang.solver.AbstractBoomerangSolver;
import soot.SootMethod;
import sync.pds.solver.nodes.Node;
import wpds.impl.Weight;

/**
 * Created by johannesspath on 06.12.17.
 */
public interface IBoomerangStats<W extends Weight> {
    void registerSolver(Query key, AbstractBoomerangSolver<W> solver);

    void registerCallSitePOI(WeightedBoomerang<W>.ForwardCallSitePOI key);

    void registerFieldWritePOI(WeightedBoomerang<W>.FieldWritePOI key);

    void registerFieldReadPOI(WeightedBoomerang<W>.FieldReadPOI key);

    Set<SootMethod> getCallVisitedMethods();
    
    Collection<? extends Node<Statement, Val>> getForwardReachesNodes();

	void terminated(ForwardQuery query, ForwardBoomerangResults<W> forwardBoomerangResults);

	void terminated(BackwardQuery query, BackwardBoomerangResults<W> backwardBoomerangResults);

}
