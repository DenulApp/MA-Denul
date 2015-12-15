package de.velcommuta.denul.crypto;

import android.location.Location;

import junit.framework.TestCase;

import org.joda.time.Instant;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import de.velcommuta.denul.data.DataBlock;
import de.velcommuta.denul.data.GPSTrack;
import de.velcommuta.denul.data.KeySet;
import de.velcommuta.denul.data.Shareable;
import de.velcommuta.denul.data.TokenPair;

/**
 * SHA256SharingEncryption test cases
 * TODO Add negative test cases
 * TODO Test if it correctly applies the friend ID found in the DataBlock
 */
public class AESSharingEncryptionTest extends TestCase {
    /**
     * Test case for generic, correct usage.
     * Code partially adapted from {@link SHA256IdentifierDerivationTest}
     */
    public void testAESSharingEncryption() {
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

        // Test AESSharingEncryption implementation
        // Prepare GPSTrack object
        List<Location> loclist = new LinkedList<>();
        for (double i = 0; i < 1; i = i + 0.2) {
            Location loc = new Location("");
            loc.setLatitude(i);
            loc.setLongitude(i);
            loc.setTime((long) i + 100);
            loclist.add(loc);
        }
        String name = "test";
        int mode = GPSTrack.VALUE_RUNNING;
        GPSTrack testtrack = new GPSTrack(loclist, name, mode, new Instant().getMillis(), new Instant().getMillis(), "Europe/Berlin");
        // Get a derivation instance
        IdentifierDerivation d = new SHA256IdentifierDerivation();
        // Get a AESSharingEncryption instance
        AESSharingEncryption enc = new AESSharingEncryption();
        // Encrypt Shareable with random identifier
        TokenPair rand = d.generateRandomIdentifier();
        DataBlock block = enc.encryptShareable(testtrack, rand);
        // Encrypt identifier and key of the block for the second keyset
        byte[] encrypted = enc.encryptKeysAndIdentifier(block, ks1);
        // Decrypt identifier and key of the block using the second keyset
        DataBlock decryptedBlock = enc.decryptKeysAndIdentifier(encrypted, ks2);
        // Verify that the blocks match
        assertTrue(Arrays.equals(decryptedBlock.getIdentifier(), block.getIdentifier()));
        assertTrue(Arrays.equals(decryptedBlock.getKey(), block.getKey()));
        // Copy over the ciphertext (to simulate retrieval from the server)
        decryptedBlock.setCiphertext(block.getCiphertext());
        // Decrypt the decryptedBlock
        Shareable sh = enc.decryptShareable(decryptedBlock);
        // Ensure it is a GPSTrack
        assertTrue(sh instanceof GPSTrack);
        // Cast to GPSTrack
        GPSTrack track = (GPSTrack) sh;
        // Compare with original
        assertTrue(track.equals(testtrack));
    }
}
