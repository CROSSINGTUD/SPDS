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
package test.cases.context;

import org.junit.Test;

import test.core.AbstractBoomerangTest;
import test.core.selfrunning.AllocatedObject;

public class LoopInContextRequesterTest extends AbstractBoomerangTest{

  @Test
  public void loop() {
    ILoop c;
    c = new Loop1();
    c.loop();
  }
	public interface ILoop {
		void loop();
	}

	public class Loop1 implements ILoop {
		A a = new A();
		@Override
		public void loop() {
			if(staticallyUnknown())
				loop();
			AllocatedObject x = a.d;
			queryFor(x);
		}

	}

	public class A{
		AllocatedObject d = new AllocatedObject() {
		};
		A f = new A();
	}
}
