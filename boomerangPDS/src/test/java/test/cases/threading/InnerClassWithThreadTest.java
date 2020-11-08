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
package test.cases.threading;

import org.junit.Ignore;
import org.junit.Test;
import test.cases.fields.Alloc;
import test.core.AbstractBoomerangTest;
import test.core.selfrunning.AllocatedObject;

@Ignore
public class InnerClassWithThreadTest extends AbstractBoomerangTest {
  private static Alloc param;

  @Ignore
  @Test
  public void runWithThreadStatic() {
    param = new Alloc();
    Runnable r =
        new Runnable() {

          @Override
          public void run() {
            String cmd = System.getProperty("");
            // if(cmd!=null){
            // param = new Allocation();
            // }
            for (int i = 1; i < 3; i++) {
              Object t = param;
              Object a = t;
              queryFor(a);
            }
          }
        };
    Thread t = new Thread(r);
    t.start();
  }

  @Test
  public void runWithThread() {
    final Alloc u = new Alloc();
    Runnable r =
        new Runnable() {

          @Override
          public void run() {
            // String cmd = System.getProperty("");
            // if(cmd!=null){
            // param = new Allocation();
            // }
            for (int i = 1; i < 3; i++) {
              queryFor(u);
            }
          }
        };
    Thread t = new Thread(r);
    t.start();
  }

  @Test
  public void threadQuery() {
    for (int i = 1; i < 3; i++) {
      Thread t = new MyThread();
      t.start();
      queryFor(t);
    }
  }

  private static class MyThread extends Thread implements AllocatedObject {}

  protected AnalysisMode[] getAnalyses() {
    return new AnalysisMode[] {AnalysisMode.DemandDrivenBackward};
  }

  @Override
  protected boolean includeJDK() {
    return true;
  }
}
