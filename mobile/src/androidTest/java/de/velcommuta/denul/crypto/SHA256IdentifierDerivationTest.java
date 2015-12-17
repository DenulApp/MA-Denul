package de.velcommuta.denul.crypto;

import android.os.Message;
import android.widget.ArrayAdapter;

import junit.framework.TestCase;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

import de.velcommuta.denul.data.KeySet;
import de.velcommuta.denul.data.TokenPair;

/**
 * Identifier derivation test
 */
public class SHA256IdentifierDerivationTest extends TestCase {
    /**
     * Test case for generic, correct usage.
     * Code partially adapted from {@link HKDFKeyExpansionTest#testKeyExpansion()}
     * @throws NoSuchAlgorithmException Never, because SHA256 is supported.
     */
    public void testSHA256Derivation() throws NoSuchAlgorithmException {
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

        // Derive a number of identifiers and ensure they match
        // Get a derivation instance
        IdentifierDerivation d = new SHA256IdentifierDerivation();
        // Get a message digest instance
        MessageDigest md = MessageDigest.getInstance("SHA256");
        // Derive ten identifiers
        for (int i = 0; i < 10; i++) {
            // Derive the identifiers in one direction
            TokenPair token_in = d.generateInboundIdentifier(ks1);
            TokenPair token_out = d.generateOutboundIdentifier(ks2);
            // Compare the Identifiers and revocation tokens
            assertTrue(Arrays.equals(token_in.getIdentifier(), token_out.getIdentifier()));
            assertTrue(Arrays.equals(token_in.getRevocation(), token_out.getRevocation()));
            // Make sure the token hashes to the identifier
            md.update(token_in.getRevocation());
            assertTrue(Arrays.equals(md.digest(), token_in.getIdentifier()));
            // Increment counters
            d.notifyInboundIdentifierUsed(ks1);
            d.notifyOutboundIdentifierUsed(ks2);

            // Derive the identifiers in the other direction
            token_in = d.generateInboundIdentifier(ks2);
            token_out = d.generateOutboundIdentifier(ks1);
            assertTrue(Arrays.equals(token_in.getIdentifier(), token_out.getIdentifier()));
            assertTrue(Arrays.equals(token_in.getRevocation(), token_out.getRevocation()));
            md.update(token_in.getRevocation());
            assertTrue(Arrays.equals(md.digest(), token_in.getIdentifier()));
            d.notifyInboundIdentifierUsed(ks2);
            d.notifyOutboundIdentifierUsed(ks1);
        }
    }


    /**
     * Test the generation of arbitrary identifiers (that are not related to any specific contact)
     * @throws NoSuchAlgorithmException Never, because SHA256 is supported
     */
    public void testArbitraryIdentifierGeneration() throws NoSuchAlgorithmException {
        // Get derivation instance
        IdentifierDerivation d = new SHA256IdentifierDerivation();
        // get random TokenPair
        TokenPair p = d.generateRandomIdentifier();
        // Get SHA256 Digest instance
        MessageDigest md = MessageDigest.getInstance("SHA256");
        // Check that revocation token hashes to the identifier
        md.update(p.getRevocation());
        assertTrue(Arrays.equals(md.digest(), p.getIdentifier()));
    }
}
