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
package boomerang;

import boomerang.scene.CallSiteStatement;
import boomerang.scene.Statement;
import boomerang.scene.Val;

public class BackwardQuery extends Query {
  protected BackwardQuery(Statement stmt, Val variable) {
    super(stmt, variable);
  }

  @Override
  public String toString() {
    return "BackwardQuery: " + super.toString();
  }

  public static BackwardQuery make(Statement stmt, Val variable) {
    return new BackwardQuery(
        (stmt instanceof CallSiteStatement)
            ? ((CallSiteStatement) stmt).getReturnSiteStatement()
            : stmt,
        variable);
  }
}
