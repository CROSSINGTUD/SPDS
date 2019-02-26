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
package boomerang;

import boomerang.callgraph.ObservableICFG;
import boomerang.jimple.AllocVal;
import boomerang.jimple.Statement;
import boomerang.jimple.Val;
import com.google.common.base.Optional;
import soot.SootMethod;
import soot.Unit;
import soot.Value;
import soot.jimple.*;

import java.util.concurrent.atomic.AtomicReference;

public class IntAndStringBoomerangOptions extends DefaultBoomerangOptions {

    public boolean isAllocationVal(Value val) {
        if (val instanceof IntConstant) {
            return true;
        }
        return super.isAllocationVal(val);
    }

    @Override
    protected boolean isArrayAllocationVal(Value val) {
        return (val instanceof NewArrayExpr || val instanceof NewMultiArrayExpr);
    }

    @Override
    public Optional<AllocVal> getAllocationVal(SootMethod m, Stmt stmt, Val fact,
            ObservableICFG<Unit, SootMethod> icfg) {
        if (!(stmt instanceof AssignStmt)) {
            return Optional.absent();
        }
        AssignStmt as = (AssignStmt) stmt;
        if (!as.getLeftOp().equals(fact.value())) {
            return Optional.absent();
        }
        if (as.getRightOp() instanceof LengthExpr) {
            return Optional.of(new AllocVal(as.getLeftOp(), m, as.getRightOp(), new Statement(stmt, m)));
        }

        if (as.containsInvokeExpr()) {
            // AtomicReference<AllocVal> returnValue = new AtomicReference<>();
            // icfg.addCalleeListener(new AllocationValCalleeListener(returnValue, as, icfg, m));
            // if (returnValue.get() != null){
            // return Optional.of(returnValue.get());
            // }
            SootMethod method = as.getInvokeExpr().getMethod();
            String sig = method.getSignature();
            if (sig.equals("<java.math.BigInteger: java.math.BigInteger valueOf(long)>")) {
                Value arg = as.getInvokeExpr().getArg(0);
                return Optional.of(new AllocVal(as.getLeftOp(), m, arg, new Statement(stmt, m)));
            }
        }
        return super.getAllocationVal(m, stmt, fact, icfg);
    }

    @Override
    public boolean trackStrings() {
        return true;
    }
}
