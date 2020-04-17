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
package inference.example;

public class InferenceExample {
  public static void main(String... args) {
    File file = new File();
    file.open();
    staticCallOnFile(file);
    file.open();
  }

  private static void staticCallOnFile(File file) {
    file.close();
  }

  public static class File {

    public void open() {}

    public void close() {}
  }
}
