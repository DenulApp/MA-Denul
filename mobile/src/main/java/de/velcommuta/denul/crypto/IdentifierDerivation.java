package de.velcommuta.denul.crypto;

import de.velcommuta.denul.data.Friend;
import de.velcommuta.denul.data.KeySet;
import de.velcommuta.denul.data.TokenPair;

/**
 * Interface for identifier derivation classes. Identifier derivation implementations derive
 * identifiers that are used to store data on a server.
 */
public interface IdentifierDerivation {
    /**
     * Generate a pair of identifier and revocation token used to pass a message TO a specific
     * {@link Friend}
     * @param keyset the {@link KeySet}to use
     * @return A {@link TokenPair} containing the identifier and revocation token
     */
    TokenPair generateOutboundIdentifier(KeySet keyset);

    /**
     * Generate a pair of identifier and revocation token used to receive a message FROM a specific
     * {@link Friend}
     * @param keyset the {@link KeySet}to use
     * @return A {@link TokenPair} containing the identifier and revocation token
     */
    TokenPair generateInboundIdentifier(KeySet keyset);

    /**
     * Function to notify the Derivation implementation that an inbound identifier was used and that
     * it should thus update its state (if any) to derive the next identifier on the next call to
     * {@link #generateInboundIdentifier(KeySet)}.
     *  @param keyset the {@link KeySet}to use
     *  @return the updated {@link KeySet}
     */
    KeySet notifyInboundIdentifierUsed(KeySet keyset);

    /**
     * Function to notify the Derivation implementation that an outbound identifier was used and that
     * it should thus update its state (if any) to derive the next identifier on the next call to
     * {@link #generateOutboundIdentifier(KeySet)}.
     * @param keyset The {@link KeySet} to use
     * @return The updated {@link KeySet}
     */
    KeySet notifyOutboundIdentifierUsed(KeySet keyset);

    /**
     * Generate a pair of identifier and revocation token, unrelated to any specific friend.
     * @return A {@link TokenPair} containing the identifier and revocation token.
     */
    TokenPair generateRandomIdentifier();
}
