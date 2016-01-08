package de.velcommuta.denul.service;

import org.joda.time.DateTime;

import java.util.Hashtable;
import java.util.List;

import de.velcommuta.denul.data.DataBlock;
import de.velcommuta.denul.data.GPSTrack;
import de.velcommuta.denul.data.KeySet;
import de.velcommuta.denul.data.Friend;
import de.velcommuta.denul.data.Shareable;
import de.velcommuta.denul.data.StudyRequest;
import de.velcommuta.denul.data.TokenPair;

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
     * Add a GPS track to the database, owned by the owner of the device
     * @param track The GPS track to add
     */
    void addGPSTrack(GPSTrack track);

    /**
     * Add a GPS track to the database, owned by the specified friend
     * @param track The track
     * @param friend The friend
     */
    void addGPSTrackForFriend(GPSTrack track, Friend friend);

    /**
     * Retrieve the list of all GPS tracks from the database and return it
     * @return A list of all GPSTracks in the database
     */
    List<GPSTrack> getGPSTracks();

    /**
     * Retrieve the list of all GPS tracks owned by the user from the database and return it
     * @return A list of all GPSTracks owned by the user in the database
     */
    List<GPSTrack> getOwnerGPSTracks();

    /**
     * Get all GPS tracks shared by a specific friend
     * @param friend The Friend. MUST NOT return -1 on a call to {@link Friend#getID()}
     * @return A List of all GPSTracks shared by the friend, or an empty List.
     */
    List<GPSTrack> getGPSTrackByFriend(Friend friend);

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
     * @param name The new name of the GPS track
     */
    void renameGPSTrack(GPSTrack track, String name);

    /**
     * Integrate the pedometer cache with the database, ensuring that the cached data is persistently
     * saved
     * @param cache The cache object
     */
    void integratePedometerCache(Hashtable<DateTime, Long> cache);

    /**
     * Retrieve the step count for a certain {@link DateTime} timestamp from the database
     * @param ts The Timestamp
     * @return The step count, or -1 if no data is in the database for that timestamp
     */
    int getStepCountForTimestamp(DateTime ts);

    /**
     * Retrieve the pedometer private key from the database, encoded as a String
     * @return The pedometer private key, as string, if it exists
     */
    String getPedometerPrivateKey();

    /**
     * Store the keypair used by the pedometer to protect its cache files
     * @param pubkey The String-encoded public key
     * @param privkey The String-encoded private key
     */
    void storePedometerKeypair(String pubkey, String privkey);

    /**
     * Retrieve the step count history for a specific day from the database
     * @param date The day for which the history should be retrieved
     * @return The history, if available, or an empty Hashtable
     */
    Hashtable<DateTime, Long> getStepCountForDay(DateTime date);

    /**
     * Getter for the largest used step counter sequence number (used during encryption of the cache)
     * @return The sequence number, or -1 if none was found
     */
    int getMaxStepCounterSequenceNumber();

    /**
     * Update the stored maximum sequence number of the step counter in the database
     * @param seqnr The sequence number
     */
    void storeStepCounterSequenceNumber(int seqnr);

    /**
     * Check if a certain sharable has already been shared before
     * @param sh The shareable
     * @return true if it was shared before (i.e. an uploaded DataBlock exists), false otherwise
     */
    boolean isShared(Shareable sh);

    /**
     * Determine the share ID of a Shareable (the ID referencing the sharing information of the
     * {@link DataBlock} associated with the shareable, if it exists).
     * @param sh The shareable
     * @return The ID of the DataBlock information in the database, if available, or -1, if the
     *         Shareable has not been shared
     */
    int getShareID(Shareable sh);

    /**
     * Determine the granularity with which a Shareable was shared.
     * @param sh The Shareable (with {@link Shareable#getID()} != -1)
     * @return The share granularity, or -1 if the Shareable was not shared yet
     */
    int getShareGranularity(Shareable sh);

    /**
     * Add information about a shared {@link DataBlock} to the database
     * @param sh The {@link Shareable} represented by the DataBlock
     * @param pair The TokenPair containing identifier and revocation token for the Data block
     * @param block The data block itself
     * @return The database ID of the new share in the DataShareLog table, or -1 if the Shareable
     *         was already shared with different data.
     */
    int addShare(Shareable sh, TokenPair pair, DataBlock block);

    /**
     * Add information about a recipient for an existing DataBlock share to the database
     * @param shareid The ID of the entry in the DataShareLog table
     * @param friend The Friend to whom the data was shared
     * @param pair The TokenPair that was used
     */
    void addShareRecipient(int shareid, Friend friend, TokenPair pair);

    /**
     * Return a List of all Friends who received the provided shareable
     * @param shareable A {@link Shareable} with {@link Shareable#getID()} != -1
     * @return A List of {@link Friend}s that received the Shareable
     */
    List<Friend> getShareRecipientsForShareable(Shareable shareable);

    /**
     * Get a list of all tokens associated with a shareable. This includes the tokens used to store
     * the shareable itself, and all tokens of data referring to the shareable.
     * @param shareable The shareable. MUST NOT return -1 on a call to {@link Shareable#getID()}.
     * @return A List of {@link TokenPair} objects containing identifiers and revocation information
     *         for the shareable. The List may be empty if the Shareable has not been shared yet
     */
    List<TokenPair> getTokensForShareable(Shareable shareable);

    /**
     * Get a List of all TokenPairs sharing data to a specific Friend.
     * @param friend The {@link Friend} to check for. MUST NOT return -1 on a call to {@link Friend#getID()}
     * @return A List of {@link TokenPair}s
     */
    List<TokenPair> getTokensForSharesToFriend(Friend friend);

    /**
     * Get the token used to share a specific {@link Shareable} to a specific {@link Friend}
     * @param shareable The shareable. MUST NOT return -1 on {@link Shareable#getID()}
     * @param friend The Friend. MUST NOT return -1 on {@link Friend#getID()}
     * @return The {@link TokenPair} associated with the share
     */
    TokenPair getTokenForShareToFriend(Shareable shareable, Friend friend);

    /**
     * Retrieve the share data from the database
     * @param shareid The ID of the entry in the share database
     * @return A {@link DataBlock} with the Identifier and Key properties set
     */
    DataBlock getShareData(int shareid);

    /**
     * Find a {@link TokenPair} in the database and delete it
     * @param tokenPair The TokenPair
     * @return True if the deletion succeeded, false otherwise
     */
    boolean deleteShareByToken(TokenPair tokenPair);

    /**
     * Delete all data that has been shared by a specific user
     * @param friend The friend owning the data. The object MUST NOT return -1 on the
     *               {@link Friend#getID()} call.
     */
    void deleteSharesByFriend(Friend friend);

    /**
     * Add a shareable to the database (i.e. determine what the type is, and insert it into the
     * proper database with the proper owner)
     * @param shareable The shareable to add
     */
    void addShareable(Shareable shareable);

    /**
     * Delete a shareable from the database
     * @param shareable The shareable to delete. The object MUST NOT return -1 on a call to
     *                  {@link Shareable#getID}
     */
    void deleteShareable(Shareable shareable);

    /**
     * Update the description of a shareable in the database
     * @param shareable The shareable object with the new description returned by
     * {@link Shareable#getDescription()}
     */
    void updateShareableDescription(Shareable shareable);

    /**
     * Add a {@link StudyRequest} to the database
     * @param studyRequest The study request
     * @return The ID of the inserted record, or -1 in case of an error
     */
    long addStudyRequest(StudyRequest studyRequest);

    /**
     * Delete a study from the database
     * @param req The study
     */
    void deleteStudy(StudyRequest req);

    /**
     * Retrieve a specific {@link StudyRequest} by its ID
     * @param id The ID
     * @return The study request
     */
    StudyRequest getStudyRequestByID(long id);

    /**
     * Retrieve all {@link StudyRequest}s from the database
     * @return A List of study requests
     */
    List<StudyRequest> getStudyRequests();

    /**
     * Update a study in the database with a new participation status and keys
     * @param req The StudyRequest with the updated data
     */
    void updateStudy(StudyRequest req);
}
