package de.velcommuta.denul.crypto;

import android.util.Log;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Random;

import de.velcommuta.denul.data.KeySet;
import de.velcommuta.denul.data.TokenPair;

/**
 * {@link IdentifierDerivation} implementation that uses SHA256 to derive Identifier-Revocation pairs.
 * The revocation token is a 256-bit hex string. The identifier is the SHA256 hash of the revocation token.
 */
public class SHA256IdentifierDerivation implements IdentifierDerivation {
    private static final String TAG = "SHA256ID";

    @Override
    public TokenPair generateOutboundIdentifier(KeySet keyset) {
        MessageDigest md;
        try {
            md = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            Log.e(TAG, "generateIdentifier: SHA256 not supported");
            return null;
        }
        // Calculate revocation token
        md.update(keyset.getOutboundKey());
        md.update(keyset.getOutboundCtr());
        byte[] revocation = md.digest();
        // Calculate identifier
        md.update(revocation);
        byte[] identifier = md.digest();
        // Return result
        return new TokenPair(identifier, revocation);
    }

    @Override
    public TokenPair generateInboundIdentifier(KeySet keyset) {
        MessageDigest md;
        try {
            md = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            Log.e(TAG, "generateIdentifier: SHA256 not supported");
            return null;
        }
        // Calculate revocation token
        md.update(keyset.getInboundKey());
        md.update(keyset.getInboundCtr());
        byte[] revocation = md.digest();
        // Calculate identifier
        md.update(revocation);
        byte[] identifier = md.digest();
        // Return the result
        return new TokenPair(identifier, revocation);
    }


    @Override
    public KeySet notifyInboundIdentifierUsedForFriend(KeySet keyset) {
        // Increment counter
        keyset.incrementInboundCtr();
        return keyset;
    }


    @Override
    public KeySet notifyOutboundIdentifierUsedForFriend(KeySet keyset) {
        // Increment counter
        keyset.incrementOutboundCtr();
        return keyset;
    }


    @Override
    public TokenPair generateRandomIdentifier() {
        MessageDigest md;
        try {
            md = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            Log.e(TAG, "generateIdentifier: SHA256 not supported");
            return null;
        }
        // Get a random revocation token of the correct length
        byte[] revocation = new byte[md.getDigestLength()];
        new Random().nextBytes(revocation);
        // Get the matching Identifier
        md.update(revocation);
        byte[] identifier = md.digest();
        // Create and return TokenPair
        return new TokenPair(identifier, revocation);
    }
}
