package de.velcommuta.denul.service;

import android.content.ContentValues;

import net.sqlcipher.Cursor;

import java.util.List;

import de.velcommuta.denul.data.GPSTrack;
import de.velcommuta.denul.data.KeySet;
import de.velcommuta.denul.data.Friend;

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

    /**
     * Query the SQLite database. Many of the parameters can be nulled if they should not be used
     * @param table The table to query
     * @param columns The columns to return
     * @param selection Filter to query which rows should be displayed
     * @param selectionArgs Arguments to selection (for "?" wildcards)
     * @param groupBy Grouping clause (excluding the GROUP BY statement)
     * @param having Filtering clause (excluding the HAVING)
     * @param orderBy Ordering clause (excluding the ORDER BY)
     * @return A cursor object that can be used to retrieve the data
     */
    Cursor query(String table, String[] columns, String selection, String[] selectionArgs, String groupBy, String having, String orderBy);

    /**
     * Send an UPDATE query to the SQLite database
     * @param table The table to be updated
     * @param values The values to update it with
     * @param selection The selection of which entries should be updated
     * @param selectionArgs The arguments to replace wildcards ("?")
     * @return The number of updated entries
     */
    int update(String table, ContentValues values, String selection, String[] selectionArgs);

    /**
     * Query the database for all friends and return the result as a cursor
     * @return A cursor containing the result of a SELECT on the friend database
     */
    List<Friend> getFriends();

    /**
     * Get a specific friend from the database, based on the ID
     * @param id ID of the friend
     * @return The Friend
     */
    Friend getFriendById(int id);

    /**
     * Get the {@link KeySet} for a {@link Friend} from the database
     * @param friend The Friend to get the KeySet for
     * @return The KeySet
     */
    KeySet getKeySetForFriend(Friend friend);

    /**
     * Update the {@link KeySet} in the database with the provided values. Will only update the
     * Keys and Counters
     * @param keyset The updated KeySet object
     */
    void updateKeySet(KeySet keyset);

    /**
     * Add a friend to the database
     * @param friend The {@link Friend} object describing the new friend
     * @param keys The {@link KeySet} containing the keys for communication with that friend
     */
    void addFriend(Friend friend, KeySet keys);

    /**
     * Remove a {@link Friend} and the related {@link KeySet} from the database.
     * @param friend The Friend to remove
     */
    void deleteFriend(Friend friend);

    /**
     * Update an existing friend in the database
     * @param friend The {@link Friend} object containing the new values
     */
    void updateFriend(Friend friend);

    /**
     * Check if a certain name is available
     * @param name The name to check for
     * @return True if the name is not yet taken, false otherwise
     */
    boolean isNameAvailable(String name);

    /**
     * Retrieve the list of all GPS tracks from the database and return it
     * @return A list of all GPSTracks in the database
     */
    List<GPSTrack> getGPSTracks();

    /**
     * Retrieve a specific GPS track from the database
     * @param id The ID of the GPS track
     * @return The GPS track, or null if no such GPS track exists
     */
    GPSTrack getGPSTrackById(int id);

    /**
     * Delete a {@link GPSTrack} from the database. The track MUST have the ID property set (i.e.
     * {@link GPSTrack#getID()} must not return -1).
     * @param track The track object
     */
    void deleteGPSTrack(GPSTrack track);

    /**
     * Rename a {@link GPSTrack} in the database. The provided track MUST have the ID property set
     * (i.e. {@link GPSTrack#getID()} must not return -1) and it MUST NOT have been modified since
     * it was set when it was loaded from the database.
     * @param track The track object
     */
    void renameGPSTrack(GPSTrack track, String name);
}
