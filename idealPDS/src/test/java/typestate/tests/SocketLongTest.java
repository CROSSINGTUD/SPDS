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
package typestate.tests;

import java.io.IOException;
import java.net.Socket;
import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import org.junit.Ignore;
import org.junit.Test;
import test.IDEALTestingFramework;
import typestate.finiteautomata.TypeStateMachineWeightFunctions;
import typestate.impl.statemachines.SocketStateMachine;

@Ignore
public class SocketLongTest extends IDEALTestingFramework {

  @Test
  public void test1() throws IOException {
    Socket socket = new Socket();
    socket.connect(new SocketAddress() {});
    socket.sendUrgentData(2);
    mustBeInAcceptingState(socket);
  }

  @Test
  public void test2() throws IOException {
    Socket socket = new Socket();
    socket.sendUrgentData(2);
    mustBeInErrorState(socket);
  }

  @Test
  public void test3() throws IOException {
    Socket socket = new Socket();
    socket.sendUrgentData(2);
    socket.sendUrgentData(2);
    mustBeInErrorState(socket);
  }

  @Test
  public void test4() throws IOException {
    Collection<Socket> sockets = createSockets();
    for (Iterator<Socket> it = sockets.iterator(); it.hasNext(); ) {
      Socket s = (Socket) it.next();
      s.connect(null);
      talk(s);
      mustBeInAcceptingState(s);
    }

    Collection<Socket> s1 = createOther();
  }

  private Collection<Socket> createOther() {
    Collection<Socket> result = new ArrayList<>();
    for (int i = 0; i < 5; i++) {
      result.add(new Socket());
    }
    return result;
  }

  @Test
  public void test5() throws IOException {
    Collection<Socket> sockets = createSockets();
    for (Iterator<Socket> it = sockets.iterator(); it.hasNext(); ) {
      Socket s = (Socket) it.next();
      talk(s);
      mayBeInErrorState(s);
    }
  }

  public static Socket createSocket() {
    return new Socket();
  }

  public static Collection<Socket> createSockets() {
    Collection<Socket> result = new LinkedList<>();
    for (int i = 0; i < 5; i++) {
      result.add(new Socket());
    }
    return result;
  }

  public static void talk(Socket s) throws IOException {
    s.getChannel();
  }

  @Override
  protected TypeStateMachineWeightFunctions getStateMachine() {
    return new SocketStateMachine();
  }
}
