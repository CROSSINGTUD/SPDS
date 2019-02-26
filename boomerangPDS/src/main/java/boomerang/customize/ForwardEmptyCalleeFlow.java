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
package boomerang.customize;

import java.util.Collection;
import java.util.Collections;

import boomerang.jimple.Statement;
import boomerang.jimple.Val;
import soot.SootMethod;
import soot.Value;
import soot.jimple.Stmt;
import sync.pds.solver.nodes.Node;
import wpds.interfaces.State;

public class ForwardEmptyCalleeFlow extends EmptyCalleeFlow {

    public Collection<? extends State> getEmptyCalleeFlow(SootMethod caller, Stmt curr, Val value, Stmt callSite) {
        if (isSystemArrayCopy(curr.getInvokeExpr().getMethod())) {
            return systemArrayCopyFlow(caller, curr, value, callSite);
        }
        return Collections.emptySet();
    }

    @Override
    protected Collection<? extends State> systemArrayCopyFlow(SootMethod caller, Stmt curr, Val value, Stmt callSite) {
        if (value.equals(new Val(curr.getInvokeExpr().getArg(0), caller))) {
            Value arg = curr.getInvokeExpr().getArg(2);
            return Collections.singleton(new Node<Statement, Val>(new Statement(curr, caller), new Val(arg, caller)));
        }
        return Collections.emptySet();
    }

}
