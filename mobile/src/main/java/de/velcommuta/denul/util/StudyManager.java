package de.velcommuta.denul.util;

import android.os.AsyncTask;
import android.util.Log;

import java.io.IOException;
import java.util.List;

import de.velcommuta.denul.data.StudyRequest;
import de.velcommuta.denul.networking.Connection;
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

    public class RetrieveStudies extends AsyncTask<Void, Void, Void> {
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
                    if (!reqs.contains(sreq) && !mBinder.isParticipatingInStudy(sreq)) {
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

    public interface StudyManagerCallback {
        /**
         * Called when an update with the server is finished
         */
        void onUpdateFinished();
    }
}
