package de.velcommuta.denul.crypto;

import android.util.Log;

import java.security.AlgorithmParameters;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.Security;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

/**
 * AES cryptography
 */
public class AES {
    // Logging Tag
    private static final String TAG = "AES";

    // Insert provider
    static {
        Security.insertProviderAt(new org.spongycastle.jce.provider.BouncyCastleProvider(), 1);
    }


    ///// Key Generation
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


    ///// Encryption
    /**
     * Encrypt some data using AES in GCM mode.
     * @param data The data that is to be encrypted
     * @param keyenc The key, as a byte[]
     * @return The encrypted data as a byte[]
     */
    public static byte[] encryptAES(byte[] data, byte[] keyenc) {
        return encryptAES(data, keyenc, null);
    }


    /**
     * Encrypt some data using AES in GCM mode.
     * @param data The data that is to be encrypted
     * @param keyenc The key, as a byte[]
     * @param header The header (with length field nulled), to be added and as Associated Data
     * @return The encrypted data as a byte[]
     */
    public static byte[] encryptAES(byte[] data, byte[] keyenc, byte[] header) {
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
            // Add header to authentication
            if (header != null) {
                aesCipher.updateAAD(header);
            }
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


    ///// Decryption
    /**
     * Decrypt a piece of AES256-encrypted data with its key
     * @param datawithiv Data with first bytes representing the IV
     * @param keyenc byte[]-encoded key
     * @return Decrypted data as byte[]
     * @throws BadPaddingException If the padding was bad. This indicates that the ciphertext was
     * tampered with (i.e. the authentication failed)
     */
    public static byte[] decryptAES(byte[] datawithiv, byte[] keyenc) throws BadPaddingException {
        return decryptAES(datawithiv, keyenc, null);
    }


    /**
     * Decrypt a piece of AES256-encrypted data with its key
     * @param datawithiv Data with first bytes representing the IV
     * @param keyenc byte[]-encoded key
     * @param header The header, to be authenticated using AEAD
     * @return Decrypted data as byte[]
     * @throws BadPaddingException If the padding was bad. This indicates that the ciphertext was
     * tampered with (i.e. the authentication failed)
     */
    public static byte[] decryptAES(byte[] datawithiv, byte[] keyenc, byte[] header) throws BadPaddingException {
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
            // Add header for AAD
            if (header != null) {
                aesCipher.updateAAD(header);
            }
            // Perform the decryption
            return aesCipher.doFinal(encrypted);
        } catch (NoSuchPaddingException | InvalidAlgorithmParameterException | NoSuchAlgorithmException
                | IllegalBlockSizeException | NoSuchProviderException | InvalidKeyException e) {
            Log.e(TAG, "decryptAES: An Exception occured during decryption: ", e);
        }
        return null;
    }
}
