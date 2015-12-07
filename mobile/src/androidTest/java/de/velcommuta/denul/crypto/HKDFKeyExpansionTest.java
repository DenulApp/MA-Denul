package de.velcommuta.denul.crypto;

import junit.framework.TestCase;

import java.util.Arrays;

import de.velcommuta.denul.data.KeySet;

/**
 * Test cases for the HKDFKeyExpansion algorithm
 */
public class HKDFKeyExpansionTest extends TestCase {
    /**
     * Perform a full key exchange and expand the generated key using the HKDFKeyExpansion
     */
    public void testKeyExpansion() {
        // First, we need to perform a valid key exchange to get a shared secret
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

        // Now, key1 is a shared secret (as it is identical with key2). Use key expansion.
        KeyExpansion kexp1 = new HKDFKeyExpansion(key1);
        KeyExpansion kexp2 = new HKDFKeyExpansion(key2);
        KeySet ks1 = kexp1.expand(true);
        KeySet ks2 = kexp2.expand(false);
        // Make sure the keys are valid and match
        assertTrue(Arrays.equals(ks1.getInboundKey(), ks2.getOutboundKey()));
        assertTrue(Arrays.equals(ks2.getInboundKey(), ks1.getOutboundKey()));
        assertTrue(Arrays.equals(ks1.getInboundCtr(), ks2.getOutboundCtr()));
        assertTrue(Arrays.equals(ks2.getInboundCtr(), ks1.getOutboundCtr()));
        assertEquals(ks1.fingerprint(), ks2.fingerprint());
    }
}
