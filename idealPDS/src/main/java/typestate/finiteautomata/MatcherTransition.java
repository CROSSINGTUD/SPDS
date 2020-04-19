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
package typestate.finiteautomata;

import boomerang.scene.DeclaredMethod;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MatcherTransition extends Transition {
  private static final Logger LOGGER = LoggerFactory.getLogger(MatcherTransition.class);
  private Type type;
  private Parameter param;
  private String methodMatcher;
  private boolean negate = false;

  public enum Type {
    OnCall,
    None,
    OnCallToReturn,
    OnCallOrOnCallToReturn
  }

  public enum Parameter {
    This,
    Param1,
    Param2;
  }

  public MatcherTransition(State from, String methodMatcher, Parameter param, State to, Type type) {
    super(from, to);
    this.methodMatcher = methodMatcher;
    this.type = type;
    this.param = param;
  }

  public MatcherTransition(
      State from, String methodMatcher, boolean negate, Parameter param, State to, Type type) {
    super(from, to);
    this.methodMatcher = methodMatcher;
    this.negate = negate;
    this.type = type;
    this.param = param;
  }

  public boolean matches(DeclaredMethod declaredMethod) {
    boolean matches = Pattern.matches(methodMatcher, declaredMethod.getSubSignature());
    if (matches)
      LOGGER.debug(
          "Found matching transition at call site {} for {}", declaredMethod.getInvokeExpr(), this);
    return negate ? !matches : matches;
  }

  public Type getType() {
    return type;
  }

  public Parameter getParam() {
    return param;
  }

  @Override
  public String toString() {
    return super.toString();
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = super.hashCode();
    result = prime * result + ((methodMatcher == null) ? 0 : methodMatcher.hashCode());
    result = prime * result + (negate ? 1231 : 1237);
    result = prime * result + ((param == null) ? 0 : param.hashCode());
    result = prime * result + ((type == null) ? 0 : type.hashCode());
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) return true;
    if (!super.equals(obj)) return false;
    if (getClass() != obj.getClass()) return false;
    MatcherTransition other = (MatcherTransition) obj;
    if (methodMatcher == null) {
      if (other.methodMatcher != null) return false;
    } else if (!methodMatcher.equals(other.methodMatcher)) return false;
    if (negate != other.negate) return false;
    if (param != other.param) return false;
    if (type != other.type) return false;
    return true;
  }
}
