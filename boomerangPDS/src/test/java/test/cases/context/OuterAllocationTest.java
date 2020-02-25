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
package test.cases.context;

import org.junit.Test;
import test.core.AbstractBoomerangTest;
import test.core.selfrunning.AllocatedObject;

public class OuterAllocationTest extends AbstractBoomerangTest {
  @Test
  public void main() {
    ObjectWithField container = new ObjectWithField();
    container.field = new File();
    ObjectWithField otherContainer = new ObjectWithField();
    File a = container.field;
    otherContainer.field = a;
    flows(container);
  }

  private void flows(ObjectWithField container) {
    File field = container.field;
    field.open();
    queryFor(field);
  }

  private static class File implements AllocatedObject {
    public void open() {}
  }

  private static class ObjectWithField {
    File field;
  }
}
