package de.velcommuta.denul.networking;

import junit.framework.TestCase;

import java.net.MalformedURLException;
import java.net.UnknownHostException;

import javax.net.ssl.SSLProtocolException;

/**
 * Test cases for the HTTPS Verifier
 */
public class HttpsVerifierTest extends TestCase {
    /**
     * Test if a valid URL is recognized as valid
     */
    public void testExistsValid() {
        try {
            assertTrue(HttpsVerifier.exists("https://www.google.de/intl/en/about/"));
        } catch (MalformedURLException e) {
            fail(e.toString());
        } catch (SSLProtocolException e) {
            fail("Unexpected SSL protocol error");
        } catch (UnknownHostException e) {
            fail("Unexpected unknown host exception");
        }
    }

    /**
     * Test if a valid URL is recognized as valid
     */
    public void testExistsValidButNotTLS() {
        try {
            HttpsVerifier.exists("http://example.com");
            fail("No exception thrown");
        } catch (MalformedURLException e) {
            fail(e.toString());
        } catch (SSLProtocolException e) {
            fail("Unexpected SSL protocol error");
        } catch (IllegalArgumentException e) {
            assertTrue(true);
        } catch (UnknownHostException e) {
            fail("Unexpected unknown host exception");
        }
    }

    /**
     * Test if an invalid URL is recognized as invalid
     */
    public void testExistsInvalidURL() {
        try {
            HttpsVerifier.exists("This is not a URL :(");
            fail("No exception was thrown");
        } catch (MalformedURLException e) {
            assertTrue(true);
        } catch (SSLProtocolException e) {
            fail("Unexpected SSL protocol error");
        } catch (UnknownHostException e) {
            fail("Unexpected unknown host exception");
        }
    }

    /**
     * Test if a valid URL giving a 404 is recognized as not existing
     */
    public void testExistsValidURL404() {
        try {
            assertFalse(HttpsVerifier.exists("https://google.com/thisdoesnotexist"));
        } catch (MalformedURLException e) {
            fail("Unexpected Exception " + e.toString());
        } catch (SSLProtocolException e) {
            fail("Unexpected SSL protocol error");
        } catch (UnknownHostException e) {
            fail("Unexpected unknown host exception");
        }
    }

    /**
     * Test if incomplete, protocol-less URLs are recognized anyway
     */
    public void testExistsValidURLWithoutPrefix() {
        try {
            HttpsVerifier.exists("google.com");
            fail("No exception thrown - even though it would be nice :(");
        } catch (MalformedURLException e) {
            assertTrue(true);
        } catch (SSLProtocolException e) {
            fail("Unexpected SSL protocol error");
        } catch (UnknownHostException e) {
            fail("Unexpected unknown host exception");
        }
    }

    /**
     * Test connection to URL with cert that is considered invalid by Java
     */
    public void testExistsValidURLBadCert() {
        try {
            HttpsVerifier.exists("https://cacert.org");
            fail("No exception thrown");
        } catch (MalformedURLException e) {
            fail("Unexpected Exception");
        } catch (SSLProtocolException e) {
            assertTrue(true);
        } catch (UnknownHostException e) {
            fail("Unexpected unknown host exception");
        }
    }

    /**
     * Test what happens if we connect to a plausible, but nonexistant domain
     */
    public void testExistsPlausibleButNonexistantURL() {
        try {
            HttpsVerifier.exists("https://thisurlseemslegitbutisinfactinvalid.org");
            fail("No exception thrown");
        } catch (MalformedURLException e) {
            fail("Unexpected Exception");
        } catch (SSLProtocolException e) {
            fail("Unexpected SSLProtocolException");
        } catch (UnknownHostException e) {
            assertTrue(true);
        }
    }
}