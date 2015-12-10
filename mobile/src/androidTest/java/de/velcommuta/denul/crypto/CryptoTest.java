package de.velcommuta.denul.crypto;

import junit.framework.TestCase;

import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.Arrays;
import java.util.Random;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;

/**
 * Test class for Crypto functions
 */
@SuppressWarnings("ConstantConditions")
public class CryptoTest extends TestCase {
    // TODO Split into multiple files
    ///// Encoding and decoding tests
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

    ///// AES Tests
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

    ///// RSA tests
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

    ///// Hybrid crypto tests
    /**
     * Test the (protected) header generation function for asymmetric encryption
     */
    public void testHeaderGeneration() {
        byte[] test1 = {0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00};
        byte[] test2 = {0x00, 0x00, 0x00, 0x00, 0x00, 0x01, 0x00, 0x00, 0x00, 0x10};
        byte[] test3 = {0x00, 0x00, 0x00, 0x00, 0x00, 0x20, 0x00, 0x00, 0x01, 0x00};
        byte[] header1 = Hybrid.generateHeader(Hybrid.VERSION_1, Hybrid.ALGO_RSA_OAEP_SHA256_MGF1_WITH_AES_256_GCM, 0, 0);
        byte[] header2 = Hybrid.generateHeader(Hybrid.VERSION_1, Hybrid.ALGO_RSA_OAEP_SHA256_MGF1_WITH_AES_256_GCM, 16, 1);
        byte[] header3 = Hybrid.generateHeader(Hybrid.VERSION_1, Hybrid.ALGO_RSA_OAEP_SHA256_MGF1_WITH_AES_256_GCM, 256, 32);
        assertTrue("Header not as expected in test case 1", Arrays.equals(test1, header1));
        assertTrue("Header not as expected in test case 2", Arrays.equals(test2, header2));
        assertTrue("Header not as expected in test case 3", Arrays.equals(test3, header3));
    }

    /**
     * Test if the header length is parsed correctly
     */
    public void testHeaderLengthParsing() {
        byte[] header1 = Hybrid.generateHeader(Hybrid.VERSION_1, Hybrid.ALGO_RSA_OAEP_SHA256_MGF1_WITH_AES_256_GCM, 0, 9001);
        byte[] header2 = Hybrid.generateHeader(Hybrid.VERSION_1, Hybrid.ALGO_RSA_OAEP_SHA256_MGF1_WITH_AES_256_GCM, 16, 9002);
        byte[] header3 = Hybrid.generateHeader(Hybrid.VERSION_1, Hybrid.ALGO_RSA_OAEP_SHA256_MGF1_WITH_AES_256_GCM, 256, 9003);
        try {
            assertEquals("Incorrect length parsed in test case 1", Hybrid.parseAsymCiphertextLength(header1), 0);
            assertEquals("Incorrect length parsed in test case 2", Hybrid.parseAsymCiphertextLength(header2), 16);
            assertEquals("Incorrect length parsed in test case 3", Hybrid.parseAsymCiphertextLength(header3), 256);
        } catch (BadPaddingException e) {
            assertFalse("Exception occured during header parsing", true);
        }
    }

    /**
     * Test if the header length is throwing exceptions on bad data
     */
    public void testHeaderLengthParsingFailOnBadData() {
        byte[] test1 = {0x00, 0x00, 0x00, 0x00, 0x00};
        try {
            Hybrid.parseAsymCiphertextLength(test1);
            assertFalse("No exception thrown on bad header length test 1", true);
        } catch (BadPaddingException e) {
            assertTrue("Exception occured during header parsing", true);
        }

        byte[] test2 = {0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00};
        try {
            Hybrid.parseAsymCiphertextLength(test2);
            assertFalse("No exception thrown on bad header length test 2", true);
        } catch (BadPaddingException e) {
            assertTrue("Exception occured during header parsing", true);
        }
    }

    /**
     * Test if hybrid encryption produces an output
     */
    public void testHybridEncryption() {
        PublicKey pkey = RSA.generateRSAKeypair(1024).getPublic();
        byte[] message = new byte[512];
        new Random().nextBytes(message);
        byte[] encrypted = Hybrid.encryptHybrid(message, pkey, 9004);
        assertNotNull("Hybrid encryption failed", encrypted);
    }

    /**
     * Test if Hybrid. encryption produces an output
     */
    public void testHybridEncryptionDecryption() {
        KeyPair pair = RSA.generateRSAKeypair(1024);
        PublicKey pkey = pair.getPublic();
        PrivateKey privkey = pair.getPrivate();
        byte[] message = new byte[512];
        new Random().nextBytes(message);
        byte[] encrypted = Hybrid.encryptHybrid(message, pkey, 9003);
        assertNotNull("Hybrid encryption failed", encrypted);
        byte[] decrypted = null;
        try {
            decrypted = Hybrid.decryptHybrid(encrypted, privkey, 9003);
        } catch (BadPaddingException e) {
            assertTrue("Decryption failed with BadPaddingException", false);
        }
        assertNotNull("Decryption resulted in null", decrypted);
        assertTrue("Message was not decrypted to the same plaintext", Arrays.equals(message, decrypted));
    }

    /**
     * Test if hybrid encryption produces an error if the header version field was modified
     */
    public void testHybridEncryptionDecryptionFailOnModifiedVersionField() {
        KeyPair pair = RSA.generateRSAKeypair(1024);
        PublicKey pkey = pair.getPublic();
        PrivateKey privkey = pair.getPrivate();
        byte[] message = new byte[512];
        new Random().nextBytes(message);
        byte[] encrypted = Hybrid.encryptHybrid(message, pkey, 9002);
        assertNotNull("Hybrid encryption failed", encrypted);
        byte[] decrypted = null;
        encrypted[0] = 0x32;
        try {
            decrypted = Hybrid.decryptHybrid(encrypted, privkey, 9002);
            assertFalse("No exception thrown", true);
        } catch (BadPaddingException e) {
            assertTrue("Decryption did not throw exception", true);
        }
        assertNull("Decryption did not result in null", decrypted);
    }


    /**
     * Test if hybrid encryption produces an error if the header algorithm field was modified
     */
    public void testHybridEncryptionDecryptionFailOnModifiedAlgorithmField() {
        KeyPair pair = RSA.generateRSAKeypair(1024);
        PublicKey pkey = pair.getPublic();
        PrivateKey privkey = pair.getPrivate();
        byte[] message = new byte[512];
        new Random().nextBytes(message);
        byte[] encrypted = Hybrid.encryptHybrid(message, pkey, 9001);
        assertNotNull("Hybrid encryption failed", encrypted);
        byte[] decrypted = null;
        encrypted[1] = 0x32;
        try {
            decrypted = Hybrid.decryptHybrid(encrypted, privkey, 9001);
            assertFalse("No exception thrown", true);
        } catch (BadPaddingException e) {
            assertTrue("Decryption did not throw exception", true);
        }
        assertNull("Decryption did not result in null", decrypted);
    }


    /**
     * Test if hybrid encryption produces an error if the header length field was modified to a much too large value
     */
    public void testHybridEncryptionDecryptionFailOnModifiedLengthFieldTooLong() {
        KeyPair pair = RSA.generateRSAKeypair(1024);
        PublicKey pkey = pair.getPublic();
        PrivateKey privkey = pair.getPrivate();
        byte[] message = new byte[512];
        new Random().nextBytes(message);
        byte[] encrypted = Hybrid.encryptHybrid(message, pkey, 9001);
        assertNotNull("Hybrid encryption failed", encrypted);
        byte[] decrypted = null;
        encrypted[7] = 0x32;
        try {
            decrypted = Hybrid.decryptHybrid(encrypted, privkey, 9001);
            assertFalse("No exception thrown", true);
        } catch (BadPaddingException e) {
            assertTrue("Decryption did not throw exception", true);
        }
        assertNull("Decryption did not result in null", decrypted);
    }


    /**
     * Test if hybrid encryption produces an error if the header length field was modified to plausible but incorrect value
     */
    public void testHybridEncryptionDecryptionFailOnModifiedLengthFieldPlausible() {
        KeyPair pair = RSA.generateRSAKeypair(1024);
        PublicKey pkey = pair.getPublic();
        PrivateKey privkey = pair.getPrivate();
        byte[] message = new byte[512];
        new Random().nextBytes(message);
        byte[] encrypted = Hybrid.encryptHybrid(message, pkey, 9001);
        assertNotNull("Hybrid encryption failed", encrypted);
        byte[] decrypted = null;
        encrypted[9] = (byte) ((int)encrypted[9] ^ 1);
        try {
            decrypted = Hybrid.decryptHybrid(encrypted, privkey, 9001);
            assertFalse("No exception thrown", true);
        } catch (BadPaddingException e) {
            assertTrue("Decryption did not throw exception", true);
        }
        assertNull("Decryption did not result in null", decrypted);
    }

    /**
     * Test if hybrid encryption produces an error if the sequence number field was modified
     */
    public void testHybridEncryptionDecryptionFailOnModifiedSequenceNumber() {
        KeyPair pair = RSA.generateRSAKeypair(1024);
        PublicKey pkey = pair.getPublic();
        PrivateKey privkey = pair.getPrivate();
        byte[] message = new byte[512];
        new Random().nextBytes(message);
        byte[] encrypted = Hybrid.encryptHybrid(message, pkey, 9001);
        assertNotNull("Hybrid encryption failed", encrypted);
        byte[] decrypted = null;
        encrypted[5] = (byte) ((int)encrypted[5] ^ 1);
        try {
            decrypted = Hybrid.decryptHybrid(encrypted, privkey, 9001);
            assertFalse("No exception thrown", true);
        } catch (BadPaddingException e) {
            assertTrue("Decryption did not throw exception", true);
        }
        assertNull("Decryption did not result in null", decrypted);
    }

    /**
     * Test if hybrid encryption produces an error if the wrong sequence number was expected
     */
    public void testHybridEncryptionDecryptionFailOnWrongExpectedSequenceNumber() {
        KeyPair pair = RSA.generateRSAKeypair(1024);
        PublicKey pkey = pair.getPublic();
        PrivateKey privkey = pair.getPrivate();
        byte[] message = new byte[512];
        new Random().nextBytes(message);
        byte[] encrypted = Hybrid.encryptHybrid(message, pkey, 9001);
        assertNotNull("Hybrid encryption failed", encrypted);
        byte[] decrypted = null;
        try {
            decrypted = Hybrid.decryptHybrid(encrypted, privkey, 9002);
            assertFalse("No exception thrown", true);
        } catch (BadPaddingException e) {
            assertTrue("Decryption did not throw exception", true);
        }
        assertNull("Decryption did not result in null", decrypted);
    }

    /**
     * Test if hybrid encryption produces an error if the seqNr is modified and the modified value is expected
     */
    public void testHybridEncryptionDecryptionFailOnWrongExpectedModifiedSequenceNumber() {
        KeyPair pair = RSA.generateRSAKeypair(1024);
        PublicKey pkey = pair.getPublic();
        PrivateKey privkey = pair.getPrivate();
        byte[] message = new byte[512];
        new Random().nextBytes(message);
        byte[] encrypted = Hybrid.encryptHybrid(message, pkey, 0);
        encrypted[5] = 0x01;
        assertNotNull("Hybrid encryption failed", encrypted);
        byte[] decrypted = null;
        try {
            decrypted = Hybrid.decryptHybrid(encrypted, privkey, 1);
            assertFalse("No exception thrown", true);
        } catch (BadPaddingException e) {
            assertTrue("Decryption did not throw exception", true);
        }
        assertNull("Decryption did not result in null", decrypted);
    }


    /**
     * Test if hybrid encryption produces an error if the seqNr is modified and the modified value is expected
     */
    public void testHybridEncryptionDecryptionSuccessOnNonVerifiedSequenceNumber() {
        KeyPair pair = RSA.generateRSAKeypair(1024);
        PublicKey pkey = pair.getPublic();
        PrivateKey privkey = pair.getPrivate();
        byte[] message = new byte[512];
        new Random().nextBytes(message);
        byte[] encrypted = Hybrid.encryptHybrid(message, pkey, 1337);
        encrypted[5] = 0x01;
        assertNotNull("Hybrid encryption failed", encrypted);
        byte[] decrypted = null;
        try {
            decrypted = Hybrid.decryptHybrid(encrypted, privkey, -1);
            assertFalse("No exception thrown", true);
        } catch (BadPaddingException e) {
            assertTrue("Decryption did not throw exception", true);
        }
        assertNull("Decryption did not result in null", decrypted);
    }

    ///// Full combination tests
    /**
     * Test the full combination of encoding, decoding, and hybrid encryption
     */
    public void testFullCombination() {
        // Create a message
        byte[] message = new byte[512];
        new Random().nextBytes(message);
        // Generate keypair
        KeyPair kp = RSA.generateRSAKeypair(1024);
        // Encode to string
        String pk_enc = RSA.encodeKey(kp.getPublic());
        String pr_enc = RSA.encodeKey(kp.getPrivate());
        // Decode pubkey
        PublicKey pk = RSA.decodePublicKey(pk_enc);
        // Encrypt the message
        byte[] ciphertext = Hybrid.encryptHybrid(message, pk, 9001);
        // Decrypt the ciphertext
        byte[] decoded = null;
        try {
            PrivateKey pr = RSA.decodePrivateKey(pr_enc);
            decoded = Hybrid.decryptHybrid(ciphertext, pr, 9001);
        } catch (BadPaddingException e) {
            assertFalse("Error during decryption", true);
        }
        assertTrue("Decrypted text does not match", Arrays.equals(message, decoded));
    }
}