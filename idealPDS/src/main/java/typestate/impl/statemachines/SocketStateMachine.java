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
package typestate.impl.statemachines;

import java.net.Socket;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import boomerang.WeightedForwardQuery;
import soot.SootClass;
import soot.SootMethod;
import soot.Unit;
import typestate.TransitionFunction;
import typestate.finiteautomata.MatcherTransition;
import typestate.finiteautomata.MatcherTransition.Parameter;
import typestate.finiteautomata.MatcherTransition.Type;
import typestate.finiteautomata.State;
import typestate.finiteautomata.TypeStateMachineWeightFunctions;

public class SocketStateMachine extends TypeStateMachineWeightFunctions {

    public static enum States implements State {
        INIT, CONNECTED, ERROR;

        @Override
        public boolean isErrorState() {
            return this == ERROR;
        }

        @Override
        public boolean isInitialState() {
            return false;
        }

        @Override
        public boolean isAccepting() {
            return false;
        }
    }

    public SocketStateMachine() {
        addTransition(new MatcherTransition(States.INIT, connect(), Parameter.This, States.CONNECTED, Type.OnReturn));
        addTransition(new MatcherTransition(States.ERROR, connect(), Parameter.This, States.ERROR, Type.OnReturn));
        addTransition(
                new MatcherTransition(States.CONNECTED, useMethods(), Parameter.This, States.CONNECTED, Type.OnReturn));
        addTransition(new MatcherTransition(States.INIT, useMethods(), Parameter.This, States.ERROR, Type.OnReturn));
        addTransition(
                new MatcherTransition(States.CONNECTED, connect(), Parameter.This, States.CONNECTED, Type.OnReturn));
        addTransition(new MatcherTransition(States.ERROR, useMethods(), Parameter.This, States.ERROR, Type.OnReturn));
    }

    private Set<SootMethod> socketConstructor() {
        List<SootClass> subclasses = getSubclassesOf("java.net.Socket");
        Set<SootMethod> out = new HashSet<>();
        for (SootClass c : subclasses) {
            for (SootMethod m : c.getMethods())
                if (m.isConstructor())
                    out.add(m);
        }
        return out;
    }

    private Set<SootMethod> connect() {
        return selectMethodByName(getSubclassesOf("java.net.Socket"), "connect");
    }

    private Set<SootMethod> useMethods() {
        List<SootClass> subclasses = getSubclassesOf("java.net.Socket");
        Set<SootMethod> connectMethod = connect();
        Set<SootMethod> out = new HashSet<>();
        for (SootClass c : subclasses) {
            for (SootMethod m : c.getMethods())
                if (!m.isConstructor() && m.isPublic() && !connectMethod.contains(m) && !m.isStatic()
                        && !m.getName().startsWith("is"))
                    out.add(m);
        }
        return out;
    }

    @Override
    public Collection<WeightedForwardQuery<TransitionFunction>> generateSeed(SootMethod m, Unit unit) {
        return generateAtAllocationSiteOf(m, unit, Socket.class);
    }

    @Override
    protected State initialState() {
        return States.INIT;
    }
}
