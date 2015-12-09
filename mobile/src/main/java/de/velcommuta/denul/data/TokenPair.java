package de.velcommuta.denul.data;

import de.velcommuta.denul.util.FormatHelper;

/**
 * Container class for pairs of identifier-revocation-tokens.
 */
public class TokenPair {
    private String mIdentifier;
    private String mRevocation;


    /**
     * Constructor for data class. Takes an Identifier-String and a revocation token string.
     * @param identifier The identifier
     * @param revocation The matching revocation token
     */
    public TokenPair(String identifier, String revocation) {
        mIdentifier = identifier;
        mRevocation = revocation;
    }


    /**
     * Constructor for data class. Takes an Identifier- and revocation-token byte-array, and converts
     * them to Strings for storage
     * @param identifier The identifier
     * @param revocation The revocation token
     */
    public TokenPair(byte[] identifier, byte[] revocation) {
        mIdentifier = FormatHelper.bytesToHex(identifier);
        mRevocation = FormatHelper.bytesToHex(revocation);
    }


    /**
     * Getter for the identifier
     * @return The identifier
     */
    public String getIdentifier() {
        return mIdentifier;
    }


    /**
     * Getter for the revocation token
     * @return The revocation token
     */
    public String getRevocation() {
        return mRevocation;
    }
}
