/*******************************************************************************
 * Copyright (c) 2018 Fraunhofer IEM, Paderborn, Germany.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Johannes Spaeth - initial API and implementation
 *******************************************************************************/
package typestate.tests;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;

import org.junit.Test;

import test.IDEALTestingFramework;
import typestate.finiteautomata.TypeStateMachineWeightFunctions;
import typestate.impl.statemachines.alloc.PrintStreamStateMachine;

public class PrintStreamTest extends IDEALTestingFramework {

	@Test
	public void test1() throws FileNotFoundException {
		PrintStream inputStream = new PrintStream("");
		inputStream.close();
		inputStream.flush();
		mustBeInErrorState(inputStream);
	}

	@Test
	public void test(){
		try {
			FileOutputStream out = new FileOutputStream("foo.txt");
			PrintStream p = new PrintStream(out);
			p.close();
			p.println("foo!");
			p.write(42);
			mustBeInErrorState(p);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	
	@Override
	protected TypeStateMachineWeightFunctions getStateMachine() {
		return new PrintStreamStateMachine();
	}
}