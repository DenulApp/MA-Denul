package de.velcommuta.denul.networking;

import android.util.Log;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;

import javax.net.SocketFactory;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLHandshakeException;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;

/**
 * A TCP connection using TLS to communicate with the server.
 */
public class TLSConnection implements Connection {
    private static final String TAG = "TLSConnection";

    SSLSocket mSocket;

    /**
     * Establish a TCP connection protected by TLS.
     * @param host Either the IP or the FQDN of the server to connect to
     * @param port The port number to connect to
     * @throws IOException If the underlying socket throws it
     * @throws UnknownHostException If the underlying socket throws it
     * @throws SSLHandshakeException If the certificate hostname validation fails
     */
    public TLSConnection(String host, int port) throws IOException, UnknownHostException, SSLHandshakeException {
        Log.d(TAG, "TLSConnection: Establishing connection to " + host + ":" + port);
        // Get SSL Socket factory
        SocketFactory factory = SSLSocketFactory.getDefault();
        // Create a socket and connect to the host and port, throwing an exception if anything
        // goes wrong
        mSocket = (SSLSocket) factory.createSocket(host, port);
        // We need to verify the certificate hostname explicitly. Get a hostname verifier
        HostnameVerifier hv = HttpsURLConnection.getDefaultHostnameVerifier();
        // Get an SSLSession object
        SSLSession s = mSocket.getSession();
        // Verify the hostname
        if (!hv.verify(host, s)) {
            Log.e(TAG, "TLSConnection: Hostname verification failed - expected " + host + ", found " + s.getPeerPrincipal());
            throw new SSLHandshakeException("Expected " + host + ", but found " + s.getPeerPrincipal());
        }
        Log.d(TAG, "TLSConnection: Connection established using " + s.getProtocol() + " (" +  s.getCipherSuite() + ")");
    }

    @Override
    public byte[] transceive(byte[] message) throws IOException {
        // Get an output stream from the socket (to send data over)
        OutputStream out = mSocket.getOutputStream();
        // Get an input stream from the socket (to receive data with)
        BufferedInputStream in = new BufferedInputStream(mSocket.getInputStream());

        // Prepare the byte[] with the length information of the message
        byte[] len = ByteBuffer.allocate(4).putInt(message.length).array();
        // Combine length and message into one byte[]
        byte[] fullmsg = new byte[4 + message.length];
        System.arraycopy(len, 0, fullmsg, 0, len.length);
        System.arraycopy(message, 0, fullmsg, len.length, message.length);

        // Send the message over the socket
        out.write(fullmsg);
        out.flush();
        Log.d(TAG, "transceive: Message sent");

        // Receive the reply - Receive the length of the reply
        byte[] lenbytes = new byte[4];
        // Read 4 bytes from the wire (in a loop to make sure that we actually get 4 bytes)
        int rcvlen = 0;
        do {
            rcvlen += in.read(lenbytes, rcvlen, 4-rcvlen);
        } while (rcvlen < 4);
        // Parse the received bytes into an integer
        int replylen = ByteBuffer.wrap(lenbytes).getInt();
        Log.d(TAG, "transceive: Reply has " + replylen + " bytes");

        // Receive the body of the reply (again, in a loop to make sure we get it all)
        byte[] replyBytes = new byte[replylen];
        rcvlen = 0;
        do {
            rcvlen += in.read(replyBytes, rcvlen, replylen-rcvlen);
        } while (rcvlen < replylen);

        // Return received bytes
        Log.d(TAG, "transceive: Reply received, returning");
        return replyBytes;
    }

    @Override
    public void close() throws IOException {
        if (mSocket.isConnected()) {
            Log.d(TAG, "close: Closing open socket");
            mSocket.close();
        } else {
            Log.w(TAG, "close: Trying to close socket that is not open");
        }
    }

    @Override
    public boolean isOpen() {
        return mSocket.isConnected();
    }
}
