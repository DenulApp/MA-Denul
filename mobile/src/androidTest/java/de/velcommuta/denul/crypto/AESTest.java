package de.velcommuta.denul.crypto;

import junit.framework.TestCase;

import java.util.Arrays;
import java.util.Random;

import javax.crypto.BadPaddingException;

/**
 * Test cases for the AES crypto
 */
public class AESTest extends TestCase {
    /**
     * Test the generation of AES keys
     */
    public void testAesKeyGen() {
        byte[] key = AES.generateAES256Key();
        assertNotNull("Generated key was null", key);
    }

    /**
     * Test encryption of random data
     */
    public void testEncryption() {
        byte[] key = AES.generateAES256Key();
        byte[] message = new byte[128];
        new Random().nextBytes(message);
        byte[] ciphertext = AES.encryptAES(message, key);
        assertNotNull("Generated Ciphertext was null", ciphertext);
    }

    /**
     * Test encryption and decryption of random data
     */
    public void testDecryption() {
        byte[] key = AES.generateAES256Key();
        byte[] message = new byte[128];
        new Random().nextBytes(message);
        byte[] ciphertext = AES.encryptAES(message, key);
        try {
            byte[] cleartext = AES.decryptAES(ciphertext, key);
            assertEquals("Decrypted cleartext does not match original text", new String(cleartext), new String(message));
        } catch (BadPaddingException e) {
            assertTrue("Exception occured during decryption", false);
        }
    }

    /**
     * Test if the decryption really fails with a different key
     */
    public void testDecryptionFailWithOtherKey() {
        byte[] key = AES.generateAES256Key();
        byte[] key2 = AES.generateAES256Key();
        byte[] message = new byte[128];
        new Random().nextBytes(message);
        byte[] ciphertext = AES.encryptAES(message, key);
        try {
            byte[] cleartext = AES.decryptAES(ciphertext, key2);
            assertNull("Decryption did not fail with incorrect key", cleartext);
        } catch (BadPaddingException e) {
            assertTrue("No exception was raised", true);
        }
    }

    /**
     * Test if the decryption raises an exception if the _message_ was changed
     */
    public void testDecryptionFailWithChangedMessage() {
        byte[] key = AES.generateAES256Key();
        byte[] message = new byte[128];
        new Random().nextBytes(message);
        byte[] ciphertext = AES.encryptAES(message, key);
        try {
            ciphertext[23] = (byte) ((int) ciphertext[23] ^ 1);
            AES.decryptAES(ciphertext, key);
            assertTrue("No exception was raised during decryption", false);
        } catch (BadPaddingException e) {
            assertTrue(true);
        }
    }

    /**
     * Test if the decryption raises an exception if the _IV_ was changed
     */
    public void testDecryptionFailWithChangedIV() {
        byte[] key = AES.generateAES256Key();
        byte[] message = new byte[128];
        new Random().nextBytes(message);
        byte[] ciphertext = AES.encryptAES(message, key);
        try {
            ciphertext[1] = (byte) ((int) ciphertext[1] ^ 1);
            AES.decryptAES(ciphertext, key);
            assertTrue("No exception was raised during decryption", false);
        } catch (BadPaddingException e) {
            assertTrue(true);
        }
    }


    /**
     * Test if the encryption and decryption works when explicitly specifying an IV
     */
    public void testEncryptionDecryptionWithSpecifiedIV() {
        byte[] key = AES.generateAES256Key();
        byte[] message = new byte[128];
        byte[] iv = new byte[32];
        new Random().nextBytes(message);
        new Random().nextBytes(iv);
        byte[] ciphertext = AES.encryptAES(message, key, null, iv);
        try {
            byte[] plaintext = AES.decryptAES(ciphertext, key, null, iv);
            assertTrue(Arrays.equals(plaintext, message));
        } catch (BadPaddingException e) {
            fail(e.toString());
        }
    }

    /**
     * Test if the encryption and decryption works when explicitly specifying an IV
     */
    public void testEncryptionDecryptionWithSpecifiedBadIV() {
        byte[] key = AES.generateAES256Key();
        byte[] message = new byte[128];
        byte[] iv = new byte[31];
        new Random().nextBytes(message);
        new Random().nextBytes(iv);
        byte[] ciphertext = AES.encryptAES(message, key, null, iv);
        assertNull(ciphertext);
    }
}
