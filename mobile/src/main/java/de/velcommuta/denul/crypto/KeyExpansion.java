package de.velcommuta.denul.crypto;

import de.velcommuta.denul.data.KeySet;

/**
 * Interface for key expansion implementations. They are used to expand a small secret (e.g. the
 * result of a {@link KeyExchange}) into multiple secure symmetric keys and counters, as required
 * by the protocol.
 * Implementations have to be deterministic (i.e. return the same value for the same input every
 * time), as they will be executed on both devices and the outputs have to match.
 */
public interface KeyExpansion {
    // We assume that the constructor will receive the secret bytes

    /**
     * Perform the Key Expansion and return the Result as a {@link KeySet}
     * @param isInitiatingParty Boolean indicating if the device this is running on initiated the
     *                          key exchange or not. Used to make sure that the directional keys and
     *                          counters are bound to the correct fields in the {@link KeySet}.
     * @return A {@link KeySet} containing secure symmetric keys reproducibly derived from the input
     * secret
     */
    KeySet expand(boolean isInitiatingParty);
}
