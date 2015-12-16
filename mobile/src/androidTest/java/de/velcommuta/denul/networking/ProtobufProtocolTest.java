package de.velcommuta.denul.networking;

import junit.framework.TestCase;

import java.io.IOException;
import java.net.UnknownHostException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;

import javax.net.ssl.SSLHandshakeException;

import de.velcommuta.denul.data.DataBlock;
import de.velcommuta.denul.data.TokenPair;
import de.velcommuta.denul.util.FormatHelper;

/**
 * Test suite for the ProtobufProtocol implementation
 */
public class ProtobufProtocolTest extends TestCase {
    // The host and port to connect to. Please make sure that:
    // - The server application is running on that host and port
    // - the server is using a valid certificate for that hostname
    private static final String host = "denul.velcommuta.de";
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
            byte[] reply = p.get(new TokenPair("abadkey".getBytes(), "abadrevocation".getBytes()));
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
     * Test a Get for a missing key
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
            byte[] missingkey = new byte[32];
            new Random().nextBytes(missingkey);
            byte[] reply = p.get(new TokenPair(missingkey, missingkey));
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
     * Test the put functions, get the value, and delete it afterwards
     */
    public void testPutGetDelete() {
        try {
            // Establish a TLS connection
            Connection c = new TLSConnection(host, port);
            // Protocol object
            Protocol p = new ProtobufProtocol();
            // Connect
            p.connect(c);
            // Get a random key and value
            byte[] auth = new byte[32];
            new Random().nextBytes(auth);
            byte[] key = authToKey(auth);
            TokenPair tokens = new TokenPair(key, auth);
            DataBlock data = new DataBlock(key, auth, key);
            // Put it on the server
            assertEquals(p.put(data), Protocol.PUT_OK);
            // Retrieve the value
            byte[] stored = p.get(tokens);
            // Test if the returned value is equal to the one we stored
            assertTrue(Arrays.equals(auth, stored));
            // Delete the key from the server and ensure it worked
            assertEquals(p.del(tokens), Protocol.DEL_OK);
            // Disconnect
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
     * Test the *Many-function of the ProtobufProtocol
     */
    public void testPutGetDeleteMany() {
        try {
            // Est. connection
            Connection c = new TLSConnection(host, port);
            // Protocol
            Protocol p = new ProtobufProtocol();
            // Connect
            p.connect(c);
            // Prepare five sets of keys- values and authenticators
            List<DataBlock> keyvalue = new LinkedList<>();
            List<TokenPair> keypairs = new LinkedList<>();
            Map<TokenPair, DataBlock> kv = new HashMap<>();
            for (int i = 0; i < 5; i++) {
                byte[] value = new byte[32];
                new Random().nextBytes(value);
                byte[] key = authToKey(value);
                TokenPair pair = new TokenPair(key, value);
                DataBlock data = new DataBlock(key, value, key);
                keyvalue.add(data);
                keypairs.add(pair);
                kv.put(pair, data);
            }
            // Insert all key-value-pairs
            Map<DataBlock, Integer> insert_return = p.putMany(keyvalue);
            // Make sure it worked
            for (DataBlock key : keyvalue) {
                assertEquals((int) insert_return.get(key), Protocol.PUT_OK);
            }
            // Query all key-value-pairs
            Map<TokenPair, byte[]> get_return = p.getMany(keypairs);
            // Make sure it worked
            for (TokenPair key : kv.keySet()) {
                assertTrue(FormatHelper.bytesToHex(kv.get(key).getCiphertext()) + " != " + FormatHelper.bytesToHex(get_return.get(key)),
                        Arrays.equals(kv.get(key).getCiphertext(), get_return.get(key)));
            }
            // Delete all key-value-pairs
            Map<TokenPair, Integer> del_return = p.delMany(keypairs);
            // Make sure it worked
            for (TokenPair key : keypairs) {
                assertEquals((int) del_return.get(key), Protocol.DEL_OK);
            }
            // Close connection
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
     * Test the put functions, get the value, and delete it afterwards
     */
    public void testPutGetRevokeDelete() {
        try {
            // Establish a TLS connection
            Connection c = new TLSConnection(host, port);
            // Protocol object
            Protocol p = new ProtobufProtocol();
            // Connect
            p.connect(c);
            // Get a random key and value
            byte[] auth = new byte[32];
            new Random().nextBytes(auth);
            byte[] key = authToKey(auth);
            TokenPair tokens = new TokenPair(key, auth);
            DataBlock data = new DataBlock(key, auth, key);
            // Put it on the server
            assertEquals(p.put(data), Protocol.PUT_OK);
            // Retrieve the value
            byte[] stored = p.get(tokens);
            // Test if the returned value is equal to the one we stored
            assertTrue(Arrays.equals(auth, stored));
            // Revoke the key on th server
            assertEquals(p.revoke(tokens), Protocol.REV_OK);
            // Ensure that the replacement worked
            assertTrue(Arrays.equals(p.get(tokens), new byte[] {0x42}));
            // Delete the key from the server and ensure it worked
            assertEquals(p.del(tokens), Protocol.DEL_OK);
            // Disconnect
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
     * Test putting a bad key on the server
     */
    public void testPutBadKey() {
        try {
            // Establish a TLS connection
            Connection c = new TLSConnection(host, port);
            // Protocol object
            Protocol p = new ProtobufProtocol();
            // Connect
            p.connect(c);
            // Get a random key and value
            byte[] value = new byte[31];
            new Random().nextBytes(value);
            byte[] key = value;
            // Put it on the server
            assertEquals(p.put(new DataBlock(key, value, key)), Protocol.PUT_FAIL_KEY_FMT);
            // Retrieve the value
            byte[] stored = p.get(new TokenPair(key, value));
            // Test if the returned value is equal to the one we stored
            assertTrue(Arrays.equals(stored, Protocol.GET_FAIL_KEY_FMT));
            // Disconnect
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
     * Helper function to derive a key that can be authenticated using the provided auth string
     * @param auth Authenticator
     * @return A key that is authenticated by that authenticator
     */
    private byte[] authToKey(byte[] auth) {
        MessageDigest md;
        try {
            md = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            fail("SHA256 not supported");
            return null;
        }
        md.update(auth);
        return md.digest();
    }
}
