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
package boomerang.scene.wala;

import boomerang.scene.AllocVal;
import boomerang.scene.Type;
import boomerang.scene.Val;
import boomerang.scene.WrappedClass;
import com.ibm.wala.analysis.typeInference.ConeType;
import com.ibm.wala.analysis.typeInference.PointType;
import com.ibm.wala.analysis.typeInference.TypeAbstraction;
import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.types.TypeReference;
import java.util.Iterator;

public class WALAType implements Type {

  private final TypeAbstraction typeAbstraction;

  public WALAType(TypeAbstraction typeAbstraction) {
    this.typeAbstraction = typeAbstraction;
  }

  @Override
  public boolean isNullType() {
    return typeAbstraction.equals(TypeAbstraction.TOP);
  }

  @Override
  public boolean isRefType() {
    return true;
  }

  @Override
  public boolean isBooleanType() {
    // TODO
    return false;
  }

  @Override
  public boolean isArrayType() {
    return typeAbstraction.getTypeReference() != null
        && typeAbstraction.getTypeReference().isArrayType();
  }

  @Override
  public Type getArrayBaseType() {
    return null;
  }

  @Override
  public WrappedClass getWrappedClass() {
    // TODO Auto-generated method stub
    return new WALAClass(typeAbstraction.getTypeReference());
  }

  @Override
  public boolean doesCastFail(Type targetVal, Val target) {
    if (target instanceof AllocVal && ((AllocVal) target).getAllocVal().isNewExpr()) {
      boolean subclassOfNew = this.isSubclassOf((WALAType) targetVal);
      return !subclassOfNew;
    }

    boolean castFails =
        this.isSubclassOf((WALAType) targetVal) || ((WALAType) targetVal).isSubclassOf(this);
    return !castFails;
  }

  private boolean isSubclassOf(WALAType targetType) {
    TypeAbstraction meet = typeAbstraction.meet(targetType.typeAbstraction);
    return meet.equals(typeAbstraction);
  }

  private TypeAbstraction getDelegate() {
    return typeAbstraction;
  }

  @Override
  public boolean isSubtypeOf(String type) {
    String newType = "L" + type.replace(".", "/");
    if (typeAbstraction instanceof ConeType) {
      ConeType coneType = (ConeType) typeAbstraction;
      // TODO misplaced
      if (coneType.isArrayType()) return true;
      if (hasClassInterface(coneType.getType(), newType)) {
        return true;
      }
      Iterator<IClass> iterateImplementors = coneType.iterateImplementors();
      while (iterateImplementors.hasNext()) {
        IClass next = iterateImplementors.next();
        if (hasClassInterface(next, newType)) {
          return true;
        }
      }
    }
    if (typeAbstraction instanceof PointType) {
      PointType pointType = (PointType) typeAbstraction;
      // TODO misplaced
      if (pointType.isArrayType()) return true;
      if (hasClassInterface(pointType.getIClass(), newType)) {
        return true;
      }
    }
    if (typeAbstraction.equals(TypeAbstraction.TOP)) return true;
    return false;
  }

  private boolean hasClassInterface(IClass next, String t) {
    if (next.getName().toString().equals(t)) {
      return true;
    }
    for (IClass i : next.getAllImplementedInterfaces()) {
      if (i.getName().toString().equals(t)) {
        return true;
      }
    }
    return false;
  }

  @Override
  public String toString() {
    if (typeAbstraction.equals(TypeAbstraction.TOP)) {
      return "TOP";
    }
    if (typeAbstraction instanceof PointType) {
      PointType pointType = (PointType) typeAbstraction;
      return toString(pointType.getTypeReference());
    }
    if (typeAbstraction instanceof ConeType) {
      ConeType coneType = (ConeType) typeAbstraction;
      return toString(coneType.getTypeReference());
    }
    return typeAbstraction.toString();
  }

  private String toString(TypeReference typeReference) {
    if (typeReference.isPrimitiveType()) {
      if (typeReference == TypeReference.Byte) {
        return "byte";
      }
      if (typeReference == TypeReference.Char) {
        return "char";
      }
      if (typeReference == TypeReference.Boolean) {
        return "boolean";
      }
      if (typeReference == TypeReference.Int) {
        return "int";
      }
      if (typeReference == TypeReference.Long) {
        return "long";
      }
      if (typeReference == TypeReference.Short) {
        return "short";
      }
    }
    if (typeReference.isArrayType()) {
      return toString(typeReference.getArrayElementType()) + "[]";
    }

    return typeReference.getName().toString();
  }
}
