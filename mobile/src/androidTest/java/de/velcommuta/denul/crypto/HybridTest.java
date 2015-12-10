package de.velcommuta.denul.crypto;

import junit.framework.TestCase;

import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.Arrays;
import java.util.Random;

import javax.crypto.BadPaddingException;

/**
 * Test cases for the hybrid encryption and decryption
 */
public class HybridTest extends TestCase {
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
