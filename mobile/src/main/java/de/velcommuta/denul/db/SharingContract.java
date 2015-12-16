package de.velcommuta.denul.db;

import android.provider.BaseColumns;

/**
 * DB contract class for sharing data housekeeping
 * We are using two different tables: The DataShareLog contains information about the uploaded data
 * packets (their key, identifier and revocation token). The FriendShareLog contains information
 * about who received these shares and with which values, so they can be changed and revoked.
 */
public class SharingContract {

    public static abstract class DataShareLog implements BaseColumns {
        public static final String TABLE_NAME = "data_share_log";

        public static final String COLUMN_IDENTIFIER = "identifier";
        public static final String COLUMN_REVOCATION_TOKEN = "revocation_token";
        public static final String COLUMN_KEY = "encryption_key";
        public static final String COLUMN_GRANULARITY = "granularity";
    }

    public static abstract class FriendShareLog implements BaseColumns {
        public static final String TABLE_NAME = "friend_share_log";

        // Foreign key referencing DataShareLog
        public static final String COLUMN_DATASHARE_ID = "datashare_id";
        // Foreign key referencing FriendList
        public static final String COLUMN_FRIEND_ID = "friend_id";
        // revocation token and identifier (key is the regular key of the friend)
        public static final String COLUMN_IDENTIFIER = "identifier";
        public static final String COLUMN_REVOCATION_TOKEN = "revocation_token";
    }
}
