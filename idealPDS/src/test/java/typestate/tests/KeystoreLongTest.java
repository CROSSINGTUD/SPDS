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
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import org.junit.Test;
import test.IDEALTestingFramework;
import typestate.finiteautomata.TypeStateMachineWeightFunctions;
import typestate.impl.statemachines.KeyStoreStateMachine;

public class KeystoreLongTest extends IDEALTestingFramework {

  @Test
  public void test1()
      throws KeyStoreException, NoSuchAlgorithmException, CertificateException, IOException {
    KeyStore ks = KeyStore.getInstance(KeyStore.getDefaultType());

    java.io.FileInputStream fis = null;
    try {
      fis = new java.io.FileInputStream("keyStoreName");
      ks.load(fis, null);
    } finally {
      if (fis != null) {
        fis.close();
      }
    }
    mustBeInAcceptingState(ks);
  }

  @Test
  public void test4()
      throws KeyStoreException, NoSuchAlgorithmException, CertificateException, IOException {
    KeyStore ks = KeyStore.getInstance(KeyStore.getDefaultType());
    KeyStore x = ks;
    java.io.FileInputStream fis = null;
    ks.load(fis, null);
    mustBeInAcceptingState(ks);
    mustBeInAcceptingState(x);
  }

  @Test
  public void test2() throws KeyStoreException {
    KeyStore ks = KeyStore.getInstance(KeyStore.getDefaultType());
    ks.aliases();
    mustBeInErrorState(ks);
  }

  @Test
  public void test3()
      throws KeyStoreException, NoSuchAlgorithmException, CertificateException, IOException {
    KeyStore ks = KeyStore.getInstance(KeyStore.getDefaultType());

    java.io.FileInputStream fis = null;
    try {
      fis = new java.io.FileInputStream("keyStoreName");
      ks.load(fis, null);
    } finally {
      if (fis != null) {
        fis.close();
      }
    }
    ks.aliases();
    mustBeInAcceptingState(ks);
  }

  @Test
  public void catchClause() {
    try {
      final KeyStore keyStore = KeyStore.getInstance("JKS");
      // ... Some code
      int size = keyStore.size(); // Hit !
      mustBeInErrorState(keyStore);
    } catch (KeyStoreException e) {
      e.printStackTrace();
    }
  }

  @Override
  protected TypeStateMachineWeightFunctions getStateMachine() {
    return new KeyStoreStateMachine();
  }
}
