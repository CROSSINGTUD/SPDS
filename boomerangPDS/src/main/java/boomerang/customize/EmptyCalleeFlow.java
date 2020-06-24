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

import boomerang.scene.DeclaredMethod;
import boomerang.scene.Method;
import boomerang.scene.Statement;
import boomerang.scene.Val;
import java.util.Collection;
import wpds.interfaces.State;

public abstract class EmptyCalleeFlow {

  protected DeclaredMethod systemArrayCopyMethod;
  protected boolean fetchedSystemArrayCopyMethod;

  protected boolean isSystemArrayCopy(DeclaredMethod method) {
    return method.getName().equals("arraycopy")
        && method.getDeclaringClass().getName().equals("java.lang.System");
  }

  public abstract Collection<? extends State> getEmptyCalleeFlow(
      Method caller, Statement curr, Val value, Statement succ);

  protected abstract Collection<? extends State> systemArrayCopyFlow(
      Method caller, Statement callSite, Val value, Statement returnSite);
}
