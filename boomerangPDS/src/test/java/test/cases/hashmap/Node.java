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

import java.util.Map;
import java.util.Objects;

class Node<K, V> implements Map.Entry<K, V> {
  final int hash;
  final K key;
  V value;
  Node<K, V> next;

  Node(int hash, K key, V value, Node<K, V> next) {
    this.hash = hash;
    this.key = key;
    this.value = value;
    this.next = next;
  }

  public final K getKey() {
    return key;
  }

  public final V getValue() {
    return value;
  }

  public final String toString() {
    return key + "=" + value;
  }

  public final int hashCode() {
    return Objects.hashCode(key) ^ Objects.hashCode(value);
  }

  public final V setValue(V newValue) {
    V oldValue = value;
    value = newValue;
    return oldValue;
  }

  public final boolean equals(Object o) {
    if (o == this) return true;
    if (o instanceof Map.Entry) {
      Map.Entry<?, ?> e = (Map.Entry<?, ?>) o;
      if (Objects.equals(key, e.getKey()) && Objects.equals(value, e.getValue())) return true;
    }
    return false;
  }
}
