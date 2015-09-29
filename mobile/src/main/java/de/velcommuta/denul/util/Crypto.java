package de.velcommuta.denul.util;

import android.util.Base64;
import android.util.Log;

import org.spongycastle.jce.provider.BouncyCastleProvider;

import java.security.AlgorithmParameters;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Security;
import java.security.spec.X509EncodedKeySpec;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

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

    /**
     * Generates a random AES256 key and returns it as a byte[]
     * @return The generated AES256 key
     */
    public static byte[] generateAES256Key() {
        try {
            // Get a key generator instance
            KeyGenerator kgen = KeyGenerator.getInstance("AES", "SC");
            // Request an AES256 key
            kgen.init(256);
            // Generate the actual key
            SecretKey seckey = kgen.generateKey();
            // Return the encoded key
            return seckey.getEncoded();
        } catch (Exception e) {
            Log.e(TAG, "generateAES256Key: Exception occured: ", e);
            e.printStackTrace();
            return null;
        }
    }

    ///// Encryption and Decryption
    /**
     * Encrypt some data using AES in GCM mode with PKCS#7 padding.
     * @param data The data that is to be encrypted
     * @param keyenc The key, as a byte[]
     * @return The encrypted data as a byte[]
     */
    public static byte[] encryptAES(byte[] data, byte[] keyenc) {
        try {
            // Get Cipher instance
            Cipher aesCipher = Cipher.getInstance("AES/GCM/NoPadding", "SC");
            // Create SecretKey object
            SecretKey key = new SecretKeySpec(keyenc, "AES");
            // Initialize the Cipher object
            aesCipher.init(Cipher.ENCRYPT_MODE, key);
            // Get the algorithm parameters
            AlgorithmParameters params = aesCipher.getParameters();
            // Extract the IV
            byte[] iv = params.getParameterSpec(IvParameterSpec.class).getIV();
            // Perform the encryption
            byte[] encrypted = aesCipher.doFinal(data);
            byte[] returnvalue = new byte[iv.length + encrypted.length];
            System.arraycopy(iv, 0, returnvalue, 0, iv.length);
            System.arraycopy(encrypted, 0, returnvalue, iv.length, encrypted.length);
            return returnvalue;

        } catch (Exception e) {
            Log.e(TAG, "encryptAES: Encoutered Exception during encryption: ", e);
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Decrypt a piece of AES256-encrypted data with its key
     * @param datawithiv Data with first bytes representing the IV
     * @param keyenc byte[]-encoded key
     * @return Decrypted data as byte[]
     * @throws BadPaddingException If the padding was bad. This indicates that the ciphertext was
     * tampered with (i.e. the authentication failed)
     */
    public static byte[] decryptAES(byte[] datawithiv, byte[] keyenc) throws BadPaddingException {
        try {
            // Get Cipher instance
            Cipher aesCipher = Cipher.getInstance("AES/GCM/NoPadding", "SC");
            // Create SecretKey object
            SecretKey key = new SecretKeySpec(keyenc, "AES");
            // Split datawithiv into iv and data
            byte[] iv = new byte[16];
            byte[] encrypted = new byte[datawithiv.length -16];
            System.arraycopy(datawithiv, 0, iv, 0, 16);
            System.arraycopy(datawithiv, iv.length, encrypted, 0, datawithiv.length - 16);
            // Initialize cipher
            aesCipher.init(Cipher.DECRYPT_MODE, key, new IvParameterSpec(iv));
            // Perform the decryption
            return aesCipher.doFinal(encrypted);
        } catch (NoSuchPaddingException | InvalidAlgorithmParameterException | NoSuchAlgorithmException
                | IllegalBlockSizeException | NoSuchProviderException | InvalidKeyException e) {
            Log.e(TAG, "decryptAES: An Exception occured during decryption: ", e);
        }
        return null;
    }

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
}
