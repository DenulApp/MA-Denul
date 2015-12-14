package de.velcommuta.denul.data;

import android.util.Log;

import java.util.Arrays;

/**
 * A DataBlock contains encrypted {@link Shareable} data
 */
public class DataBlock {
    private static final String TAG = "DataBlock";

    private byte[] mKey;
    private byte[] mIdentifier;
    private byte[] mCiphertext;
    private Friend mOwner;


    /**
     * Constructor for data object
     * @param key The key used to encrypt the data
     * @param ciphertext The encrypted data
     * @param identifier The identifier associated with this data
     */
    public DataBlock(byte[] key, byte[] ciphertext, byte[] identifier) {
        mKey = Arrays.copyOf(key, key.length);
        mCiphertext = Arrays.copyOf(ciphertext, ciphertext.length);
        mIdentifier = Arrays.copyOf(identifier, identifier.length);
    }


    /**
     * Constructor for the data object if the ciphertext is not known
     * @param key The key used to encrypt the data
     * @param identifier The identifier of the data
     */
    public DataBlock(byte[] key, byte[] identifier) {
        mKey = Arrays.copyOf(key, key.length);
        mIdentifier = Arrays.copyOf(identifier, identifier.length);
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
        return mCiphertext;
    }


    /**
     * Getter for the Identifier
     * @return The Identifier
     */
    public byte[] getIdentifier() {
        return mIdentifier;
    }


    /**
     * Setter for the optional Owner field
     * @param owner The owner of the data
     */
    public void setOwner(Friend owner) {
        mOwner = owner;
    }


    /**
     * Getter for the optional owner field
     * @return The owner, if set, or null;
     */
    public Friend getOwner() {
        return mOwner;
    }
}
