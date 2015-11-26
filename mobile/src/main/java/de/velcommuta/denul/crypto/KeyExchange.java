package de.velcommuta.denul.crypto;

/**
 * Interface for different Key Exchange algorithms
 */
public interface KeyExchange {
    // Initialization of the Kex happens in the constructor, which is not defined in the interface.
    // The interface assumes that all initialization is already done and the kex only requires one
    // exchange of public information

    /**
     * Get the public key exchange data that needs to be passed to the other party.
     * @return a  byte[] representation of the public key exchange data
     */
    byte[] getPublicKexData();

    /**
     * Update the Key Exchange with the public information of the other party
     * @param data A byte[]-representation of the kex information of the other party
     * @return true if the data was valid, false otherwise
     */
    boolean putPartnerKexData(byte[] data);

    /**
     * Return the agreed-upon key after the key exchange has been performed
     * @return The agreed-upon key, as byte[]
     */
    byte[] getAgreedKey();
}
