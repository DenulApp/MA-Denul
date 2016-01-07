package de.velcommuta.denul.networking;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.UnknownHostException;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLHandshakeException;
import javax.net.ssl.SSLProtocolException;

import de.velcommuta.denul.crypto.RSA;
import de.velcommuta.denul.data.StudyRequest;

/**
 * HTTPS connection class offering static methods to check for the existance of specific HTTPS URLs and retrieve their
 * content. NOT an implementation of the {@link Connection} interface, as it has different goals
 */
public class HttpsVerifier {
    /**
     * Establish a TLS connection
     * @param addr The address to connect to. Must be a valid URL including the protocol prefix, or an exception will be
     *             thrown
     * @return A connected HttpsURLConnection
     * @throws MalformedURLException If the URL was invalid
     * @throws SSLProtocolException If the URL was valid, but no TLS connection could be established
     * @throws SSLHandshakeException If the URL was valid, a TLS connection could be established, but the certificate was invalid
     * @throws UnknownHostException If the URL was valid, but no such domain exists
     * @throws ClassCastException If the URL was a valid URL, but not an HTTPS URL.
     * @throws IOException If a general IOError occured. Note that the other exceptions are subclasses of this class,
     * so you will have to explicitly catch the others first or do some instanceof checking.
     */
    private static HttpsURLConnection connect(String addr) throws MalformedURLException, SSLHandshakeException, SSLProtocolException, UnknownHostException, ClassCastException, IOException {
        URL url = new URL(addr);
        return connect(url);
    }


    /**
     * Establish a TLS connection
     * @param url The URL object to use for the connection
     * @return A connected HttpsURLConnection
     * @throws SSLProtocolException If the URL was valid, but no TLS connection could be established
     * * @throws SSLHandshakeException If the URL was valid, a TLS connection could be established, but the certificate was invalid
     * @throws UnknownHostException If the URL was valid, but no such domain exists
     * @throws ClassCastException If the URL was a valid URL, but not an HTTPS URL.
     * @throws IOException If a general IOError occured. Note that the other exceptions are subclasses of this class,
     * so you will have to explicitly catch the others first or do some instanceof checking.
     */
    private static HttpsURLConnection connect(URL url) throws UnknownHostException, SSLProtocolException, ClassCastException, IOException {
        return (HttpsURLConnection) url.openConnection();
    }


    /**
     * Check if a specified URL exists and is reachable via HTTPS
     * @param addr The URL
     * @return True if the URL is a valid URL and does not 404, false if the URL is valid, but gives a 404 or the Host
     * cannot be found.
     * @throws MalformedURLException If the provided string is not a valid URL
     * @throws SSLProtocolException If the cert is invalid
     * @throws UnknownHostException If the URL seems valid, but no such domain exists
     */
    public static boolean exists(String addr) throws MalformedURLException, SSLProtocolException, UnknownHostException {
        try {
            HttpsURLConnection conn = connect(addr);
            return conn.getResponseCode() == 200;
        } catch (IOException e) {
            // If the exception was a MalformedURLException or SSLProtocolException, re-raise them, we're interested
            if (e instanceof MalformedURLException) throw (MalformedURLException) e;
            if (e instanceof SSLProtocolException) throw (SSLProtocolException) e;
            if (e instanceof UnknownHostException) throw (UnknownHostException) e;
            // If an SSLHandshakeException occurs, translate it into a generic SSLProtocolException
            if (e instanceof SSLHandshakeException) throw new SSLProtocolException(e.toString());
            e.printStackTrace();
        } catch (ClassCastException e) {
            // The URL was an HTTP URL => Invalid
            throw new IllegalArgumentException("URL was http, not https");
        }
        return false;
    }


    /**
     * Perform a file-based verification
     * @param request The request object
     * @return true if verification was successful, false otherwise
     */
    public static boolean verifyFile(StudyRequest request) {
        assert request != null;
        assert request.webpage != null;
        assert request.pubkey != null;
        try {
            // Build base URL to verify it works
            URL url = new URL(request.webpage);
            if (!url.getProtocol().equals("https")) {
                throw new IllegalArgumentException("URL is not https");
            }
            // Replace the path with a path to https://domain.tld/old/path/.study.txt
            URL verify = new URL(url, ".study.txt");
            InputStream in = verify.openStream();
            BufferedReader reader = new BufferedReader(new InputStreamReader(in));
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.split(" ")[0].equals(RSA.fingerprint(request.pubkey))) {
                    return true;
                }
            }
            // If this statement is reached, the file did not contain the verification token in the right format
        } catch (ClassCastException | UnknownHostException | SSLProtocolException | SSLHandshakeException | MalformedURLException | FileNotFoundException e) {
            // We're not interested in a stack trace here
        } catch (IOException e) {
            // A stacktrace could be helpful, print it
            e.printStackTrace();
        }
        return false;
    }


    /**
     * Perform a <meta>-Tag-based verification
     * @param request The request object
     * @return true if verification was successful, false otherwise
     */
    public static boolean verifyMeta(StudyRequest request) {
        assert request != null;
        assert request.webpage != null;
        assert request.pubkey != null;
        try {
            // Build base URL to verify it works
            URL url = new URL(request.webpage);
            // Ensure the URL is https
            if (!url.getProtocol().equals("https")) {
                throw new IllegalArgumentException("URL is not https");
            }
            // Read the code from the website
            InputStream in = url.openStream();
            BufferedReader reader = new BufferedReader(new InputStreamReader(in));
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.contains(String.format("<meta name='study-key' content='%s'>", RSA.fingerprint(request.pubkey)))) {
                    return true;
                }
            }
            // If this statement is reached, the file did not contain the verification token in the right format
        } catch (ClassCastException | UnknownHostException | SSLProtocolException | SSLHandshakeException | MalformedURLException | FileNotFoundException e) {
            // We're not interested in a stack trace here
        } catch (IOException e) {
            // A stacktrace could be helpful, print it
            e.printStackTrace();
        }
        return false;
    }
}
