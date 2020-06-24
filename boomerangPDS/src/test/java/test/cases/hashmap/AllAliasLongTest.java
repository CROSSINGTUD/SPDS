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
package test.cases.hashmap;

import org.junit.Test;
import test.core.AbstractBoomerangTest;

public class AllAliasLongTest extends AbstractBoomerangTest {
  @Test
  public void test() {
    TreeNode<Object, Object> a = new TreeNode<Object, Object>(0, new Object(), new Object(), null);
    TreeNode<Object, Object> t = new TreeNode<Object, Object>(0, null, new Object(), a);
    t.balanceDeletion(t, a);
    // t.balanceInsertion(t, t);
    t.treeify(new TreeNode[] {a, t});
    // t.moveRootToFront(new TreeNode[]{a,t},a);
    queryFor(t);
  }
}
