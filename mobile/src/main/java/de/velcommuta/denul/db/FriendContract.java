package de.velcommuta.denul.db;

import android.provider.BaseColumns;

/**
 * SQL Contract class for the friend connection database, as proposed in
 * https://developer.android.com/training/basics/data-storage/databases.html
 */
public class FriendContract {
    /**
     * The FriendList table contains meta information about the friend. For now, it will only
     * contain a name and the verification status of the keypairs, but the list may grow.
     */
    public static abstract class FriendList implements BaseColumns {
        // Name of the SQLite table
        public static final String TABLE_NAME = "friend_list";

        // Column for the name of the friend
        public static final String COLUMN_NAME_FRIEND = "name";
        // Status of the verification of the friend (cryptographic keys)
        public static final String COLUMN_NAME_VERIFIED = "verification_status";
    }


    /**
     * The FriendKeys table will contain the directional cryptographic keys and counters used
     * in the protocol
     */
    public static abstract class FriendKeys implements BaseColumns {
        // Name of the table
        public static final String TABLE_NAME = "friend_keys";

        // Column with the ID of the friend (_id in the FriendList, provided by BaseColumns)
        // Will become a foreign key
        public static final String COLUMN_NAME_FRIEND_ID = "friend_id";
        // Key to use when encrypting FOR this person
        public static final String COLUMN_NAME_KEY_OUT = "key_out";
        // Counter to use when encrypting FOR this person
        public static final String COLUMN_NAME_CTR_OUT = "ctr_out";
        // Key to use when decrypting FROM this person
        public static final String COLUMN_NAME_KEY_IN = "key_in";
        // Counter to use when decrypting FROM this person
        public static final String COLUMN_NAME_CTR_IN = "ctr_in";
        // Did this device initiate the key exchange?
        public static final String COLUMN_NAME_INITIATED = "initiated";
    }
}
