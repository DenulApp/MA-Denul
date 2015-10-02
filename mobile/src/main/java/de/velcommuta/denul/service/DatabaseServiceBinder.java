package de.velcommuta.denul.service;

import android.content.ContentValues;

/**
 * Interface for the DatabaseService class, for use with an IBinder implementation
 */
public interface DatabaseServiceBinder {
    /**
     * Open the secure database with the specified password
     * @param password The password to decrypt the database
     */
    void openDatabase(String password);

    /**
     * Method to check if the database is currently open
     * @return true if yes, false if not
     */
    boolean isDatabaseOpen();

    /**
     * Method to retrieve the encoded private key
     * @return The String-encoded private key
     */
    String getEncodedPedometerPrivkey();


    /**
     * Begin a database transaction
     */
    void beginTransaction();


    /**
     * Commit a database transaction
     */
    void commit();


    /**
     * Roll back a database transaction
     */
    void revert();

    
    /**
     * Insert a ContentValues object into the database
     * @param table Table to insert into
     * @param nullable Nullable columns, as per the insert logic of the database interface
     * @param values The ContentValues object
     * @return The RowID of the inserted record, as per the original APIs
     */
    long insert(String table, String nullable, ContentValues values);
}
