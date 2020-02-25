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
package test.cases.realworld;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import org.junit.Test;
import test.core.AbstractBoomerangTest;

public class ScalabilityOfBackwardAnalysis extends AbstractBoomerangTest {
  @Test
  public void simpleButDifficult() throws IOException {
    // This test case scales in Whole Program PTS Analysis when we do NOT track subtypes of
    // Exceptions.
    // The backward analysis runs into scalability problen, when we enable unbalanced flows.
    InputStream inputStream = new FileInputStream("");
    inputStream.close();
    inputStream.read();
    queryFor(inputStream);
  }

  @Override
  protected boolean includeJDK() {
    return true;
  }
}
