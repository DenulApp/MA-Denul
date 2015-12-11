package de.velcommuta.denul.util;

import android.os.AsyncTask;
import android.util.Log;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.velcommuta.denul.crypto.AESSharingEncryption;
import de.velcommuta.denul.crypto.IdentifierDerivation;
import de.velcommuta.denul.crypto.SHA256IdentifierDerivation;
import de.velcommuta.denul.crypto.SharingEncryption;
import de.velcommuta.denul.data.DataBlock;
import de.velcommuta.denul.data.Friend;
import de.velcommuta.denul.data.KeySet;
import de.velcommuta.denul.data.Shareable;
import de.velcommuta.denul.data.TokenPair;
import de.velcommuta.denul.networking.Connection;
import de.velcommuta.denul.networking.ProtobufProtocol;
import de.velcommuta.denul.networking.Protocol;
import de.velcommuta.denul.networking.TLSConnection;
import de.velcommuta.denul.service.DatabaseServiceBinder;

/**
 * Helper class for sharing data
 */
public class ShareManager {
    public class ShareWithProgress extends AsyncTask<List<Friend>, Integer, Boolean> {
        private static final String TAG = "ShareWP";

        private Shareable[] mShareableList;
        private DatabaseServiceBinder mBinder;
        private ShareManagerCallback mCallback;

        /**
         * Constructor
         * @param sh The shareable to share
         * @param binder A database binder
         * @param callback A callback that should be notified on status updates
         */
        public ShareWithProgress(DatabaseServiceBinder binder, ShareManagerCallback callback, Shareable... sh) {
            if (sh == null || binder == null || !binder.isDatabaseOpen())
                throw new IllegalArgumentException("Shareable and Binder must not be null, database must be open");
            mShareableList = sh;
            for (Shareable sha : mShareableList) {
                if (sha == null || sha.getID() == -1) throw new IllegalArgumentException("Shareable must have been added to the database already");
            }
            mBinder = binder;
            mCallback = callback;
        }

        @Override
        protected Boolean doInBackground(List<Friend>... friendslist) {
            // TODO Add status updates at sensible positions
            // Establish a connection to the server (if this fails, we can avoid spending time encrypting stuff)
            Connection conn;
            List<Friend> friends = friendslist[0];
            try {
                conn = new TLSConnection("denul.velcommuta.de", 5566);  // TODO Move definitions to $somewhere
            } catch (Exception e) {
                Log.e(TAG, "doInBackground:", e);
                return false;
            }
            // Attach a protocol
            Protocol proto = new ProtobufProtocol();
            proto.connect(conn);
            // Notify that connection is working
            publishProgress(1);
            // Prepare instances and variables to hold data
            // SharingEncryption instance for crypto operations
            SharingEncryption enc = new AESSharingEncryption();
            // IdentifierDerivation instance for identifer generation
            IdentifierDerivation deriv = new SHA256IdentifierDerivation();
            // Map mapping identifiers to data that is to be saved on the server
            Map<String, byte[]> outbox = new HashMap<>();
            // Map mapping identifiers to deferred database operations
            Map<String, DeferDB> defer = new HashMap<>();
            // Iterate through provided shareables
            for (Shareable shareable : mShareableList) {
                DataBlock data;
                // Check if the data has already been shared
                int s_id = mBinder.getShareID(shareable);
                if (s_id == -1) {
                    // Shareable has not been shared before
                    // Generate a random identifier and revocation token
                    TokenPair data_identifier = deriv.generateRandomIdentifier();
                    // Encrypt the shareable
                    data = enc.encryptShareable(shareable, data_identifier);
                    // Submit to the server
                    int rv = proto.put(FormatHelper.bytesToHex(data_identifier.getIdentifier()), data.getCiphertext());
                    if (rv == Protocol.PUT_OK) {
                        // Save to database
                        s_id = mBinder.addShare(shareable, data_identifier, data);
                    } else {
                        Log.e(TAG, "doInBackground: An error occured during upload of the DataBlock. Skipping it");
                        // Skip this whole DataBlock
                        // TODO Find a better solution
                        continue;
                    }
                } else {
                    // Shareable has already been shared, reuse existing Data block
                    data = mBinder.getShareData(s_id);
                }
                // Iterate through all recipients and prepare messages for them
                for (Friend friend : friends) {
                    // TODO Test if data has already been shared with this user
                    // Retrieve keys
                    KeySet keys = mBinder.getKeySetForFriend(friend);
                    // Generate identifier
                    TokenPair ident = deriv.generateOutboundIdentifier(keys);
                    // Encrypt identifier and key of data block and add to outbox
                    outbox.put(FormatHelper.bytesToHex(ident.getIdentifier()), enc.encryptKeysAndIdentifier(data, keys));
                    // Mark the counter value as used
                    keys = deriv.notifyOutboundIdentifierUsed(keys);
                    // Schedule a deferred database update
                    defer.put(FormatHelper.bytesToHex(ident.getIdentifier()), new DeferDB(ident, keys, friend, s_id));
                    // mBinder.updateKeySet(keys);
                    // Add information about the share to the database
                    // mBinder.addShareRecipient(s_id, friend, ident);
                }
                // At this point, the current shareable has been prepared for all friends
            }
            // At this point, all Shareables have been prepared for all friends
            // Notify that encryption finished
            publishProgress(2);
            // Send ALL THE store messages
            Map<String, Integer> rv = proto.putMany(outbox);
            for (String ident : outbox.keySet()) {
                switch (rv.get(ident)) {
                    case Protocol.PUT_OK:
                        DeferDB d = defer.get(ident);
                        mBinder.updateKeySet(d.keyset);
                        mBinder.addShareRecipient(d.share_id, d.friend, d.tokens);
                        break;
                    case Protocol.PUT_FAIL_KEY_TAKEN:
                        Log.e(TAG, "doInBackground: PUT failed for identifier " + ident + ": KEY_TAKEN");
                        break;
                    case Protocol.PUT_FAIL_KEY_FMT:
                        Log.e(TAG, "doInBackground: PUT failed for identifier " + ident + ": KEY_FMT");
                        break;
                    case Protocol.PUT_FAIL_PROTOCOL_ERROR:
                        Log.e(TAG, "doInBackground: PUT failed for identifier " + ident + ": PROTOCOL_ERROR");
                        break;
                    case Protocol.PUT_FAIL_NO_CONNECTION:
                        Log.e(TAG, "doInBackground: PUT failed for identifier " + ident + ": NO_CONNECTION");
                        break;
                    default:
                        Log.e(TAG, "doInBackground: Unknown error code for PUT");
                }
            }
            return true;
        }

        @Override
        protected void onProgressUpdate(Integer... progress) {
            mCallback.onShareStatusUpdate(progress[0]);
        }

        @Override
        protected void onPostExecute(Boolean result) {
            mCallback.onShareFinished(result);
        }

        // Nested data container class, used to defer database updates until the upload has succeeded
        private class DeferDB {
            protected KeySet keyset;
            protected int share_id;
            protected Friend friend;
            protected TokenPair tokens;

            public DeferDB(TokenPair ident, KeySet keys, Friend f, int sid) {
                tokens = ident;
                keyset = keys;
                friend = f;
                share_id = sid;
            }
        }
    }


    /**
     * Callback interface for receiving status updates
     */
    public interface ShareManagerCallback {
        /**
         * Function to receive status updates from the ShareManager.
         * @param status Status code
         */
        void onShareStatusUpdate(int status);

        /**
         * Callback to receive a notification once the sharing process is finished
         * @param success true if the share was successful, false otherwise
         */
        void onShareFinished(boolean success);
    }
}
