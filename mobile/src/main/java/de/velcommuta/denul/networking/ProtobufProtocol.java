package de.velcommuta.denul.networking;

import android.support.annotation.Nullable;
import android.util.Log;

import com.google.protobuf.InvalidProtocolBufferException;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Dictionary;
import java.util.List;
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
        // Get a clientHello message
        MetaMessage.Wrapper ch = getClientHelloMsg();

        // Transceive and get reply wrapper message
        Log.d(TAG, "connect: Sending ClientHello");
        MetaMessage.Wrapper reply = transceiveWrapper(ch);
        if (reply == null) {
            Log.e(TAG, "connect: Wrapper parsing failed, aborting");
            return CONNECT_FAIL_UNKNOWN_MESSAGE;
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
                    return CONNECT_FAIL_UNKNOWN_MESSAGE;
                }
            } else {
                Log.e(TAG, "connect: ServerHello did not contain VICBF data");
                return CONNECT_FAIL_UNKNOWN_MESSAGE;
            }
        } else {
            Log.e(TAG, "connect: ServerHello parsing failed");
            return CONNECT_FAIL_UNKNOWN_MESSAGE;
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
        return new byte[0];
    }


    @Override
    public Dictionary<String, byte[]> getMany(List<String> keys) {
        return null;
    }


    @Override
    public int put(String key, byte[] value) {
        return 0;
    }


    @Override
    public Dictionary<String, Integer> putMany(Dictionary<String, byte[]> records) {
        return null;
    }


    @Override
    public int del(String key, String authenticator) {
        return 0;
    }


    @Override
    public Dictionary<String, Integer> delMany(Dictionary<String, String> records) {
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


    final protected static char[] hexArray = "0123456789ABCDEF".toCharArray();
    /**
     * Temporary helper function to be removed later. Used to convert byte[] to Strings for debugging
     * @param bytes The byte[]
     * @return A hexadecimal string representation of the byte[]
     * Source: http://stackoverflow.com/a/9855338/1232833
     */
    private static String bytesToHex(byte[] bytes) {
        char[] hexChars = new char[bytes.length * 2];
        for ( int j = 0; j < bytes.length; j++ ) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = hexArray[v >>> 4];
            hexChars[j * 2 + 1] = hexArray[v & 0x0F];
        }
        return new String(hexChars);
    }
}
