package de.velcommuta.denul.crypto;

import java.security.KeyPair;

/**
 * Created by max on 07.01.16.
 */
public class KexStub implements KeyExchange {
    private byte[] mKexData;

    /**
     * Constructor
     * @param pubdata Public key data
     */
    public KexStub(byte[] pubdata) {
        mKexData = pubdata;
    }

    @Override
    public byte[] getPublicKexData() {
        return mKexData;
    }

    // Key Exchange functions do not work on this
    @Override
    public boolean putPartnerKexData(byte[] data) {
        throw new IllegalArgumentException("Not implemented");
    }

    @Override
    public byte[] getAgreedKey() {
        throw new IllegalArgumentException("Not implemented");
    }

    @Override
    public KeyPair getKeypair() {
        throw new IllegalArgumentException("Not implemented");
    }
}
