package de.velcommuta.denul.util;

import android.util.Base64;
import android.util.Log;

import org.spongycastle.jce.provider.BouncyCastleProvider;

import java.security.Key;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Security;
import java.security.spec.X509EncodedKeySpec;

/**
 * Wrapper for the horrible BouncyCastle / SpongyCastle API
 */
public class Crypto {
    public static final String TAG = "Crypto";

    // Insert provider
    static {
        Security.insertProviderAt(new org.spongycastle.jce.provider.BouncyCastleProvider(), 1);
    }

    ///// Key encoding and decoding
    /**
     * Encode a key (public or private) into a base64 String
     * @param key The Key to encode
     * @return Base64-encoded key
     */
    public static String encodeKey(Key key) {
        return new String(Base64.encode(key.getEncoded(), 0, key.getEncoded().length, Base64.NO_WRAP));
    }

    /**
     * Decode a private key encoded with encodeKey
     * @param encoded The base64-encoded private key
     * @return The decoded private key
     */
    public static PrivateKey decodePrivateKey(String encoded) {
        try {
            KeyFactory kFactory = KeyFactory.getInstance("RSA", new BouncyCastleProvider());
            byte[] keybytes = Base64.decode(encoded, Base64.NO_WRAP);
            X509EncodedKeySpec keySpec = new X509EncodedKeySpec(keybytes);
            return kFactory.generatePrivate(keySpec);
        } catch (Exception e) {
            Log.e(TAG, "decodePrivateKey: Error decoding private key: ", e);
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Decode a public key encoded with encodeKey
     * @param encoded The base64-encoded public key
     * @return The decoded public key
     */
    public static PublicKey decodePublicKey(String encoded) {
        try {
            KeyFactory kFactory = KeyFactory.getInstance("RSA", new BouncyCastleProvider());
            byte[] keybytes = Base64.decode(encoded, Base64.NO_WRAP);
            X509EncodedKeySpec keySpec = new X509EncodedKeySpec(keybytes);
            return kFactory.generatePublic(keySpec);
        } catch (Exception e) {
            Log.e(TAG, "decodePublicKey: Error decoding public key: ", e);
            e.printStackTrace();
            return null;
        }
    }

    ///// Key generation
    /**
     * Generate an RSA keypair with the specified bitstrength
     * @param bitstrength An integer giving the bit strength of the generated key pair. Should be
     *                    one of 1024, 2048, 3072, 4096.
     * @return The generated KeyPair object, or null if an error occured
     */
    public static KeyPair generateRSAKeypair(int bitstrength) {
        try {
            // Get KeyPairGenerator for RSA, using the SpongyCastle provider
            KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA", "SC");
            // Initialize with target key size
            keyGen.initialize(bitstrength);
            // Generate the keys
            return keyGen.generateKeyPair();
        } catch (Exception e) {
            Log.e(TAG, "generateRSAKeypair: Keypair generation failed: ", e);
            e.printStackTrace();
            return null;
        }
    }
}
