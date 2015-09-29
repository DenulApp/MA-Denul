package de.velcommuta.denul.util;

import junit.framework.TestCase;

import java.util.Random;

import javax.crypto.BadPaddingException;

/**
 * Test class for Crypto functions
 */
public class CryptoTest extends TestCase {
    /**
     * Test the generation of AES keys
     */
    public void testAesKeyGen() {
        byte[] key = Crypto.generateAES256Key();
        assertNotNull("Generated key was null", key);
    }

    /**
     * Test encryption of random data
     */
    public void testEncryption() {
        byte[] key = Crypto.generateAES256Key();
        byte[] message = new byte[128];
        new Random().nextBytes(message);
        byte[] ciphertext = Crypto.encryptAES(message, key);
        assertNotNull("Generated Ciphertext was null", ciphertext);
    }

    /**
     * Test encryption and decryption of random data
     */
    public void testDecryption() {
        byte[] key = Crypto.generateAES256Key();
        byte[] message = new byte[128];
        new Random().nextBytes(message);
        byte[] ciphertext = Crypto.encryptAES(message, key);
        try {
            byte[] cleartext = Crypto.decryptAES(ciphertext, key);
            assertEquals("Decrypted cleartext does not match original text", new String(cleartext), new String(message));
        } catch (BadPaddingException e) {
            assertTrue("Exception occured during decryption", false);
        }
    }

    /**
     * Test if the decryption really fails with a different key
     */
    public void testDecryptionFailWithOtherKey() {
        byte[] key = Crypto.generateAES256Key();
        byte[] key2 = Crypto.generateAES256Key();
        byte[] message = new byte[128];
        new Random().nextBytes(message);
        byte[] ciphertext = Crypto.encryptAES(message, key);
        try {
            byte[] cleartext = Crypto.decryptAES(ciphertext, key2);
            assertNull("Decryption did not fail with incorrect key", cleartext);
        } catch (BadPaddingException e) {
            assertTrue("No exception was raised", true);
        }
    }

    /**
     * Test if the decryption raises an exception if the _message_ was changed
     */
    public void testDecryptionFailWithChangedMessage() {
        byte[] key = Crypto.generateAES256Key();
        byte[] message = new byte[128];
        new Random().nextBytes(message);
        byte[] ciphertext = Crypto.encryptAES(message, key);
        try {
            ciphertext[23] = 0x00;
            byte[] cleartext = Crypto.decryptAES(ciphertext, key);
            assertTrue("No exception was raised during decryption", false);
        } catch (BadPaddingException e) {
            assertTrue(true);
        }
    }

    /**
     * Test if the decryption raises an exception if the _IV_ was changed
     */
    public void testDecryptionFailWithChangedIV() {
        byte[] key = Crypto.generateAES256Key();
        byte[] message = new byte[128];
        new Random().nextBytes(message);
        byte[] ciphertext = Crypto.encryptAES(message, key);
        try {
            ciphertext[1] = 0x00;
            byte[] cleartext = Crypto.decryptAES(ciphertext, key);
            assertTrue("No exception was raised during decryption", false);
        } catch (BadPaddingException e) {
            assertTrue(true);
        }
    }
}