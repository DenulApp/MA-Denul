package de.velcommuta.denul.service;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.os.SystemClock;
import android.util.Log;

import net.sqlcipher.Cursor;
import net.sqlcipher.database.SQLiteDatabase;
import net.sqlcipher.database.SQLiteException;

import de.greenrobot.event.EventBus;
import de.velcommuta.denul.db.FriendContract;
import de.velcommuta.denul.db.SecureDbHelper;
import de.velcommuta.denul.event.DatabaseAvailabilityEvent;

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
        @Override
        public void beginTransaction() throws SQLiteException {
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
        @Override
        public void commit() throws SQLiteException{
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
        @Override
        public void revert() throws SQLiteException {
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
        @Override
        public long insert(String table, String nullable, ContentValues values) throws SQLiteException {
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
         */
        @Override
        public Cursor query(String table, String[] columns, String selection, String[] selectionArgs, String groupBy, String having, String orderBy) throws SQLiteException {
            assertOpen();
            Log.d(TAG, "query: Running query");
            return mSQLiteHandler.query(table, columns, selection, selectionArgs, groupBy, having, orderBy);
        }

        @Override
        public int update(String table, ContentValues values, String selection, String[] selectionArgs) {
            assertOpen();
            Log.d(TAG, "update: Running update");
            return mSQLiteHandler.update(table, values, selection, selectionArgs);
        }


        @Override
        public Cursor getFriends() {
            assertOpen();
            return query(FriendContract.FriendList.TABLE_NAME,
                    null,
                    null,
                    null,
                    null,
                    null,
                    FriendContract.FriendList._ID + " ASCENDING");
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
