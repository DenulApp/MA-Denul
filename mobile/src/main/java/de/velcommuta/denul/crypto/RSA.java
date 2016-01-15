package de.velcommuta.denul.crypto;

import android.util.Base64;
import android.util.Log;

import org.spongycastle.jce.provider.BouncyCastleProvider;

import java.security.InvalidKeyException;
import java.security.Key;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.Security;
import java.security.Signature;
import java.security.SignatureException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

import de.velcommuta.denul.util.FormatHelper;

/**
 * RSA Cryptography
 */
public class RSA {
    // Logging Tag
    private static final String TAG = "RSA";
    
    // Insert provider
    static {
        Security.insertProviderAt(new org.spongycastle.jce.provider.BouncyCastleProvider(), 1);
    }

    ///// Key Generation
    /**
     * Generate an RSA keypair with the specified bitstrength
     * @param bitstrength An integer giving the bit strength of the generated key pair. Should be
     *                    one of 1024, 2048, 3072, 4096.
     * @return The generated KeyPair object, or null if an error occured
     */
    public static KeyPair generateRSAKeypair(int bitstrength) {
        if (bitstrength != 1024 && bitstrength != 2048 && bitstrength != 3072 && bitstrength != 4096) {
            Log.e(TAG, "generateRSAKeypair: Incorrect bitstrength: " + bitstrength);
            return null;
        }
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

    ///// Signatures

    /**
     * Sign data with a private RSA key, using PKCS1
     * @param data The data to sign
     * @param privateKey The private key to use
     * @return The signature, as a byte[], or null if an error occured
     * Based on http://www.java2s.com/Tutorial/Java/0490__Security/RSASignatureGeneration.htm
     */
    public static byte[] sign(byte[] data, PrivateKey privateKey) {
        try {
            Signature sig = Signature.getInstance("SHA256withRSA", "BC");
            sig.initSign(privateKey, new SecureRandom());
            sig.update(data);
            return sig.sign();
        } catch (NoSuchAlgorithmException | SignatureException | InvalidKeyException | NoSuchProviderException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Verify a signature over a piece of data using a public key
     * @param data The data to verify
     * @param signature The signature bytes
     * @param pubkey The public key
     * @return True if the signature is valid, false otherwise
     * Based on http://www.java2s.com/Tutorial/Java/0490__Security/RSASignatureGeneration.htm
     */
    public static boolean verify(byte[] data, byte[] signature, PublicKey pubkey) {
        try {
            Signature sig = Signature.getInstance("SHA256withRSA", "BC");
            sig.initVerify(pubkey);
            sig.update(data);
            return sig.verify(signature);
        } catch (NoSuchAlgorithmException | NoSuchProviderException | InvalidKeyException | SignatureException e) {
            e.printStackTrace();
        }
        return false;
    }


    ///// Key Encoding / Decoding
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
            PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(keybytes);
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


    /**
     * Decode the byte-encoded public key data into a PublicKey object
     * @param encoded The byte-encoded public key
     * @return The PublicKey
     */
    public static PublicKey decodePublicKey(byte[] encoded) {
        try {
            KeyFactory kFactory = KeyFactory.getInstance("RSA", new BouncyCastleProvider());
            X509EncodedKeySpec keySpec = new X509EncodedKeySpec(encoded);
            return kFactory.generatePublic(keySpec);
        } catch (Exception e) {
            Log.e(TAG, "decodePublicKey: Error decoding public key: ", e);
            e.printStackTrace();
            return null;
        }
    }


    ///// Encryption
    /**
     * Encrypt a piece of data using RSA public key encryption
     * @param data The data to be encrypted
     * @param pubkey The Public Key to use
     * @return The encrypted data
     * @throws IllegalBlockSizeException If the data is too long for the provided public key
     */
    public static byte[] encryptRSA(byte[] data, PublicKey pubkey) throws IllegalBlockSizeException {
        try {
            // Get Cipher instance
            Cipher rsaCipher = Cipher.getInstance("RSA/NONE/OAEPWithSHA256AndMGF1Padding", "SC");
            // Initialize cipher
            rsaCipher.init(Cipher.ENCRYPT_MODE, pubkey);
            // Return the encrypted data
            return rsaCipher.doFinal(data);
        } catch (NoSuchAlgorithmException | NoSuchProviderException | NoSuchPaddingException | InvalidKeyException | BadPaddingException e) {
            Log.e(TAG, "encryptRSA: Encountered an Exception: ", e);
        } catch (ArrayIndexOutOfBoundsException e) {
            throw new IllegalBlockSizeException("Too much data for RSA block");
        }
        return null;
    }


    ///// Decryption
    /**
     * Decrypt RSA-encrypted data with the corresponding private key
     * @param data Encrypted data
     * @param privkey Private key to decrypt the data with
     * @return Decrypted data as byte[]
     * @throws IllegalBlockSizeException If the data is too large to decrypt (what are you doing?)
     * @throws BadPaddingException If the padding was incorrect (data manipulated?)
     */
    public static byte[] decryptRSA(byte[] data, PrivateKey privkey) throws IllegalBlockSizeException, BadPaddingException {
        try {
            Cipher rsaCipher = Cipher.getInstance("RSA/NONE/OAEPWithSHA256AndMGF1Padding", "SC");
            rsaCipher.init(Cipher.DECRYPT_MODE, privkey);
            return rsaCipher.doFinal(data);
        } catch (NoSuchPaddingException | NoSuchAlgorithmException | NoSuchProviderException | InvalidKeyException e) {
            Log.e(TAG, "decryptRSA: Encountered Exception: ", e);
        }
        return null;
    }

    ///// Fingerprinting
    /**
     * Calculate a fingerprint of a public key
     * @param pubkey The public key
     * @return The fingerprint, or null if the four horsemen of the apocalypse have arrived and removed SHA256 from the
     * list of supported hash functions.
     */
    public static String fingerprint(PublicKey pubkey) {
        assert pubkey != null;
        try {
            // Get a SHA256 hash function
            MessageDigest md = MessageDigest.getInstance("SHA256");
            // Add the bytes of the public key
            md.update(pubkey.getEncoded());
            // Calculate hash
            byte[] hash = md.digest();
            // Return String-representation
            return FormatHelper.bytesToHex(hash);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            return null;
        }
    }
}
