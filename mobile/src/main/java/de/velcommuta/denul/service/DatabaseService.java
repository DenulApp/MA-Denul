package de.velcommuta.denul.service;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.os.Binder;
import android.os.IBinder;
import android.os.SystemClock;
import android.util.Log;

import net.sqlcipher.Cursor;
import net.sqlcipher.database.SQLiteDatabase;
import net.sqlcipher.database.SQLiteException;

import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.List;

import de.greenrobot.event.EventBus;
import de.velcommuta.denul.crypto.KexStub;
import de.velcommuta.denul.crypto.RSA;
import de.velcommuta.denul.data.DataBlock;
import de.velcommuta.denul.data.GPSTrack;
import de.velcommuta.denul.data.KeySet;
import de.velcommuta.denul.data.Shareable;
import de.velcommuta.denul.data.StudyRequest;
import de.velcommuta.denul.data.TokenPair;
import de.velcommuta.denul.db.FriendContract;
import de.velcommuta.denul.db.LocationLoggingContract;
import de.velcommuta.denul.db.SecureDbHelper;
import de.velcommuta.denul.db.SharingContract;
import de.velcommuta.denul.db.StepLoggingContract;
import de.velcommuta.denul.db.StudyContract;
import de.velcommuta.denul.db.VaultContract;
import de.velcommuta.denul.event.DatabaseAvailabilityEvent;
import de.velcommuta.denul.data.Friend;
import de.velcommuta.denul.util.FormatHelper;

/**
 * Database service to hold a handle on the protected database and close it after a certain time of
 * inactivity
 */
public class DatabaseService extends Service {
    // Logging tag
    private static final String TAG = "DatabaseService";

    // Instance variables
    private SQLiteDatabase mSQLiteHandler;
    private MyBinder mBinder = new MyBinder();

    // Alarm manager
    private AlarmManager mManager;
    private PendingIntent mIntent;

    // Formatting constants
    private String FORMAT_DATE = "dd/MM/yyyy";
    private String FORMAT_TIME = "HH:mm";

    ///// Lifecycle Management
    /**
     * Required empty constructor
     */
    public DatabaseService() {
    }


    /**
     * OnBind. Called if the service is bound for the first time (appearently, future binds do not
     * call this function until the last bind has been unbound)
     * @param intent Intent the service is bound with
     * @return A binder
     */
    @Override
    public IBinder onBind(Intent intent) {
        Log.d(TAG, "onBind: Service is bound, returning binder");
        if (mManager != null) {
            mManager.cancel(mIntent);
        }
        return mBinder;
    }


    /**
     * Called when the service is explicitly started. Note that it may be called multiple times
     * during the lifetime of one instance - each call to startService will call this function,
     * no matter if the service is already running.
     * @param intent Intent to start the service
     * @param flags extra flags
     * @param StartId unique startId
     * @return Service mode, as one of the defined constants
     */
    @Override
    public int onStartCommand(Intent intent, int flags, int StartId) {
        if (intent != null && intent.getBooleanExtra("shutdown", false)) {
            Log.d(TAG, "onStartCommand: Received shutdown intent");
            stopSelf();
            return START_NOT_STICKY;
        }
        Log.d(TAG, "onStartCommand: Service started");
        EventBus.getDefault().postSticky(new DatabaseAvailabilityEvent(DatabaseAvailabilityEvent.STARTED));

        return START_NOT_STICKY;
    }


    @Override
    public void onDestroy() {
        // Close SQLite handler
        Log.d(TAG, "onDestroy: Closing database");

        if (mSQLiteHandler != null) {
            mSQLiteHandler.close();
            EventBus.getDefault().postSticky(new DatabaseAvailabilityEvent(DatabaseAvailabilityEvent.CLOSED));
        }
        mSQLiteHandler = null;
        EventBus.getDefault().postSticky(new DatabaseAvailabilityEvent(DatabaseAvailabilityEvent.STOPPED));
    }


    /**
     * Called when the LAST client unbound - after this call, no more clients are bound to the
     * Service. However, it will keep running until it is explicitly stopped, as long as it was
     * started before it was bound.
     * @param intent The intent used to unbind from the service
     * @return True if onRebind should be called if further binds occur after this unbind, false
     *         otherwise.
     */
    @Override
    public boolean onUnbind(Intent intent) {
        Log.i(TAG, "onUnbind: All clients have unbound, setting self-destruct timer");
        mManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        Intent database = new Intent(this, DatabaseService.class);
        database.putExtra("shutdown", true);
        mIntent = PendingIntent.getService(this, 0, database, 0);

        mManager.set(AlarmManager.ELAPSED_REALTIME_WAKEUP,
                SystemClock.elapsedRealtime() + 15 * 60 * 1000, // TODO Let the user choose this time somewhere
                mIntent);
        return true;
    }


    /**
     * OnRebind is called if the service is re-bound after the last client has unbound AND onUnbind
     * returned true, indicating that onRebind should be called on re-binds.
     * @param intent The intent used to bind to the service
     */
    @Override
    public void onRebind(Intent intent) {
        if (mManager != null) {
            Log.i(TAG, "onRebind: Rebound, canceling timer");
            mManager.cancel(mIntent);
        }
    }


    /**
     * Returns the context of this service (used in binder)
     * @return this context
     */
    private Context getServiceContext() {
        return this;
    }


    /**
     * Binder class, used for outside access to service functionality
     */
    private class MyBinder extends Binder implements DatabaseServiceBinder {
        ///// Database Management
        /**
         * Open an existing database with the provided password
         * @param password The password to decrypt the database
         */
        @Override
        public void openDatabase(String password) throws SecurityException {
            // TODO Port to AsyncTask
            if (mSQLiteHandler != null) {
                Log.w(TAG, "openDatabase: Attempted to open database even though it's already open. Ignoring.");
                return;
            }
            Log.d(TAG, "openDatabase: Attempting to open the database");
            SecureDbHelper dbh = new SecureDbHelper(getServiceContext());
            mSQLiteHandler = dbh.getWritableDatabase(password);
            if (mSQLiteHandler == null || !mSQLiteHandler.isOpen()) {
                throw new SecurityException("Something went wrong while opening the database - wrong key?");
            } else {
                Log.d(TAG, "openDatabase: Database opened");
            }
            EventBus.getDefault().postSticky(new DatabaseAvailabilityEvent(DatabaseAvailabilityEvent.OPENED));
        }


        /**
         * Check the status of the database
         * @return True if the database is open, false otherwise
         */
        @Override
        public boolean isDatabaseOpen() {
            return mSQLiteHandler != null && mSQLiteHandler.isOpen();
        }


        ///// Transaction Management
        /**
         * Begin a database transaction
         * @throws SQLiteException Thrown in case the database is not open or already in a transaction
         */
        private void beginTransaction() throws SQLiteException {
            assertOpen();
            if (mSQLiteHandler.inTransaction()) {
                Log.e(TAG, "beginTransaction: Already in transaction, aborting");
                throw new SQLiteException("Already in a transaction");
            }
            Log.d(TAG, "beginTransaction: Beginning database transaction");
            mSQLiteHandler.beginTransaction();
        }


        /**
         * Commit a database transaction
         * @throws SQLiteException Thrown in case the database is not open or not in a transaction
         */
        private void commit() throws SQLiteException{
            assertOpen();
            if (!mSQLiteHandler.inTransaction()) {
                Log.e(TAG, "commit: Not in transaction");
                throw new SQLiteException("Not in a transaction, aborting");
            }
            Log.d(TAG, "commit: Committing database transaction");
            mSQLiteHandler.setTransactionSuccessful();
            mSQLiteHandler.endTransaction();
        }


        /**
         * Revert a database transaction
         * @throws SQLiteException Thrown in case the database is not open or not in a transaction
         */
        private void revert() throws SQLiteException {
            assertOpen();
            if (!mSQLiteHandler.inTransaction()) {
                Log.e(TAG, "revert: Not in transaction, aborting");
                throw new SQLiteException("Not in a transaction");
            }
            Log.d(TAG, "revert: Reverting database transaction");
            mSQLiteHandler.endTransaction();
        }


        ///// Arbitrary data insert
        /**
         * Insert data into a table
         * @param table Table to insert into
         * @param nullable Nullable columns, as per the insert logic of the database interface
         * @param values The ContentValues object
         * @return The row of the inserted data, as per the SQLite interface
         * @throws SQLiteException If the database is not open or the insert failed
         */
        private long insert(String table, String nullable, ContentValues values) throws SQLiteException {
            assertOpen();
            Log.d(TAG, "insert: Running insert");
            return mSQLiteHandler.insertOrThrow(table, nullable, values);
        }


        ///// Arbitrary data retrieval
        /**
         * A query method for the database
         * @param table The table to query
         * @param columns The columns to return
         * @param selection Filter to query which rows should be displayed
         * @param selectionArgs Arguments to selection (for "?" wildcards)
         * @param groupBy Grouping clause (excluding the GROUP BY statement)
         * @param having Filtering clause (excluding the HAVING)
         * @param orderBy Ordering clause (excluding the ORDER BY)
         * @return A cursor object that allows interaction with the data
         * @throws SQLiteException if the underlying query function throws it
         */
        private Cursor query(String table, String[] columns, String selection, String[] selectionArgs, String groupBy, String having, String orderBy) throws SQLiteException {
            assertOpen();
            return mSQLiteHandler.query(table, columns, selection, selectionArgs, groupBy, having, orderBy);
        }

        /**
         * Send an UPDATE query to the SQLite database
         * @param table The table to be updated
         * @param values The values to update it with
         * @param selection The selection of which entries should be updated
         * @param selectionArgs The arguments to replace wildcards ("?")
         * @return The number of updated entries
         */
        private int update(String table, ContentValues values, String selection, String[] selectionArgs) {
            assertOpen();
            Log.d(TAG, "update: Running update");
            return mSQLiteHandler.update(table, values, selection, selectionArgs);
        }


        /**
         * Run a delete on the database
         * @param table The table to delete from
         * @param whereClause The WHERE clause
         * @param whereArgs Arguments for the WHERE clause to replace "?" wildcards
         * @return The number of deleted rows
         */
        private int delete(String table, String whereClause, String[] whereArgs) {
            assertOpen();
            Log.d(TAG, "delete: Running delete");
            beginTransaction();
            int rv = mSQLiteHandler.delete(table, whereClause, whereArgs);
            commit();
            return rv;
        }


        @Override
        public List<Friend> getFriends() {
            assertOpen();
            List<Friend> res = new ArrayList<>();
            Cursor c = query(FriendContract.FriendList.TABLE_NAME,
                    null,
                    null,
                    null,
                    null,
                    null,
                    FriendContract.FriendList._ID + " ASC");
            while (c.moveToNext()) {
                Friend f = new Friend(c.getString(c.getColumnIndexOrThrow(FriendContract.FriendList.COLUMN_NAME_FRIEND)),
                                      c.getInt(   c.getColumnIndexOrThrow(FriendContract.FriendList.COLUMN_NAME_VERIFIED)),
                                      c.getInt(   c.getColumnIndexOrThrow(FriendContract.FriendList._ID)));
                res.add(f);
            }
            c.close();
            return res;
        }

        @Override
        public Friend getFriendById(int id) {
            assertOpen();
            String[] whereArgs = {"" + id};
            Cursor c = query(FriendContract.FriendList.TABLE_NAME,
                    null,
                    FriendContract.FriendList._ID + " LIKE ?",
                    whereArgs,
                    null,
                    null,
                    null);
            Friend rv = null;
            if (c.moveToFirst()) {
                rv = new Friend(c.getString(c.getColumnIndexOrThrow(FriendContract.FriendList.COLUMN_NAME_FRIEND)),
                        c.getInt(c.getColumnIndexOrThrow(FriendContract.FriendList.COLUMN_NAME_VERIFIED)),
                        c.getInt(c.getColumnIndexOrThrow(FriendContract.FriendList._ID)));
            }
            c.close();
            return rv;
        }

        @Override
        public KeySet getKeySetForFriend(Friend friend) {
            assertOpen();
            String[] whereArgs = {"" + friend.getID()};
            Cursor c = query(FriendContract.FriendKeys.TABLE_NAME,
                    null,
                    FriendContract.FriendKeys.COLUMN_NAME_FRIEND_ID + " LIKE ?",
                    whereArgs,
                    null,
                    null,
                    null);
            c.moveToFirst();
            KeySet rv = new KeySet(c.getBlob(c.getColumnIndexOrThrow(FriendContract.FriendKeys.COLUMN_NAME_KEY_IN)),
                                   c.getBlob(c.getColumnIndexOrThrow(FriendContract.FriendKeys.COLUMN_NAME_KEY_OUT)),
                                   c.getBlob(c.getColumnIndexOrThrow(FriendContract.FriendKeys.COLUMN_NAME_CTR_IN)),
                                   c.getBlob(c.getColumnIndexOrThrow(FriendContract.FriendKeys.COLUMN_NAME_CTR_OUT)),
                                   c.getInt (c.getColumnIndexOrThrow(FriendContract.FriendKeys.COLUMN_NAME_INITIATED)) == 1,
                                   c.getInt (c.getColumnIndexOrThrow(FriendContract.FriendKeys._ID)));
            c.close();
            return rv;
        }


        @Override
        public void updateKeySet(KeySet keyset) {
            assertOpen();
            if (keyset == null) throw new SQLiteException("KeySet cannot be null");
            // Prepare ContentValues with new values
            ContentValues key_entry = new ContentValues();
            key_entry.put(FriendContract.FriendKeys.COLUMN_NAME_KEY_IN, keyset.getInboundKey());
            key_entry.put(FriendContract.FriendKeys.COLUMN_NAME_KEY_OUT, keyset.getOutboundKey());
            key_entry.put(FriendContract.FriendKeys.COLUMN_NAME_CTR_IN, keyset.getInboundCtr());
            key_entry.put(FriendContract.FriendKeys.COLUMN_NAME_CTR_OUT, keyset.getOutboundCtr());
            String[] whereArgs = {"" + keyset.getID() };
            // Perform the update
            beginTransaction();
            update(FriendContract.FriendKeys.TABLE_NAME,
                    key_entry,
                    FriendContract.FriendKeys._ID + " LIKE ?",
                    whereArgs);
            commit();
        }


        @Override
        public void addFriend(Friend friend, KeySet keys) {
            // Make sure the database is open
            assertOpen();
            // Check if a friend with the name already exists
            String query = FriendContract.FriendList.COLUMN_NAME_FRIEND + " LIKE ?";
            String[] queryArgs = {friend.getName()};
            String[] columns = {FriendContract.FriendList._ID};
            Cursor c = query(FriendContract.FriendList.TABLE_NAME, columns, query, queryArgs, null, null, null);
            if (c.getCount() > 0) throw new SQLiteException("Name already taken");
            c.close();
            // Begin a transaction
            beginTransaction();
            // Prepare ContentValues
            ContentValues friend_entry = new ContentValues();
            friend_entry.put(FriendContract.FriendList.COLUMN_NAME_FRIEND, friend.getName());
            friend_entry.put(FriendContract.FriendList.COLUMN_NAME_VERIFIED, friend.getVerified());
            // Run insert
            long rv = insert(FriendContract.FriendList.TABLE_NAME, null, friend_entry);
            // Make sure insert worked
            if (rv == -1) throw new IllegalArgumentException("Insert of friend failed");
            // Query for the ID of the inserted entry
            c = query(FriendContract.FriendList.TABLE_NAME, columns, query, queryArgs, null, null, null);
            c.moveToFirst();
            int id = c.getInt(c.getColumnIndexOrThrow(FriendContract.FriendList._ID));
            c.close();
            // Prepare ContentValues for the key entry
            ContentValues keys_entry = new ContentValues();
            keys_entry.put(FriendContract.FriendKeys.COLUMN_NAME_FRIEND_ID, id);
            keys_entry.put(FriendContract.FriendKeys.COLUMN_NAME_KEY_IN, keys.getInboundKey());
            keys_entry.put(FriendContract.FriendKeys.COLUMN_NAME_KEY_OUT, keys.getOutboundKey());
            keys_entry.put(FriendContract.FriendKeys.COLUMN_NAME_CTR_IN, keys.getInboundCtr());
            keys_entry.put(FriendContract.FriendKeys.COLUMN_NAME_CTR_OUT, keys.getOutboundCtr());
            keys_entry.put(FriendContract.FriendKeys.COLUMN_NAME_INITIATED, keys.hasInitiated() ? 1 : 0);
            // Run the insert
            rv = insert(FriendContract.FriendKeys.TABLE_NAME, null, keys_entry);
            // Make sure everything worked
            if (rv == -1) throw new IllegalArgumentException("Insert of keys failed");
            // Commit transaction
            commit();
        }

        @Override
        public void deleteFriend(Friend friend) {
            assertOpen();
            if (friend == null) throw new SQLiteException("Friend cannot be null");
            String[] whereArgs = { "" + friend.getID(), friend.getName() };
            int deleted = delete(FriendContract.FriendList.TABLE_NAME,
                                 FriendContract.FriendList._ID +  " LIKE ? AND " + FriendContract.FriendList.COLUMN_NAME_FRIEND + " LIKE ?",
                                 whereArgs);
            if (deleted != 1) {
                throw new SQLiteException("Wanted to delete 1 row, but deleted " + deleted);
            }
        }

        @Override
        public void updateFriend(Friend friend) {
            assertOpen();
            if (friend == null) throw new SQLiteException("Friend cannot be null");
            // Prepare ContentValues with new values
            ContentValues friend_entry = new ContentValues();
            friend_entry.put(FriendContract.FriendList.COLUMN_NAME_FRIEND, friend.getName());
            friend_entry.put(FriendContract.FriendList.COLUMN_NAME_VERIFIED, friend.getVerified());
            String[] whereArgs = {"" + friend.getID()};
            // Perform the update
            beginTransaction();
            update(FriendContract.FriendList.TABLE_NAME,
                    friend_entry,
                    FriendContract.FriendList._ID + " LIKE ?",
                    whereArgs);
            commit();
        }

        @Override
        public boolean isNameAvailable(String name) {
            assertOpen();
            String[] selectArgs = {name};
            Cursor c = query(FriendContract.FriendList.TABLE_NAME,
                             null,
                             FriendContract.FriendList.COLUMN_NAME_FRIEND + " LIKE ?",
                             selectArgs,
                             null,
                             null,
                             null);
            boolean rv = c.getCount() == 0;
            c.close();
            return rv;
        }

        @Override
        public void addGPSTrack(GPSTrack track) {
            assertOpen();
            addGPSTrack(track, -1);
        }

        @Override
        public void addGPSTrackForFriend(GPSTrack track, Friend friend) {
            assertOpen();
            addGPSTrack(track, friend.getID());
        }

        /**
         * Private helper function to add a track with a certain owner ID to the database
         * @param track The track
         * @param ownerid The owner ID ({@link Friend#getID()}, or -1 if the track is owned by the
         *                device owner)
         */
        private void addGPSTrack(GPSTrack track, int ownerid) {
            // Start a transaction to get an all-or-nothing write to the database
            beginTransaction();
            // Write new database entry with metadata for the track
            ContentValues metadata = new ContentValues();

            metadata.put(LocationLoggingContract.LocationSessions.COLUMN_NAME_SESSION_START, track.getTimestamp());
            metadata.put(LocationLoggingContract.LocationSessions.COLUMN_NAME_SESSION_END, track.getTimestampEnd());
            metadata.put(LocationLoggingContract.LocationSessions.COLUMN_NAME_NAME, track.getSessionName());
            metadata.put(LocationLoggingContract.LocationSessions.COLUMN_NAME_MODE, track.getModeOfTransportation());
            metadata.put(LocationLoggingContract.LocationSessions.COLUMN_NAME_TIMEZONE, track.getTimezone());
            metadata.put(LocationLoggingContract.LocationSessions.COLUMN_NAME_DESCRIPTION, track.getDescription());
            metadata.put(LocationLoggingContract.LocationSessions.COLUMN_NAME_DISTANCE, track.getDistance());
            metadata.put(LocationLoggingContract.LocationSessions.COLUMN_NAME_OWNER, ownerid);

            long rowid = insert(LocationLoggingContract.LocationSessions.TABLE_NAME, null, metadata);

            // Write the individual steps in the track
            for (Location cLoc : track.getPosition()) {
                // Prepare ContentValues object
                ContentValues entry = new ContentValues();
                // Set values for ContentValues
                entry.put(LocationLoggingContract.LocationLog.COLUMN_NAME_SESSION, rowid);
                entry.put(LocationLoggingContract.LocationLog.COLUMN_NAME_LAT, cLoc.getLatitude());
                entry.put(LocationLoggingContract.LocationLog.COLUMN_NAME_LONG, cLoc.getLongitude());
                entry.put(LocationLoggingContract.LocationLog.COLUMN_NAME_TIMESTAMP, cLoc.getTime());
                // Save ContentValues into Database
                insert(LocationLoggingContract.LocationLog.TABLE_NAME, null, entry);
            }
            // Finish transaction
            commit();
        }


        @Override
        public List<GPSTrack> getGPSTracks() {
            assertOpen();
            List<GPSTrack> trackList = new LinkedList<>();
            String[] columns = {LocationLoggingContract.LocationSessions._ID};
            // Retrieve session IDs
            Cursor session = query(LocationLoggingContract.LocationSessions.TABLE_NAME,
                    columns,
                    null,
                    null,
                    null,
                    null,
                    LocationLoggingContract.LocationSessions.COLUMN_NAME_SESSION_START + " DESC");
            while (session.moveToNext()) {
                // For each session ID, load the track from the database
                GPSTrack track = getGPSTrackById(session.getInt(session.getColumnIndexOrThrow(LocationLoggingContract.LocationSessions._ID)));
                trackList.add(track);
            }
            // Close the session cursor
            session.close();
            // Return
            return trackList;
        }

        @Override
        public List<GPSTrack> getOwnerGPSTracks() {
            return getGPSTracksForOwnerID(-1);
        }


        /**
         * Private helper function to retrieve all GPS tracks by a specific owner
         * @param id The owner ID (i.e. the database ID of a Friend, or -1 to get all GPSTracks by
         *           the device owner
         * @return A List of GPSTracks owned by the user with that ID, or an empty List.
         */
        private List<GPSTrack> getGPSTracksForOwnerID(int id) {
            assertOpen();
            List<GPSTrack> trackList = new LinkedList<>();
            String[] columns = {LocationLoggingContract.LocationSessions._ID};
            String[] whereArgs = { ""+id };
            // Retrieve session IDs
            Cursor session = query(LocationLoggingContract.LocationSessions.TABLE_NAME,
                    columns,
                    LocationLoggingContract.LocationSessions.COLUMN_NAME_OWNER + " LIKE ?",
                    whereArgs,
                    null,
                    null,
                    LocationLoggingContract.LocationSessions.COLUMN_NAME_SESSION_START + " DESC");
            while (session.moveToNext()) {
                // For each session ID, load the track from the database
                GPSTrack track = getGPSTrackById(session.getInt(session.getColumnIndexOrThrow(LocationLoggingContract.LocationSessions._ID)));
                trackList.add(track);
            }
            // Close the session cursor
            session.close();
            // Return
            return trackList;
        }


        @Override
        public List<GPSTrack> getGPSTrackByFriend(Friend friend) {
            if (friend == null || friend.getID() == -1) throw new IllegalArgumentException("Bad friend object");
            return getGPSTracksForOwnerID(friend.getID());
        }


        @Override
        public GPSTrack getGPSTrackById(int id) {
            assertOpen();
            GPSTrack track = null;
            String[] selectionArgs = { "" + id };
            Cursor session = query(LocationLoggingContract.LocationSessions.TABLE_NAME,
                    null,
                    LocationLoggingContract.LocationSessions._ID + " LIKE ?",
                    selectionArgs,
                    null,
                    null,
                    null);
            if (session.moveToFirst()) {
                // Prepare output list
                List<Location> locList = new LinkedList<>();
                // Query for the Locations in the session
                String[] whereArgs = {"" + id};
                Cursor locs = query(LocationLoggingContract.LocationLog.TABLE_NAME,
                        null,
                        LocationLoggingContract.LocationLog.COLUMN_NAME_SESSION + " LIKE ?",
                        whereArgs,
                        null,
                        null,
                        null);
                while (locs.moveToNext()) {
                    // For each location, construct a Location object
                    Location loc = new Location("");
                    loc.setLongitude(locs.getFloat(locs.getColumnIndexOrThrow(LocationLoggingContract.LocationLog.COLUMN_NAME_LONG)));
                    loc.setLatitude(locs.getFloat(locs.getColumnIndexOrThrow(LocationLoggingContract.LocationLog.COLUMN_NAME_LAT)));
                    loc.setTime(locs.getLong(locs.getColumnIndexOrThrow(LocationLoggingContract.LocationLog.COLUMN_NAME_TIMESTAMP)));
                    locList.add(loc);
                }
                // Close the location cursor
                locs.close();
                // Create the GPSTrack object
                track = new GPSTrack(locList,
                        session.getString (session.getColumnIndexOrThrow(LocationLoggingContract.LocationSessions.COLUMN_NAME_NAME)),
                        session.getInt    (session.getColumnIndexOrThrow(LocationLoggingContract.LocationSessions.COLUMN_NAME_MODE)),
                        session.getLong   (session.getColumnIndexOrThrow(LocationLoggingContract.LocationSessions.COLUMN_NAME_SESSION_START)),
                        session.getLong   (session.getColumnIndexOrThrow(LocationLoggingContract.LocationSessions.COLUMN_NAME_SESSION_END)),
                        session.getString (session.getColumnIndexOrThrow(LocationLoggingContract.LocationSessions.COLUMN_NAME_TIMEZONE)),
                        session.getInt    (session.getColumnIndexOrThrow(LocationLoggingContract.LocationSessions.COLUMN_NAME_OWNER)),
                        session.getFloat  (session.getColumnIndexOrThrow(LocationLoggingContract.LocationSessions.COLUMN_NAME_DISTANCE)));
                track.setID(session.getInt(session.getColumnIndexOrThrow(LocationLoggingContract.LocationSessions._ID)));
                track.setDescription(session.getString(session.getColumnIndexOrThrow(LocationLoggingContract.LocationSessions.COLUMN_NAME_DESCRIPTION)));
            }
            session.close();
            return track;
        }


        @Override
        public void deleteGPSTrack(GPSTrack track) {
            // Ensure database is open
            assertOpen();
            // sanity checks on the object
            if (track == null) throw new SQLiteException("Friend cannot be null");
            if (track.getID() == -1) throw new SQLiteException("ID must be set");
            // Prepare and perform deletion
            // Entries in LocationLoggingContract.LocationLog will be automatically deleted due to
            // the foreign key ON DELETE CASCADE statement
            String[] whereArgs = { "" + track.getID() };
            int deleted = delete(LocationLoggingContract.LocationSessions.TABLE_NAME,
                    LocationLoggingContract.LocationSessions._ID + " LIKE ?",
                    whereArgs);
            // Ensure the correct number of items was deleted
            if (deleted != 1) {
                throw new SQLiteException("Wanted to delete 1 row, but deleted " + deleted);
            }
        }


        @Override
        public void renameGPSTrack(GPSTrack track, String name) {
            assertOpen();
            // Sanity checks
            if (track == null) throw new SQLiteException("Track cannot be null");
            if (track.getID() == -1) throw new SQLiteException("ID must be set");
            if (name == null || name.equals("")) throw new SQLiteException("Name must be set");
            // Prepare ContentValues with new values
            ContentValues track_entry = new ContentValues();
            track_entry.put(LocationLoggingContract.LocationSessions.COLUMN_NAME_NAME, name);
            String[] whereArgs = {"" + track.getID()};
            // Perform the update
            beginTransaction();
            update(LocationLoggingContract.LocationSessions.TABLE_NAME,
                    track_entry,
                    LocationLoggingContract.LocationSessions._ID + " LIKE ?",
                    whereArgs);
            commit();
        }


        @Override
        public void integratePedometerCache(Hashtable<DateTime, Long> cache) {
            assertOpen();
            beginTransaction();
            for (DateTime ts : cache.keySet()) {
                int c = getStepCountForTimestamp(ts);
                if (c != -1) {
                    if (c < cache.get(ts)) {
                        ContentValues update = new ContentValues();
                        update.put(StepLoggingContract.StepCountLog.COLUMN_VALUE, cache.get(ts));
                        String selection = StepLoggingContract.StepCountLog.COLUMN_DATE + " LIKE ? AND "
                                + StepLoggingContract.StepCountLog.COLUMN_TIME + " LIKE ?";
                        String[] selectionArgs = {formatDate(ts), formatTime(ts)};
                        update(StepLoggingContract.StepCountLog.TABLE_NAME, update, selection, selectionArgs);
                    }
                } else {
                    Log.d(TAG, "saveToDatabase: Inserting value");
                    ContentValues entry = new ContentValues();
                    entry.put(StepLoggingContract.StepCountLog.COLUMN_DATE, formatDate(ts));
                    entry.put(StepLoggingContract.StepCountLog.COLUMN_TIME, formatTime(ts));
                    entry.put(StepLoggingContract.StepCountLog.COLUMN_VALUE, cache.get(ts));
                    insert(StepLoggingContract.StepCountLog.TABLE_NAME, null, entry);
                }
            }
            Log.d(TAG, "saveToDatabase: All values saved, committing");
            commit();
        }

        @Override
        public int getStepCountForTimestamp(DateTime dt) {
            assertOpen();
            int rv = -1;
            Cursor c = query(StepLoggingContract.StepCountLog.TABLE_NAME,
                    new String[]{StepLoggingContract.StepCountLog.COLUMN_VALUE},
                    StepLoggingContract.StepCountLog.COLUMN_DATE + " = '" + formatDate(dt) + "' AND " +
                            StepLoggingContract.StepCountLog.COLUMN_TIME + " = '" + formatTime(dt) + "'",
                    null,
                    null,
                    null,
                    null);
            if (c.moveToFirst()) {
                rv = c.getInt(c.getColumnIndexOrThrow(StepLoggingContract.StepCountLog.COLUMN_VALUE));
            }
            c.close();
            return rv;
        }


        @Override
        public String getPedometerPrivateKey() {
            assertOpen();
            String encoded = null;
            // Query the database for the private key
            Cursor c = query(VaultContract.KeyStore.TABLE_NAME,
                    new String[] {VaultContract.KeyStore.COLUMN_KEY_BYTES},
                    VaultContract.KeyStore.COLUMN_KEY_NAME + " LIKE ?",
                    new String[] {VaultContract.KeyStore.NAME_PEDOMETER_PRIVATE},
                    null,
                    null,
                    null);
            // Retrieve the encoded string
            if (c.moveToFirst()) {
                encoded = c.getString(
                        c.getColumnIndexOrThrow(VaultContract.KeyStore.COLUMN_KEY_BYTES)
                );
            }
            // Close the cursor
            c.close();
            // Return
            return encoded;
        }


        @Override
        public void storePedometerKeypair(String pubkey, String privkey) {
            assertOpen();
            // Begin a database transaction
            beginTransaction();
            // Prepare database entry for the private key
            ContentValues keyEntry = new ContentValues();
            // Set type to private RSA key
            keyEntry.put(VaultContract.KeyStore.COLUMN_KEY_TYPE, VaultContract.KeyStore.TYPE_RSA_PRIV);
            // Set the key descriptor to Pedometer key
            keyEntry.put(VaultContract.KeyStore.COLUMN_KEY_NAME, VaultContract.KeyStore.NAME_PEDOMETER_PRIVATE);
            // Add the actual key to the insert
            keyEntry.put(VaultContract.KeyStore.COLUMN_KEY_BYTES, privkey);
            // Insert the values into the database
            insert(VaultContract.KeyStore.TABLE_NAME, null, keyEntry);

            // Perform the same steps for the public key (as a backup)
            keyEntry = new ContentValues();
            keyEntry.put(VaultContract.KeyStore.COLUMN_KEY_TYPE, VaultContract.KeyStore.TYPE_RSA_PUB);
            keyEntry.put(VaultContract.KeyStore.COLUMN_KEY_NAME, VaultContract.KeyStore.NAME_PEDOMETER_PUBLIC);
            keyEntry.put(VaultContract.KeyStore.COLUMN_KEY_BYTES, pubkey);
            // insert the values
            insert(VaultContract.KeyStore.TABLE_NAME, null, keyEntry);
            // Finish the transaction
            commit();
        }


        @Override
        public Hashtable<DateTime, Long> getStepCountForDay(DateTime dt) {
            assertOpen();
            String date = formatDate(dt);
            Hashtable<DateTime, Long> rv = new Hashtable<>();
            Cursor c = query(StepLoggingContract.StepCountLog.TABLE_NAME,
                    new String[]{StepLoggingContract.StepCountLog.COLUMN_TIME, StepLoggingContract.StepCountLog.COLUMN_VALUE},
                    StepLoggingContract.StepCountLog.COLUMN_DATE + " LIKE ?",
                    new String[]{date},
                    null, // groupby
                    null, // having
                    null); // orderby
            if (c.getCount() > 0) {
                while (c.moveToNext()) {
                    // retrieve values
                    long value = c.getInt(c.getColumnIndexOrThrow(StepLoggingContract.StepCountLog.COLUMN_VALUE));
                    String time = c.getString(c.getColumnIndexOrThrow(StepLoggingContract.StepCountLog.COLUMN_TIME));
                    // Convert to DateTime
                    DateTime timestamp = DateTime.parse(date + " " + time, DateTimeFormat.forPattern(FORMAT_DATE + " " + FORMAT_TIME));
                    rv.put(timestamp, value);
                }
                Log.d(TAG, "getStepCountForDay: Got a result");
            } else {
                Log.d(TAG, "getStepCountForDay: Nothing in the database for specified day");
            }
            c.close();
            return rv;
        }


        @Override
        public int getMaxStepCounterSequenceNumber() {
            assertOpen();
            int rv = -1;
            Cursor c = query(VaultContract.SequenceNumberStore.TABLE_NAME,
                    new String[]{VaultContract.SequenceNumberStore.COLUMN_SNR_VALUE},
                    VaultContract.SequenceNumberStore.COLUMN_SNR_TYPE + " LIKE ?",
                    new String[]{VaultContract.SequenceNumberStore.TYPE_PEDOMETER},
                    null,
                    null,
                    null);
            if (c.moveToFirst()) {
                rv = c.getInt(c.getColumnIndexOrThrow(VaultContract.SequenceNumberStore.COLUMN_SNR_VALUE));
            }
            c.close();
            return rv;
        }


        @Override
        public void storeStepCounterSequenceNumber(int seqnr) {
            int cSeqNr = getMaxStepCounterSequenceNumber();
            if (cSeqNr == -1) {
                Log.d(TAG, "storeStepCounterSequenceNumber: Inserting new value");
                ContentValues insert = new ContentValues();
                insert.put(VaultContract.SequenceNumberStore.COLUMN_SNR_TYPE, VaultContract.SequenceNumberStore.TYPE_PEDOMETER);
                insert.put(VaultContract.SequenceNumberStore.COLUMN_SNR_VALUE, seqnr);
                insert(VaultContract.SequenceNumberStore.TABLE_NAME, null, insert);
            } else {
                Log.d(TAG, "storeStepCounterSequenceNumber: Updating value");
                ContentValues values = new ContentValues();
                values.put(VaultContract.SequenceNumberStore.COLUMN_SNR_VALUE, seqnr);
                update(VaultContract.SequenceNumberStore.TABLE_NAME,
                        values,
                        VaultContract.SequenceNumberStore.COLUMN_SNR_TYPE + " LIKE ?",
                        new String[]{VaultContract.SequenceNumberStore.TYPE_PEDOMETER}
                );
            }
        }


        @Override
        public boolean isShared(Shareable sh) {
            assertOpen();
            // Sanity check
            if (sh.getID() == -1) {
                Log.e(TAG, "isShared: Shareable had ID -1");
                return false;
            }
            // Prepare arguments to the query, based on the shareable
            String[] whereArgs = { "" + sh.getID() };
            String[] columns = { getShareIDColumnForShareable(sh)};
            // Ensure that the shareable was known to our helper functions
            String table = getTableForShareable(sh);
            if (table == null) {
                // Perform check here to avoid NPE on query definition below if Shareable is unknown
                Log.e(TAG, "isShared: Unknown shareable type");
                return false;
            }
            String query = getIDColumnForShareable(sh) + " LIKE ? AND " + getShareIDColumnForShareable(sh) + " IS NOT NULL";
            // Perform the query
            Cursor c = query(table,
                    columns,
                    query,
                    whereArgs,
                    null,
                    null,
                    null);
            // If we got a result, the Shareable has already been shared, as that is the only situation
            // in which the Share ID column will not be NULL
            boolean rv = c.getCount() == 1;
            // Close the cursor
            c.close();
            // Return
            return rv;
        }

        @Override
        public int getShareID(Shareable sh) {
            // Sanity checks
            assertOpen();
            if (!isShared(sh)) return -1;
            // Prepare arguments to query based on Shareable
            String[] whereArgs = { "" + sh.getID() };
            String table = getTableForShareable(sh);
            if (table == null) {
                // We perform the sanity check here because the query definition would NPE if the shareable is unknown
                Log.e(TAG, "isShared: Unknown shareable type");
                return -1;
            }
            String[] columns = { getShareIDColumnForShareable(sh)};
            String query = getIDColumnForShareable(sh) + " LIKE ?";
            // Perform query
            Cursor c = query(table,
                    columns,
                    query,
                    whereArgs,
                    null,
                    null,
                    null);
            // Retrieve data (there must be a result, otherwise isShared(sh) would have returned false)
            c.moveToFirst();
            int rv = c.getInt(c.getColumnIndexOrThrow(getShareIDColumnForShareable(sh)));
            // Close cursor, return
            c.close();
            return rv;
        }


        @Override
        public int getShareGranularity(Shareable sh) {
            assertOpen();
            // Retrieve ID
            int id = getShareID(sh);
            // If ID is -1, the item is not shared => return -1
            if (id == -1) return -1;
            // Prepare and execute query
            String[] whereArgs = {String.valueOf(id)};
            String[] columns = {SharingContract.DataShareLog.COLUMN_GRANULARITY};
            Cursor c = query(SharingContract.DataShareLog.TABLE_NAME,
                    columns,
                    SharingContract.DataShareLog._ID + " LIKE ?",
                    whereArgs,
                    null,
                    null,
                    null);
            // Prepare return value
            int rv = -1;
            // Grab return value from cursor
            if (c.moveToFirst()) {
                rv = c.getInt(c.getColumnIndexOrThrow(SharingContract.DataShareLog.COLUMN_GRANULARITY));
            }
            // Close cursor and return
            c.close();
            return rv;
        }


        @Override
        public int addShare(Shareable sh, TokenPair pair, DataBlock block) {
            assertOpen();
            // Sanity checks
            if (sh.getID() == -1) return -1;
            if (isShared(sh)) return -1;
            // Begin a database transaction
            beginTransaction();
            // Prepare entry in DataShareLog
            ContentValues data = new ContentValues();
            data.put(SharingContract.DataShareLog.COLUMN_KEY, block.getKey());
            data.put(SharingContract.DataShareLog.COLUMN_IDENTIFIER, pair.getIdentifier());
            data.put(SharingContract.DataShareLog.COLUMN_REVOCATION_TOKEN, pair.getRevocation());
            data.put(SharingContract.DataShareLog.COLUMN_GRANULARITY, block.getGranularity());
            // Insert
            int dataid = (int) insert(SharingContract.DataShareLog.TABLE_NAME, null, data);

            // Prepare insert into regular database to link the Shareable with the DataShareLog
            ContentValues link = new ContentValues();
            link.put(getShareIDColumnForShareable(sh), dataid);
            // Prepare table information
            String table = getTableForShareable(sh);
            String selection = getIDColumnForShareable(sh) + " LIKE ?";
            String[] selectArgs = { "" + sh.getID() };
            // Perform update
            int updated = update(table, link, selection, selectArgs);
            // Ensure the update took place
            if (updated == 0) {
                Log.e(TAG, "addShare: Update of Shareable database failed, rolling back");
                revert();
                return -1;
            } else {
                commit();
            }
            return dataid;
        }


        @Override
        public void addShareRecipient(int datashareid, Friend friend, TokenPair pair) {
            assertOpen();
            // Sanity checks
            if (datashareid == -1 || friend.getID() == -1) {
                Log.e(TAG, "addShareRecipient: Bad ID. Data = " + datashareid + ", Friend = " + friend.getID());
                return;
            }
            // Prepare contentValues
            ContentValues share = new ContentValues();
            share.put(SharingContract.FriendShareLog.COLUMN_DATASHARE_ID, datashareid);
            share.put(SharingContract.FriendShareLog.COLUMN_FRIEND_ID, friend.getID());
            share.put(SharingContract.FriendShareLog.COLUMN_IDENTIFIER, pair.getIdentifier());
            share.put(SharingContract.FriendShareLog.COLUMN_REVOCATION_TOKEN, pair.getRevocation());
            // Perform insert
            beginTransaction();
            insert(SharingContract.FriendShareLog.TABLE_NAME, null, share);
            commit();
        }


        @Override
        public List<Friend> getShareRecipientsForShareable(Shareable shareable) {
            assertOpen();
            if (shareable == null || shareable.getID() == -1) throw new IllegalArgumentException("shareable must have database ID set");
            List<Friend> rv = new LinkedList<>();
            int share_id = getShareID(shareable);
            if (share_id == -1) return rv;
            String[] whereArgs = { "" + share_id };
            String[] columns = { FriendContract.FriendList._ID };
            Cursor c = query(SharingContract.FriendShareLog.TABLE_NAME,
                    columns,
                    SharingContract.FriendShareLog.COLUMN_DATASHARE_ID + " LIKE ?",
                    whereArgs,
                    null,
                    null,
                    null);
            while (c.moveToNext()) {
                rv.add(getFriendById(c.getInt(c.getColumnIndexOrThrow(FriendContract.FriendList._ID))));
            }
            c.close();
            return rv;
        }


        @Override
        public DataBlock getShareData(int shareid) {
            assertOpen();
            if (shareid == -1) {
                Log.e(TAG, "getShareData: shareid cannot be -1");
                return null;
            }
            String[] whereArgs = { "" + shareid };
            String[] columns = {SharingContract.DataShareLog.COLUMN_IDENTIFIER, SharingContract.DataShareLog.COLUMN_KEY};
            Cursor c = query(SharingContract.DataShareLog.TABLE_NAME,
                    columns,
                    SharingContract.DataShareLog._ID + " LIKE ?",
                    whereArgs,
                    null,
                    null,
                    null);
            DataBlock rv;
            if (c.moveToFirst()) {
                rv = new DataBlock(c.getBlob(c.getColumnIndexOrThrow(SharingContract.DataShareLog.COLUMN_KEY)),
                                   c.getBlob(c.getColumnIndexOrThrow(SharingContract.DataShareLog.COLUMN_IDENTIFIER)));
            } else {
                Log.e(TAG, "getShareData: No results for ID " + shareid);
                rv = null;
            }
            c.close();
            return rv;
        }


        @Override
        public boolean deleteShareByToken(TokenPair tokenPair) {
            assertOpen();
            int deleted = delete(SharingContract.FriendShareLog.TABLE_NAME,
                    SharingContract.FriendShareLog.COLUMN_REVOCATION_TOKEN + " LIKE x'" + FormatHelper.bytesToHex(tokenPair.getRevocation()) +
                            "' AND " + SharingContract.FriendShareLog.COLUMN_IDENTIFIER + " LIKE x'" + FormatHelper.bytesToHex(tokenPair.getIdentifier()) + "'",
                    null);
            if (deleted == 0) {
                deleted = delete(SharingContract.DataShareLog.TABLE_NAME,
                        SharingContract.DataShareLog.COLUMN_IDENTIFIER + " LIKE x'" + FormatHelper.bytesToHex(tokenPair.getIdentifier()) +
                                "' AND " + SharingContract.DataShareLog.COLUMN_REVOCATION_TOKEN + " LIKE x'" + FormatHelper.bytesToHex(tokenPair.getRevocation()) + "'",
                        null);
                if (deleted == 0) {
                    return false;
                }
            }
            return true;
        }


        @Override
        public void deleteSharesByFriend(Friend friend) {
            assertOpen();
            if (friend == null || friend.getID() == -1) throw new IllegalArgumentException("Bad friend object");
            List<GPSTrack> shares = getGPSTrackByFriend(friend);
            for (GPSTrack track : shares) {
                deleteGPSTrack(track);
            }
            // TODO Add further shareable types here
        }


        @Override
        public List<TokenPair> getTokensForShareable(Shareable shareable) {
            assertOpen();
            // Prepare return list
            List<TokenPair> rv = new LinkedList<>();
            // Get the share ID for the shareable
            int share_id = getShareID(shareable);
            // Check if it is set
            if (share_id == -1) return rv;
            // Retrieve identifier
            TokenPair data = getTokenPairForShareID(share_id);
            if (data != null) {
                rv.add(data);
            }
            // Retrieve TokenPairs of all KeyBlocks referring to the data share
            rv.addAll(getFriendShareTokenPairsForShareID(share_id));
            // Return
            return rv;
        }

        @Override
        public List<TokenPair> getTokensForSharesToFriend(Friend friend) {
            assertOpen();
            // Prepare return List
            List<TokenPair> rv = new LinkedList<>();
            if (friend.getID() == -1) return rv;
            // Prepare query
            String[] whereArgs = {String.valueOf(friend.getID())};
            String[] columns = {SharingContract.FriendShareLog.COLUMN_IDENTIFIER, SharingContract.FriendShareLog.COLUMN_REVOCATION_TOKEN};
            Cursor c = query(SharingContract.FriendShareLog.TABLE_NAME,
                    columns,
                    SharingContract.FriendShareLog.COLUMN_FRIEND_ID + " LIKE ?",
                    whereArgs,
                    null,
                    null,
                    null);
            // Read out results
            while (c.moveToNext()) {
                rv.add(new TokenPair(c.getBlob(c.getColumnIndexOrThrow(SharingContract.FriendShareLog.COLUMN_IDENTIFIER)),
                                     c.getBlob(c.getColumnIndexOrThrow(SharingContract.FriendShareLog.COLUMN_REVOCATION_TOKEN))));
            }
            // Close cursor
            c.close();
            return rv;
        }

        @Override
        public TokenPair getTokenForShareToFriend(Shareable shareable, Friend friend) {
            TokenPair rv = null;
            if (shareable.getID() == -1 || friend.getID() == -1) return null;
            int share_id = getShareID(shareable);
            if (share_id == -1) return null;
            // Prepare query
            String[] whereArgs = {String.valueOf(share_id), String.valueOf(friend.getID())};
            String[] columns = {SharingContract.FriendShareLog.COLUMN_IDENTIFIER, SharingContract.FriendShareLog.COLUMN_REVOCATION_TOKEN};
            Cursor c = query(SharingContract.FriendShareLog.TABLE_NAME,
                    columns,
                    SharingContract.FriendShareLog.COLUMN_DATASHARE_ID + " LIKE ? AND " + SharingContract.FriendShareLog.COLUMN_FRIEND_ID + " LIKE ?",
                    whereArgs,
                    null,
                    null,
                    null);
            // Read results
            if (c.moveToFirst()) {
                rv = new TokenPair(c.getBlob(c.getColumnIndexOrThrow(SharingContract.FriendShareLog.COLUMN_IDENTIFIER)),
                                   c.getBlob(c.getColumnIndexOrThrow(SharingContract.FriendShareLog.COLUMN_REVOCATION_TOKEN)));
            }
            // Close cursor
            c.close();
            return rv;
        }


        /**
         * Retrieve the TokenPair associated with a specific shareID in the DataShareLog table
         * @param shareid The ShareID in the DataShareLog table
         * @return The TokenPair, or null if none exists
         */
        private TokenPair getTokenPairForShareID(int shareid) {
            if (shareid == -1) return null;
            // Prepare query
            String[] whereArgs = {String.valueOf(shareid)};
            String[] columns = {SharingContract.DataShareLog.COLUMN_IDENTIFIER, SharingContract.DataShareLog.COLUMN_REVOCATION_TOKEN};
            Cursor c = query(SharingContract.DataShareLog.TABLE_NAME,
                    columns,
                    SharingContract.DataShareLog._ID + " LIKE ?",
                    whereArgs,
                    null,
                    null,
                    null);
            // Read out result
            TokenPair rv = null;
            if (c.moveToFirst()) {
                rv = new TokenPair(c.getBlob(c.getColumnIndexOrThrow(SharingContract.DataShareLog.COLUMN_IDENTIFIER)),
                                   c.getBlob(c.getColumnIndexOrThrow(SharingContract.DataShareLog.COLUMN_REVOCATION_TOKEN)));
            }
            // Close cursor
            c.close();
            return rv;
        }


        /**
         * Retrieve all TokenPairs associated with a specific ShareID from the FriendShareLog table
         * @param shareid The ShareID (referring to an entry in the DataShareLog table)
         * @return A List of {@link TokenPair}s of entries referring to the shareID
         */
        private List<TokenPair> getFriendShareTokenPairsForShareID(int shareid) {
            // Prepare return value
            List<TokenPair> rv = new LinkedList<>();
            if (shareid == -1) return rv;
            // Prepare query
            String[] whereArgs = {String.valueOf(shareid)};
            String[] columns = {SharingContract.FriendShareLog.COLUMN_IDENTIFIER, SharingContract.FriendShareLog.COLUMN_REVOCATION_TOKEN};
            Cursor c = query(SharingContract.FriendShareLog.TABLE_NAME,
                    columns,
                    SharingContract.FriendShareLog.COLUMN_DATASHARE_ID + " LIKE ?",
                    whereArgs,
                    null,
                    null,
                    null);
            // Iterate through results and create TokenPairs
            while (c.moveToNext()) {
                rv.add(new TokenPair(c.getBlob(c.getColumnIndexOrThrow(SharingContract.FriendShareLog.COLUMN_IDENTIFIER)),
                                     c.getBlob(c.getColumnIndexOrThrow(SharingContract.FriendShareLog.COLUMN_REVOCATION_TOKEN))));
            }
            // Close cursor
            c.close();
            return rv;
        }


        @Override
        public void addShareable(Shareable shareable) {
            assertOpen();
            if (shareable == null) return;
            switch (shareable.getType()) {
                case Shareable.SHAREABLE_TRACK:
                    addGPSTrack((GPSTrack) shareable, shareable.getOwner());
                    break;
                case Shareable.SHAREABLE_STEPCOUNT:
                    // TODO
                    Log.e(TAG, "addShareable: Step count Not implemented");
                    break;
            }
        }


        @Override
        public void deleteShareable(Shareable shareable) {
            assertOpen();
            if (shareable == null || shareable.getID() == -1) throw new IllegalArgumentException("Bad shareable");
            switch (shareable.getType()) {
                case Shareable.SHAREABLE_TRACK:
                    deleteGPSTrack((GPSTrack) shareable);
                    break;
                // TODO Add new Shareables here
                default:
                    Log.e(TAG, "deleteShareable: Unknown shareable type");
            }
        }


        @Override
        public void updateShareableDescription(Shareable shareable) {
            switch (shareable.getType()) {
                case Shareable.SHAREABLE_TRACK:
                    updateGPSTrackDescription((GPSTrack) shareable);
                    break;
                default:
                    throw new IllegalArgumentException("Bad shareable - unknown type");
            }
        }


        @Override
        public long addStudyRequest(StudyRequest req) {
            // When the study reaches this place, the signature will have already been verified -
            // signature verification happens on deserialization.  Thus, it is not important that
            // we are saving the signature nowhere, working under the assumption that the database
            // is secure.
            assertOpen();
            if (req == null) throw new SQLiteException("StudyRequest cannot be null");
            // Insert the study itself
            ContentValues study = new ContentValues();
            study.put(StudyContract.Studies.COLUMN_NAME, req.name);
            study.put(StudyContract.Studies.COLUMN_INSTITUTION, req.institution);
            study.put(StudyContract.Studies.COLUMN_WEB, req.webpage);
            study.put(StudyContract.Studies.COLUMN_DESCRIPTION, req.description);
            study.put(StudyContract.Studies.COLUMN_PURPOSE, req.purpose);
            study.put(StudyContract.Studies.COLUMN_PROCEDURES, req.procedures);
            study.put(StudyContract.Studies.COLUMN_RISKS, req.risks);
            study.put(StudyContract.Studies.COLUMN_BENEFITS, req.benefits);
            study.put(StudyContract.Studies.COLUMN_PAYMENT, req.payment);
            study.put(StudyContract.Studies.COLUMN_CONFLICTS,req.conflicts);
            study.put(StudyContract.Studies.COLUMN_CONFIDENTIALITY, req.confidentiality);
            study.put(StudyContract.Studies.COLUMN_PARTICIPATION, req.participationAndWithdrawal);
            study.put(StudyContract.Studies.COLUMN_RIGHTS, req.rights);
            study.put(StudyContract.Studies.COLUMN_VERIFICATION, req.verification);
            // TODO Change once more algorithms are added
            study.put(StudyContract.Studies.COLUMN_PUBKEY, RSA.encodeKey(req.pubkey));
            // TODO Change constants
            study.put(StudyContract.Studies.COLUMN_KEYALGO, 1);
            study.put(StudyContract.Studies.COLUMN_KEX, req.exchange.getPublicKexData());
            // TODO Change constants
            study.put(StudyContract.Studies.COLUMN_KEXALGO, 1);
            study.put(StudyContract.Studies.COLUMN_QUEUE, req.queue);
            // Perform insert
            beginTransaction();
            long rv = insert(StudyContract.Studies.TABLE_NAME, null, study);
            // Insert Investigators
            for (StudyRequest.Investigator inv : req.investigators) {
                ContentValues investigator = new ContentValues();
                investigator.put(StudyContract.Investigators.COLUMN_NAME, inv.name);
                investigator.put(StudyContract.Investigators.COLUMN_GROUP, inv.group);
                investigator.put(StudyContract.Investigators.COLUMN_INSTITUTION, inv.institution);
                investigator.put(StudyContract.Investigators.COLUMN_POSITION, inv.position);
                investigator.put(StudyContract.Investigators.COLUMN_STUDY, rv);
                insert(StudyContract.Investigators.TABLE_NAME, null, investigator);
            }
            // Insert DataRequests
            for (StudyRequest.DataRequest dreq : req.requests) {
                ContentValues requests = new ContentValues();
                requests.put(StudyContract.DataRequests.COLUMN_DATATYPE, dreq.type);
                requests.put(StudyContract.DataRequests.COLUMN_FREQUENCY, dreq.frequency);
                requests.put(StudyContract.DataRequests.COLUMN_GRANULARITY, dreq.granularity);
                requests.put(StudyContract.DataRequests.COLUMN_STUDY, rv);
                insert(StudyContract.DataRequests.TABLE_NAME, null, requests);
            }
            // Commit transaction
            commit();
            return rv;
        }


        @Override
        public void deleteStudy(StudyRequest req) {
            assertOpen();
            if (req == null) throw new SQLiteException("Friend cannot be null");
            String[] whereArgs = { "" + req.id};
            int deleted = delete(StudyContract.Studies.TABLE_NAME,
                    StudyContract.Studies._ID + " LIKE ?",
                    whereArgs);
            if (deleted != 1) {
                throw new SQLiteException("Wanted to delete 1 row, but deleted " + deleted);
            }
        }


        @Override
        public StudyRequest getStudyRequestByID(long id) {
            assertOpen();
            if (id == -1) {
                Log.e(TAG, "getStudyRequestByID: id cannot be -1");
                return null;
            }
            String[] whereArgs = { "" + id };
            Cursor c = query(StudyContract.Studies.TABLE_NAME,
                    null,
                    StudyContract.Studies._ID + " LIKE ?",
                    whereArgs,
                    null,
                    null,
                    null);
            StudyRequest rv;
            if (c.moveToFirst()) {
                rv = studyRequestFromCursor(c);
            } else {
                Log.e(TAG, "getStudyRequestByID: No results for ID " + id);
                rv = null;
            }
            c.close();
            return rv;
        }


        @Override
        public List<StudyRequest> getStudyRequests() {
            assertOpen();
            List<StudyRequest> rv = new LinkedList<>();
            Cursor c = query(StudyContract.Studies.TABLE_NAME,
                    null,
                    null,
                    null,
                    null,
                    null,
                    StudyContract.Studies._ID + " ASC");
            while (c.moveToNext()) {
                rv.add(studyRequestFromCursor(c));
            }
            c.close();
            return rv;
        }


        @Override
        public void updateStudy(StudyRequest req) {
            assertOpen();
            if (req == null) throw new IllegalArgumentException("Bad StudyRequest");
            // Set values
            ContentValues update = new ContentValues();
            update.put(StudyContract.Studies.COLUMN_PARTICIPATING, req.participating ? 1 : 0);
            update.put(StudyContract.Studies.COLUMN_KEY_IN, req.key_in);
            update.put(StudyContract.Studies.COLUMN_CTR_IN, req.ctr_in);
            update.put(StudyContract.Studies.COLUMN_KEY_OUT, req.key_out);
            update.put(StudyContract.Studies.COLUMN_CTR_OUT, req.ctr_out);

            // Prepare and perform update
            String[] whereArgs = {String.valueOf(req.id)};
            beginTransaction();
            update(StudyContract.Studies.TABLE_NAME,
                    update,
                    StudyContract.Studies._ID + " LIKE ?",
                    whereArgs);
            commit();
        }


        @Override
        public List<StudyRequest.DataRequest> getActiveDataRequests() {
            assertOpen();
            // Prepare return value
            List<StudyRequest.DataRequest> rv = new LinkedList<>();
            // Prepare query
            String query = "SELECT " + StudyContract.DataRequests.TABLE_NAME + ".* FROM " +
                    StudyContract.DataRequests.TABLE_NAME + ", " + StudyContract.Studies.TABLE_NAME +
                    " WHERE " + StudyContract.DataRequests.TABLE_NAME + "." + StudyContract.DataRequests.COLUMN_STUDY +
                    " = " + StudyContract.Studies.TABLE_NAME + "." + StudyContract.Studies._ID +
                    " AND " + StudyContract.Studies.TABLE_NAME + "." + StudyContract.Studies.COLUMN_PARTICIPATING +
                    " = 1;";
            // Perform query
            Cursor c = mSQLiteHandler.rawQuery(query, null);
            while (c.moveToNext()) {
                rv.add(dataRequestFromCursor(c));
            }
            c.close();
            return rv;
        }


        @Override
        public DataBlock getStudyShareForShareable(Shareable shr, int granularity) {
            assertOpen();
            // Ensure granularity is sane
            if (granularity < 0 || granularity > Shareable.GRANULARITY_VERY_COARSE) {
                Log.e(TAG, "getStudyShareForShareable: Granularity must be one of the GRANULARITY_* constants");
                return null;
            }
            // Prepare query
            String[] whereArgs = { "" + shr.getID(), "" + granularity, "" + shr.getType()};
            Cursor c = query(StudyContract.DataShare.TABLE_NAME,
                    null,
                    StudyContract.DataShare.COLUMN_SHAREABLE_ID + " LIKE ? AND "
                            + StudyContract.DataShare.COLUMN_GRANULARITY + " LIKE ? AND "
                            + StudyContract.DataShare.COLUMN_DATATYPE + " LIKE ?",
                    whereArgs,
                    null,
                    null,
                    null);
            // Prepare return value
            DataBlock rv = null;
            // Read data, if available
            if (c.moveToFirst()) {
                rv = new DataBlock(c.getBlob(c.getColumnIndexOrThrow(StudyContract.DataShare.COLUMN_KEY)),
                                   c.getBlob(c.getColumnIndexOrThrow(StudyContract.DataShare.COLUMN_IDENT)));
                rv.setDatabaseID(c.getLong(c.getColumnIndexOrThrow(StudyContract.DataShare._ID)));
            }
            // Close cursor
            c.close();
            // return (doh)
            return rv;
        }


        @Override
        public long addStudyShare(Shareable shr, DataBlock data, int granularity) {
            assertOpen();
            if (granularity < 0 || granularity > Shareable.GRANULARITY_VERY_COARSE) {
                Log.e(TAG, "getStudyShareForShareable: Granularity must be one of the GRANULARITY_* constants");
                return -1;
            }
            // Prepare insert
            ContentValues insert = new ContentValues();
            insert.put(StudyContract.DataShare.COLUMN_DATATYPE, shr.getType());
            insert.put(StudyContract.DataShare.COLUMN_GRANULARITY, granularity);
            insert.put(StudyContract.DataShare.COLUMN_SHAREABLE_ID, shr.getID());
            insert.put(StudyContract.DataShare.COLUMN_KEY, data.getKey());
            insert.put(StudyContract.DataShare.COLUMN_REVOKE, data.getRevocationToken());
            insert.put(StudyContract.DataShare.COLUMN_IDENT, data.getIdentifier());
            // Run insert
            return insert(StudyContract.DataShare.TABLE_NAME, null, insert);
        }


        @Override
        public void addStudyShareRecipient(long shareid, StudyRequest request, TokenPair tokens) {
            assertOpen();
            if (shareid < 0 || request == null || tokens == null) {
                throw new IllegalArgumentException("addStudyShareRecipient: Bad parameters");
            }
            ContentValues insert = new ContentValues();
            insert.put(StudyContract.StudyShare.COLUMN_DATASHARE, shareid);
            insert.put(StudyContract.StudyShare.COLUMN_STUDYID, request.id);
            insert.put(StudyContract.StudyShare.COLUMN_IDENTIFIER, tokens.getIdentifier());
            insert.put(StudyContract.StudyShare.COLUMN_REVOCATION, tokens.getRevocation());
            insert(StudyContract.StudyShare.TABLE_NAME, null, insert);
        }


        @Override
        public long getStudyRequestIDByDataRequest(StudyRequest.DataRequest req) {
            assertOpen();
            if (req.id == -1) {
                Log.e(TAG, "getStudyRequestIDByDataRequest: id cannot be -1");
                return -1;
            }
            long rv;
            String[] whereArgs = { "" + req.id };
            Cursor c = query(StudyContract.DataRequests.TABLE_NAME,
                    new String[] {StudyContract.DataRequests.COLUMN_STUDY},
                    StudyContract.DataRequests._ID + " LIKE ?",
                    whereArgs,
                    null,
                    null,
                    null);
            if (c.moveToFirst()) {
                rv = c.getLong(c.getColumnIndexOrThrow(StudyContract.DataRequests.COLUMN_STUDY));
            } else {
                Log.e(TAG, "getStudyRequestIDByDataRequest: No results for ID " + req.id);
                rv = -1;
            }
            c.close();
            return rv;
        }


        /**
         * Private helper function to update the description of a GPS track in the database
         * @param track The updated GPS track
         */
        private void updateGPSTrackDescription(GPSTrack track) {
            assertOpen();
            // Ensure object is sane
            if (track == null || track.getID() == -1) throw new IllegalArgumentException("Bad GPS track");
            // Prepare contentvalues
            ContentValues description = new ContentValues();
            description.put(LocationLoggingContract.LocationSessions.COLUMN_NAME_DESCRIPTION, track.getDescription());
            // Prepare and perform update
            String[] whereArgs = {String.valueOf(track.getID())};
            beginTransaction();
            update(LocationLoggingContract.LocationSessions.TABLE_NAME,
                    description,
                    LocationLoggingContract.LocationSessions._ID + " LIKE ?",
                    whereArgs);
            commit();
        }


        ///// Utility functions
        /**
         * Assert that the database is open, or throw an exception
         * @throws SQLiteException Thrown if the database is not open
         */
        private void assertOpen() throws SQLiteException {
            if (!(mSQLiteHandler != null && mSQLiteHandler.isOpen()))
                throw new SQLiteException("Database is not open");
        }


        /**
         * Read a StudyRequest from a cursor, querying the database for Investigators and Data Requests.
         * @param c The cursor, containing results for a StudyRequest
         * @return The StudyRequest
         */
        private StudyRequest studyRequestFromCursor(Cursor c) {
            // Create studyRequest
            StudyRequest rv = new StudyRequest();
            // Set fields
            rv.id = c.getLong(c.getColumnIndexOrThrow(StudyContract.Studies._ID));
            rv.name = c.getString(c.getColumnIndexOrThrow(StudyContract.Studies.COLUMN_NAME));
            rv.institution = c.getString(c.getColumnIndexOrThrow(StudyContract.Studies.COLUMN_INSTITUTION));
            rv.webpage = c.getString(c.getColumnIndexOrThrow(StudyContract.Studies.COLUMN_WEB));
            rv.description = c.getString(c.getColumnIndexOrThrow(StudyContract.Studies.COLUMN_DESCRIPTION));
            rv.purpose = c.getString(c.getColumnIndexOrThrow(StudyContract.Studies.COLUMN_PURPOSE));
            rv.procedures = c.getString(c.getColumnIndexOrThrow(StudyContract.Studies.COLUMN_PROCEDURES));
            rv.risks = c.getString(c.getColumnIndexOrThrow(StudyContract.Studies.COLUMN_RISKS));
            rv.benefits = c.getString(c.getColumnIndexOrThrow(StudyContract.Studies.COLUMN_BENEFITS));
            rv.payment = c.getString(c.getColumnIndexOrThrow(StudyContract.Studies.COLUMN_PAYMENT));
            rv.conflicts = c.getString(c.getColumnIndexOrThrow(StudyContract.Studies.COLUMN_CONFLICTS));
            rv.confidentiality = c.getString(c.getColumnIndexOrThrow(StudyContract.Studies.COLUMN_CONFIDENTIALITY));
            rv.participationAndWithdrawal = c.getString(c.getColumnIndexOrThrow(StudyContract.Studies.COLUMN_PARTICIPATION));
            rv.rights = c.getString(c.getColumnIndexOrThrow(StudyContract.Studies.COLUMN_RIGHTS));
            rv.verification = c.getInt(c.getColumnIndexOrThrow(StudyContract.Studies.COLUMN_VERIFICATION));
            rv.pubkey = RSA.decodePublicKey(c.getString(c.getColumnIndexOrThrow(StudyContract.Studies.COLUMN_PUBKEY)));
            rv.exchange = new KexStub(c.getBlob(c.getColumnIndexOrThrow(StudyContract.Studies.COLUMN_KEX)));
            rv.queue = c.getBlob(c.getColumnIndexOrThrow(StudyContract.Studies.COLUMN_QUEUE));
            // Check if the user is participating
            rv.participating = c.getInt(c.getColumnIndexOrThrow(StudyContract.Studies.COLUMN_PARTICIPATING)) == 1;
            if (rv.participating) {
                rv.key_in = c.getBlob(c.getColumnIndexOrThrow(StudyContract.Studies.COLUMN_KEY_IN));
                rv.ctr_in = c.getBlob(c.getColumnIndexOrThrow(StudyContract.Studies.COLUMN_CTR_IN));
                rv.key_out = c.getBlob(c.getColumnIndexOrThrow(StudyContract.Studies.COLUMN_KEY_OUT));
                rv.ctr_out = c.getBlob(c.getColumnIndexOrThrow(StudyContract.Studies.COLUMN_CTR_OUT));
            }
            // Retrieve Investigators
            rv.investigators = getInvestigatorsForStudy(rv.id);
            // Retrieve DataRequests
            rv.requests = getDataRequestsForStudy(rv.id);
            return rv;
        }


        /**
         * Retrieve all Investigators related to a StudyRequest from the database
         * @param studyid The database ID of the study
         * @return A List of Investigators
         */
        private List<StudyRequest.Investigator> getInvestigatorsForStudy(long studyid) {
            List<StudyRequest.Investigator> rv = new LinkedList<>();
            assertOpen();
            if (studyid < 0) {
                Log.e(TAG, "getInvestigatorsForStudy: id cannot be -1");
                return null;
            }
            String[] whereArgs = { "" + studyid };
            Cursor c = query(StudyContract.Investigators.TABLE_NAME,
                    null,
                    StudyContract.Investigators.COLUMN_STUDY + " LIKE ?",
                    whereArgs,
                    null,
                    null,
                    null);
            while (c.moveToNext()) {
                rv.add(investigatorFromCursor(c));
            }
            c.close();
            return rv;
        }


        /**
         * Extract an investigator from a Cursor object
         * @param c The cursor
         * @return An investigator
         */
        private StudyRequest.Investigator investigatorFromCursor(Cursor c) {
            // Prepare investigator
            StudyRequest.Investigator rv = new StudyRequest.Investigator();
            // Fill fields
            rv.name = c.getString(c.getColumnIndexOrThrow(StudyContract.Investigators.COLUMN_NAME));
            rv.group = c.getString(c.getColumnIndexOrThrow(StudyContract.Investigators.COLUMN_GROUP));
            rv.institution = c.getString(c.getColumnIndexOrThrow(StudyContract.Investigators.COLUMN_INSTITUTION));
            rv.position = c.getString(c.getColumnIndexOrThrow(StudyContract.Investigators.COLUMN_POSITION));
            rv.id = c.getLong(c.getColumnIndexOrThrow(StudyContract.Investigators._ID));
            // Return
            return rv;
        }


        /**
         * Retrieve all DataRequests related to a StudyRequest from the database
         * @param studyid The database ID of the study
         * @return A List of DataRequests
         */
        private List<StudyRequest.DataRequest> getDataRequestsForStudy(long studyid) {
            List<StudyRequest.DataRequest> rv = new LinkedList<>();
            assertOpen();
            if (studyid < 0) {
                Log.e(TAG, "getDataRequestsForStudy: id cannot be -1");
                return null;
            }
            String[] whereArgs = { "" + studyid };
            Cursor c = query(StudyContract.DataRequests.TABLE_NAME,
                    null,
                    StudyContract.DataRequests.COLUMN_STUDY + " LIKE ?",
                    whereArgs,
                    null,
                    null,
                    null);
            while (c.moveToNext()) {
                rv.add(dataRequestFromCursor(c));
            }
            c.close();
            return rv;
        }


        /**
         * Extract a DataRequest from a Cursor
         * @param c A cursor containing a DataRequest
         * @return The DataRequest
         */
        private StudyRequest.DataRequest dataRequestFromCursor(Cursor c) {
            // Prepare DataRequest
            StudyRequest.DataRequest rv = new StudyRequest.DataRequest();
            // Fill fields
            rv.frequency = c.getInt(c.getColumnIndexOrThrow(StudyContract.DataRequests.COLUMN_FREQUENCY));
            rv.granularity = c.getInt(c.getColumnIndexOrThrow(StudyContract.DataRequests.COLUMN_GRANULARITY));
            rv.type = c.getInt(c.getColumnIndexOrThrow(StudyContract.DataRequests.COLUMN_DATATYPE));
            rv.id = c.getLong(c.getColumnIndexOrThrow(StudyContract.DataRequests._ID));
            // Return
            return rv;
        }

        //// Shareable helper functions
        // TODO Add new shareables here
        /**
         * Helper function to determine the name of the table in which a Shareable is saved
         * @param sh The shareable
         * @return The table name
         */
        private String getTableForShareable(Shareable sh) {
            switch (sh.getType()) {
                case Shareable.SHAREABLE_TRACK:
                    return LocationLoggingContract.LocationSessions.TABLE_NAME;
                case Shareable.SHAREABLE_STEPCOUNT:
                    return StepLoggingContract.StepCountLog.TABLE_NAME;
                default:
                    return null;
            }
        }


        /**
         * Helper function to determine the name of the ID column in the table in which a shareable is saved
         * @param sh The shareable
         * @return The ID column name
         */
        private String getIDColumnForShareable(Shareable sh) {
            switch (sh.getType()) {
                case Shareable.SHAREABLE_TRACK:
                    return LocationLoggingContract.LocationSessions._ID;
                case Shareable.SHAREABLE_STEPCOUNT:
                    return StepLoggingContract.StepCountLog._ID;
                default:
                    return null;
            }
        }


        /**
         * Helper function to determine the name of the share ID column in the table in which a shareable
         * is saved
         * @param sh The Shareable
         * @return The share ID column
         */
        private String getShareIDColumnForShareable(Shareable sh) {
            switch (sh.getType()) {
                case Shareable.SHAREABLE_TRACK:
                    return LocationLoggingContract.LocationSessions.COLUMN_NAME_SHARE_ID;
                case Shareable.SHAREABLE_STEPCOUNT:
                    return StepLoggingContract.StepCountLog.COLUMN_SHARE_ID;
                default:
                    return null;
            }
        }

        // Formatting helper

        /**
         * Format a given DateTime for the date column of the database
         * @param dt The DateTime object
         * @return The String representation of the day
         */
        private String formatDate(DateTime dt) {
            return dt.toString(DateTimeFormat.forPattern(FORMAT_DATE));
        }


        /**
         * Format a given DateTime for the time column of the database
         * @param dt The DateTime Object
         * @return The String representation of the time
         */
        private String formatTime(DateTime dt) {
            return dt.toString(DateTimeFormat.forPattern(FORMAT_TIME));
        }
    }

    /**
     * Check if the database service is running
     * @param ctx Context of the calling application
     * @return True if yes, false if not
     */
    public static boolean isRunning(Context ctx) {
        ActivityManager manager = (ActivityManager) ctx.getSystemService(Activity.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if ("de.velcommuta.denul.service.DatabaseService".equals(service.service.getClassName())) {
                return true; // Package name matches, our service is running
            }
        }
        return false; // No matching package name found => Our service is not running
    }
}
