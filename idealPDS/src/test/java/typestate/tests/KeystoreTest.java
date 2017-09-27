package typestate.tests;

import java.io.IOException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;

import org.junit.Test;

import test.IDEALTestingFramework;
import typestate.ConcreteState;
import typestate.TypestateChangeFunction;
import typestate.impl.statemachines.KeyStoreStateMachine;

public class KeystoreTest extends IDEALTestingFramework {

	@Test
	public void test1() throws KeyStoreException, NoSuchAlgorithmException, CertificateException, IOException {
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
	public void test2() throws KeyStoreException {
		KeyStore ks = KeyStore.getInstance(KeyStore.getDefaultType());
		ks.aliases();
		mustBeInErrorState(ks);
	}

	@Test
	public void test3() throws KeyStoreException, NoSuchAlgorithmException, CertificateException, IOException {
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

	@Override
	protected TypestateChangeFunction<ConcreteState> createTypestateChangeFunction() {
		return new KeyStoreStateMachine();
	}
}