package de.velcommuta.denul.networking;

import android.support.annotation.Nullable;

import java.util.List;
import java.util.Map;

import de.velcommuta.denul.crypto.KeyExchange;
import de.velcommuta.denul.data.DataBlock;
import de.velcommuta.denul.data.StudyRequest;
import de.velcommuta.denul.data.TokenPair;

/**
 * Interface for communication protocol implementations
 */
public interface Protocol {
    // Return values for the connect method
    // Connection OK
    int CONNECT_OK = 0;
    // Connection failed, underlying Connection not connected
    int CONNECT_FAIL_NO_CONNECTION = 1;
    // Connection failed, server sent unexpected message
    int CONNECT_FAIL_PROTOCOL_ERROR = 2;
    // Connection failed, server protocol version unknown
    int CONNECT_FAIL_SERVER_PROTO = 3;

    // Return values for the put function
    // Put succeeded
    int PUT_OK = 0;
    // Put failed, underlying connection not open
    int PUT_FAIL_NO_CONNECTION = 1;
    // Put failed because the key was already taken
    int PUT_FAIL_KEY_TAKEN = 2;
    // Put failed, bad key format
    int PUT_FAIL_KEY_FMT = 3;
    // Put failed, protocol error
    int PUT_FAIL_PROTOCOL_ERROR = 4;

    // Return values for the delete function
    // Deletion succeeded
    int DEL_OK = 0;
    // Deletion failed, underlying connection not open
    int DEL_FAIL_NO_CONNECTION = 1;
    // Deletion failed, key is not present on the server
    int DEL_FAIL_KEY_NOT_TAKEN = 2;
    // Deletion failed, bad key format
    int DEL_FAIL_KEY_FMT = 3;
    // Deletion failed, authentication token was incorrect
    int DEL_FAIL_AUTH_INCORRECT = 4;
    // Deletion failed, protocol error
    int DEL_FAIL_PROTOCOL_ERROR = 5;

    // Return values for the revocation function
    // Revocation succeeded
    int REV_OK = 0;
    // Revocation failed, underlying connection not open
    int REV_FAIL_NO_CONNECTION = 1;
    // Revocation failed, key is not present on the server
    int REV_FAIL_KEY_NOT_TAKEN = 2;
    // Revocation failed, bad key format
    int REV_FAIL_KEY_FMT = 3;
    // Revocation failed, authentication token was incorrect
    int REV_FAIL_AUTH_INCORRECT = 4;
    // Revocation failed, protocol error
    int REV_FAIL_PROTOCOL_ERROR = 5;

    // Return values for the Get function in case of errors
    // Get failed, key not taken
    byte[] GET_FAIL_KEY_NOT_TAKEN = null;
    // Get failed, not connected
    byte[] GET_FAIL_NO_CONNECTION = new byte[] {0x00};
    // Get failed, protocol error
    byte[] GET_FAIL_PROTOCOL_ERROR = new byte[] {0x01};
    // Get failed, bad key format
    byte[] GET_FAIL_KEY_FMT = new byte[] {0x02};

    // Return values for the studyJoin function
    // Join suceeded
    int JOIN_OK = 0;
    // Join failed, no connection
    int JOIN_FAIL_NO_CONNECTION = 1;
    // Join failed, no such study
    int JOIN_FAIL_NO_STUDY = 2;
    // Join failed, protocol error
    int JOIN_FAIL_PROTOCOL_ERROR = 3;


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
     * @param tokens The {@link TokenPair} with the Identifier that should be retrieved
     * @return The value saved under that identifier, null if the identifier is not used on the server,
     *         or one of the GET_* constants if an error occured
     */
    @Nullable
    byte[] get(TokenPair tokens);

    /**
     * Retrieve all values stored under a List of keys from the server
     * @param tokens The List of {@link TokenPair}s that should be retrieved
     * @return A dictionary mapping the Identifiers to the byte[] values, null if they are not
     *         on the server, or one of the GET_* constants if an error occured
     */
    Map<TokenPair, byte[]> getMany(List<TokenPair> tokens);

    /**
     * Insert a value into the database of the server
     * @param data A {@link DataBlock} representing the identifier and value that should be saved
     * @return One of the PUT_* constants defined by the interface, indicating the result of the
     * operation
     */
    int put(DataBlock data);

    /**
     * Insert a number of key-value-pairs into the database of the server
     * @param values A List of {@link DataBlock} objects mapping Identifiers and values that should
     *               be saved on the server
     * @return A Dictionary mapping the DataBlocks to PUT_* constants defined by the interface,
     *         indicating the individual results of the put operations
     */
    Map<DataBlock, Integer> putMany(List<DataBlock> values);

    /**
     * Delete a key from the database of the server, authenticating the deletion operation by
     * whatever means the protocol requires
     * @param tokens A {@link TokenPair} containing the identifier and the revocation token
     * @return One of the DEL_* constants defined by the interface, indicating the result
     */
    int del(TokenPair tokens);

    /**
     * Delete a number of keys from the database of the server, authenticating each operation by
     * whatever means the protocol requires
     * @param records A List of {@link TokenPair}s containing identifier and revocation tokens
     * @return A dictionary mapping the TokenPairs to one of the DEL_* constants defined in the interface,
     * indicating the result of the operation
     */
    Map<TokenPair, Integer> delMany(List<TokenPair> records);

    /**
     * Revoke a share identified by the TokenPair. The difference to the delete operation is that in
     * this case, the implementation MUST make sure that the deletion does not impact the protocol
     * stability if the recipient has not yet retrieved the data.
     * @param pair The {@link TokenPair} containing identifier and revocation authenticator
     * @return One of the REV_* constants defined by the interface, indicating the result of the
     * operation
     */
    int revoke(TokenPair pair);

    /**
     * Delete a number of keys from the database of the server, authenticating each operation by
     * whatever means the protocol requires. The difference to the delete operation is that in
     * this case, the implementation MUST make sure that the deletion does not impact the protocol
     * stability if the recipient has not yet retrieved the data.
     * @param pairs A List of {@link TokenPair}s containing identifiers and revocation authenticators
     * @return A dictionary mapping the {@link TokenPair}s to one of the REV_* constants defined by
     * the interface, indicating the result of the operation
     */
    Map<TokenPair, Integer> revokeMany(List<TokenPair> pairs);

    /**
     * Retrieve all registered studies from the server and return them as a List of StudyRequests
     * @return A List of StudyRequests, or an empty list if no requests have been registered. Null
     * if an error occured
     */
    List<StudyRequest> listRegisteredStudies();

    /**
     * Join a study
     * @param req The Study to join. The Key Exchange should already have happened.
     * @param kex The used KeyExchange
     * @return One of the JOIN_* constants defined by the interface, indicating the result of the
     * operation.
     */
    int joinStudy(StudyRequest req, KeyExchange kex);
}
