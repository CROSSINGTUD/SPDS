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
package boomerang.preanalysis;

import java.util.Map;

import soot.Body;
import soot.Scene;
import soot.SceneTransformer;
import soot.SootClass;
import soot.SootMethod;
import soot.jimple.internal.JNopStmt;

public class AddNopStatementTransformer extends SceneTransformer {

  @Override
  protected void internalTransform(String phaseName, Map<String, String> options) {
    for (SootClass c : Scene.v().getClasses()) {
      for (SootMethod m : c.getMethods()) {
        if (!m.hasActiveBody()) {
          continue;
        }
        Body b = m.getActiveBody();
        b.getUnits().addFirst(new JNopStmt());
      }
    }
  }

}
