package de.velcommuta.denul.util;

import de.velcommuta.denul.data.KeySet;
import de.velcommuta.denul.service.DatabaseServiceBinder;
import de.velcommuta.denul.data.Friend;

/**
 * Helper class to perform common friend-related operations. Some of these functions just call
 * through to the database binder, but in the interest of having a unified API, they were included
 * anyway.
 */
public class FriendManager {
    private static final String TAG = "FriendManager";


    /**
     * Add a friend to the database
     * @param friend The friend
     * @param keys The KeySet for the friend
     * @param binder The {@link DatabaseServiceBinder} to use
     */
    public static void addFriend(Friend friend, KeySet keys, DatabaseServiceBinder binder) {
        binder.addFriend(friend, keys);
    }


    /**
     * Delete a friend and related keys from the database
     * @param friend The friend to delete
     * @param binder The {@link DatabaseServiceBinder} to use
     */
    public static void deleteFriend(Friend friend, DatabaseServiceBinder binder) {
        // TODO Delete data?
        // TODO Delete from server?
        binder.deleteFriend(friend);
    }


    /**
     * Retrieve a Friend from the database, based on the database ID
     * @param id The ID of the friend
     * @param binder The {@link DatabaseServiceBinder} to use
     * @return The Friend
     */
    public static Friend getFriendById(int id, DatabaseServiceBinder binder) {
        return binder.getFriendById(id);
    }


    /**
     * Retrieve the {@link KeySet} for a {@link Friend} from the Database
     * @param friend The Friend for which the KeySet should be retrieved
     * @param binder The {@link DatabaseServiceBinder} to use
     * @return The {@link KeySet}
     */
    public static KeySet getKeySetForFriend(Friend friend, DatabaseServiceBinder binder) {
        return binder.getKeySetForFriend(friend);
    }


    /**
     * Update the database entry for an existing friend
     * @param friend The {@link Friend} object with the updated values
     * @param binder The {@link DatabaseServiceBinder} to use
     */
    public static void updateFriend(Friend friend, DatabaseServiceBinder binder) {
        binder.updateFriend(friend);
    }


    /**
     * Check if a certain name is still available in the database
     * @param name The name to check for
     * @param binder The {@link DatabaseServiceBinder} to use
     * @return True if the name is available, false otherwise
     */
    public static boolean isNameAvailable(String name, DatabaseServiceBinder binder) {
        return binder.isNameAvailable(name);
    }
}
