package de.velcommuta.denul.util;

import android.os.AsyncTask;
import android.util.Log;

import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import de.velcommuta.denul.crypto.AESSharingEncryption;
import de.velcommuta.denul.crypto.ECDHKeyExchange;
import de.velcommuta.denul.crypto.HKDFKeyExpansion;
import de.velcommuta.denul.crypto.IdentifierDerivation;
import de.velcommuta.denul.crypto.KeyExchange;
import de.velcommuta.denul.crypto.KeyExpansion;
import de.velcommuta.denul.crypto.SHA256IdentifierDerivation;
import de.velcommuta.denul.crypto.SharingEncryption;
import de.velcommuta.denul.data.DataBlock;
import de.velcommuta.denul.data.Friend;
import de.velcommuta.denul.data.KeySet;
import de.velcommuta.denul.data.Shareable;
import de.velcommuta.denul.data.StudyRequest;
import de.velcommuta.denul.data.TokenPair;
import de.velcommuta.denul.networking.Connection;
import de.velcommuta.denul.networking.DNSVerifier;
import de.velcommuta.denul.networking.HttpsVerifier;
import de.velcommuta.denul.networking.ProtobufProtocol;
import de.velcommuta.denul.networking.Protocol;
import de.velcommuta.denul.networking.TLSConnection;
import de.velcommuta.denul.service.DatabaseServiceBinder;

/**
 * Helper class for handling research requests and -data
 */
public class StudyManager {
    protected String host = "denul.velcommuta.de";
    protected int port = 5566;
    // TODO Move definitions somewhere sensible

    private class RetrieveStudies extends AsyncTask<Void, Void, Void> {
        private static final String TAG = "RetrieveStudies";

        private DatabaseServiceBinder mBinder;
        private StudyManagerCallback mCallback;

        /**
         * Constructor
         * @param binder A connected DatabaseServiceBinder
         * @param callback The callback to notify once the operation is finished
         */
        public RetrieveStudies(DatabaseServiceBinder binder, StudyManagerCallback callback) {
            if (!binder.isDatabaseOpen()) {
                throw new IllegalArgumentException("Database binder must be open");
            }
            mBinder = binder;
            mCallback = callback;
        }


        /**
         * Constructor
         * @param binder A connected DatabaseServiceBinder
         */
        public RetrieveStudies(DatabaseServiceBinder binder) {
            this(binder, null);
        }


        @Override
        protected Void doInBackground(Void... voids) {
            try {
                // Connnect to server and attach Protocol
                Connection conn = new TLSConnection(host, port);
                Protocol p = new ProtobufProtocol();
                p.connect(conn);

                // Retrieve Studies from the server
                Log.d(TAG, "doInBackground: Retrieving studies from server");
                List<StudyRequest> reqs = p.listRegisteredStudies();
                // Retrieve locally cached studies
                List<StudyRequest> local = mBinder.getStudyRequests();
                // Add new studies
                Log.d(TAG, "doInBackground: Adding new studies");
                for (StudyRequest sreq: reqs) {
                    if (!local.contains(sreq)) {
                        mBinder.addStudyRequest(sreq);
                    }
                }
                Log.d(TAG, "doInBackground: Deleting removed studies");
                // Remove studies that are no longer active and that the user is not participating in
                for (StudyRequest sreq : local) {
                    if (!reqs.contains(sreq) && !sreq.participating) {
                        mBinder.deleteStudy(sreq);
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            return null;
        }


        @Override
        protected void onPostExecute(Void v) {
            if (mCallback != null) mCallback.onUpdateFinished();
        }
    }

    private class JoinStudy extends AsyncTask<StudyRequest, Void, Void> {
        private static final String TAG = "JoinStudy";

        private DatabaseServiceBinder mBinder;
        private StudyManagerCallback mCallback;


        /**
         * Constructor
         * @param binder An open DatabaseServiceBinder
         * @param callback The callback to notify once the operation finished
         */
        public JoinStudy(DatabaseServiceBinder binder, StudyManagerCallback callback) {
            if (binder == null || !binder.isDatabaseOpen()) throw new IllegalArgumentException("Database binder must be open");
            mBinder = binder;
            mCallback = callback;
        }


        /**
         * Constructor
         * @param binder An open DatabaseServiceBinder
         */
        public JoinStudy(DatabaseServiceBinder binder) {
            this(binder, null);
        }


        @Override
        protected Void doInBackground(StudyRequest... studyRequests) {
            for (StudyRequest req : studyRequests) {
                // Check if we are already participating in this study
                if (req.participating) {
                    Log.w(TAG, "doInBackground: Already participating in study - skipping");
                    continue;
                }
                // Perform key exchange
                // TODO Add switch for KEX algo
                KeyExchange kex = new ECDHKeyExchange();
                kex.putPartnerKexData(req.exchange.getPublicKexData());
                KeyExpansion kexp = new HKDFKeyExpansion(kex.getAgreedKey());
                // Expand keys
                KeySet studykeys = kexp.expand(false);
                // Set values on studyRequest
                req.key_in = studykeys.getInboundKey();
                req.ctr_in = studykeys.getInboundCtr();
                req.key_out = studykeys.getOutboundKey();
                req.ctr_out = studykeys.getOutboundCtr();
                req.participating = true;
                // Send join message
                try {
                    // Establish connection
                    Connection conn = new TLSConnection(host, port);
                    // Attach protocol
                    Protocol p = new ProtobufProtocol();
                    // Connect
                    p.connect(conn);
                    // Send join
                    int rv = p.joinStudy(req, kex);
                    // Check return
                    if (rv != Protocol.JOIN_OK) {
                        Log.e(TAG, "Join failed - Code " + rv);
                        continue;
                    }
                    mBinder.updateStudy(req);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            return null;
        }


        @Override
        protected void onPostExecute(Void v) {
            if (mCallback != null) mCallback.onUpdateFinished();
        }
    }


    private class VerifyStudy extends AsyncTask<StudyRequest, Void, Boolean> {
        private static final String TAG = "VerifyStudy";
        private VerificationCallback mCallback;


        /**
         * Constructor
         * @param callback The callback to notify when the verification is finished
         */
        public VerifyStudy(VerificationCallback callback) {
            mCallback = callback;
        }

        @Override
        protected Boolean doInBackground(StudyRequest... studyRequests) {
            StudyRequest req = studyRequests[0];
            switch (req.verification) {
                case StudyRequest.VERIFY_DNS:
                    return DNSVerifier.verify(req);
                case StudyRequest.VERIFY_FILE:
                    return HttpsVerifier.verifyFile(req);
                case StudyRequest.VERIFY_META:
                    return HttpsVerifier.verifyMeta(req);
                default:
                    Log.e(TAG, "doInBackground: Unknown verification system");
                    return false;
            }
        }

        protected void onPostExecute(Boolean ok) {
            if (mCallback != null) mCallback.onVerificationFinished(ok);
        }
    }


    private class CheckShareable extends AsyncTask<Shareable, Void, Void> {
        private static final String TAG = "CheckShareable";

        private DatabaseServiceBinder mBinder;

        private Map<DataBlock, DeferDB> mDeferred;
        private Map<Long, StudyRequest> mStudyCache;

        /**
         * Constructor
         * @param binder An open DatabaseServiceBinder
         */
        public CheckShareable(DatabaseServiceBinder binder) {
            if (binder == null) throw new IllegalArgumentException("Binder must not be null");
            if (!binder.isDatabaseOpen()) throw new IllegalArgumentException("Binder must be open");
            mBinder = binder;
            mDeferred = new HashMap<>();
            mStudyCache = new HashMap<>();
        }

        @Override
        protected Void doInBackground(Shareable... shareables) {
            HashMap<Shareable, List<StudyRequest.DataRequest>> matching = new HashMap<>();
            List<StudyRequest.DataRequest> reqs = mBinder.getActiveDataRequests();
            // Check all DataRequests...
            for (StudyRequest.DataRequest req : reqs) {
                // ...against all Shareables...
                for (Shareable sh : shareables) {
                    // ...and check if the DataRequest covers the Shareable...
                    if (req.matches(sh)) {
                        // ...and if it does, save it for later processing
                        if (matching.containsKey(sh)) {
                            matching.get(sh).add(req);
                        } else {
                            List<StudyRequest.DataRequest> l = new LinkedList<>();
                            l.add(req);
                            matching.put(sh, l);
                        }
                    }
                }
            }
            // All Shareables that should be uploaded have been saved in the "matching" HashMap
            Log.d(TAG, "doInBackground: Found " + matching.size() + " matched Shareables");
            uploadStudyData(matching);
            return null;
        }


        /**
         * Upload data for matched studies
         * @param upload A Hashmap mapping Shareables to a List of DataRequests matching them
         */
        private void uploadStudyData(HashMap<Shareable, List<StudyRequest.DataRequest>> upload) {
            // Establish connection to server
            Protocol proto;
            try {
                Connection conn = new TLSConnection(host, port);
                proto = new ProtobufProtocol();
                proto.connect(conn);
            } catch (IOException e) {
                e.printStackTrace();
                Log.e(TAG, "uploadStudyData: Connection failed");
                return;
            }
            // Prepare List of DataBlocks to upload
            List<DataBlock> toUpload = new LinkedList<>();
            for (Shareable shr : upload.keySet()) {
                DataBlock data_granularity_very_coarse = mBinder.getStudyShareForShareable(shr, Shareable.GRANULARITY_VERY_COARSE); // TODO Remember to set Database ID
                DataBlock data_granularity_coarse = mBinder.getStudyShareForShareable(shr, Shareable.GRANULARITY_COARSE);
                DataBlock data_granularity_fine = mBinder.getStudyShareForShareable(shr, Shareable.GRANULARITY_FINE);
                for (StudyRequest.DataRequest req : upload.get(shr)) {
                    long share_id;
                    if (req.granularity == Shareable.GRANULARITY_VERY_COARSE) {
                        if (data_granularity_very_coarse == null) {
                            data_granularity_very_coarse = prepareShareable(shr, req.granularity);
                            int rv = proto.put(data_granularity_very_coarse);
                            if (rv == Protocol.PUT_OK) {
                                share_id = mBinder.addStudyShare(shr, data_granularity_very_coarse, req.granularity);
                                data_granularity_very_coarse.setDatabaseID(share_id);
                            } else {
                                Log.e(TAG, "uploadStudyData: Upload failed!");
                                // TODO Find a better solution
                                continue;
                            }
                        }
                        DataBlock keyblock = keyBlockForRequest(req, data_granularity_very_coarse);
                        toUpload.add(keyblock);
                    } else if (req.granularity == Shareable.GRANULARITY_COARSE) {
                        if (data_granularity_coarse == null) {
                            data_granularity_coarse = prepareShareable(shr, req.granularity);
                            int rv = proto.put(data_granularity_coarse);
                            if (rv == Protocol.PUT_OK) {
                                share_id = mBinder.addStudyShare(shr, data_granularity_coarse, req.granularity);
                                data_granularity_coarse.setDatabaseID(share_id);
                            } else {
                                Log.e(TAG, "uploadStudyData: Upload failed!");
                                // TODO Find a better solution
                                continue;
                            }
                        }
                        DataBlock keyblock = keyBlockForRequest(req, data_granularity_coarse);
                        toUpload.add(keyblock);
                    } else if (req.granularity == Shareable.GRANULARITY_FINE) {
                        if (data_granularity_fine == null) {
                            data_granularity_fine = prepareShareable(shr, req.granularity);
                            int rv = proto.put(data_granularity_fine);
                            if (rv == Protocol.PUT_OK) {
                                share_id = mBinder.addStudyShare(shr, data_granularity_fine, req.granularity);
                                data_granularity_fine.setDatabaseID(share_id);
                            } else {
                                Log.e(TAG, "uploadStudyData: Upload failed!");
                                // TODO Find a better solution
                                continue;
                            }
                        }
                        DataBlock keyblock = keyBlockForRequest(req, data_granularity_fine);
                        toUpload.add(keyblock);
                    }
                }
            }
            Map<DataBlock, Integer> rv = proto.putMany(toUpload);
            for (DataBlock block : rv.keySet()) {
                switch (rv.get(block)) {
                    case Protocol.PUT_OK:
                        DeferDB deferred = mDeferred.get(block);
                        mBinder.addStudyShareRecipient(deferred.share_id, deferred.request, deferred.tokens);
                        mBinder.updateStudy(deferred.request);
                        break;
                    case Protocol.PUT_FAIL_NO_CONNECTION:
                        Log.e(TAG, "uploadStudyData: ERR NO CONNECTION");
                        break;
                    case Protocol.PUT_FAIL_PROTOCOL_ERROR:
                        Log.e(TAG, "uploadStudyData: ERR PROTO FAILURE");
                        break;
                    case Protocol.PUT_FAIL_KEY_FMT:
                        Log.e(TAG, "uploadStudyData: ERR KEY FMT");
                        break;
                    case Protocol.PUT_FAIL_KEY_TAKEN:
                        Log.e(TAG, "uploadStudyData: ERR KEY TAKEN");
                        break;
                }
            }
        }


        /**
         * Prepare a DataBlock representing a Shareable with a certain Granularity
         * @param shr The Shareable
         * @param granularity The granularity
         * @return The DataBlock
         */
        private DataBlock prepareShareable(Shareable shr, int granularity) {
            IdentifierDerivation deriv = new SHA256IdentifierDerivation();
            SharingEncryption enc = new AESSharingEncryption();
            // Generate a random identifier and revocation token
            TokenPair data_identifier = deriv.generateRandomIdentifier();
            // Encrypt the shareable and return the result
            DataBlock rv = enc.encryptShareable(shr, granularity, data_identifier);
            rv.setRevocationToken(data_identifier.getRevocation());
            return rv;
        }


        /**
         * Create a DataBlock with key information for a provided DataBlock
         * @param req The DataRequest
         * @param data The DataBlock to reference
         * @return A Datablock referencing the provided DataBlock
         */
        private DataBlock keyBlockForRequest(StudyRequest.DataRequest req, DataBlock data) {
            IdentifierDerivation deriv = new SHA256IdentifierDerivation();
            SharingEncryption enc = new AESSharingEncryption();
            // Retrieve keys
            // We have to use a cache, as multiple DataRequests from the same study may match
            // Shareables, in which case the same keys would be re-used if they were loaded from the
            // database every time without updating the database. However, if we were to update the
            // database instantly, a failed insert on the server could desync the clients, which
            // would prevent them from ever communicating again. So, we have to use this slightly
            // weird solution.
            long dbid = mBinder.getStudyRequestIDByDataRequest(req);
            StudyRequest sreq = mStudyCache.get(dbid);
            if (sreq == null) {
                sreq = mBinder.getStudyRequestByID(dbid);
            }
            KeySet keys = new KeySet(sreq.key_in, sreq.key_out, sreq.ctr_in, sreq.ctr_out, false);
            // Generate identifier
            TokenPair ident = deriv.generateOutboundIdentifier(keys);
            // Create matching DataBlock
            byte[] ciphertext = enc.encryptKeysAndIdentifier(data, keys);
            DataBlock keyblock = new DataBlock(ident.getIdentifier(), ciphertext, ident.getIdentifier());
            // Mark the counter value as used
            keys = deriv.notifyOutboundIdentifierUsed(keys);
            mDeferred.put(keyblock, new DeferDB(ident, sreq, data.getDatabaseID()));
            // Update keys and cache the StudyRequest
            sreq.key_in = keys.getInboundKey();
            sreq.key_out = keys.getOutboundKey();
            sreq.ctr_in = keys.getInboundCtr();
            sreq.ctr_out = keys.getOutboundCtr();
            mStudyCache.put(sreq.id, sreq);
            // Return
            return keyblock;
        }

        // Nested data container class, used to defer database updates until the upload has succeeded
        private class DeferDB {
            protected long share_id;
            protected StudyRequest request;
            protected TokenPair tokens;


            /**
             * Constructor for DeferDB object
             * @param ident Identifier
             * @param req StudyRequest
             * @param sid share_id
             */
            public DeferDB(TokenPair ident, StudyRequest req, long sid) {
                tokens = ident;
                request = req;
                share_id = sid;
            }
        }
    }


    /**
     * Verify the authenticity of a study
     * @param callback The callback to notify about the result
     * @param request The StudyRequest to test
     */
    public static void verifyStudy(VerificationCallback callback, StudyRequest request) {
        new StudyManager().new VerifyStudy(callback).execute(request);
    }


    /**
     * Join a study
     * @param binder An open DatabaseServiceBinder
     * @param callback The callback to notify on completion
     * @param req The StudyRequest to join
     */
    public static void joinStudy(DatabaseServiceBinder binder, StudyManagerCallback callback, StudyRequest req) {
        new StudyManager().new JoinStudy(binder, callback).execute(req);
    }


    /**
     * Retrieve the List of Studies from the server
     * @param binder An open DatabaseServiceBinder
     * @param callback The callback to notify on completion
     */
    public static void retrieveStudies(DatabaseServiceBinder binder, StudyManagerCallback callback) {
        new StudyManager().new RetrieveStudies(binder, callback).execute();
    }


    /**
     * Leave a previously joined study
     * @param binder An open DatabaseServiceBinder
     * @param req The Study to leave
     */
    public static void leaveStudy(DatabaseServiceBinder binder, StudyRequest req) {
        req.participating = false;
        req.key_in = null;
        req.ctr_in = null;
        req.key_out = null;
        req.ctr_out = null;
        binder.updateStudy(req);
    }


    /**
     * Check a Shareable against the DataRequests of active Studies to see if it matches any
     * DataRequests. If it matches, it will be uploaded automatically.
     * TODO Maybe just add it to a queue to upload once the next regular connection is established?
     * @param binder An open DatabaseServiceBinder
     * @param sh The Shareable(s) to check
     */
    public static void checkShareable(DatabaseServiceBinder binder, Shareable... sh) {
        new StudyManager().new CheckShareable(binder).execute(sh);
    }




    public interface StudyManagerCallback {
        /**
         * Called when an update with the server is finished
         */
        void onUpdateFinished();
    }

    public interface VerificationCallback {
        /**
         * Called when a verification task is finished
         * @param ok true if the verification was successful, false otherwise
         */
        void onVerificationFinished(boolean ok);
    }
}
