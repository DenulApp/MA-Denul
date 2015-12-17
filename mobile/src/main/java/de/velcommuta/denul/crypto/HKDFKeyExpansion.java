package de.velcommuta.denul.crypto;

import org.spongycastle.crypto.Digest;
import org.spongycastle.crypto.digests.SHA256Digest;
import org.spongycastle.crypto.generators.HKDFBytesGenerator;
import org.spongycastle.crypto.params.HKDFParameters;

import java.security.Security;

import de.velcommuta.denul.data.KeySet;

/**
 * {@link KeyExpansion} based on the HKDF function defined in RFC 5869.
 */
public class HKDFKeyExpansion implements KeyExpansion {
    // Insert BouncyCastle provider
    static {
        Security.insertProviderAt(new org.spongycastle.jce.provider.BouncyCastleProvider(), 1);
    }

    private HKDFBytesGenerator mGenerator;

    /**
     * Constructor for the HKDFKeyExpansion.
     * @param secret The secret bytes to base everything on
     */
    public HKDFKeyExpansion(byte[] secret) {
        // Use SHA256 as the hash function
        Digest digest = new SHA256Digest();
        // Initialize the Generator with the digest function
        mGenerator = new HKDFBytesGenerator(digest);
        // Initialize Generator with HDKF parameters
        // TODO Populate IV and INFO fields with values from the connection?
        mGenerator.init(new HKDFParameters(secret, null, null));
    }

    @Override
    public KeySet expand(boolean isInitiatingParty) {
        byte[] key1 = new byte[32];
        byte[] key2 = new byte[32];
        byte[] ctr1 = new byte[32];
        byte[] ctr2 = new byte[32];
        mGenerator.generateBytes(key1, 0, 32);
        mGenerator.generateBytes(key2, 0, 32);
        mGenerator.generateBytes(ctr1, 0, 32);
        mGenerator.generateBytes(ctr2, 0, 32);
        if (isInitiatingParty) {
            return new KeySet(key1, key2, ctr1, ctr2, true);
        } else {
            return new KeySet(key2, key1, ctr2, ctr1, false);
        }
    }
}
