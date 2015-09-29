package de.velcommuta.denul.util;

import junit.framework.TestCase;

import java.security.KeyPair;
import java.util.Random;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;

/**
 * Test class for Crypto functions
 */
public class CryptoTest extends TestCase {
    ///// AES Tests
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

    ///// RSA tests
    /**
     * Test if the RSA Key generation works
     */
    public void testRsaGen() {
        KeyPair kp = Crypto.generateRSAKeypair(1024);
        assertNotNull("Keypair was null!", kp);
    }

    /**
     * Test if the function correctly refuses to generated weird key sizes
     */
    public void testRsaGenIncorrectBit() {
        KeyPair kp = Crypto.generateRSAKeypair(1025);
        assertNull("Incorrect bit size was accepted.", kp);
    }

    /**
     * Test if the function correctly encrypts data
     */
    public void testRsaEncryption() {
        KeyPair kp = Crypto.generateRSAKeypair(1024);
        byte[] message = new byte[5];
        new Random().nextBytes(message);
        try {
            byte[] ciphertext = Crypto.encryptRSA(message, kp.getPublic());
            assertNotNull("No ciphertext generated even though it should have been", ciphertext);
        } catch (IllegalBlockSizeException e) {
            assertFalse("Illegal block size even though it should be legal", true);
        }
    }

    /**
     * Test if the function correctly refuses to encrypt too large pieces of data
     */
    public void testRsaEncryptionFailOnTooLarge() {
        KeyPair kp = Crypto.generateRSAKeypair(1024);
        byte[] message = new byte[256];
        new Random().nextBytes(message);
        byte[] ciphertext = null;
        try {
            ciphertext = Crypto.encryptRSA(message, kp.getPublic());
        } catch (IllegalBlockSizeException e) {
            assertTrue("Illegal block size accepted", true);
        }
        assertNull("Ciphertext generated even though it should not have been", ciphertext);
    }

    /**
     * Test if correctly encrypted data is correctly decrypted
     */
    public void testRsaDecryption() {
        KeyPair kp = Crypto.generateRSAKeypair(1024);
        byte[] message = new byte[5];
        new Random().nextBytes(message);
        try {
            byte[] ciphertext = Crypto.encryptRSA(message, kp.getPublic());
            byte[] plaintext = Crypto.decryptRSA(ciphertext, kp.getPrivate());
            assertEquals("Decryption does not equal plaintext", new String(message), new String(plaintext));
        } catch (IllegalBlockSizeException e) {
            assertFalse("Illegal block size even though it should be legal", true);
        } catch (BadPaddingException e) {
            assertFalse("Illegal padding detected even though it should be legal", true);
        }
    }

    /**
     * Test if modified encrypted data is correctly rejected
     */
    public void testRsaDecryptionFailOnModifiedData() {
        KeyPair kp = Crypto.generateRSAKeypair(1024);
        byte[] message = new byte[5];
        new Random().nextBytes(message);
        byte[] plaintext = null;
        try {
            byte[] ciphertext = Crypto.encryptRSA(message, kp.getPublic());
            ciphertext[12] = 0x00;
            plaintext = Crypto.decryptRSA(ciphertext, kp.getPrivate());
            assertFalse("No exception thrown", true);
        } catch (IllegalBlockSizeException e) {
            assertFalse("Illegal block size even though it should be legal", true);
        } catch (BadPaddingException e) {
            assertTrue("Modified data accepted", true);
        }
        assertNull("Plaintext not null", plaintext);
    }
}