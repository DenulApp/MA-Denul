package de.velcommuta.denul.crypto;

import junit.framework.TestCase;

import java.util.Arrays;

/**
 * Test cases for the ECDH Key Exchange implementation
 */
public class ECDHKeyExchangeTest extends TestCase {
    /**
     * Perform a full key exchange, using only valid data
     */
    public void testValidKex() {
        // initialize two kex instances
        KeyExchange kex1 = new ECDHKeyExchange();
        KeyExchange kex2 = new ECDHKeyExchange();
        // Get the public messages
        byte[] kex1to2 = kex1.getPublicKexData();
        byte[] kex2to1 = kex2.getPublicKexData();
        // Ensure that the public keys differ
        assertFalse(Arrays.equals(kex1to2, kex2to1));
        // Pass the messages to the other kex
        assertTrue(kex1.putPartnerKexData(kex2to1));
        assertTrue(kex2.putPartnerKexData(kex1to2));
        // Retrieve generated keys
        byte[] key1 = kex1.getAgreedKey();
        byte[] key2 = kex2.getAgreedKey();
        // Ensure the keys match
        assertTrue(Arrays.equals(key1, key2));
    }


    /**
     * Test what happens if bad data is passed to the putPartnerKexData function
     */
    public void testInvalidKeyData() {
        // initialize two kex instances
        KeyExchange kex1 = new ECDHKeyExchange();
        // Get the public messages
        byte[] kex1to2 = {0x00, 0x01};
        // Pass the messages to the other kex
        assertFalse(kex1.putPartnerKexData(kex1to2));
        assertNull(kex1.getAgreedKey());
    }


    /**
     * Test what happens if we try to insert data twice
     */
    public void testDoubleDataInsert() {
        // initialize two kex instances
        KeyExchange kex1 = new ECDHKeyExchange();
        KeyExchange kex2 = new ECDHKeyExchange();
        // Get the public messages
        byte[] kex1to2 = kex1.getPublicKexData();
        byte[] kex2to1 = kex2.getPublicKexData();
        // Pass the messages to the other kex
        assertTrue(kex1.putPartnerKexData(kex2to1));
        assertTrue(kex2.putPartnerKexData(kex1to2));
        assertFalse(kex2.putPartnerKexData(kex1to2));
        // Retrieve generated keys
        byte[] key1 = kex1.getAgreedKey();
        byte[] key2 = kex2.getAgreedKey();
        // Ensure the keys match
        assertTrue(Arrays.equals(key1, key2));
    }


    /**
     * Test Man-in-the-Middle during key generation
     */
    public void testMitmKeyDerivation() {
        // initialize three kex instances
        KeyExchange kex1 = new ECDHKeyExchange();
        KeyExchange kex2 = new ECDHKeyExchange();
        KeyExchange kex3 = new ECDHKeyExchange();
        KeyExchange kex4 = new ECDHKeyExchange();
        // Get the public messages
        byte[] kex1to2 = kex1.getPublicKexData();
        byte[] kex2to1 = kex2.getPublicKexData();
        byte[] kex3to4 = kex3.getPublicKexData();
        byte[] kex4to3 = kex4.getPublicKexData();
        // Pass the messages to the other kex
        assertTrue(kex1.putPartnerKexData(kex2to1));
        assertTrue(kex2.putPartnerKexData(kex1to2));
        assertTrue(kex3.putPartnerKexData(kex4to3));
        assertTrue(kex4.putPartnerKexData(kex3to4));
        // Retrieve generated keys
        byte[] key1 = kex1.getAgreedKey();
        byte[] key2 = kex2.getAgreedKey();
        byte[] key3 = kex3.getAgreedKey();
        byte[] key4 = kex4.getAgreedKey();
        // Ensure the generated keys match
        assertTrue(Arrays.equals(key1, key2));
        assertTrue(Arrays.equals(key3, key4));
        // Ensure that the unrelated keys do not match
        assertFalse(Arrays.equals(key1, key3));
    }
}
