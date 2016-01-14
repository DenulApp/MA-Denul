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
    private byte[] mRevoke;
    private byte[] mCiphertext;
    private Friend mOwner;
    private int mGranularity;
    private long share_id;


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
     * Constructor for data object
     * @param key The key used to encrypt the data
     * @param ciphertext The encrypted data
     * @param identifier The identifier associated with this data
     * @param granularity The granularity
     */
    public DataBlock(byte[] key, byte[] ciphertext, byte[] identifier, int granularity) {
        mKey = Arrays.copyOf(key, key.length);
        mCiphertext = Arrays.copyOf(ciphertext, ciphertext.length);
        mIdentifier = Arrays.copyOf(identifier, identifier.length);
        mGranularity = granularity;
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
     * Set the granularity level of this datablock
     * @param granularity One of the GRANULARITY_* constants defined in the {@link Shareable} interface
     */
    public void setGranularity(int granularity) {
        mGranularity = granularity;
    }


    /**
     * Getter for the granularity
     * @return The granularity level
     */
    public int getGranularity() {
        return mGranularity;
    }


    /**
     * Getter for the optional owner field
     * @return The owner, if set, or null;
     */
    public Friend getOwner() {
        return mOwner;
    }


    /**
     * Setter for the database ID of a DataBlock
     * @param id The Database ID
     */
    public void setDatabaseID(long id) {
        share_id = id;
    }


    /**
     * Getter for the database ID of a DataBlock
     * @return The Database ID
     */
    public long getDatabaseID() {
        return share_id;
    }


    /**
     * Setter for the revocation token
     * @param revoke The revocation token
     */
    public void setRevocationToken(byte[] revoke) {
        mRevoke = Arrays.copyOf(revoke, revoke.length);
    }


    /**
     * Getter for the revocation token
     * @return The revocation token
     */
    public byte[] getRevocationToken() {
        return mRevoke;
    }


    public boolean equals(Object o) {
        if (!(o instanceof DataBlock)) return false;
        DataBlock d = (DataBlock) o;
        return Arrays.equals(getCiphertext(), d.getCiphertext())
                && Arrays.equals(getKey(), d.getKey())
                && Arrays.equals(getIdentifier(), d.getIdentifier())
                && getOwner().equals(d.getOwner());
    }
}
