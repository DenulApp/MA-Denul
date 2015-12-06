package de.velcommuta.denul.util;

import de.velcommuta.denul.crypto.KeySet;
import de.velcommuta.denul.service.DatabaseServiceBinder;
import de.velcommuta.denul.ui.adapter.Friend;

/**
 * Helper class to perform common friend-related operations. Some of these functions just call
 * through to the database binder, but in the interest of having a unified API, they were included
 * anyway.
 */
public class FriendManagement {
    private static final String TAG = "FriendManagement";


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
}
