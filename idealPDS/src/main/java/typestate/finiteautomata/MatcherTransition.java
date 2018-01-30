/*******************************************************************************
 * Copyright (c) 2018 Fraunhofer IEM, Paderborn, Germany.
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *  
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Johannes Spaeth - initial API and implementation
 *******************************************************************************/
package typestate.finiteautomata;

import java.util.Collection;
import java.util.HashSet;
import java.util.regex.Pattern;

import soot.MethodOrMethodContext;
import soot.Scene;
import soot.SootMethod;
import soot.jimple.toolkits.callgraph.ReachableMethods;
import soot.util.queue.QueueReader;

public class MatcherTransition extends Transition {
	private Collection<SootMethod> matchingMethods = new HashSet<>();
	private Type type;
	private Parameter param;

	public enum Type {
		OnCall, OnReturn, None, OnCallToReturn
	}

	public enum Parameter {
		This, Param1, Param2;
	}

	public MatcherTransition(State from, String methodMatcher, Parameter param, State to, Type type) {
		super(from, to);
		this.type = type;
		this.param = param;
		ReachableMethods methods = Scene.v().getReachableMethods();
		QueueReader<MethodOrMethodContext> listener = methods.listener();
		while (listener.hasNext()) {
			MethodOrMethodContext next = listener.next();
			SootMethod method = next.method();
			if (Pattern.matches(methodMatcher, method.getSignature())) {
				matchingMethods.add(method);
			}
		}
	}

	public MatcherTransition(State from, Collection<SootMethod> matchingMethods, Parameter param, State to, Type type) {
		super(from, to);
		this.type = type;
		this.param = param;
		this.matchingMethods = matchingMethods;
	}

	public boolean matches(SootMethod method) {
		return matchingMethods.contains(method);
	}

	public Type getType() {
		return type;
	}

	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + ((matchingMethods == null) ? 0 : matchingMethods.hashCode());
		result = prime * result + ((param == null) ? 0 : param.hashCode());
		result = prime * result + ((type == null) ? 0 : type.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (!super.equals(obj))
			return false;
		if (getClass() != obj.getClass())
			return false;
		MatcherTransition other = (MatcherTransition) obj;
		if (matchingMethods == null) {
			if (other.matchingMethods != null)
				return false;
		} else if (!matchingMethods.equals(other.matchingMethods))
			return false;
		if (param != other.param)
			return false;
		if (type != other.type)
			return false;
		return true;
	}

	public Parameter getParam() {
		return param;
	}
	@Override
	public String toString() {
		return super.toString() + " with " + matchingMethods;
	}
}
