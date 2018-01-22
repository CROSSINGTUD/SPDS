/*******************************************************************************
 * Copyright (c) 2018 Fraunhofer IEM, Paderborn, Germany.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Johannes Spaeth - initial API and implementation
 *******************************************************************************/
package wpds.impl;

public abstract class Weight {
  public abstract Weight extendWith(Weight other);

  public abstract Weight combineWith(Weight other);


  public static NoWeight NO_WEIGHT_ONE = new Weight.NoWeight();
  public static NoWeight NO_WEIGHT_ZERO = new Weight.NoWeight();

  public static class NoWeight extends Weight {
    @Override
    public Weight extendWith(Weight other) {
      return other;
    }

    @Override
    public Weight combineWith(Weight other) {
      return other;
    }

    @Override
    public String toString() {
      return "";
    }


  }
}
