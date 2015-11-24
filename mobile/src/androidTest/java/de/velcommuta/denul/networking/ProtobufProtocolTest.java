package de.velcommuta.denul.networking;

import junit.framework.TestCase;

import java.io.IOException;
import java.net.UnknownHostException;

import javax.net.ssl.SSLHandshakeException;

/**
 * Test suite for the ProtobufProtocol implementation
 */
public class ProtobufProtocolTest extends TestCase {
    // The host and port to connect to. Please make sure that:
    // - The server application is running on that host and port
    // - the server is using a valid certificate for that hostname
    private static final String host = "cdn.velcommuta.de";
    private static final int port = 5566;

    /**
     * Test the connection establishment
     */
    public void testConnectionEst() {
        try {
            // Establish a TLS connection
            Connection c = new TLSConnection(host, port);
            // Create a protocol object
            Protocol p = new ProtobufProtocol();
            // connect to the server using the protocol and TLS connection
            p.connect(c);
            // Disconnect from the server
            p.disconnect();
        } catch (UnknownHostException e) {
            e.printStackTrace();
            fail("UnknownHost - please make sure the host variable is set correctly");
        } catch (SSLHandshakeException e) {
            e.printStackTrace();
            fail("SSLHandshake failed - are you sure the certificate is valid?");
        } catch (IOException e) {
            e.printStackTrace();
            fail("IOException - are you sure the server is running?");
        }
    }
}
