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
        // Encoded key
        public static final String COLUMN_KEY_BYTES = "key";

        ///// Values
        public static final int TYPE_RSA_PRIV = 0;
        public static final int TYPE_RSA_PUB  = 1;
    }
}
