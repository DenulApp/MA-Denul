package de.velcommuta.denul.crypto;

import junit.framework.TestCase;

import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.Random;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;

/**
 * Testcases for the RSA implementation
 */
public class RSATest extends TestCase {
    /**
     * Test if the key encoding and decoding functions work
     */
    public void testKeypairEncodingDecoding() {
        KeyPair kp = RSA.generateRSAKeypair(1024);
        String pubkey_enc = RSA.encodeKey(kp.getPublic());
        String privkey_enc = RSA.encodeKey(kp.getPrivate());
        PublicKey pubkey = RSA.decodePublicKey(pubkey_enc);
        PrivateKey privkey = RSA.decodePrivateKey(privkey_enc);
        assertNotNull("pubkey was null", pubkey);
        assertNotNull("privkey was null", privkey);
        assertEquals("Public keys do not match", pubkey, kp.getPublic());
        assertEquals("Private Keys do not match", privkey, kp.getPrivate());
    }

    /**
     * Test if the RSA Key generation works
     */
    public void testRsaGen() {
        KeyPair kp = RSA.generateRSAKeypair(1024);
        assertNotNull("Keypair was null!", kp);
    }

    /**
     * Test if the function correctly refuses to generated weird key sizes
     */
    public void testRsaGenIncorrectBit() {
        KeyPair kp = RSA.generateRSAKeypair(1025);
        assertNull("Incorrect bit size was accepted.", kp);
    }

    /**
     * Test if the function correctly encrypts data
     */
    public void testRsaEncryption() {
        KeyPair kp = RSA.generateRSAKeypair(1024);
        byte[] message = new byte[5];
        new Random().nextBytes(message);
        try {
            byte[] ciphertext = RSA.encryptRSA(message, kp.getPublic());
            assertNotNull("No ciphertext generated even though it should have been", ciphertext);
        } catch (IllegalBlockSizeException e) {
            assertFalse("Illegal block size even though it should be legal", true);
        }
    }

    /**
     * Test if the function correctly refuses to encrypt too large pieces of data
     */
    public void testRsaEncryptionFailOnTooLarge() {
        KeyPair kp = RSA.generateRSAKeypair(1024);
        byte[] message = new byte[256];
        new Random().nextBytes(message);
        byte[] ciphertext = null;
        try {
            ciphertext = RSA.encryptRSA(message, kp.getPublic());
        } catch (IllegalBlockSizeException e) {
            assertTrue("Illegal block size accepted", true);
        }
        assertNull("Ciphertext generated even though it should not have been", ciphertext);
    }

    /**
     * Test if correctly encrypted data is correctly decrypted
     */
    public void testRsaDecryption() {
        KeyPair kp = RSA.generateRSAKeypair(1024);
        byte[] message = new byte[5];
        new Random().nextBytes(message);
        try {
            byte[] ciphertext = RSA.encryptRSA(message, kp.getPublic());
            byte[] plaintext = RSA.decryptRSA(ciphertext, kp.getPrivate());
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
        KeyPair kp = RSA.generateRSAKeypair(1024);
        byte[] message = new byte[5];
        new Random().nextBytes(message);
        byte[] plaintext = null;
        try {
            byte[] ciphertext = RSA.encryptRSA(message, kp.getPublic());
            ciphertext[12] = (byte) ((int)ciphertext[12] ^ 1);
            plaintext = RSA.decryptRSA(ciphertext, kp.getPrivate());
            assertFalse("No exception thrown", true);
        } catch (IllegalBlockSizeException e) {
            assertFalse("Illegal block size even though it should be legal", true);
        } catch (BadPaddingException e) {
            assertTrue("Modified data accepted", true);
        }
        assertNull("Plaintext not null", plaintext);
    }
}
