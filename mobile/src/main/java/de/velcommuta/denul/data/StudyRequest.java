package de.velcommuta.denul.data;

import com.google.protobuf.InvalidProtocolBufferException;

import java.security.PublicKey;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

import de.velcommuta.denul.crypto.KexStub;
import de.velcommuta.denul.crypto.KeyExchange;
import de.velcommuta.denul.crypto.RSA;
import de.velcommuta.denul.networking.protobuf.study.StudyMessage;

/**
 * Data holder class for study requests
 */
public class StudyRequest {
    // Static constants
    public static final int VERIFY_UNKNOWN = 0;
    public static final int VERIFY_FILE = 1;
    public static final String VERIFY_FILE_TITLE = "(easy) File";
    public static final String VERIFY_FILE_DESC_SHORT = "You will have to put a file into a specific location relative to your study URL";
    public static final String VERIFY_FILE_DESC_LONG = "To authenticate your request, you will have to put the following string into its own line in the file at %s:\n%s";
    public static final int VERIFY_META = 2;
    public static final String VERIFY_META_TITLE = "(advanced) <meta>-Tag";
    public static final String VERIFY_META_DESC_SHORT = "You will have to add a special <meta> tag to the source code of your study website";
    public static final String VERIFY_META_DESC_LONG = "To authenticate your request, you will have to put the following <meta> tag into the <head> of the HTML document at %s:\n<meta name='study-key' content='%s'>";
    public static final int VERIFY_DNS = 3;
    public static final String VERIFY_DNS_TITLE = "(expert) DNS Entry";
    public static final String VERIFY_DNS_DESC_SHORT = "You will have to add a TXT record to the DNS entries of your domain";
    public static final String VERIFY_DNS_DESC_LONG = "To authenticate your request, you will have to put the following value into the TXT record of the domain %s:\n%s \nNote that you can only have one such record at a time per domain name.";
    // FIXME Debugging helper, remove
    public static final String VERIFY_SKIP = "(debug) Skip";
    public static final String VERIFY_SKIP_DESC_SHORT = "Skip verification (debugging helper, remove)";

    public static final HashMap<String, String> verificationOptions = new HashMap<>();
    public static final HashMap<String, String> verificationDetails = new HashMap<>();
    static {
        // Fill short descriptions
        verificationOptions.put(VERIFY_FILE_TITLE, VERIFY_FILE_DESC_SHORT);
        verificationOptions.put(VERIFY_META_TITLE, VERIFY_META_DESC_SHORT);
        verificationOptions.put(VERIFY_DNS_TITLE, VERIFY_DNS_DESC_SHORT);
        // FIXME Debugging helper, remove
        verificationOptions.put(VERIFY_SKIP, VERIFY_SKIP_DESC_SHORT);
        // Fill long descriptions
        verificationDetails.put(VERIFY_FILE_TITLE, VERIFY_FILE_DESC_LONG);
        verificationDetails.put(VERIFY_META_TITLE, VERIFY_META_DESC_LONG);
        verificationDetails.put(VERIFY_DNS_TITLE, VERIFY_DNS_DESC_LONG);
    }

    // Value holder fields
    public long id;
    public String name;
    public String institution;
    public String webpage;
    public String description;
    public String purpose;
    public String procedures;
    public String risks;
    public String benefits;
    public String payment;
    public String conflicts;
    public String confidentiality;
    public String participationAndWithdrawal;
    public String rights;
    public List<Investigator> investigators = new LinkedList<>();
    public List<DataRequest> requests = new LinkedList<>();
    public int verification = VERIFY_UNKNOWN;

    public boolean participating;

    // Cryptographic material
    public PublicKey pubkey;
    public KeyExchange exchange;

    public byte[] key_in;
    public byte[] ctr_in;
    public byte[] key_out;
    public byte[] ctr_out;

    // Queue identifier on the server
    public byte[] queue;

    /**
     * Constructor. Initializes the Queue Identifier with random data
     */
    public StudyRequest() {
        randomizeQueueIdentifier();
    }

    /**
     * Randomize the queue identifier
     */
    public void randomizeQueueIdentifier() {
        queue = new byte[16];
        new Random().nextBytes(queue);
    }

    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("Name: ");
        builder.append(name);
        builder.append("\n");

        builder.append("Institution: ");
        builder.append(institution);
        builder.append("\n");

        builder.append("Web page: ");
        builder.append(webpage);
        builder.append("\n\n");

        builder.append("Description:\n");
        builder.append(description);
        builder.append("\n\n");

        builder.append("Purpose:\n");
        builder.append(purpose);
        builder.append("\n\n");

        builder.append("Procedures:\n");
        builder.append(procedures);
        builder.append("\n\n");

        builder.append("Risks:\n");
        builder.append(risks);
        builder.append("\n\n");

        builder.append("Benefits:\n");
        builder.append(benefits);
        builder.append("\n\n");

        builder.append("Payment:\n");
        builder.append(payment);
        builder.append("\n\n");

        builder.append("Conflicts of Interest:\n");
        builder.append(conflicts);
        builder.append("\n\n");

        builder.append("Confidentiality:\n");
        builder.append(confidentiality);
        builder.append("\n\n");

        builder.append("Participation and Withdrawal:\n");
        builder.append(participationAndWithdrawal);
        builder.append("\n\n");

        builder.append("Subjects Rights:\n");
        builder.append(rights);
        builder.append("\n\n");

        for (int i = 0; i < investigators.size(); i++) {
            builder.append("Investigator #");
            builder.append(i+1);
            builder.append(":\n");
            builder.append(investigators.get(i).toString());
            builder.append("\n");
        }

        builder.append("\n");
        for (int i = 0; i < requests.size(); i++) {
            builder.append("Data Request #");
            builder.append(i+1);
            builder.append(":\n");
            builder.append(requests.get(i).toString());
            builder.append("\n");
        }

        return builder.toString();
    }

    /**
     * Data holder class for Investigators associated with a study
     */
    public static class Investigator {
        public String name;
        public String institution;
        public String group;
        public String position;

        public String toString() {
            StringBuilder builder = new StringBuilder();
            builder.append("Investigator Name: ");
            builder.append(name);

            builder.append("\nInstitution: ");
            builder.append(institution);

            builder.append("\nGroup: ");
            builder.append(group);

            builder.append("\nPosition: ");
            builder.append(position);

            builder.append("\n");
            return builder.toString();
        }

        @Override
        public boolean equals(Object o) {
            if (o == null || !(o instanceof Investigator)) return false;
            Investigator other = (Investigator) o;
            return ((other.name.equals(name)) &&
                    (other.institution.equals(institution)) &&
                    (other.group.equals(group)) &&
                    (other.position.equals(position)));
        }


        /**
         * Deserialize an Investigator from a Protocol Buffer
         * @param inv The serialized investigator
         * @return The deserialized investigator
         */
        public static Investigator fromProtobuf(StudyMessage.StudyCreate.Investigator inv) {
            Investigator rv = new Investigator();
            rv.name = inv.getName();
            rv.institution = inv.getInstitution();
            rv.position = inv.getPosition();
            rv.group = inv.getGroup();
            return rv;
        }
    }

    /**
     * Data holder class for information about what data is requested in what granularity
     */
    public static class DataRequest {
        public static final String[] TYPES = {"GPS Tracks"};
        public static final int TYPE_GPS = 0;

        public static final String[] GRANULARITIES_GPS = {"Full GPS tracks", "Duration, time and distance only"};
        public static final int GRANULARITY_FINE = 0;
        public static final int GRANULARITY_COARSE = 1;
        public static final int GRANULARITY_VERY_COARSE = 2;

        public Integer type;
        public Integer granularity;
        public Integer frequency;


        /**
         * Check if the provided Shareable is covered by the DataRequest
         * @param sh The shareable to consider
         * @return true if the DataRequest covers the Shareable, false otherwise
         */
        public boolean matches(Shareable sh) {
            if (sh == null) return false;
            switch (sh.getType()) {
                case Shareable.SHAREABLE_TRACK:
                    return type == TYPE_GPS;
                default: // TODO Add further shareable / request types here
                    return false;
            }
        }

        public String toString() {
            StringBuilder builder = new StringBuilder();
            builder.append("Data type: ");
            if (type != null) {
                builder.append(TYPES[type]);
            } else {
                builder.append("unset");
            }

            builder.append("\nGranularity: ");
            if (granularity != null) {
                if (type == TYPE_GPS) {
                    builder.append(GRANULARITIES_GPS[granularity]);
                }
            } else {
                builder.append("unset");
            }

            if (frequency != null) {
                builder.append("\nFrequency: Updated every ");
                builder.append(frequency);
                builder.append(" hour(s)");
            } else {
                builder.append("\nFrequency: unset");
            }

            return builder.toString();
        }

        @Override
        public boolean equals(Object o) {
            if (o == null || !(o instanceof DataRequest)) return false;
            DataRequest other = (DataRequest) o;
            return other.frequency.equals(frequency) &&
                    other.type.equals(type) &&
                    other.granularity.equals(granularity);
        }


        /**
         * Deserialize a DataRequest from a protocol buffer
         * @param req The protocol buffer
         * @return The deserialized data request
         */
        public static DataRequest fromProtobuf(StudyMessage.StudyCreate.DataRequest req) {
            DataRequest rv = new DataRequest();
            if (req.getDatatype() == StudyMessage.StudyCreate.DataType.DATA_GPS_TRACK) {
                rv.type = TYPE_GPS;
            } // TODO Add further data types here
            if (req.getGranularity() == StudyMessage.StudyCreate.DataGranularity.GRAN_FINE) {
                rv.granularity = GRANULARITY_FINE;
            } else if (req.getGranularity() == StudyMessage.StudyCreate.DataGranularity.GRAN_COARSE) {
                rv.granularity = GRANULARITY_COARSE;
            } else if (req.getGranularity() == StudyMessage.StudyCreate.DataGranularity.GRAN_VERY_COARSE) {
                rv.granularity = GRANULARITY_VERY_COARSE;
            }
            rv.frequency = req.getFrequency();
            return rv;
        }
    }


    @Override
    public boolean equals(Object o) {
        if (o == null) return false;
        if (!(o instanceof StudyRequest)) return false;
        StudyRequest other = (StudyRequest) o;
        // We do not check the ID for equality, as that would prevent using equals to determine if a
        // StudyRequest retrieved from the server matches a StudyRequest saved in the database
        return (other.name.equals(name)) &&
                (other.institution.equals(institution)) &&
                (other.webpage.equals(webpage)) &&
                (other.description.equals(description)) &&
                (other.purpose.equals(purpose)) &&
                (other.procedures.equals(procedures)) &&
                (other.risks.equals(risks)) &&
                (other.benefits.equals(benefits)) &&
                (other.payment.equals(payment)) &&
                (other.conflicts.equals(conflicts)) &&
                (other.confidentiality.equals(confidentiality)) &&
                (other.participationAndWithdrawal.equals(participationAndWithdrawal)) &&
                (other.rights.equals(rights)) &&
                (other.verification == verification) &&
                (other.pubkey.equals(pubkey)) &&
                (Arrays.equals(other.exchange.getPublicKexData(), exchange.getPublicKexData())) &&
                (other.exchange.equals(exchange)) &&
                (Arrays.equals(other.queue, queue)) &&
                (other.investigators.equals(investigators)) &&
                (other.requests.equals(requests));
    }

    /**
     * Deserialize a wrapped StudyCreate message into a StudyRequest, verifying its signature
     * @param wrapper The wrapper containing the StudyCreate message
     * @return The deserialized StudyRequest, or null if an error occured
     */
    public static StudyRequest fromStudyWrapper(StudyMessage.StudyWrapper wrapper) {
        StudyRequest rv = new StudyRequest();
        try {
            // Decode into StudyCreate message
            StudyMessage.StudyCreate scr = StudyMessage.StudyCreate.parseFrom(wrapper.getMessage());
            // Load the public key
            rv.pubkey = RSA.decodePublicKey(scr.getPublicKey().toByteArray());
            // Verify the signature
            if (!RSA.verify(wrapper.getMessage().toByteArray(), wrapper.getSignature().toByteArray(), rv.pubkey)) {
                return null;
            }
            // Extract the data
            rv.name = scr.getStudyName();
            rv.institution = scr.getInstitution();
            rv.webpage = scr.getWebpage();
            rv.description = scr.getDescription();
            rv.purpose = scr.getPurpose();
            rv.procedures = scr.getProcedures();
            rv.risks = scr.getRisks();
            rv.benefits = scr.getBenefits();
            rv.payment = scr.getPayment();
            rv.conflicts = scr.getConflicts();
            rv.confidentiality = scr.getConfidentiality();
            rv.participationAndWithdrawal = scr.getParticipationAndWithdrawal();
            rv.rights = scr.getRights();
            // Extract investigators
            for (StudyMessage.StudyCreate.Investigator inv : scr.getInvestigatorsList()) {
                rv.investigators.add(Investigator.fromProtobuf(inv));
            }
            // Extract DataRequests
            for (StudyMessage.StudyCreate.DataRequest req : scr.getDataRequestList()) {
                rv.requests.add(DataRequest.fromProtobuf(req));
            }
            // Extract keys
            rv.exchange = new KexStub(scr.getKexData().toByteArray());
            // Queue identifier
            rv.queue = scr.getQueueIdentifier().toByteArray();
            // Verification system
            if (scr.getVerificationStrategy() == StudyMessage.StudyCreate.VerificationStrategy.VF_DNS_TXT) {
                rv.verification = VERIFY_DNS;
            } else if (scr.getVerificationStrategy() == StudyMessage.StudyCreate.VerificationStrategy.VF_FILE) {
                rv.verification = VERIFY_FILE;
            } else if (scr.getVerificationStrategy() == StudyMessage.StudyCreate.VerificationStrategy.VF_META) {
                rv.verification = VERIFY_META;
            }
        } catch (InvalidProtocolBufferException e) {
            e.printStackTrace();
            return null;
        }
        return rv;
    }
}
