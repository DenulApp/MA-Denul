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

    /**
     * Test a Get for a malformed key
     */
    public void testGetBadKey() {
        try {
            // Establish a TLS connection
            Connection c = new TLSConnection(host, port);
            // Create protocol object
            Protocol p = new ProtobufProtocol();
            // Connect to the server
            p.connect(c);
            // Try a Get for a bad key
            byte[] reply = p.get("abadkey");
            // make sure the reply is GET_FAIL_KEY_FMT
            assertEquals(reply, Protocol.GET_FAIL_KEY_FMT);
            // Disconnect from the server
            p.disconnect();
        } catch (UnknownHostException e) {
            e.printStackTrace();
            fail("UnknownHostException - please make sure the host variable is set correctly");
        } catch (SSLHandshakeException e) {
            e.printStackTrace();
            fail("SSLHandshale failed - are you sure the certificate is valid?");
        } catch (IOException e) {
            e.printStackTrace();
            fail("IOException - are you sure the server is running?");
        }
    }


    /**
     * Test a Get for a malformed key
     */
    public void testGetMissingKey() {
        try {
            // Establish a TLS connection
            Connection c = new TLSConnection(host, port);
            // Create protocol object
            Protocol p = new ProtobufProtocol();
            // Connect to the server
            p.connect(c);
            // Try a Get for a nonexistant key
            byte[] reply = p.get("88c1428301b12afd0ad86124bcbe5cd2451f21c2e49797a9999f151df0cafd74");
            // make sure the reply is null
            assertNull(reply);
            // Disconnect from the server
            p.disconnect();
        } catch (UnknownHostException e) {
            e.printStackTrace();
            fail("UnknownHostException - please make sure the host variable is set correctly");
        } catch (SSLHandshakeException e) {
            e.printStackTrace();
            fail("SSLHandshale failed - are you sure the certificate is valid?");
        } catch (IOException e) {
            e.printStackTrace();
            fail("IOException - are you sure the server is running?");
        }
    }


    /**
     * Test the key format verifier
     */
    public void testKeyVerifier() {
        // test with good SHA256
        assertTrue(ProtobufProtocol.checkKeyFormat ("88c1428301b12afd0ad86124bcbe5cd2451f21c2e49797a9999f151df0cafd74"));
        // test with character outside of hex space
        assertFalse(ProtobufProtocol.checkKeyFormat("88c1428301b12afd0ad86124bcbe5cd2451f21c2e49797a9999f151df0cafd7g"));
        // test with bad length
        assertFalse(ProtobufProtocol.checkKeyFormat("88c1428301b12afd0ad86124bcbe5cd2451f21c2e49797a9999f151df0cafd7"));
        // test with null
        assertFalse(ProtobufProtocol.checkKeyFormat(null));
    }
}
