package de.velcommuta.denul.crypto;

import de.velcommuta.denul.data.DataBlock;
import de.velcommuta.denul.data.KeySet;
import de.velcommuta.denul.data.Shareable;
import de.velcommuta.denul.data.TokenPair;

/**
 * Interface for implementations of encryption protocols for sharing
 */
public interface SharingEncryption {
    /**
     * Encrypt a {@link Shareable} object with a random key and IV and return the result as a
     * {@link DataBlock}.
     * @param shareable The {@link Shareable} to encrypt
     * @param tokens The {@link TokenPair} containing the identifier under which the data will be shared
     * @return A {@link DataBlock} containing the encrypted and authenticated {@link Shareable},
     *         cryptographically bound to the identifier in the {@link TokenPair}.
     */
    DataBlock encryptShareable(Shareable shareable, TokenPair tokens);

    /**
     * Encrypt the keys and identifier of a {@link DataBlock}.
     * @param data The {@link DataBlock} to encrypt
     * @param keys The keys to use for this encryption operation
     * @return The encrypted and authenticated DataBlock, cryptographically bound to the Identifier
     *         of the {@link TokenPair} included in the {@link DataBlock}.
     */
    byte[] encryptKeysAndIdentifier(DataBlock data, KeySet keys);

    /**
     * Authenticate, decrypt and deserialize an encrypted {@link Shareable} and return it
     * @param encrypted The encrypted Shareable, its key, and its {@link TokenPair}
     * @return The authenticated, decrypted and deserialized {@link Shareable}
     */
    Shareable decryptShareable(DataBlock encrypted);

    /**
     * Authenticate and decrypt an encrypted DataBlock
     * @param encrypted The ciphertext of an encrypted {@link DataBlock}
     * @param keys The {@link KeySet} used to encrypt
     * @return The authenticated, decrypted and deserialized DataBlock
     */
    DataBlock decryptKeysAndIdentifier(byte[] encrypted, KeySet keys);
}
