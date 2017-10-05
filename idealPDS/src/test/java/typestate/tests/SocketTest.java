package typestate.tests;

import java.io.IOException;
import java.net.Socket;
import java.net.SocketAddress;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;

import org.junit.Test;

import test.IDEALTestingFramework;
import typestate.finiteautomata.MatcherStateMachine;
import typestate.impl.statemachines.SocketStateMachine;

public class SocketTest extends IDEALTestingFramework {

	@Test
	public void test1() throws IOException {
		Socket socket = new Socket();
		socket.connect(new SocketAddress() {
		});
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
	    for (Iterator<Socket> it = sockets.iterator(); it.hasNext();) {
	      Socket s = (Socket) it.next();
	      s.connect(null);
	      talk(s);
	      mustBeInAcceptingState(s);
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
	protected MatcherStateMachine getStateMachine() {
		return new SocketStateMachine();
	}
}
