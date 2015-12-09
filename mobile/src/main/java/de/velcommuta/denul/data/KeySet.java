package de.velcommuta.denul.data;

import org.spongycastle.crypto.Digest;
import org.spongycastle.crypto.digests.SHA256Digest;

import java.util.Arrays;

import de.velcommuta.denul.util.FormatHelper;

/**
 * A data structure containing two symmetric keys and two counters
 */
public class KeySet {
    private byte[] mInboundKey;
    private byte[] mOutboundKey;
    private byte[] mInboundCtr;
    private byte[] mOutboundCtr;
    private boolean mInitiated;
    private int mID;


    /**
     * Constructor
     * @param KeyIn Inbound key
     * @param KeyOut Outbound key
     * @param CtrIn Inbound counter
     * @param CtrOut Outbound counter
     * @param initiated Indicates if the key exchange that generated these values was initiated by
     *                  this device. Used to derive matching fingerprints on both ends
     */
    public KeySet(byte[] KeyIn, byte[] KeyOut, byte[] CtrIn, byte[] CtrOut, boolean initiated) {
        this(KeyIn, KeyOut, CtrIn, CtrOut, initiated, -1);
    }


    /**
     * Constructor
     * @param KeyIn Inbound key
     * @param KeyOut Outbound key
     * @param CtrIn Inbound counter
     * @param CtrOut Outbound counter
     * @param initiated Indicates if the key exchange that generated these values was initiated by
     *                  this device. Used to derive matching fingerprints on both ends
     * @param id The database identifier of the KeySet
     */
    public KeySet(byte[] KeyIn, byte[] KeyOut, byte[] CtrIn, byte[] CtrOut, boolean initiated, int id) {
        if (KeyIn.length != 32 || KeyOut.length != 32) throw new IllegalArgumentException("Bad key length");
        if (CtrIn.length != 32 || CtrOut.length != 32) throw new IllegalArgumentException("Bad ctr length");
        mInboundKey  = Arrays.copyOf(KeyIn, KeyIn.length);
        mOutboundKey = Arrays.copyOf(KeyOut, KeyOut.length);
        mInboundCtr  = Arrays.copyOf(CtrIn, CtrIn.length);
        mOutboundCtr = Arrays.copyOf(CtrOut, CtrOut.length);
        mInitiated   = initiated;
        mID          = id;
    }

    /**
     * Getter for the inbound key
     * @return The inbound key, as byte[]
     */
    public byte[] getInboundKey() {
        return mInboundKey;
    }


    /**
     * Getter for the outbound key
     * @return The outbound key, as byte[]
     */
    public byte[] getOutboundKey() {
        return mOutboundKey;
    }


    /**
     * Getter for the inbound counter
     * @return The inbound counter, as int
     */
    public byte[] getInboundCtr() {
        return mInboundCtr;
    }


    /**
     * Getter for the outbound counter
     * @return The outbound counter, as int
     */
    public byte[] getOutboundCtr() {
        return mOutboundCtr;
    }


    /**
     * Getter for the information if this device initiated the key exchange
     * @return true if this device initiated the key exchange, false otherwise
     */
    public boolean hasInitiated() {
        return mInitiated;
    }


    /**
     * Getter for the database ID of this KeySet
     * @return The Database ID of this KeySet, or -1 if the KeySet is not yet in the database
     */
    public int getID() {
        return mID;
    }


    /**
     * Compute a fingerprint over the keys and counters contained in this KeySet and return it.
     * The fingerprint should be identical on both ends of the connection where the keys have been
     * generated.
     * @return The string representation of the fingerprint
     */
    public String fingerprint() {
        Digest hash = new SHA256Digest();
        if (mInitiated) {
            hash.update(mInboundKey, 0, mInboundKey.length);
            hash.update(mOutboundKey, 0, mOutboundKey.length);
            hash.update(mInboundCtr, 0, mInboundCtr.length);
            hash.update(mOutboundCtr, 0, mOutboundCtr.length);
        } else {
            hash.update(mOutboundKey, 0, mOutboundKey.length);
            hash.update(mInboundKey, 0, mInboundKey.length);
            hash.update(mOutboundCtr, 0, mOutboundCtr.length);
            hash.update(mInboundCtr, 0, mInboundCtr.length);
        }
        byte[] output = new byte[hash.getDigestSize()];
        hash.doFinal(output, 0);
        return FormatHelper.bytesToHex(output);
    }


    /**
     * Increment the inbound counter value
     */
    public void incrementInboundCtr() {
        mInboundCtr = incrementByteArray(mInboundCtr);
    }


    /**
     * Increment the outbound counter value
     */
    public void incrementOutboundCtr() {
        mOutboundCtr = incrementByteArray(mOutboundCtr);
    }

    /**
     * Helper function to increment a byte[]
     * @param input The byte[] to increment (will stay unmodified, a copy will be returned)
     * @return A copy of the byte array, incremented by one (unsigned)
     */
    private byte[] incrementByteArray(byte[] input) {
        byte[] output = Arrays.copyOf(input, input.length);
        for (int i = output.length - 1; i >= 0; i--) {
            output[i]++;
            if (output[i] != (byte) 0) {
                break;
            }
        }
        return output;
    }
}
