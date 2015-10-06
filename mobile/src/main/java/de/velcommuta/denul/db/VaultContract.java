package de.velcommuta.denul.db;

import android.provider.BaseColumns;

/**
 * Contract for the Vault table in the encrypted SQLite database.
 * The Vault is used to store cryptographic keys and other important secrets.
 */
public class VaultContract {
    /**
     * Empty constructor. This Class should not be instantiated
     */
    public VaultContract() {}

    public static abstract class KeyStore implements BaseColumns {
        // Name of the SQLite table
        public static final String TABLE_NAME = "keystore";

        ///// Column Names
        // Key type identifier (RSA, AES, ...)
        public static final String COLUMN_KEY_TYPE = "keytype";
        // Key instance identifier (Pedometer, ...)
        public static final String COLUMN_KEY_NAME = "name";
        // Encoded key
        public static final String COLUMN_KEY_BYTES = "key";

        ///// Constants
        public static final int TYPE_RSA_PRIV = 0;
        public static final int TYPE_RSA_PUB  = 1;

        public static final String NAME_PEDOMETER_PRIVATE = "internal-pedometer-private-key";
        public static final String NAME_PEDOMETER_PUBLIC = "internal-pedometer-public-key";
    }


    public static abstract class SequenceNumberStore implements BaseColumns {
        // Name of the SQLite table
        public static final String TABLE_NAME = "seqnrstore";

        ///// Column names
        // Sequence number type identifier
        public static final String COLUMN_SNR_TYPE = "seqnr_type";
        // Sequence number
        public static final String COLUMN_SNR_VALUE = "seqnr";

        ///// Constants
        public static final String TYPE_PEDOMETER = "internal-pedometer";
    }
}
