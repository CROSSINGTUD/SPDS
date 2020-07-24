/**
 * ***************************************************************************** Copyright (c) 2020
 * CodeShield GmbH, Paderborn, Germany. This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * <p>SPDX-License-Identifier: EPL-2.0
 *
 * <p>Contributors: Johannes Spaeth - initial API and implementation
 * *****************************************************************************
 */
package test.cases.array;

import org.junit.Test;
import test.cases.array.ArrayTest.NoAllocation;
import test.core.AbstractBoomerangTest;
import test.core.selfrunning.AllocatedObject;
import test.core.selfrunning.NoAllocatedObject;

public class ArrayIndexSensitiveTest extends AbstractBoomerangTest {

  public static class Allocation implements AllocatedObject {}

  public static class NoAllocation implements NoAllocatedObject {}

  @Test
  public void simpleAssignment() {
    Object[] array = new Object[3];
    Allocation alloc = new Allocation();
    NoAllocation alias = new NoAllocation();
    array[1] = alias;
    array[2] = alloc;
    Object query = array[2];
    queryFor(query);
  }

  @Test
  public void arrayIndexOverwrite() {
    Object[] array = new Object[3];
    array[1] = new NoAllocation();
    Allocation allocation = new Allocation();
    array[1] = allocation;
    Object query = array[1];
    queryFor(query);
  }

  @Test
  public void arrayIndexNoOverwrite() {
    Object[] array = new Object[3];
    array[1] = new Allocation();
    NoAllocation noAlloc = new NoAllocation();
    array[2] = noAlloc;
    Object query = array[1];
    queryFor(query);
  }

  @Test
  public void arrayLoadInLoop() {
    Object[] array = new Object[3];
    array[0] = new NoAllocation();
    array[0] = new Allocation();
    array[1] = new Allocation();
    array[2] = new Allocation(); // bw: pop 2, fw: push 2
    Object q = null;
    for (int i = 0; i < 3; i++) {
      q = array[i]; // bw: push ALL, fw: pop ALL
    }
    queryFor(q);
  }

  @Test
  public void arrayStoreInLoop() {
    Object[] array = new Object[3];
    Object q = new Allocation();
    for (int i = 0; i < 3; i++) {
      array[i] = q; // bw: pop ALL, fw: push ALL
    }
    Object query = array[1]; // bw: push 1, fw: pop 1
    queryFor(query);
  }

  @Test
  public void copyArray() {
    Object[] array = new Object[3];
    array[0] = new NoAllocation();
    array[0] = new NoAllocation();
    array[1] = new Allocation();
    array[2] = new NoAllocation(); // bw: pop 2, fw: push 2
    Object[] array2 = array;
    Object q = array2[1];
    queryFor(q);
  }
}
