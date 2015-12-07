package de.velcommuta.denul.crypto;

import junit.framework.TestCase;

import java.util.Arrays;
import java.util.Random;

/**
 * Test cases for the ECDH Key Exchange implementation
 */
public class KeySetTest extends TestCase {
    /**
     * Test a valid keyset initialization
     */
    public void testValidKeySet() {
        byte[] key1 = new byte[32];
        byte[] key2 = new byte[32];
        byte[] ctr1 = new byte[32];
        byte[] ctr2 = new byte[32];
        new Random().nextBytes(key1);
        new Random().nextBytes(key2);
        new Random().nextBytes(ctr1);
        new Random().nextBytes(ctr2);
        KeySet ks1 = new KeySet(key1, key2, ctr1, ctr2, true);
        KeySet ks2 = new KeySet(key2, key1, ctr2, ctr1, false);
        assertNotNull(ks1);
        assertNotNull(ks2);
        assertEquals(ks1.fingerprint(), ks2.fingerprint());
    }


    /**
     * Test initialization of KeySet with bad lengths
     */
    public void testInvalidKeySetBadKey() {
        byte[] key1 = new byte[31];
        byte[] key2 = new byte[32];
        byte[] ctr1 = new byte[32];
        byte[] ctr2 = new byte[32];
        new Random().nextBytes(key1);
        new Random().nextBytes(key2);
        new Random().nextBytes(ctr1);
        new Random().nextBytes(ctr2);
        try {
            new KeySet(key1, key2, ctr1, ctr2, true);
            fail();
        } catch (IllegalArgumentException e) {
            assertTrue(true);
        }
        key1 = new byte[33];
        new Random().nextBytes(key1);
        try {
            new KeySet(key1, key2, ctr1, ctr2, true);
            fail();
        } catch (IllegalArgumentException e) {
            assertTrue(true);
        }
    }


    /**
     * test KeySet Initialization with bad length for the counters
     */
    public void testInvalidKeySetBadCtr() {
        byte[] key1 = new byte[32];
        byte[] key2 = new byte[32];
        byte[] ctr1 = new byte[31];
        byte[] ctr2 = new byte[32];
        new Random().nextBytes(key1);
        new Random().nextBytes(key2);
        new Random().nextBytes(ctr1);
        new Random().nextBytes(ctr2);
        try {
            new KeySet(key1, key2, ctr1, ctr2, true);
            fail();
        } catch (IllegalArgumentException e) {
            assertTrue(true);
        }
        ctr1 = new byte[33];
        new Random().nextBytes(ctr1);
        try {
            new KeySet(key1, key2, ctr1, ctr2, true);
            fail();
        } catch (IllegalArgumentException e) {
            assertTrue(true);
        }
    }
}
