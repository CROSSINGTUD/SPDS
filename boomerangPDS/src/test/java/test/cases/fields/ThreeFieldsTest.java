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

import org.junit.Test;
import test.core.AbstractBoomerangTest;
import test.core.selfrunning.AllocatedObject;

public class ThreeFieldsTest extends AbstractBoomerangTest {
  public static class Level1 {
    Level2 l2 = new Level2();
  }

  public static class Level2 {
    Level3 l3 = new Level3();
  }

  public static class Level3 {
    Level4 l4;
  }

  public static class Level4 implements AllocatedObject {}

  @Test
  public void indirectAllocationSite() {
    Level1 l = new Level1();
    Level2 x = l.l2;
    setField(l);
    Level3 intermediate = x.l3;
    Level4 alias2 = intermediate.l4;
    queryFor(alias2);
  }

  @Test
  public void indirectAllocationSite3Address() {
    Level1 l = new Level1();
    Level2 x = l.l2;
    setField3Address(l);
    Level3 intermediate = x.l3;
    Level4 alias2 = intermediate.l4;
    queryFor(alias2);
  }

  public void setField3Address(Level1 l) {
    Level2 xAlias = l.l2;
    Level3 level3 = xAlias.l3;
    Level4 alloc = new Level4();
    level3.l4 = alloc;
  }

  @Test
  public void indirectAllocationSiteNoRead() {
    Level1 l = new Level1();
    setField(l);
    Level4 alias = l.l2.l3.l4;
    queryFor(alias);
  }

  public void setField(Level1 l) {
    l.l2.l3.l4 = new Level4();
  }

  @Test
  public void indirectAllocationSiteNoReadOnlyTwoLevel() {
    Level2 l = new Level2();
    setFieldLevel2(l);
    Level4 alias = l.l3.l4;
    queryFor(alias);
  }

  public void setFieldLevel2(Level2 l) {
    l.l3.l4 = new Level4();
  }

  @Test
  public void test() {
    Level1 l = new Level1();
    Level2 x = l.l2;
    wrappedSetField(l);
    Level4 alias = l.l2.l3.l4;
    Level4 alias2 = x.l3.l4;
    queryFor(alias2);
  }

  private void wrappedSetField(Level1 l) {
    setField(l);
  }
}
