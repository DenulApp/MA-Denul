package de.velcommuta.denul.data;

import org.spongycastle.crypto.Digest;
import org.spongycastle.crypto.digests.SHA256Digest;

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
        if (KeyIn.length != 32 || KeyOut.length != 32) throw new IllegalArgumentException("Bad key length");
        if (CtrIn.length != 32 || CtrOut.length != 32) throw new IllegalArgumentException("Bad ctr length");
        mInboundKey  = KeyIn;
        mOutboundKey = KeyOut;
        mInboundCtr  = CtrIn;
        mOutboundCtr = CtrOut;
        mInitiated   = initiated;
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
}
