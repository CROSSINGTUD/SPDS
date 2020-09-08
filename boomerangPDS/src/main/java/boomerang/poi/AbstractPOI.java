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
package boomerang.poi;

import boomerang.scene.ControlFlowGraph.Edge;

public abstract class AbstractPOI<Statement, Val, Field>
    extends PointOfIndirection<Statement, Val, Field> {

  private final Val baseVar;
  private final Field field;
  private final Val storedVar;
  private Edge cfgEdge;

  public AbstractPOI(Edge cfgEdge, Val baseVar, Field field, Val storedVar) {
    this.cfgEdge = cfgEdge;
    this.baseVar = baseVar;
    this.field = field;
    this.storedVar = storedVar;
  }

  public Val getBaseVar() {
    return baseVar;
  }

  public Field getField() {
    return field;
  }

  public Val getStoredVar() {
    return storedVar;
  }

  public Edge getCfgEdge() {
    return cfgEdge;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((field == null) ? 0 : field.hashCode());
    result = prime * result + ((baseVar == null) ? 0 : baseVar.hashCode());
    result = prime * result + ((storedVar == null) ? 0 : storedVar.hashCode());
    result = prime * result + ((cfgEdge == null) ? 0 : cfgEdge.hashCode());
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) return true;
    if (obj == null) return false;
    if (getClass() != obj.getClass()) return false;
    AbstractPOI other = (AbstractPOI) obj;
    if (field == null) {
      if (other.field != null) return false;
    } else if (!field.equals(other.field)) return false;
    if (baseVar == null) {
      if (other.baseVar != null) return false;
    } else if (!baseVar.equals(other.baseVar)) return false;
    if (storedVar == null) {
      if (other.storedVar != null) return false;
    } else if (!storedVar.equals(other.storedVar)) return false;
    if (cfgEdge == null) {
      if (other.cfgEdge != null) return false;
    } else if (!cfgEdge.equals(other.cfgEdge)) return false;
    return true;
  }

  @Override
  public String toString() {
    return "POI:" + cfgEdge.toString();
  }
}
