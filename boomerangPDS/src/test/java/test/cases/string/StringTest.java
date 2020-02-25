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
package test.cases.string;

import org.junit.Test;
import test.core.AbstractBoomerangTest;
import test.core.selfrunning.AllocatedObject;

public class StringTest extends AbstractBoomerangTest {
  @Test
  public void stringConcat() {
    Object query = "a" + "b";
    if (staticallyUnknown()) query += "c";
    queryFor(query);
  }

  @Test
  public void stringConcatQueryByPass() {
    T t = new T("a" + staticallyUnknown());
    queryFor(t);
  }

  @Test
  public void stringbufferQueryByPass() {
    StringBuffer s = new StringBuffer();
    s.append("");
    s.append("");
    s.append("");
    T t = new T(s.toString());
    StringBuffer t2 = new StringBuffer();
    queryFor(t);
  }

  private static class T implements AllocatedObject {

    private String string;

    public T(String string) {
      this.string = string;
    }
  }

  @Test
  public void stringToCharArray() {
    char[] s = "password".toCharArray();
    queryFor(s);
  }

  @Test
  public void stringBuilderTest() {
    StringBuilder b = new StringBuilder("Test");
    b.append("ABC");
    String s = b.toString();
    queryFor(s);
  }

  @Test
  public void stringBuilder1Test() {
    String alloc = "Test";
    MyStringBuilder b = new MyStringBuilder(alloc);
    String s = b.toString();
    queryFor(s);
  }

  private static class MyAbstractStringBuilder {
    /** The value is used for character storage. */
    char[] value;

    /** The count is the number of characters used. */
    int count;

    /** This no-arg constructor is necessary for serialization of subclasses. */
    MyAbstractStringBuilder() {}

    /** Creates an AbstractStringBuilder of the specified capacity. */
    MyAbstractStringBuilder(int capacity) {
      value = new char[capacity];
    }

    public MyAbstractStringBuilder append(String str) {
      // if (str == null)
      // return appendNull();
      int len = str.length();
      // ensureCapacityInternal(count + len);
      str.getChars(0, len, value, count);
      count += len;
      return this;
    }
  }

  private static class MyStringBuilder extends MyAbstractStringBuilder {
    public MyStringBuilder(String str) {
      super(str.length() + 16);
      append(str);
    }

    @Override
    public String toString() {
      // Create a copy, don't share the array
      return new String(value, 0, count);
    }
  }
}
