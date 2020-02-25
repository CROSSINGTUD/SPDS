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
package test.cases.fields;

import org.junit.Ignore;
import org.junit.Test;
import test.core.AbstractBoomerangTest;
import test.core.selfrunning.AllocatedObject;

public class FieldsBranchedTest extends AbstractBoomerangTest {
  @Ignore
  @Test
  public void twoFieldsNoLoop() {
    Node x = new Node();
    if (staticallyUnknown()) {
      x.left.right = x;
    } else if (staticallyUnknown()) {
      x.right.left = x;
    }
    Node t;
    if (staticallyUnknown()) {
      t = x.left.right;
    } else {
      t = x.right.left;
    }
    Node h = t;
    queryFor(h);
  }

  @Test
  @Ignore
  public void twoFieldsNoLoop2() {
    Node x = new Node();
    Node t = null;
    if (staticallyUnknown()) {
      x.left.right = x;
      t = x.left.right;
    } else if (staticallyUnknown()) {
      x.right.left = x;
      t = x.right.left;
    }
    Node h = t;
    queryFor(h);
  }

  @Test
  public void oneFieldsNoLoop() {
    Node x = new Node();
    if (staticallyUnknown()) {
      x.left = x;
    } else if (staticallyUnknown()) {
      x.right = x;
    }
    Node t;
    if (staticallyUnknown()) {
      t = x.left;
    } else {
      t = x.right;
    }
    Node h = t;
    queryFor(h);
  }

  private class Node implements AllocatedObject {
    Node left = new Node();
    Node right = new Node();
  }
}
