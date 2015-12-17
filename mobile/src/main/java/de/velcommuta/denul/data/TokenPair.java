package de.velcommuta.denul.data;

import java.util.Arrays;

/**
 * Container class for pairs of identifier-revocation-tokens.
 */
public class TokenPair {
    private byte[] mIdentifier;
    private byte[] mRevocation;


    /**
     * Constructor for data class. Takes an Identifier- and revocation-token byte-array, and converts
     * them to Strings for storage
     * @param identifier The identifier
     * @param revocation The revocation token
     */
    public TokenPair(byte[] identifier, byte[] revocation) {
        mIdentifier = Arrays.copyOf(identifier, identifier.length);
        mRevocation = Arrays.copyOf(revocation, revocation.length);
    }


    /**
     * Getter for the identifier
     * @return The identifier
     */
    public byte[] getIdentifier() {
        return mIdentifier;
    }


    /**
     * Getter for the revocation token
     * @return The revocation token
     */
    public byte[] getRevocation() {
        return mRevocation;
    }

    public boolean equals(Object o) {
        if (!(o instanceof TokenPair)) return false;
        TokenPair t = (TokenPair) o;
        return Arrays.equals(getIdentifier(), t.getIdentifier())
                && Arrays.equals(getRevocation(), t.getRevocation());
    }
}
