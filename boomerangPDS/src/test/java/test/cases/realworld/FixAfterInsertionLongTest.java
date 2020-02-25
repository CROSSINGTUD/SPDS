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

import org.junit.Ignore;
import org.junit.Test;
import test.cases.realworld.FixAfterInsertion.Entry;
import test.core.AbstractBoomerangTest;

@Ignore
public class FixAfterInsertionLongTest extends AbstractBoomerangTest {

  @Test
  public void main() {
    Entry<Object, Object> entry = new Entry<Object, Object>(null, null, null);
    entry = new Entry<Object, Object>(null, null, entry);
    new FixAfterInsertion<>().fixAfterInsertion(entry);
    Entry<Object, Object> query = entry.parent;
    queryFor(query);
  }

  @Test
  public void rotateLeftAndRightInLoop() {
    Entry<Object, Object> entry = new Entry<Object, Object>(null, null, null);
    entry = new Entry<Object, Object>(null, null, entry);
    while (true) {
      new FixAfterInsertion<>().rotateLeft(entry);
      new FixAfterInsertion<>().rotateRight(entry);
      if (staticallyUnknown()) break;
    }
    Entry<Object, Object> query = entry.parent;
    queryFor(query);
  }
}
