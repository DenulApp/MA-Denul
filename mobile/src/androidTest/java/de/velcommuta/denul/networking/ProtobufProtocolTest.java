package de.velcommuta.denul.networking;

import junit.framework.TestCase;

import java.io.IOException;
import java.net.UnknownHostException;

import javax.net.ssl.SSLHandshakeException;

/**
 * Test suite for the ProtobufProtocol implementation
 */
public class ProtobufProtocolTest extends TestCase {
    private static final String host = "cdn.velcommuta.de";
    private static final int port = 5566;

    /**
     * Test the connection establishment
     */
    public void testConnectionEst() {
        try {
            Connection c = new TLSConnection(host, port);
            Protocol p = new ProtobufProtocol();
            p.connect(c);
            p.disconnect();
        } catch (UnknownHostException e) {
            e.printStackTrace();
            fail("UnknownHost");
        } catch (SSLHandshakeException e) {
            e.printStackTrace();
            fail("SSLHandshake");
        } catch (IOException e) {
            e.printStackTrace();
            fail("IOException");
        }
    }
}
