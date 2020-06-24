/**
 * ***************************************************************************** Copyright (c) 2018
 * Fraunhofer IEM, Paderborn, Germany. This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * <p>SPDX-License-Identifier: EPL-2.0
 *
 * <p>Contributors: Johannes Spaeth - initial API and implementation
 * *****************************************************************************
 */
package boomerang.customize;

import boomerang.scene.Method;
import boomerang.scene.Statement;
import boomerang.scene.Val;
import java.util.Collection;
import java.util.Collections;
import sync.pds.solver.nodes.Node;
import wpds.interfaces.State;

public class BackwardEmptyCalleeFlow extends EmptyCalleeFlow {

  public Collection<? extends State> getEmptyCalleeFlow(
      Method caller, Statement callSite, Val value, Statement returnSite) {
    if (isSystemArrayCopy(callSite.getInvokeExpr().getMethod())) {
      return systemArrayCopyFlow(caller, callSite, value, returnSite);
    }
    return Collections.emptySet();
  }

  @Override
  protected Collection<? extends State> systemArrayCopyFlow(
      Method caller, Statement callSite, Val value, Statement returnSite) {
    if (value.equals(callSite.getInvokeExpr().getArg(2))) {
      Val arg = callSite.getInvokeExpr().getArg(0);
      return Collections.singleton(new Node<Statement, Val>(returnSite, arg));
    }
    return Collections.emptySet();
  }
}
