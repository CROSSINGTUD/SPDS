package typestate.tests;

import java.io.IOException;
import java.net.HttpURLConnection;

import org.junit.Test;

import test.IDEALTestingFramework;
import typestate.ConcreteState;
import typestate.TypestateChangeFunction;
import typestate.impl.statemachines.URLConnStateMachine;

public class URLConnTest extends IDEALTestingFramework {

	@Test
	public void test1() throws IOException {
	    HttpURLConnection httpURLConnection = new HttpURLConnection(null) {

	        @Override
	        public void connect() throws IOException {
	          // TODO Auto-generated method stub
	          System.out.println("");
	        }

	        @Override
	        public boolean usingProxy() {
	          // TODO Auto-generated method stub
	          return false;
	        }

	        @Override
	        public void disconnect() {
	          // TODO Auto-generated method stub

	        }
	      };
	      httpURLConnection.connect();
	      httpURLConnection.setDoOutput(true);
	      mustBeInErrorState(httpURLConnection);
	      httpURLConnection.setAllowUserInteraction(false);
	      mustBeInErrorState(httpURLConnection);
	}

	@Test
	public void test2() throws IOException {
	    HttpURLConnection httpURLConnection = new HttpURLConnection(null) {

	        @Override
	        public void connect() throws IOException {
	          // TODO Auto-generated method stub
	          System.out.println("");
	        }

	        @Override
	        public boolean usingProxy() {
	          // TODO Auto-generated method stub
	          return false;
	        }

	        @Override
	        public void disconnect() {
	          // TODO Auto-generated method stub

	        }
	      };
	      httpURLConnection.setDoOutput(true);
	      httpURLConnection.setAllowUserInteraction(false);

	      httpURLConnection.connect();
	      mustBeInAcceptingState(httpURLConnection);
	}

	@Override
	protected TypestateChangeFunction<ConcreteState> createTypestateChangeFunction() {
		return new URLConnStateMachine();
	}
}