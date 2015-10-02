package de.velcommuta.denul.service;

import android.app.Service;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

import net.sqlcipher.database.SQLiteDatabase;
import net.sqlcipher.database.SQLiteException;

import de.velcommuta.denul.db.SecureDbHelper;

/**
 * Database service to hold a handle on the protected database and close it after a certain time of
 * inactivity
 */
public class DatabaseService extends Service {
    // Logging tag
    private static final String TAG = "DatabaseService";

    // Instance variables
    private SQLiteDatabase mSQLiteHandler;


    ///// Lifecycle Management
    /**
     * Required empty constructor
     */
    public DatabaseService() {
    }


    @Override
    public IBinder onBind(Intent intent) {
        Log.d(TAG, "onBind: Service is bound, returning binder");
        return new MyBinder();
    }


    @Override
    public int onStartCommand(Intent intent, int flags, int StartId) {
        Log.d(TAG, "onStartCommand: Service started");
        return START_STICKY;
    }


    @Override
    public void onDestroy() {
        // Close SQLite handler
        Log.d(TAG, "onDestroy: Closing database");
        mSQLiteHandler.close();
        mSQLiteHandler = null;
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
            SecureDbHelper dbh = new SecureDbHelper(getServiceContext());
            mSQLiteHandler = dbh.getWritableDatabase(password);
            if (mSQLiteHandler == null || !mSQLiteHandler.isOpen()) {
                throw new SecurityException("Something went wrong while opening the database - wrong key?");
            }
        }


        /**
         * Check the status of the database
         * @return True if the database is open, false otherwise
         */
        @Override
        public boolean isDatabaseOpen() {
            return mSQLiteHandler != null && mSQLiteHandler.isOpen();
        }


        ///// Data retrieval from database
        /**
         * Retrieve the encoded private key for the pedometer service
         * @return The encoded private key, as a String
         */
        @Override
        public String getEncodedPedometerPrivkey() {
            // TODO Stub
            return null;
        }


        ///// Transaction Management
        /**
         * Begin a database transaction
         * @throws SQLiteException Thrown in case the database is not open or already in a transaction
         */
        @Override
        public void beginTransaction() throws SQLiteException {
            assertOpen();
            if (mSQLiteHandler.inTransaction())
                throw new SQLiteException("Already in a transaction");
            mSQLiteHandler.beginTransaction();
        }


        /**
         * Commit a database transaction
         * @throws SQLiteException Thrown in case the database is not open or not in a transaction
         */
        @Override
        public void commit() throws SQLiteException{
            assertOpen();
            if (!mSQLiteHandler.inTransaction())
                throw new SQLiteException("Not in a transaction");
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
            if (!mSQLiteHandler.inTransaction())
                throw new SQLiteException("Not in a transaction");
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
            return mSQLiteHandler.insertOrThrow(table, nullable, values);
        }

        /**
         * Assert that the database is open, or throw an exception
         * @throws SQLiteException Thrown if the database is not open
         */
        private void assertOpen() throws SQLiteException {
            if (!(mSQLiteHandler != null && mSQLiteHandler.isOpen()))
                throw new SQLiteException("Database is not open");
        }
    }
}
