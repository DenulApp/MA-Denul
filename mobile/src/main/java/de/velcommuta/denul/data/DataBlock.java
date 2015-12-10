package de.velcommuta.denul.data;

import android.util.Log;

import java.util.Arrays;

/**
 * A DataBlock contains encrypted {@link Shareable} data
 */
public class DataBlock {
    private static final String TAG = "DataBlock";

    private byte[] mKey;
    private TokenPair mIdentifier;
    private byte[] mCiphertext;


    /**
     * Constructor for data object
     * @param key The key used to encrypt the data
     * @param ciphertext The encrypted data
     * @param identifier The identifier associated with this data
     */
    public DataBlock(byte[] key, byte[] ciphertext, TokenPair identifier) {
        mKey = Arrays.copyOf(key, key.length);
        mCiphertext = Arrays.copyOf(ciphertext, ciphertext.length);
        mIdentifier = identifier;
    }


    /**
     * Constructor for the data object if the ciphertext is not known
     * @param key The key used to encrypt the data
     * @param identifier The identifier of the data
     */
    public DataBlock(byte[] key, TokenPair identifier) {
        mKey = Arrays.copyOf(key, key.length);
        mIdentifier = identifier;
        mCiphertext = null;
    }


    /**
     * Setter for the ciphertext, IF the ciphertext has not yet been set
     * @param ciphertext The ciphertext
     */
    public void setCiphertext(byte[] ciphertext) {
        if (mCiphertext == null) {
            mCiphertext = Arrays.copyOf(ciphertext, ciphertext.length);
        } else {
            Log.e(TAG, "setCiphertext: Ciphertext already set");
        }
    }


    /**
     * Getter for the encryption key
     * @return The encryption key
     */
    public byte[] getKey() {
        return Arrays.copyOf(mKey, mKey.length);
    }


    /**
     * Getter for the ciphertext
     * @return The ciphertext
     */
    public byte[] getCiphertext() {
        return Arrays.copyOf(mCiphertext, mCiphertext.length);
    }


    /**
     * Getter for the identifier
     * @return The identifier
     */
    public TokenPair getIdentifier() {
        return mIdentifier;
    }
}
