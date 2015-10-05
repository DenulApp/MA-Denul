package de.velcommuta.denul.db;

import android.content.Context;

import net.sqlcipher.database.SQLiteDatabase;
import net.sqlcipher.database.SQLiteOpenHelper;

/**
 * DB Helper for the location logging database
 */
public class SecureDbHelper extends SQLiteOpenHelper {
    // Universal constants for the construction of SQL queries
    private static final String TYPE_TEXT = " TEXT";
    private static final String TYPE_INT = " INTEGER";
    private static final String TYPE_FLOAT = " REAL";
    private static final String TYPE_DATETIME = " DATETIME";
    private static final String FKEY_DECL = "FOREIGN KEY(";
    private static final String FKEY_REFS = ") REFERENCES ";
    private static final String FKEY_ONDELETE_CASCADE = " ON DELETE CASCADE";
    private static final String OPT_PRIMARY_KEY = " PRIMARY KEY";
    private static final String OPT_NOT_NULL = " NOT NULL";
    private static final String OPT_DEFAULT_ZERO = " DEFAULT 0";
    private static final String OPT_DEFAULT_NOW = " DEFAULT CURRENT_TIMESTAMP";
    private static final String COMMA_SEP = ",";

    private static final String SQL_CREATE_ENTRIES_LOCATIONLOG
            = "CREATE TABLE " + LocationLoggingContract.LocationLog.TABLE_NAME + "(" +
            LocationLoggingContract.LocationLog._ID + TYPE_INT + OPT_PRIMARY_KEY + COMMA_SEP +
            LocationLoggingContract.LocationLog.COLUMN_NAME_SESSION + TYPE_INT + OPT_NOT_NULL + COMMA_SEP +
            LocationLoggingContract.LocationLog.COLUMN_NAME_TIMESTAMP + TYPE_DATETIME + OPT_DEFAULT_NOW + COMMA_SEP +
            LocationLoggingContract.LocationLog.COLUMN_NAME_LAT + TYPE_FLOAT + OPT_NOT_NULL + COMMA_SEP +
            LocationLoggingContract.LocationLog.COLUMN_NAME_LONG + TYPE_FLOAT + OPT_NOT_NULL + COMMA_SEP +
            FKEY_DECL + LocationLoggingContract.LocationLog.COLUMN_NAME_SESSION + FKEY_REFS +
            LocationLoggingContract.LocationSessions.TABLE_NAME + "(" + LocationLoggingContract.LocationSessions._ID + ")" +
            FKEY_ONDELETE_CASCADE + ");";

    private static final String SQL_CREATE_ENTRIES_LOCATIONSESSIONS
            = "CREATE TABLE " + LocationLoggingContract.LocationSessions.TABLE_NAME + "(" +
            LocationLoggingContract.LocationSessions._ID + TYPE_INT + OPT_PRIMARY_KEY + COMMA_SEP +
            LocationLoggingContract.LocationSessions.COLUMN_NAME_NAME + TYPE_TEXT + COMMA_SEP +
            LocationLoggingContract.LocationSessions.COLUMN_NAME_SESSION_START + TYPE_DATETIME + OPT_DEFAULT_NOW + COMMA_SEP +
            LocationLoggingContract.LocationSessions.COLUMN_NAME_SESSION_END + TYPE_DATETIME + COMMA_SEP +
            LocationLoggingContract.LocationSessions.COLUMN_NAME_MODE + TYPE_INT + OPT_DEFAULT_ZERO +
            ");";

    private static final String SQL_CREATE_ENTRIES_KEYSTORE
            = "CREATE TABLE " + VaultContract.KeyStore.TABLE_NAME + "(" +
            VaultContract.KeyStore._ID + TYPE_INT + OPT_PRIMARY_KEY + COMMA_SEP +
            VaultContract.KeyStore.COLUMN_KEY_TYPE + TYPE_INT + OPT_NOT_NULL + COMMA_SEP +
            VaultContract.KeyStore.COLUMN_KEY_NAME + TYPE_TEXT + COMMA_SEP +
            VaultContract.KeyStore.COLUMN_KEY_BYTES + TYPE_TEXT + OPT_NOT_NULL +
            ");";

    private static final String SQL_CREATE_ENTRIES_STEPCOUNTER
            = "CREATE TABLE " + StepLoggingContract.StepCountLog.TABLE_NAME + "(" +
            StepLoggingContract.StepCountLog._ID + TYPE_INT + OPT_PRIMARY_KEY + COMMA_SEP +
            StepLoggingContract.StepCountLog.COLUMN_DATE + TYPE_DATETIME + OPT_NOT_NULL + COMMA_SEP +
            StepLoggingContract.StepCountLog.COLUMN_TIME + TYPE_DATETIME + OPT_NOT_NULL + COMMA_SEP +
            StepLoggingContract.StepCountLog.COLUMN_VALUE + TYPE_INT + OPT_NOT_NULL +
            ");";

    private static final String SQL_CREATE_ENTRIES_SEQUENCE_NUMBERS
            = "CREATE TABLE " + VaultContract.SequenceNumberStore.TABLE_NAME + "(" +
            VaultContract.SequenceNumberStore._ID + TYPE_INT + OPT_PRIMARY_KEY + COMMA_SEP +
            VaultContract.SequenceNumberStore.COLUMN_SNR_TYPE + TYPE_TEXT + OPT_NOT_NULL + COMMA_SEP +
            VaultContract.SequenceNumberStore.COLUMN_SNR_VALUE + TYPE_TEXT + OPT_NOT_NULL +
            ");";

    private static final String SQL_DROP_LOCATIONLOG =
            "DROP TABLE " + LocationLoggingContract.LocationLog.TABLE_NAME + ";";

    private static final String SQL_DROP_LOCATIONSESSIONS =
            "DROP TABLE " + LocationLoggingContract.LocationSessions.TABLE_NAME + ";";

    private static final String SQL_DROP_KEYSTORE =
            "DROP TABLE " + VaultContract.KeyStore.TABLE_NAME + ";";

    private static final String SQL_DROP_STEPCOUNTER =
            "DROP TABLE " + StepLoggingContract.StepCountLog.TABLE_NAME + ";";

    private static final String SQL_DROP_SEQUENCE_NUMBERS =
            "DROP TABLE " + VaultContract.SequenceNumberStore.TABLE_NAME + ";";

    public static final String DATABASE_NAME = "location.db"; // TODO Update

    public static final int DATABASE_VERSION = 6;


    /**
     * Constructor for Helper function
     * @param ctx Context object
     */
    public SecureDbHelper(Context ctx) {
        super(ctx, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onOpen(SQLiteDatabase db) {
        super.onOpen(db);
        if (!db.isReadOnly()) {
            // Enable foreign key constraints
            db.execSQL("PRAGMA foreign_keys=ON;");
        }
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(SQL_CREATE_ENTRIES_LOCATIONSESSIONS);
        db.execSQL(SQL_CREATE_ENTRIES_LOCATIONLOG);
        db.execSQL(SQL_CREATE_ENTRIES_KEYSTORE);
        db.execSQL(SQL_CREATE_ENTRIES_STEPCOUNTER);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion == 5 && newVersion == 6) {
            db.execSQL(SQL_CREATE_ENTRIES_SEQUENCE_NUMBERS);
        } else {
            db.execSQL(SQL_DROP_LOCATIONLOG);
            db.execSQL(SQL_DROP_LOCATIONSESSIONS);
            db.execSQL(SQL_DROP_KEYSTORE);
            db.execSQL(SQL_DROP_STEPCOUNTER);
            db.execSQL(SQL_DROP_SEQUENCE_NUMBERS);
            onCreate(db);
        }
    }
}
