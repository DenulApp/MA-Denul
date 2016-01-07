package de.velcommuta.denul.db;

import android.content.Context;
import android.util.Log;

import net.sqlcipher.database.SQLiteDatabase;
import net.sqlcipher.database.SQLiteOpenHelper;

/**
 * DB Helper for the location logging database
 */
public class SecureDbHelper extends SQLiteOpenHelper {
    private static final String TAG = "SecureDbHelper";

    // Universal constants for the construction of SQL queries
    private static final String TYPE_TEXT = " TEXT";
    private static final String TYPE_INT = " INTEGER";
    private static final String TYPE_FLOAT = " REAL";
    private static final String TYPE_DATETIME = " DATETIME";
    private static final String TYPE_BLOB = " BLOB";
    private static final String FKEY_DECL = "FOREIGN KEY(";
    private static final String FKEY_REFS = ") REFERENCES ";
    private static final String FKEY_ONDELETE_CASCADE = " ON DELETE CASCADE";
    private static final String FKEY_ONDELETE_NULL = " ON DELETE SET NULL";
    private static final String OPT_PRIMARY_KEY = " PRIMARY KEY";
    private static final String OPT_NOT_NULL = " NOT NULL";
    private static final String OPT_DEFAULT_ZERO = " DEFAULT 0";
    private static final String OPT_DEFAULT_NULL = " DEFAULT NULL";
    private static final String OPT_DEFAULT_MINUS_ONE = " DEFAULT -1";
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
                FKEY_ONDELETE_CASCADE +
            ");";

    private static final String SQL_CREATE_ENTRIES_LOCATIONSESSIONS
            = "CREATE TABLE " + LocationLoggingContract.LocationSessions.TABLE_NAME + "(" +
            LocationLoggingContract.LocationSessions._ID + TYPE_INT + OPT_PRIMARY_KEY + COMMA_SEP +
            LocationLoggingContract.LocationSessions.COLUMN_NAME_NAME + TYPE_TEXT + COMMA_SEP +
            LocationLoggingContract.LocationSessions.COLUMN_NAME_OWNER + TYPE_INT + OPT_NOT_NULL + OPT_DEFAULT_MINUS_ONE + COMMA_SEP +
            LocationLoggingContract.LocationSessions.COLUMN_NAME_SESSION_START + TYPE_DATETIME + OPT_DEFAULT_NOW + OPT_NOT_NULL + COMMA_SEP +
            LocationLoggingContract.LocationSessions.COLUMN_NAME_SESSION_END + TYPE_DATETIME + OPT_NOT_NULL + COMMA_SEP +
            LocationLoggingContract.LocationSessions.COLUMN_NAME_TIMEZONE + TYPE_TEXT + OPT_NOT_NULL + COMMA_SEP +
            LocationLoggingContract.LocationSessions.COLUMN_NAME_DISTANCE + TYPE_FLOAT + OPT_NOT_NULL + COMMA_SEP +
            LocationLoggingContract.LocationSessions.COLUMN_NAME_MODE + TYPE_INT + OPT_DEFAULT_ZERO + COMMA_SEP +
            LocationLoggingContract.LocationSessions.COLUMN_NAME_DESCRIPTION + TYPE_TEXT + OPT_DEFAULT_NULL + COMMA_SEP +
            LocationLoggingContract.LocationSessions.COLUMN_NAME_SHARE_ID + TYPE_INT + OPT_DEFAULT_NULL + COMMA_SEP +
            FKEY_DECL + LocationLoggingContract.LocationSessions.COLUMN_NAME_SHARE_ID + FKEY_REFS +
                SharingContract.DataShareLog.TABLE_NAME + "(" + SharingContract.DataShareLog._ID + ")" +
                FKEY_ONDELETE_NULL +
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
            StepLoggingContract.StepCountLog.COLUMN_OWNER + TYPE_INT + OPT_NOT_NULL + OPT_DEFAULT_MINUS_ONE + COMMA_SEP +
            StepLoggingContract.StepCountLog.COLUMN_DATE + TYPE_DATETIME + OPT_NOT_NULL + COMMA_SEP +
            StepLoggingContract.StepCountLog.COLUMN_TIME + TYPE_DATETIME + OPT_NOT_NULL + COMMA_SEP +
            StepLoggingContract.StepCountLog.COLUMN_VALUE + TYPE_INT + OPT_NOT_NULL + COMMA_SEP +
            StepLoggingContract.StepCountLog.COLUMN_SHARE_ID + TYPE_INT + OPT_DEFAULT_NULL + COMMA_SEP +
            FKEY_DECL + StepLoggingContract.StepCountLog.COLUMN_SHARE_ID + FKEY_REFS +
                SharingContract.DataShareLog.TABLE_NAME + "(" + SharingContract.DataShareLog._ID + ")" +
                FKEY_ONDELETE_NULL +
            ");";

    private static final String SQL_CREATE_ENTRIES_SEQUENCE_NUMBERS
            = "CREATE TABLE " + VaultContract.SequenceNumberStore.TABLE_NAME + "(" +
            VaultContract.SequenceNumberStore._ID + TYPE_INT + OPT_PRIMARY_KEY + COMMA_SEP +
            VaultContract.SequenceNumberStore.COLUMN_SNR_TYPE + TYPE_TEXT + OPT_NOT_NULL + COMMA_SEP +
            VaultContract.SequenceNumberStore.COLUMN_SNR_VALUE + TYPE_INT + OPT_NOT_NULL +
            ");";

    private static final String SQL_CREATE_ENTRIES_FRIENDLIST
            = "CREATE TABLE " + FriendContract.FriendList.TABLE_NAME + "(" +
            FriendContract.FriendList._ID + TYPE_INT + OPT_PRIMARY_KEY + COMMA_SEP +
            FriendContract.FriendList.COLUMN_NAME_FRIEND + TYPE_TEXT + OPT_NOT_NULL + COMMA_SEP +
            FriendContract.FriendList.COLUMN_NAME_VERIFIED + TYPE_INT + OPT_DEFAULT_ZERO +
            ");";

    private static final String SQL_CREATE_ENTRIES_FRIENDKEYS
            = "CREATE TABLE " + FriendContract.FriendKeys.TABLE_NAME + "(" +
            FriendContract.FriendKeys._ID + TYPE_INT + OPT_PRIMARY_KEY + COMMA_SEP +
            FriendContract.FriendKeys.COLUMN_NAME_FRIEND_ID + TYPE_INT + OPT_NOT_NULL + COMMA_SEP +
            FriendContract.FriendKeys.COLUMN_NAME_KEY_IN + TYPE_BLOB + OPT_NOT_NULL + COMMA_SEP +
            FriendContract.FriendKeys.COLUMN_NAME_CTR_IN + TYPE_BLOB + OPT_NOT_NULL + COMMA_SEP +
            FriendContract.FriendKeys.COLUMN_NAME_KEY_OUT + TYPE_BLOB + OPT_NOT_NULL + COMMA_SEP +
            FriendContract.FriendKeys.COLUMN_NAME_CTR_OUT + TYPE_BLOB + OPT_NOT_NULL + COMMA_SEP +
            FriendContract.FriendKeys.COLUMN_NAME_INITIATED + TYPE_INT + OPT_NOT_NULL + COMMA_SEP +
            FKEY_DECL + FriendContract.FriendKeys.COLUMN_NAME_FRIEND_ID + FKEY_REFS +
                FriendContract.FriendList.TABLE_NAME + "(" + FriendContract.FriendList._ID + ")" +
                FKEY_ONDELETE_CASCADE +
            ");";

    private static final String SQL_CREATE_ENTRIES_DATASHARELOG
            = "CREATE TABLE " + SharingContract.DataShareLog.TABLE_NAME + "(" +
            SharingContract.DataShareLog._ID + TYPE_INT + OPT_PRIMARY_KEY + COMMA_SEP +
            SharingContract.DataShareLog.COLUMN_IDENTIFIER + TYPE_BLOB + OPT_NOT_NULL + COMMA_SEP +
            SharingContract.DataShareLog.COLUMN_REVOCATION_TOKEN + TYPE_BLOB + OPT_NOT_NULL + COMMA_SEP +
            SharingContract.DataShareLog.COLUMN_KEY + TYPE_BLOB + OPT_NOT_NULL + COMMA_SEP +
            SharingContract.DataShareLog.COLUMN_GRANULARITY + TYPE_INT + OPT_NOT_NULL + OPT_DEFAULT_ZERO +
            ");";

    private static final String SQL_CREATE_ENTRIES_FRIENDSHAREDLOG
            = "CREATE TABLE " + SharingContract.FriendShareLog.TABLE_NAME + "(" +
            SharingContract.FriendShareLog._ID + TYPE_INT + OPT_PRIMARY_KEY + COMMA_SEP +
            SharingContract.FriendShareLog.COLUMN_DATASHARE_ID + TYPE_INT + OPT_NOT_NULL + COMMA_SEP +
            SharingContract.FriendShareLog.COLUMN_FRIEND_ID + TYPE_INT + OPT_NOT_NULL + COMMA_SEP +
            SharingContract.FriendShareLog.COLUMN_IDENTIFIER + TYPE_BLOB + OPT_NOT_NULL + COMMA_SEP +
            SharingContract.FriendShareLog.COLUMN_REVOCATION_TOKEN + TYPE_BLOB + OPT_NOT_NULL + COMMA_SEP +
            FKEY_DECL + SharingContract.FriendShareLog.COLUMN_DATASHARE_ID + FKEY_REFS +
                SharingContract.DataShareLog.TABLE_NAME + "(" + SharingContract.DataShareLog._ID + ")" +
                FKEY_ONDELETE_CASCADE + COMMA_SEP +
            FKEY_DECL + SharingContract.FriendShareLog.COLUMN_FRIEND_ID + FKEY_REFS +
                FriendContract.FriendList.TABLE_NAME + "(" + FriendContract.FriendList._ID + ")" +
                FKEY_ONDELETE_CASCADE +
            ");";

    private static final String SQL_CREATE_ENTRIES_STUDIES
            = "CREATE TABLE " + StudyContract.Studies.TABLE_NAME + " (" +
            StudyContract.Studies._ID + TYPE_INT + OPT_PRIMARY_KEY + COMMA_SEP +
            StudyContract.Studies.COLUMN_NAME + TYPE_TEXT + COMMA_SEP +
            StudyContract.Studies.COLUMN_INSTITUTION + TYPE_TEXT + COMMA_SEP +
            StudyContract.Studies.COLUMN_WEB + TYPE_TEXT + COMMA_SEP +
            StudyContract.Studies.COLUMN_DESCRIPTION + TYPE_TEXT + COMMA_SEP +
            StudyContract.Studies.COLUMN_PURPOSE + TYPE_TEXT + COMMA_SEP +
            StudyContract.Studies.COLUMN_PROCEDURES + TYPE_TEXT + COMMA_SEP +
            StudyContract.Studies.COLUMN_RISKS + TYPE_TEXT + COMMA_SEP +
            StudyContract.Studies.COLUMN_BENEFITS + TYPE_TEXT + COMMA_SEP +
            StudyContract.Studies.COLUMN_PAYMENT + TYPE_TEXT + COMMA_SEP +
            StudyContract.Studies.COLUMN_CONFLICTS + TYPE_TEXT + COMMA_SEP +
            StudyContract.Studies.COLUMN_CONFIDENTIALITY + TYPE_TEXT + COMMA_SEP +
            StudyContract.Studies.COLUMN_PARTICIPATION + TYPE_TEXT + COMMA_SEP +
            StudyContract.Studies.COLUMN_RIGHTS + TYPE_TEXT + COMMA_SEP +
            StudyContract.Studies.COLUMN_VERIFICATION + TYPE_INT + COMMA_SEP +
            StudyContract.Studies.COLUMN_PRIVKEY + TYPE_TEXT + COMMA_SEP +
            StudyContract.Studies.COLUMN_PUBKEY + TYPE_TEXT + COMMA_SEP +
            StudyContract.Studies.COLUMN_KEYALGO + TYPE_INT + COMMA_SEP +
            StudyContract.Studies.COLUMN_KEX + TYPE_BLOB + COMMA_SEP +
            StudyContract.Studies.COLUMN_KEXALGO + TYPE_INT + COMMA_SEP +
            StudyContract.Studies.COLUMN_QUEUE + TYPE_BLOB +
            ");";

    private static final String SQL_CREATE_ENTRIES_INVESTIGATORS
            = "CREATE TABLE " + StudyContract.Investigators.TABLE_NAME + " (" +
            StudyContract.Investigators._ID + TYPE_INT + OPT_PRIMARY_KEY + COMMA_SEP +
            StudyContract.Investigators.COLUMN_STUDY + TYPE_INT + OPT_NOT_NULL + COMMA_SEP +
            StudyContract.Investigators.COLUMN_NAME + TYPE_TEXT + OPT_NOT_NULL + COMMA_SEP +
            StudyContract.Investigators.COLUMN_INSTITUTION + TYPE_TEXT + OPT_NOT_NULL + COMMA_SEP +
            StudyContract.Investigators.COLUMN_GROUP + TYPE_TEXT + OPT_NOT_NULL + COMMA_SEP +
            StudyContract.Investigators.COLUMN_POSITION + TYPE_TEXT + OPT_NOT_NULL + COMMA_SEP +
            FKEY_DECL + StudyContract.Investigators.COLUMN_STUDY + FKEY_REFS + StudyContract.Studies.TABLE_NAME +
            "(" + StudyContract.Studies._ID + ") " + FKEY_ONDELETE_CASCADE +
            ");";

    private static final String SQL_CREATE_ENTRIES_DATAREQUESTS
            = "CREATE TABLE " + StudyContract.DataRequests.TABLE_NAME + " (" +
            StudyContract.DataRequests._ID + TYPE_INT + OPT_PRIMARY_KEY + COMMA_SEP +
            StudyContract.DataRequests.COLUMN_STUDY + TYPE_INT + OPT_NOT_NULL + COMMA_SEP +
            StudyContract.DataRequests.COLUMN_DATATYPE + TYPE_INT + OPT_NOT_NULL + COMMA_SEP +
            StudyContract.DataRequests.COLUMN_GRANULARITY + TYPE_INT + OPT_NOT_NULL + COMMA_SEP +
            StudyContract.DataRequests.COLUMN_FREQUENCY + TYPE_INT + OPT_NOT_NULL + COMMA_SEP +
            FKEY_DECL + StudyContract.DataRequests.COLUMN_STUDY + FKEY_REFS + StudyContract.Studies.TABLE_NAME +
            "(" + StudyContract.Studies._ID + ")" + FKEY_ONDELETE_CASCADE +
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

    private static final String SQL_DROP_FRIENDLIST =
            "DROP TABLE " + FriendContract.FriendList.TABLE_NAME + ";";

    private static final String SQL_DROP_FRIENDKEYS =
            "DROP TABLE " + FriendContract.FriendKeys.TABLE_NAME + ";";

    private static final String SQL_DROP_DATASHARELOG =
            "DROP TABLE " + SharingContract.DataShareLog.TABLE_NAME + ";";

    private static final String SQL_DROP_FRIENDSHARELOG =
            "DROP TABLE " + SharingContract.FriendShareLog.TABLE_NAME + ";";

    private static final String SQL_DROP_STUDIES =
            "DROP TABLE " + StudyContract.Studies.TABLE_NAME + ";";

    private static final String SQL_DROP_INVESTIGATORS =
            "DROP TABLE " + StudyContract.Investigators.TABLE_NAME + ";";

    private static final String SQL_DROP_DATAREQUESTS =
            "DROP TABLE " + StudyContract.DataRequests.TABLE_NAME + ";";

    public static final String DATABASE_NAME = "location.db"; // TODO Update

    public static final int DATABASE_VERSION = 17;


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
        db.execSQL(SQL_CREATE_ENTRIES_SEQUENCE_NUMBERS);
        db.execSQL(SQL_CREATE_ENTRIES_FRIENDLIST);
        db.execSQL(SQL_CREATE_ENTRIES_FRIENDKEYS);
        db.execSQL(SQL_CREATE_ENTRIES_DATASHARELOG);
        db.execSQL(SQL_CREATE_ENTRIES_FRIENDSHAREDLOG);
        db.execSQL(SQL_CREATE_ENTRIES_STUDIES);
        db.execSQL(SQL_CREATE_ENTRIES_INVESTIGATORS);
        db.execSQL(SQL_CREATE_ENTRIES_DATAREQUESTS);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        Log.d(TAG, "onUpgrade: Old = " + oldVersion + " new = " + newVersion);
        if (oldVersion == 16 && newVersion == 17) {
            db.execSQL(SQL_CREATE_ENTRIES_STUDIES);
            db.execSQL(SQL_CREATE_ENTRIES_INVESTIGATORS);
            db.execSQL(SQL_CREATE_ENTRIES_DATAREQUESTS);
        } else if (oldVersion == 15 && newVersion == 16) {
            // Rebuild everything except KeyStore and StepCounter
            db.execSQL(SQL_DROP_LOCATIONLOG);
            db.execSQL(SQL_DROP_LOCATIONSESSIONS);
            db.execSQL(SQL_DROP_SEQUENCE_NUMBERS);
            db.execSQL(SQL_DROP_FRIENDLIST);
            db.execSQL(SQL_DROP_FRIENDKEYS);
            db.execSQL(SQL_DROP_FRIENDSHARELOG);
            db.execSQL(SQL_DROP_DATASHARELOG);
            db.execSQL(SQL_CREATE_ENTRIES_LOCATIONSESSIONS);
            db.execSQL(SQL_CREATE_ENTRIES_LOCATIONLOG);
            db.execSQL(SQL_CREATE_ENTRIES_SEQUENCE_NUMBERS);
            db.execSQL(SQL_CREATE_ENTRIES_FRIENDLIST);
            db.execSQL(SQL_CREATE_ENTRIES_FRIENDKEYS);
            db.execSQL(SQL_CREATE_ENTRIES_DATASHARELOG);
            db.execSQL(SQL_CREATE_ENTRIES_FRIENDSHAREDLOG);
        } else {
            db.execSQL(SQL_DROP_LOCATIONLOG);
            db.execSQL(SQL_DROP_LOCATIONSESSIONS);
            db.execSQL(SQL_DROP_KEYSTORE);
            db.execSQL(SQL_DROP_STEPCOUNTER);
            db.execSQL(SQL_DROP_SEQUENCE_NUMBERS);
            db.execSQL(SQL_DROP_FRIENDLIST);
            db.execSQL(SQL_DROP_FRIENDKEYS);
            db.execSQL(SQL_DROP_FRIENDSHARELOG);
            db.execSQL(SQL_DROP_DATASHARELOG);
            db.execSQL(SQL_DROP_STUDIES);
            db.execSQL(SQL_DROP_INVESTIGATORS);
            db.execSQL(SQL_DROP_DATAREQUESTS);
            onCreate(db);
        }
    }
}
