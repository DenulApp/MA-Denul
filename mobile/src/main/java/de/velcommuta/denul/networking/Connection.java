package de.velcommuta.denul.networking;

import java.io.IOException;

/**
 * Interface for network connections. Abstracted so that different underlying connection systems
 * can be used (e.g. TCP, UDP, TCP over Tor, ...)
 */
public interface Connection {
    /**
     * Send a byte[] via the connection and return the byte[] that is sent in reply.
     * @param message the byte[] to be sent
     * @return The byte[] that was returned
     * @throws IOException if the underlying socket throws it
     */
    byte[] transceive(byte[] message) throws IOException;

    /**
     * Close the connection.
     * @throws IOException if the underlying socket throws it
     */
    void close() throws IOException;

    /**
     * Check if the connection is open
     * @return true if the connection is open, false otherwise
     */
    boolean isOpen();
}
