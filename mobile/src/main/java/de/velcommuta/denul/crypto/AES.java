package de.velcommuta.denul.crypto;

import android.util.Log;

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
     * Encrypt some data using AES in GCM.
     * @param data The data that is to be encrypted
     * @param keyenc The key, as a byte[]
     * @return The encrypted data as a byte[], with the IV prepended to it
     */
    public static byte[] encryptAES(byte[] data, byte[] keyenc) {
        return encryptAES(data, keyenc, null);
    }


    /**
     * Encrypt some data using AES in GCM
     * @param data The data to encrypt
     * @param keyenc The key, as byte[]
     * @param aad Additional authenticated data
     * @return The encrypted data as byte[], with the IV prepended to it
     */
    public static byte[] encryptAES(byte[] data, byte[] keyenc, byte[] aad) {
        return encryptAES(data, keyenc, aad, null);
    }


    /**
     * Encrypt some data using AES in GCM.
     * @param data The data that is to be encrypted
     * @param keyenc The key, as a byte[]
     * @param aad The associated data to add (will not be included in the ciphertext, but authenticated)
     * @param iv The Initialization Vector to use. If it is null, a random IV will be generated
     * @return The encrypted data as a byte[]. If the IV was null, the used IV will be prepended
     *         to the message, otherwise the byte[] will not include the IV.
     */
    public static byte[] encryptAES(byte[] data, byte[] keyenc, byte[] aad, byte[] iv) {
        try {
            boolean ivSpecified = iv != null;
            // Sanity check for IV length - we allow 16 byte (128 bit) and 32 byte (256 bit) IVs
            if (ivSpecified && iv.length != 16 && iv.length != 32) {
                Log.e(TAG, "encryptAES: Bad IV length");
                return null;
            }
            // Get Cipher instance
            Cipher aesCipher = Cipher.getInstance("AES/GCM/NoPadding", "SC");
            // Create SecretKey object
            SecretKey key = new SecretKeySpec(keyenc, "AES");
            // Initialize the Cipher object
            if (ivSpecified) {
                aesCipher.init(Cipher.ENCRYPT_MODE, key, new IvParameterSpec(iv));
            } else {
                aesCipher.init(Cipher.ENCRYPT_MODE, key);
                // Load IV from aesCipher object
                iv = aesCipher.getIV();
            }
            // Add header to authentication
            if (aad != null) {
                aesCipher.updateAAD(aad);
            }
            // Perform the encryption
            byte[] encrypted = aesCipher.doFinal(data);
            // If an IV was specified, return the encrypted data without the IV
            if (ivSpecified) {
                return encrypted;
            }
            // Else, prepend the IV and return iv + data
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
        byte[] iv = new byte[16];
        byte[] encrypted = new byte[datawithiv.length -16];
        System.arraycopy(datawithiv, 0, iv, 0, 16);
        System.arraycopy(datawithiv, iv.length, encrypted, 0, datawithiv.length - 16);
        return decryptAES(encrypted, keyenc, null, iv);
    }


    /**
     * Decrypt a piece of AES2656-encrypted data with its key
     * @param datawithiv Data with first bytes representing the IV
     * @param keyenc byte[]-encoded key
     * @param aad Additional Authenticated Data to verify
     * @return Decrypted data as byte[]
     * @throws BadPaddingException If the padding was bad. This indicates that the ciphertext was
     * tampered with (i.e. the authentication failed)
     */
    public static byte[] decryptAES(byte[] datawithiv, byte[] keyenc, byte[] aad) throws BadPaddingException {
        byte[] iv = new byte[16];
        byte[] encrypted = new byte[datawithiv.length -16];
        System.arraycopy(datawithiv, 0, iv, 0, 16);
        System.arraycopy(datawithiv, iv.length, encrypted, 0, datawithiv.length - 16);
        return decryptAES(encrypted, keyenc, aad, iv);
    }


    /**
     * Decrypt a piece of AES256-encrypted data with its key
     * @param encrypted Encrypted Data WITHOUT the IV
     * @param keyenc byte[]-encoded key
     * @param aad Additional authenticated data to verfiy
     * @param iv The initialization vector to use
     * @return Decrypted data as byte[]
     * @throws BadPaddingException If the padding was bad. This indicates that the ciphertext was
     * tampered with (i.e. the authentication failed)
     */
    public static byte[] decryptAES(byte[] encrypted, byte[] keyenc, byte[] aad, byte[] iv) throws BadPaddingException {
        try {
            // Get Cipher instance
            Cipher aesCipher = Cipher.getInstance("AES/GCM/NoPadding", "SC");
            // Create SecretKey object
            SecretKey key = new SecretKeySpec(keyenc, "AES");
            // Initialize cipher
            aesCipher.init(Cipher.DECRYPT_MODE, key, new IvParameterSpec(iv));
            // Add header for AAD
            if (aad != null) {
                aesCipher.updateAAD(aad);
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
