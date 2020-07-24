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
package boomerang.scene;

import wpds.interfaces.Empty;
import wpds.interfaces.Location;
import wpds.wildcard.ExclusionWildcard;
import wpds.wildcard.Wildcard;

public class Field implements Location {
  private final String rep;

  private Field(String rep) {
    this.rep = rep;
  }

  protected Field() {
    this.rep = null;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((rep == null) ? 0 : rep.hashCode());
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null) {
      return false;
    }
    if (getClass() != obj.getClass()) {
      return false;
    }
    Field other = (Field) obj;
    if (rep == null) {
      if (other.rep != null) {
        return false;
      }
    } else if (!rep.equals(other.rep)) {
      return false;
    }
    return true;
  }

  @Override
  public String toString() {
    return rep;
  }

  public static Field wildcard() {
    return new WildcardField();
  }

  public static Field empty() {
    return new EmptyField("{}");
  }

  private static class EmptyField extends Field implements Empty {
    public EmptyField(String rep) {
      super(rep);
    }
  }

  public static Field epsilon() {
    return new EmptyField("eps_f");
  }

  public static Field array() {
    return new Field("array");
  }

  private static class WildcardField extends Field implements Wildcard {
    public WildcardField() {
      super("*");
    }
  }

  private static class ExclusionWildcardField extends Field implements ExclusionWildcard<Field> {
    private final Field excludes;

    public ExclusionWildcardField(Field excl) {
      super();
      this.excludes = excl;
    }

    @Override
    public Field excludes() {
      return excludes;
    }

    @Override
    public String toString() {
      return "not " + excludes;
    }

    @Override
    public int hashCode() {
      final int prime = 31;
      int result = super.hashCode();
      result = prime * result + ((excludes == null) ? 0 : excludes.hashCode());
      return result;
    }

    @Override
    public boolean equals(Object obj) {
      if (this == obj) {
        return true;
      }
      if (!super.equals(obj)) {
        return false;
      }
      if (getClass() != obj.getClass()) {
        return false;
      }
      ExclusionWildcardField other = (ExclusionWildcardField) obj;
      if (excludes == null) {
        if (other.excludes != null) {
          return false;
        }
      } else if (!excludes.equals(other.excludes)) {
        return false;
      }
      return true;
    }
  }

  public static Field exclusionWildcard(Field exclusion) {
    return new ExclusionWildcardField(exclusion);
  }

  public boolean isInnerClassField() {
    return false;
  }
}
