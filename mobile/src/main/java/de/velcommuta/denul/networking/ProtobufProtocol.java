package de.velcommuta.denul.networking;

import android.support.annotation.Nullable;
import android.util.Log;

import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.DataFormatException;
import java.util.zip.Inflater;

import de.velcommuta.denul.networking.protobuf.c2s.C2S;
import de.velcommuta.denul.networking.protobuf.meta.MetaMessage;
import de.velcommuta.libvicbf.VICBF;

/**
 * Protocol employing Protobuf for message generation and parsing.
 */
public class ProtobufProtocol implements Protocol {
    private static final String TAG = "ProtobufProtocol";

    // Connection object
    Connection mConnection;

    VICBF mVICBF;

    @Override
    public int connect(Connection conn) {
        // Store the connection object
        mConnection = conn;
        if (!mConnection.isOpen()) {
            Log.e(TAG, "connect: Connection is not connected");
            return CONNECT_FAIL_NO_CONNECTION;
        }
        // Get a clientHello message
        MetaMessage.Wrapper ch = getClientHelloMsg();

        // Transceive and get reply wrapper message
        Log.d(TAG, "connect: Sending ClientHello");
        MetaMessage.Wrapper reply = transceiveWrapper(ch);
        if (reply == null) {
            Log.e(TAG, "connect: Wrapper parsing failed, aborting");
            return CONNECT_FAIL_PROTOCOL_ERROR;
        }
        // Extract the ServerHello from the wrapper
        C2S.ServerHello serverHello = toServerHello(reply);
        if (serverHello != null) {
            // If there are ever more protocol versions, implement a version check here.
            // For now, we will assume that the server is using a compatible protocol or will
            // dial itself back to our protocol version if it also knows later protocol versions
            if (serverHello.hasData()) {
                try {
                    Log.d(TAG, "connect: Compressed: " + bytesToHex(serverHello.getData().toByteArray()));
                    byte[] decompressed = decompress_data(serverHello.getData().toByteArray());
                    Log.d(TAG, "connect: Decompressed: " + bytesToHex(decompressed));
                    mVICBF = VICBF.deserialize(decompressed);
                    Log.d(TAG, "connect: Deserialized VICBF");
                } catch (IOException e) {
                    Log.e(TAG, "connect: IOException while parsing VICBF. Aborting");
                    return CONNECT_FAIL_PROTOCOL_ERROR;
                }
            } else {
                Log.e(TAG, "connect: ServerHello did not contain VICBF data");
                return CONNECT_FAIL_PROTOCOL_ERROR;
            }
        } else {
            Log.e(TAG, "connect: ServerHello parsing failed");
            return CONNECT_FAIL_PROTOCOL_ERROR;
        }
        return CONNECT_OK;
    }


    @Override
    public void disconnect() {
        try {
            mConnection.close();
        } catch (IOException e) {
            Log.w(TAG, "disconnect: IOException, ignoring");
        }
    }


    @Nullable
    @Override
    public byte[] get(String key) {
        // Check if the Connection is still open
        if (!mConnection.isOpen()) {
            Log.e(TAG, "get: Underlying Connection not connected");
            return GET_FAIL_NO_CONNECTION;
        } else if (!checkKeyFormat(key)) {
            Log.e(TAG, "get: Bad key format");
            return GET_FAIL_KEY_FMT;
        }
        // Check if the key is in the VICBF
        if (mVICBF.query(key)) {
            // Create a Get message for the key
            MetaMessage.Wrapper get = getGetMsg(key);
            // Query the server
            MetaMessage.Wrapper getReplyWrapper = transceiveWrapper(get);
            // Check if the server replied
            if (getReplyWrapper == null) {
                Log.e(TAG, "get: TransceiveWrapper failed, aborting");
                return GET_FAIL_NO_CONNECTION;
            }

            // Get the GetReply message from the Wrapper
            C2S.GetReply getReply = toGetReply(getReplyWrapper);
            // Ensure that we actually got something
            if (getReply == null) {
                Log.e(TAG, "get: Wrapper did not contain a GetReply, aborting");
                return GET_FAIL_PROTOCOL_ERROR;
            } else if (!getReply.getKey().equals(key)) {
                // The Keys do not match
                Log.w(TAG, "get: Server replied for different key, aborting");
                return GET_FAIL_PROTOCOL_ERROR;
            } else if (getReply.getOpcode() == C2S.GetReply.GetReplyCode.GET_FAIL_UNKNOWN_KEY) {
                // The server does not know about this key
                Log.w(TAG, "get: Get failed, server does not hold a value for the key");
                return GET_FAIL_KEY_NOT_TAKEN;
            } else if (getReply.getOpcode() == C2S.GetReply.GetReplyCode.GET_FAIL_UNKNOWN) {
                // The server has encountered an unknown error
                Log.e(TAG, "get: Get failed, server error");
                return GET_FAIL_PROTOCOL_ERROR;
            } else if (getReply.getOpcode() == C2S.GetReply.GetReplyCode.GET_FAIL_KEY_FMT) {
                // The server complained about the key format
                Log.e(TAG, "get: Get failed, bad key format");
                return GET_FAIL_KEY_FMT;
            } else if (getReply.getOpcode() == C2S.GetReply.GetReplyCode.GET_OK) {
                // The server retrieved the value for us
                // Check if the Value field is set
                if (getReply.hasValue()) {
                    // Return the value
                    return getReply.getValue().toByteArray();
                } else {
                    // The server did not send the value - this should not happen :(
                    Log.e(TAG, "get: Server reply did not contain data even though it should have");
                    return GET_FAIL_PROTOCOL_ERROR;
                }
            } else {
                // This condition should never occur if the protocol is used correctly
                Log.e(TAG, "get: No conditional held, something is wrong");
                return GET_FAIL_PROTOCOL_ERROR;
            }
        } else {
            return GET_FAIL_KEY_NOT_TAKEN;
        }
    }


    @Override
    public Map<String, byte[]> getMany(List<String> keys) {
        Map<String, byte[]> rv = new HashMap<>();
        for (String key : keys) {
            rv.put(key, get(key));
        }
        return rv;
    }


    @Override
    public int put(String key, byte[] value) {
        // Check if the Connection is still open
        if (!mConnection.isOpen()) {
            Log.e(TAG, "put: Underlying Connection not connected");
            return PUT_FAIL_NO_CONNECTION;
        } else if (!checkKeyFormat(key) || value == null) {
            Log.e(TAG, "put: Bad key or value format");
            return PUT_FAIL_KEY_FMT;
        }
        // Get a wrapper message with the key-value-pair
        MetaMessage.Wrapper store = getStoreMsg(key, value);
        // Transceive and get reply
        MetaMessage.Wrapper storeReplyWrapper = transceiveWrapper(store);
        // Check if the reply is null
        if (storeReplyWrapper == null) {
            Log.e(TAG, "put: Transceive failed, reply is null");
            return PUT_FAIL_NO_CONNECTION;
        }
        // Extract the StoreReply
        C2S.StoreReply storeReply = toStoreReply(storeReplyWrapper);
        // Check if extraction went well
        if (storeReply == null) {
            Log.e(TAG, "put: Reply did not contain a StoreReply");
            return PUT_FAIL_PROTOCOL_ERROR;
        } else if (!storeReply.getKey().equals(key)) {
            // Server did not reply with the correct key
            Log.e(TAG, "put: Reply contained incorrect key");
            return PUT_FAIL_PROTOCOL_ERROR;
        } else if (storeReply.getOpcode() == C2S.StoreReply.StoreReplyCode.STORE_FAIL_KEY_TAKEN) {
            // Server replied that the key was already taken
            Log.e(TAG, "put: Put failed, key was already taken");
            return PUT_FAIL_KEY_TAKEN;
        } else if (storeReply.getOpcode() == C2S.StoreReply.StoreReplyCode.STORE_FAIL_KEY_FMT) {
            // Server complained about the key format
            Log.e(TAG, "put: Put failed, bad key format");
            return PUT_FAIL_KEY_FMT;
        } else if (storeReply.getOpcode() == C2S.StoreReply.StoreReplyCode.STORE_FAIL_UNKNOWN) {
            // Server experienced unknown error :(
            Log.e(TAG, "put: Server got unknown error");
            return PUT_FAIL_PROTOCOL_ERROR;
        } else if (storeReply.getOpcode() == C2S.StoreReply.StoreReplyCode.STORE_OK) {
            // Success
            // Put the key into the local VICBF
            mVICBF.insert(key);
            // Return success
            return PUT_OK;
        }
        // This statement should be unreachable if nothing went completely wrong
        return PUT_FAIL_PROTOCOL_ERROR;
    }


    @Override
    public Map<String, Integer> putMany(Map<String, byte[]> records) {
        // Prepare return-hashtable
        Map<String, Integer> rv = new HashMap<>();
        // Send inserts for all values in the input dictionary
        for (String key : records.keySet()) {
            rv.put(key, put(key, records.get(key)));
        }
        return rv;
    }


    @Override
    public int del(String key, String auth) {
        // Check if the Connection is still open
        if (!mConnection.isOpen()) {
            Log.e(TAG, "del: Underlying Connection not connected");
            return DEL_FAIL_NO_CONNECTION;
        } else if (!checkKeyFormat(key) || auth == null || !checkAuthenticator(key, auth)) {
            Log.e(TAG, "del: Bad key or authenticator format");
            return DEL_FAIL_KEY_FMT;
        }
        Log.e(TAG, "del: Key: " + key);
        Log.e(TAG, "del: auth: " + auth);
        // Get a wrapper message with the key-value-pair
        MetaMessage.Wrapper delete = getDeleteMessage(key, auth);
        // Transceive and get reply
        MetaMessage.Wrapper deleteReplyWrapper = transceiveWrapper(delete);
        // Check if the reply is null
        if (deleteReplyWrapper == null) {
            Log.e(TAG, "del: Transceive failed, reply is null");
            return PUT_FAIL_NO_CONNECTION;
        }
        // Extract the DeleteReply
        C2S.DeleteReply deleteReply = toDeleteReply(deleteReplyWrapper);
        // Check if extraction went well
        if (deleteReply == null) {
            Log.e(TAG, "del: Reply did not contain a DeleteReply");
            return DEL_FAIL_PROTOCOL_ERROR;
        } else if (!deleteReply.getKey().equals(key)) {
            // Server did not reply with the correct key
            Log.e(TAG, "del: Reply contained incorrect key");
            return DEL_FAIL_PROTOCOL_ERROR;
        } else if (deleteReply.getOpcode() == C2S.DeleteReply.DeleteReplyCode.DELETE_OK) {
            // Success
            // Remove the key from the VICBF
            try {
                mVICBF.remove(key);
            } catch (Exception e) {
                Log.e(TAG, "del: Exception while trying to delete key from VICBF: " + e);
                // TODO Update if the VICBF retrieval code moves
                connect(mConnection);
            }
            return DEL_OK;
        } else if (deleteReply.getOpcode() == C2S.DeleteReply.DeleteReplyCode.DELETE_FAIL_NOT_FOUND) {
            // Server replied that no such key is stored on it
            Log.e(TAG, "del: Deletion failed, no such key");
            return DEL_FAIL_KEY_NOT_TAKEN;
        } else if (deleteReply.getOpcode() == C2S.DeleteReply.DeleteReplyCode.DELETE_FAIL_KEY_FMT) {
            // Server complained about the key format
            Log.e(TAG, "del: Deletion failed, bad key format");
            return DEL_FAIL_KEY_FMT;
        } else if (deleteReply.getOpcode() == C2S.DeleteReply.DeleteReplyCode.DELETE_FAIL_UNKNOWN) {
            // Server experienced unknown error :(
            Log.e(TAG, "del: Server got unknown error");
            return DEL_FAIL_PROTOCOL_ERROR;
        } else if (deleteReply.getOpcode() == C2S.DeleteReply.DeleteReplyCode.DELETE_FAIL_AUTH) {
            // Authentication token was not accepted by the server
            Log.e(TAG, "del: Authentication failed");
            return DEL_FAIL_AUTH_INCORRECT;
        }
        // This statement should be unreachable if nothing went completely wrong
        return PUT_FAIL_PROTOCOL_ERROR;
    }


    @Override
    public Map<String, Integer> delMany(Map<String, String> records) {
        return null;
    }

    // Helper functions
    /**
     * Send a wrapper message to the server and receive and parse a wrapper message in return
     * @param wrapper The wrapper to send to the server
     * @return The Wrapper that was received in return, or null, if an error occured
     */
    private MetaMessage.Wrapper transceiveWrapper(MetaMessage.Wrapper wrapper) {
        // prepare a byte[] for the reply
        byte[] reply;
        try {
            // Transceive, saving the result into the byte[]
            reply = mConnection.transceive(wrapper.toByteArray());
        } catch (IOException e) {
            Log.e(TAG, "connect: IOException during communcation: " + e.toString());
            return null;
        }
        // Convert byte[] into Wrapper and return it
        return toWrapperMessage(reply);
    }


    /**
     * Create a ClientHello message for the current protocol version
     * @return A wrapper message containing a ClientHello message
     */
    private MetaMessage.Wrapper getClientHelloMsg() {
        // Get a ClientHello builder and a wrapper builder
        C2S.ClientHello.Builder clientHello = C2S.ClientHello.newBuilder();
        MetaMessage.Wrapper.Builder wrapper = MetaMessage.Wrapper.newBuilder();
        // Set the client protocol version
        clientHello.setClientProto("1.0");
        // Pack the ClientHello into the Wrapper message
        wrapper.setClientHello(clientHello);
        // Build and return the Wrapper
        return wrapper.build();
    }


    /**
     * Create a Get message for a specific key
     * @param key The key to get the value for
     * @return A Get message wrapped in a Wrapper message
     */
    private MetaMessage.Wrapper getGetMsg(String key) {
        // Get a Get builder and a wrapper builder
        C2S.Get.Builder get = C2S.Get.newBuilder();
        MetaMessage.Wrapper.Builder wrapper = MetaMessage.Wrapper.newBuilder();
        // Set the key to Get
        get.setKey(key);
        // Pack the Get message in the Wrapper
        wrapper.setGet(get);
        // Build and return the Wrapper
        return wrapper.build();
    }


    /**
     * Create a Store message for a specific key-value-pair and wrap it in a Wrapper message
     * @param key The key of the KV Pair
     * @param values The value
     * @return A Wrapper containing a Store message for the Key-Value-Pair
     */
    private MetaMessage.Wrapper getStoreMsg(String key, byte[] values) {
        // Get a Put builder and a wrapper builder
        C2S.Store.Builder store = C2S.Store.newBuilder();
        MetaMessage.Wrapper.Builder wrapper = MetaMessage.Wrapper.newBuilder();
        // Set the key
        store.setKey(key);
        // Set the value
        store.setValue(ByteString.copyFrom(values));
        // Put the Store message into the Wrapper
        wrapper.setStore(store);
        // Build and return
        return wrapper.build();
    }


    /**
     * Create a Delete message for a specific key with its authenticator and wrap it in a Wrapper
     * message
     * @param key The key to delete
     * @param auth The authenticator
     * @return A Wrapper message containing a Delete message for the key-authenticator pair
     */
    private MetaMessage.Wrapper getDeleteMessage(String key, String auth) {
        // Get a Delete builder and a wrapper builder
        C2S.Delete.Builder delete = C2S.Delete.newBuilder();
        MetaMessage.Wrapper.Builder wrapper = MetaMessage.Wrapper.newBuilder();
        // Set the key
        delete.setKey(key);
        // Set the authenticator
        delete.setAuth(auth);
        // Put the Delete message into the Wrapper
        wrapper.setDelete(delete);
        // Build and return
        return wrapper.build();
    }


    /**
     * Extract a ServerHello message from a wrapper
     * @param wrapper The wrapper containing a ServerHello message
     * @return The ServerHello, or null, if the bytes did not represent a ServerHello message
     */
    private C2S.ServerHello toServerHello(MetaMessage.Wrapper wrapper) {
        if (wrapper.hasServerHello()) {
            return wrapper.getServerHello();
        } else {
            Log.e(TAG, "toServerHello: Wrapper message did not contain a ServerHello message");
            return null;
        }
    }


    /**
     * Extract a GetReply message from a wrapper
     * @param wrapper The wrapper containing a GetReply message
     * @return The GetReply, or null, if the bytes did not represent a GetReply message
     */
    private C2S.GetReply toGetReply(MetaMessage.Wrapper wrapper) {
        if (wrapper.hasGetReply()) {
            return wrapper.getGetReply();
        } else {
            Log.e(TAG, "toGetReply: Wrapper message did not contain a GetReply message");
            return null;
        }
    }


    /**
     * Extract a StoreReply message from a wrapper
     * @param wrapper The wrapper containing a StoreReply message
     * @return The StoreReply, or null, if the bytes did not represent a StoreReply message
     */
    private C2S.StoreReply toStoreReply(MetaMessage.Wrapper wrapper) {
        if (wrapper.hasStoreReply()) {
            return wrapper.getStoreReply();
        } else {
            Log.e(TAG, "toStoreReply: Wrapper message did not contain a StoreReply message");
            return null;
        }
    }


    /**
     * Extract a DeleteReply message from a wrapper
     * @param wrapper The wrapper containing a DeleteReply message
     * @return The DeleteReply, or null, if the bytes did not represent a DeleteReply message
     */
    private C2S.DeleteReply toDeleteReply(MetaMessage.Wrapper wrapper) {
        if (wrapper.hasDeleteReply()) {
            return wrapper.getDeleteReply();
        } else {
            Log.e(TAG, "toDeleteReply: Wrapper message did not contain a StoreReply message");
            return null;
        }
    }


    /**
     * Convert the byte[]-representation of a Wrapper message into a Wrapper message
     * @param bytes The byte[]-representation of a Wrapper message
     * @return The Wrapper message, or null, if the bytes did not represent a wrapper message
     */
    private MetaMessage.Wrapper toWrapperMessage(byte[] bytes) {
        try {
            return MetaMessage.Wrapper.parseFrom(bytes);
        } catch (InvalidProtocolBufferException e) {
            Log.e(TAG, "toWrapperMessage: Message was no wrapper message.");
            return null;
        }
    }


    /**
     * Uncompress a gzip'ed byte array into an uncompressed byte array
     * @param compressed The compressed byte array
     * @return The uncompressed byte array
     */
    private byte[] decompress_data(byte[] compressed) {
        // Get an Inflater to decompress the zlib-compressed input
        Inflater decompress = new Inflater();
        // Add the compressed data
        decompress.setInput(compressed);
        // Prepare an output stream and a buffer
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buffer = new byte[256];
        try {
            int n;
            // Read at most 256 bytes, set n to the number of read bytes. If n is larger than zero...
            while ((n = decompress.inflate(buffer)) > 0) {
                // ...write the read bytes into the Output Buffer
                out.write(buffer, 0, n);
            }
            // The data has been decompressed. Return the byte array
            return out.toByteArray();
        } catch (DataFormatException e) {
            Log.e(TAG, "decompress_data: Invalid data format, aborting");
            e.printStackTrace();
            return null;
        }
    }


    /**
     * Check the format of a key
     * @param key The key to check
     * @return true if the key has a valid format, false otherwise
     */
    protected static boolean checkKeyFormat(String key) {
        // The key must be a SHA256 hash => 64 hex characters
        return key != null && key.length() == 64 && key.matches("[0-9a-fA-F]+");
    }


    /**
     * Test if the authenticator actually authenticates the key
     * @param key The key to authenticate
     * @param auth The authenticator
     * @return true if the authentitcator is valid, false otherwise
     */
    protected static boolean checkAuthenticator(String key, String auth) {
        MessageDigest md;
        try {
            md = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            Log.e(TAG, "checkAuthenticator: SHA256 not supported");
            return false;
        }
        md.update(auth.getBytes());
        return ProtobufProtocol.bytesToHex(md.digest()).equals(key);
    }


    final protected static char[] hexArray = "0123456789ABCDEF".toCharArray();
    /**
     * Temporary helper function to be removed later. Used to convert byte[] to Strings for debugging
     * @param bytes The byte[]
     * @return A hexadecimal string representation of the byte[]
     * Source: http://stackoverflow.com/a/9855338/1232833
     */
    protected static String bytesToHex(byte[] bytes) {
        char[] hexChars = new char[bytes.length * 2];
        for ( int j = 0; j < bytes.length; j++ ) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = hexArray[v >>> 4];
            hexChars[j * 2 + 1] = hexArray[v & 0x0F];
        }
        return new String(hexChars);
    }
}
