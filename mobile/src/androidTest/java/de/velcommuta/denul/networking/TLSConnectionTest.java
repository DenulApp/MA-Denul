package de.velcommuta.denul.networking;

import junit.framework.TestCase;

/**
 * Test class for the TLSConnection class
 */
public class TLSConnectionTest extends TestCase {
    /**
     * Test if connecting to a known-good target with a valid key works
     */
    public void testConnectionSuccessOnGoodCert() {
        try {
            Connection c = new TLSConnection("google.com", 443);
            c.close();
        } catch (Exception e) {
            fail("Exception thrown where none was expected: " + e);
        }
    }
}
