package de.velcommuta.denul.networking;

import java.net.MalformedURLException;
import java.net.URL;
import org.xbill.DNS.Lookup;
import org.xbill.DNS.Record;
import org.xbill.DNS.TextParseException;
import org.xbill.DNS.Type;

import de.velcommuta.denul.crypto.RSA;
import de.velcommuta.denul.data.StudyRequest;

/**
 * Verifier using the DNS and TXT records
 */
public class DNSVerifier {
    /**
     * Verify the fingerprint of a public key using DNS TXT records
     * @param request The StudyRequest
     * @return True if the fingerprint matches, false if not or something goes wrong
     */
    public static boolean verify(StudyRequest request) {
        assert request != null;
        assert request.pubkey != null;
        try {
            // Calculate fingerprint
            // TODO Once the enum for the key type exists, use it to decide which fingerprint calculation to use
            String fingerprint = RSA.fingerprint(request.pubkey);
            assert fingerprint != null;
            // Extract URL
            String url = new URL(request.webpage).getHost();
            // Clear cache
            Lookup.getDefaultCache(Type.TXT).clearCache();
            // Query for TXT record
            Record[] txt = new Lookup(url, Type.TXT).run();
            // Print TXT records
            for (Record r : txt) {
                // The record value is enclosed in parenthesis
                if (r.rdataToString().equals("\"" + fingerprint + "\"")) return true;
            }
            return false;
        } catch (MalformedURLException | TextParseException e) {
            e.printStackTrace();
            return false;
        }
    }
}
