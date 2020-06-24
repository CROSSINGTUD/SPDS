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
package test.cases.integers;

import java.math.BigInteger;
import org.junit.Ignore;
import org.junit.Test;
import test.core.AbstractBoomerangTest;

@Ignore
public class IntTest extends AbstractBoomerangTest {
  @Test
  public void simpleAssign() {
    int allocation = 1;
    intQueryFor(allocation, "1");
  }

  @Test
  public void simpleAssignBranched() {
    int allocation = 2;
    if (staticallyUnknown()) {
      allocation = 1;
    }
    intQueryFor(allocation, "1,2");
  }

  @Test
  public void simpleIntraAssign() {
    int allocation = 1;
    int y = allocation;
    intQueryFor(y, "1");
  }

  @Test
  public void simpleInterAssign() {
    int allocation = 1;
    int y = foo(allocation);
    intQueryFor(y, "1");
  }

  @Test
  public void returnDirect() {
    int allocation = getVal();
    intQueryFor(allocation, "1");
  }

  @Test
  public void returnInDirect() {
    int x = getValIndirect();
    intQueryFor(x, "1");
  }

  private int getValIndirect() {
    int allocation = 1;
    return allocation;
  }

  private int getVal() {
    return 1;
  }

  private int foo(int x) {
    int y = x;
    return y;
  }

  @Ignore
  @Test
  public void wrappedType() {
    Integer integer = new Integer(1);
    int allocation = integer;
    intQueryFor(allocation, "1");
  }

  @Test
  public void wrappedTypeBigInteger() {
    BigInteger integer = BigInteger.valueOf(1);
    intQueryFor(integer, "1L");
  }
}
