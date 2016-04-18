package com.blackducksoftware.integration.hub.encryption;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.util.Properties;

import org.junit.BeforeClass;
import org.junit.Test;

public class PasswordDecrypterTest {
	private static Properties encryptedUserPassword = null;

	@BeforeClass
	public static void init() throws URISyntaxException, IOException {
		encryptedUserPassword = new Properties();
		final ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
		final InputStream is = classLoader.getResourceAsStream("encryptedPasswordFile.txt");
		try {
			encryptedUserPassword.load(is);
		} catch (final IOException e) {
			System.err.println("reading encryptedPasswordFile failed!");
		}
	}

	@Test
	public void testPasswordDecryption() throws Exception {
		assertEquals("super", PasswordDecrypter.decrypt(encryptedUserPassword.getProperty("super")));
	}

	@Test
	public void testPasswordDecryptionAgain() throws Exception {
		assertEquals("testing",
				PasswordDecrypter.decrypt(encryptedUserPassword.getProperty("test@blackducksoftware.com")));
	}

	@Test
	public void testPasswordDecryptionEmptyKey() throws Exception {
		assertNull(PasswordDecrypter.decrypt(""));
	}

	@Test
	public void testPasswordDecryptionNullKey() throws Exception {
		assertNull(PasswordDecrypter.decrypt(null));
	}

}
