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
import de.velcommuta.denul.data.DataBlock;
import de.velcommuta.denul.data.GPSTrack;
import de.velcommuta.denul.data.KeySet;
import de.velcommuta.denul.data.Shareable;
import de.velcommuta.denul.data.TokenPair;
import de.velcommuta.denul.db.FriendContract;
import de.velcommuta.denul.db.LocationLoggingContract;
import de.velcommuta.denul.db.SecureDbHelper;
import de.velcommuta.denul.db.SharingContract;
import de.velcommuta.denul.db.StepLoggingContract;
import de.velcommuta.denul.db.VaultContract;
import de.velcommuta.denul.event.DatabaseAvailabilityEvent;
import de.velcommuta.denul.data.Friend;

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
            Log.d(TAG, "query: Running query");
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
            c.moveToFirst();
            Friend rv = new Friend(c.getString(c.getColumnIndexOrThrow(FriendContract.FriendList.COLUMN_NAME_FRIEND)),
                                   c.getInt(c.getColumnIndexOrThrow(FriendContract.FriendList.COLUMN_NAME_VERIFIED)),
                                   c.getInt(c.getColumnIndexOrThrow(FriendContract.FriendList._ID)));
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

            metadata.put(LocationLoggingContract.LocationSessions.COLUMN_NAME_SESSION_START, track.getPosition().get(0).getTime());
            metadata.put(LocationLoggingContract.LocationSessions.COLUMN_NAME_SESSION_END, track.getPosition().get(track.getPosition().size() - 1).getTime());
            metadata.put(LocationLoggingContract.LocationSessions.COLUMN_NAME_NAME, track.getSessionName());
            metadata.put(LocationLoggingContract.LocationSessions.COLUMN_NAME_MODE, track.getModeOfTransportation());
            metadata.put(LocationLoggingContract.LocationSessions.COLUMN_NAME_TIMEZONE, track.getTimezone());
            metadata.put(LocationLoggingContract.LocationSessions.COLUMN_NAME_OWNER, ownerid);

            long rowid = insert(LocationLoggingContract.LocationSessions.TABLE_NAME, null, metadata);

            // Write the individual steps in the track
            for (int i = 0; i < track.getPosition().size(); i++) {
                // Prepare ContentValues object
                ContentValues entry = new ContentValues();
                // Get Location object
                Location cLoc = track.getPosition().get(i);
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
            assertOpen();
            List<GPSTrack> trackList = new LinkedList<>();
            String[] columns = {LocationLoggingContract.LocationSessions._ID};
            String[] whereArgs = { "-1" };
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
                        session.getString(session.getColumnIndexOrThrow(LocationLoggingContract.LocationSessions.COLUMN_NAME_NAME)),
                        session.getInt(session.getColumnIndexOrThrow(LocationLoggingContract.LocationSessions.COLUMN_NAME_MODE)),
                        session.getLong(session.getColumnIndexOrThrow(LocationLoggingContract.LocationSessions.COLUMN_NAME_SESSION_START)),
                        session.getString(session.getColumnIndexOrThrow(LocationLoggingContract.LocationSessions.COLUMN_NAME_TIMEZONE)),
                        session.getInt(session.getColumnIndexOrThrow(LocationLoggingContract.LocationSessions.COLUMN_NAME_OWNER)));
                track.setID(session.getInt(session.getColumnIndexOrThrow(LocationLoggingContract.LocationSessions._ID)));
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
            keyEntry.put(VaultContract.KeyStore.COLUMN_KEY_NAME, VaultContract.KeyStore.NAME_PEDOMETER_PUBLIC);;
            keyEntry.put(VaultContract.KeyStore.COLUMN_KEY_BYTES,pubkey);
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
            share.put(SharingContract.FriendShareLog.COLUMN_REVOCATION_TOKEN, pair.getRevocation());
            // Perform insert
            beginTransaction();
            insert(SharingContract.FriendShareLog.TABLE_NAME, null, share);
            commit();
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


        ///// Utility functions
        /**
         * Assert that the database is open, or throw an exception
         * @throws SQLiteException Thrown if the database is not open
         */
        private void assertOpen() throws SQLiteException {
            if (!(mSQLiteHandler != null && mSQLiteHandler.isOpen()))
                throw new SQLiteException("Database is not open");
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
