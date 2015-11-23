package de.velcommuta.denul.networking;

import android.support.annotation.Nullable;

import java.util.Dictionary;
import java.util.List;

/**
 * Interface for communication protocol implementations
 */
public interface Protocol {
    // Return values for the connect method
    // Connection OK
    int CONNECT_OK = 0;
    // TODO Create more constants for connection failures with better explanations of the cause
    // Connection failed
    int CONNECT_FAIL = 1;

    // Return values for the put function
    // Put succeeded
    int PUT_OK = 0;
    // Put failed because the key was already taken
    int PUT_FAIL_KEY_TAKEN = 1;

    // Return values for the delete function
    // Deletion succeeded
    int DEL_OK = 0;
    // Deletion failed, key is not present on the server
    int DEL_FAIL_KEY_NOT_TAKEN = 1;
    // Deletion failed, authentication token was incorrect
    int DEL_FAIL_AUTH_INCORRECT = 2;

    /**
     * Establish a connection using this protocol, via the provided Connection
     * @param conn The Connection object
     * @return One of the CONNECT_* constants defined by the interface, indicating the result
     */
    int connect(Connection conn);

    /**
     * Disconect from the server
     */
    void disconnect();

    /**
     * Retrieve a key saved under a specific value from the server.
     * @param key The key to check for
     * @return The value saved under that key, or null if the key is not set
     */
    @Nullable
    byte[] get(String key);

    /**
     * Retrieve all values stored under a List of keys from the server
     * @param keys The List of keys
     * @return A dictionary mapping the key Strings to the byte[] values, or null if they are not
     * on the server
     */
    Dictionary<String, byte[]> getMany(List<String> keys);

    /**
     * Insert a value into the database of the server
     * @param key The key under which the data should be inserted
     * @param value The value that should be inserted
     * @return One of the PUT_* constants defined by the interface, indicating the result of the
     * operation
     */
    int put(String key, byte[] value);

    /**
     * Insert a number of key-value-pairs into the database of the server
     * @param records A Dictionary mapping keys to values that should be inserted
     * @return A Dictionary mapping the keys to PUT_* constants defined by the interface, indicating
     * the individual results of the put operations
     */
    Dictionary<String, Integer> putMany(Dictionary<String, byte[]> records);

    /**
     * Delete a key from the database of the server, authenticating the deletion operation by
     * whatever means the protocol requires
     * @param key The key to be deleted
     * @param authenticator The authenticator for the operation
     * @return One of the DEL_* constants defined by the interface, indicating the result
     */
    int del(String key, String authenticator);

    /**
     * Delete a number of keys from the database of the server, authenticating each operation by
     * whatever means the protocol requires
     * @param records A Dictionary mapping the keys to delete to their authenticators
     * @return A dictionary mapping the keys to one of the DEL_* constants defined in the interface,
     * indicating the result of the operation
     */
    Dictionary<String, Integer> delMany(Dictionary<String, String> records);
}
