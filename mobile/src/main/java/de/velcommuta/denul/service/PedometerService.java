package de.velcommuta.denul.service;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.IBinder;
import android.os.SystemClock;
import android.util.Log;

import net.sqlcipher.Cursor;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormat;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;

import de.greenrobot.event.EventBus;
import de.velcommuta.denul.R;
import de.velcommuta.denul.db.StepLoggingContract;
import de.velcommuta.denul.db.VaultContract;
import de.velcommuta.denul.event.DatabaseAvailabilityEvent;
import de.velcommuta.denul.crypto.FileOperation;
import de.velcommuta.denul.crypto.Hybrid;
import de.velcommuta.denul.crypto.RSA;

/**
 * Pedometer service for step counting using the built-in pedometer, if available
 */
public class PedometerService extends Service implements SensorEventListener, ServiceConnection {
    public static final String TAG = "PedometerService";

    private SensorManager mSensorManager;
    private PublicKey mPubkey;
    private int mSeqNr;
    private EventBus mEventBus;
    private ShutdownReceiver mShutdownReceiver;

    private boolean mDatabaseAvailable = false;
    private DatabaseServiceBinder mDatabaseBinder = null;

    private Hashtable<DateTime, Long> mCache;
    private Hashtable<DateTime, Long> mToday;
    // TODO Think about replacing this with a Hashtable<DateTime, Int>

    // Daily sum of steps
    private int mTodaySum = 0;

    private long mStartTimeWall = System.currentTimeMillis();
    private long mStartTimeSystem = SystemClock.elapsedRealtime();

    private Binder mBinder;
    private List<UpdateListener> mListeners;

    private String FORMAT_DATE = "dd/MM/yyyy";
    private String FORMAT_TIME = "HH:mm";

    ///// Service lifecycle management
    /**
     * Required (empty) constructor
     */
    public PedometerService() {
    }


    @SuppressWarnings("ResultOfMethodCallIgnored")
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (mSensorManager == null) {
            // Called when the service is started
            // Check if a Step count sensor even exists
            if (!(getPackageManager().hasSystemFeature(PackageManager.FEATURE_SENSOR_STEP_COUNTER))) {
                // No step counter sensor available :(
                Log.e(TAG, "onStartCommand: No step count sensor available, stopping service");
                stopSelf();
                return Service.START_STICKY;
            }
            File oldSessionCache = new File(getFilesDir(), "pedometer-session.cache");
            if (oldSessionCache.exists()) {
                Log.i(TAG, "onStartCommand: Found orphaned session cache");
                File crashedSessionCache = new File(getFilesDir(), "pedometer-session-crash.cache");
                if (!crashedSessionCache.exists()) {
                    oldSessionCache.renameTo(crashedSessionCache);
                } else {
                    Log.w(TAG, "onStartCommand: Loosing old session cache");
                    FileOperation.secureDelete(crashedSessionCache);
                    oldSessionCache.renameTo(crashedSessionCache);
                }
            }

            // Register with EventBus
            mEventBus = EventBus.getDefault();
            mEventBus.register(this);

            // Register shutdown receiver
            IntentFilter shutdownFilter = new IntentFilter(Intent.ACTION_SHUTDOWN);
            shutdownFilter.addAction(Intent.ACTION_REBOOT);
            mShutdownReceiver = new ShutdownReceiver();
            registerReceiver(mShutdownReceiver, shutdownFilter);

            /*
            Load the public key that is used to safely preserve service results if the database
            is not open. This could happen, for example, if the device shuts down: The database will
            be locked, but data will be lost unless it is preserved on disk. But since we don't want
            unencrypted data lying around, we have to encrypt it in some way.
            The corresponding private key is saved in the SQLite vault and only available if the
            database is unlocked
            */
            mPubkey = loadPubkey();
            if (mPubkey == null) {
                Log.e(TAG, "onStartCommand: Pubkey loading failed, aborting");
                stopSelf();
                return Service.START_STICKY;
            }
            Log.d(TAG, "onStartCommand: Successfully loaded Pubkey");
            // Load sequence number
            mSeqNr = getSharedPreferences(getString(R.string.preferences_keystore), Context.MODE_PRIVATE).getInt(getString(R.string.preferences_keystore_seqnr), -1);
            if (mSeqNr == -1) {
                Log.e(TAG, "onStartCommand: Something went wrong while loading the sequence number. Restarting at zero"); // TODO Is this a good idea?
                mSeqNr = 0;
            }

            // initialize data structure
            mCache = new Hashtable<>();
            mToday = new Hashtable<>();
            mListeners = new LinkedList<>();

            // Set up the pedometer
            // Get Sensor Manager
            mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
            // Get Sensor
            Sensor stepCountSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER);
            Log.d(TAG, "run: Registering with step count sensor");
            // Register us as a listener for that sensor
            mSensorManager.registerListener(this, stepCountSensor, SensorManager.SENSOR_DELAY_NORMAL);
        }
        return Service.START_STICKY;
    }


    @Override
    public void onDestroy() {
        mSensorManager.unregisterListener(this);
        mEventBus.unregister(this);
        if (mDatabaseBinder != null) {
            saveToDatabase();
            unbindFromDatabase();
        }
        saveState();
        unregisterReceiver(mShutdownReceiver);
        Log.d(TAG, "onDestroy: Shutting down");
    }


    @Override
    public IBinder onBind(Intent intent) {
        if (!isRunning(this)) {
            Log.e(TAG, "onBind: Service bound but not started before, aborting");
            return null;
        }
        if (mBinder == null) {
            mBinder = new MyPedometerBinder();
        }
        if (DatabaseService.isRunning(this)) {
            // Save stuff to the database
            requestDatabaseBinder();
        }
        saveCache();
        return mBinder;
    }


    @Override
    public void onRebind(Intent intent) {
        if (DatabaseService.isRunning(this)) {
            // Save stuff to the database
            requestDatabaseBinder();
        }
        saveCache();
    }


    @Override
    public boolean onUnbind(Intent intent) {
        if (DatabaseService.isRunning(this)) {
            // Save stuff to the database
            requestDatabaseBinder();
        }
        saveCache();
        return true;
    }


    ///// Sensor Callbacks
    @Override
    public void onSensorChanged(SensorEvent event) {
        DateTime timestamp = getTimestamp();
        // We are saving step counts per hour. Thus, the exact value of the event is not interesting
        // to us. Instead, we use the fact that we will get one event per step, and can thus simply
        // increment the step counter, disregarding the actual value of the event
        Long cvalue = mCache.get(timestamp);
        if (cvalue != null) {
            mCache.put(timestamp, cvalue + (long) 1);
            mToday.put(timestamp, cvalue + (long) 1);
            if (cvalue % 100 == 0) {
                // Save the current step count every 100 steps
                saveCache();
            }
        } else {
            if (timestamp.getHourOfDay() == 0) {
                // We have rolled over to a new day, reset the "today" cache
                mToday.clear();
                mTodaySum = 0;
            }
            // We have just rolled over to a new hour
            mCache.put(timestamp, (long) 1);
            mToday.put(timestamp, (long) 1);
            if (DatabaseService.isRunning(this)) {
                // If the database is currently available, this is a good time to save our state to it
                // Request a database binder, which will kick off the process of saving to the database
                requestDatabaseBinder();
            }
            saveCache();
        }
        // Update daily sum
        mTodaySum += 1;
        notifyListeners();
    }


    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {
        // We don't really care about this, as it will never happen for a step counter
    }


    /**
     * Callback for EventBus to deliver DatabaseAvailabilityEvents
     * @param ev the Event
     */
    public void onEvent(DatabaseAvailabilityEvent ev) {
        if (ev.getStatus() == DatabaseAvailabilityEvent.STARTED) {
            Log.d(TAG, "onEvent(DatabaseAvailabilityEvent): Service STARTED");
            requestDatabaseBinder();
        } else if (ev.getStatus() == DatabaseAvailabilityEvent.OPENED) {
            Log.d(TAG, "onEvent(DatabaseAvailabilityEvent): DB OPENED");
            if (mDatabaseBinder == null) {
                Log.w(TAG, "onEvent(DatabaseAvailabilityEvent): DB binder not yet received. Defer processing");
                return;
            }
            loadSavedState();
            mDatabaseAvailable = true;
        } else if (ev.getStatus() == DatabaseAvailabilityEvent.CLOSED) {
            Log.d(TAG, "onEvent(DatabaseAvailabilityEvent): DB CLOSED");
            mDatabaseAvailable = false;
        } else if (ev.getStatus() == DatabaseAvailabilityEvent.STOPPED) {
            Log.d(TAG, "onEvent(DatabaseAvailabilityEvent): Service STOPPED");
            mDatabaseAvailable = false;
            mDatabaseBinder = null;
        }
    }


    /**
     * Request a binder to the Database Service
     */
    private void requestDatabaseBinder() {
        if (DatabaseService.isRunning(this)) {
            Log.d(TAG, "requestDatabaseBinder: Requesting binding");
            Intent intent = new Intent(this, DatabaseService.class);
            bindService(intent, this, 0);
        } else {
            Log.w(TAG, "requestDatabaseBinder: Database not opened");
        }
    }


    /**
     * Unbind from the database
     */
    private void unbindFromDatabase() {
        Log.i(TAG, "unbindFromDatabase: Unbinding");
        unbindService(this);
        mDatabaseBinder = null;
        mDatabaseAvailable = false;
    }


    @Override
    public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
        Log.d(TAG, "onServiceConnected: Service connection received");
        mDatabaseBinder = (DatabaseServiceBinder) iBinder;
        // Check if the database has been opened in the meantime, and if yes, notify the EventHandler
        DatabaseAvailabilityEvent ev = EventBus.getDefault().getStickyEvent(DatabaseAvailabilityEvent.class);
        if (ev.getStatus() == DatabaseAvailabilityEvent.OPENED) {
            onEvent(ev);
        }
    }


    @Override
    public void onServiceDisconnected(ComponentName componentName) {
        Log.w(TAG, "onServiceDisconnected: Service connection lost");
        mDatabaseBinder = null;
        mDatabaseAvailable = false;
    }


    ///// Utility functions
    /**
     * Get the current timestamp, in the format used in the Hashtable
     * @return The current timestamp
     */
    private DateTime getTimestamp() {
        return DateTime.now( DateTimeZone.getDefault() ).withMillisOfSecond(0).withSecondOfMinute(0).withMinuteOfHour(0);
    }


    /**
     * Recalculate todays sum total of steps
     */
    private void updateTodaySum() {
        Log.d(TAG, "updateTodaySum: called");
        // Update the cache for todays events
        mTodaySum = 0;
        // Get the current day as a string format for later comparison
        for (DateTime dt : mToday.keySet()) {
            mTodaySum += mToday.get(dt);
        }
    }


    /**
     * Helper function for the state saving functions. This function serializes and encrypts the state
     * @return The encrypted state, as byte[]
     */
    private byte[] prepareCipheredState() {
        // Serialize the state
        byte[] state = serializeToByteArray(mCache);
        if (state == null) {
            Log.e(TAG, "prepareCipheredState: Something went wrong during serialization, aborting");
            return null;
        }
        // Add a bunch of timestamps to allow us to later reconstruct when reboots happened and when
        // the service was restarted for other reasons, which will be important for cache
        // reintegration, since the pedometer resets on reboots
        // We are saving:
        // - The system clock (milliseconds since reboot) and wall time (milliseconds since epoch)
        //   at the time of the service start
        // - The system clock and wall time at the time of the saveState()-call
        byte[] uptime = ByteBuffer.allocate(32)
                .putLong(mStartTimeSystem)
                .putLong(mStartTimeWall)
                .putLong(SystemClock.elapsedRealtime())
                .putLong(System.currentTimeMillis())
                .array();
        // Combine all of them into a byte[]
        byte[] plaintext = new byte[uptime.length + state.length];
        System.arraycopy(uptime, 0, plaintext, 0, uptime.length);
        System.arraycopy(state, 0, plaintext, uptime.length, state.length);
        // Encrypt the byte[]
        byte[] cipheredState = Hybrid.encryptHybrid(plaintext, mPubkey, mSeqNr);
        if (cipheredState == null) {
            Log.e(TAG, "prepareCipheredState: Something went wrong during state encryption, aborting");
            return null;
        }
        return cipheredState;
    }


    /**
     * Write the provided encrypted state to the persistent cache file (pedometer,cache or pedometer-n.cache, with n => 1)
     * @param cipheredState The state
     * @return True if successful, false otherwise
     */
    private boolean writeToPersistentCache(byte[] cipheredState) {
        // Write to file, taking care not to overwrite existing state
        // Detect first file name that is not already taken
        int i = 0;
        java.io.File file = new java.io.File(getFilesDir(), "pedometer.cache");
        while (file.exists()) {
            i += 1;
            file = new java.io.File(getFilesDir(), "pedometer-" + i + ".cache");
        }
        // Write to file
        try {
            FileOutputStream fos = new FileOutputStream(file);
            fos.write(cipheredState);
            fos.close();
        } catch (IOException e) {
            Log.e(TAG, "writeToPersistentCache: Encountered IOException during write, aborting.", e);
            return false;
        }
        Log.d(TAG, "writeToPersistentCache: Successfully wrote state to file");
        return true;
    }


    /**
     * Write the provided encrypted state to the session cache file (pedometer-session,cache)
     * @param cipheredState The state
     * @return True if successful, false otherwise
     */
    private boolean writeToSessionCache(byte[] cipheredState) {
        File file = new File(getFilesDir(), "pedometer-session.cache");
        if (file.exists()) {
            FileOperation.secureDelete(file);
        }
        // Write to file
        try {
            FileOutputStream fos = new FileOutputStream(file);
            fos.write(cipheredState);
            fos.close();
        } catch (IOException e) {
            Log.e(TAG, "writeToSessionCache: Encountered IOException during write, aborting.", e);
            return false;
        }
        Log.d(TAG, "writeToSessionCache: Successfully wrote state to file");
        return true;
    }


    /**
     * Save the state of the service into an encrypted file
     */
    private void saveState() {
        byte[] cipheredState = prepareCipheredState();
        if (cipheredState == null) {
            Log.e(TAG, "saveState: PrepareCipheredState failed, aborting");
            return;
        }
        // Write to persistent cache
        if (writeToPersistentCache(cipheredState)) {

            // Increment sequence number
            SharedPreferences.Editor edit = getSharedPreferences(getString(R.string.preferences_keystore), Context.MODE_PRIVATE).edit();
            edit.putInt(getString(R.string.preferences_keystore_seqnr), mSeqNr + 1);
            edit.apply();
            Log.d(TAG, "saveState: Incremented Sequence number");
        } else {
            Log.e(TAG, "saveState: Saving to file failed, aborting");
        }
        File file = new File(getFilesDir(), "pedometer-session.cache");
        if (file.exists()) {
            FileOperation.secureDelete(file);
            Log.d(TAG, "saveState: Deleted Session Cache");
        }
    }


    /**
     * Save the state of the service into a temporary encrypted file
     */
    private void saveCache() {
        byte[] cipheredState = prepareCipheredState();
        if (cipheredState == null) {
            Log.e(TAG, "saveCache: prepareCipheredState failed, aborting");
            return;
        }
        if (writeToSessionCache(cipheredState)) {
            Log.d(TAG, "saveCache: Success");
        } else {
            Log.e(TAG, "saveCache: An error occured while saving to disk");
        }
    }


    /**
     * Load the saved state from Disk
     */
    public void loadSavedState() {
        if (!mDatabaseBinder.isDatabaseOpen()) {
            Log.e(TAG, "loadSavedState: Database is NOT open, aborting");
            return;
        }
        new CacheReintegrationTask().execute();
    }


    /**
     * Save the current data into the database and, if successful, remove it from the memory of the
     * service
     */
    private void saveToDatabase() {
        // TODO Compartmentalize into functions
        if (mDatabaseBinder == null | !mDatabaseAvailable) {
            Log.e(TAG, "saveToDatabase: Database unavailable");
            return;
        }
        DateTime currentTimestamp = getTimestamp();
        mDatabaseBinder.beginTransaction();
        for (DateTime ts : mCache.keySet()) {
            int c = isInDatabase(ts);
            if (c != -1) {
                if (c < mCache.get(ts)) {
                    ContentValues update = new ContentValues();
                    update.put(StepLoggingContract.StepCountLog.COLUMN_VALUE, mCache.get(ts));
                    String selection = StepLoggingContract.StepCountLog.COLUMN_DATE + " LIKE ? AND "
                            + StepLoggingContract.StepCountLog.COLUMN_TIME + " LIKE ?";
                    String[] selectionArgs = {formatDate(ts), formatTime(ts)};
                    mDatabaseBinder.update(StepLoggingContract.StepCountLog.TABLE_NAME, update, selection, selectionArgs);
                }
            } else {
                Log.d(TAG, "saveToDatabase: Inserting value");
                ContentValues entry = new ContentValues();
                entry.put(StepLoggingContract.StepCountLog.COLUMN_DATE, formatDate(ts));
                entry.put(StepLoggingContract.StepCountLog.COLUMN_TIME, formatTime(ts));
                entry.put(StepLoggingContract.StepCountLog.COLUMN_VALUE, mCache.get(ts));
                mDatabaseBinder.insert(StepLoggingContract.StepCountLog.TABLE_NAME, null, entry);
            }
        }
        Log.d(TAG, "saveToDatabase: All values saved, committing");
        mDatabaseBinder.commit();
        Log.d(TAG, "saveToDatabase: Removing saved values");
        for (DateTime ts : mCache.keySet()) {
            if (!ts.equals(currentTimestamp)) {
                mCache.remove(ts);
            }
        }

        // Load todays sum
        loadHistoryToday();
        // Unbind from database
        unbindFromDatabase();
    }


    /**
     * Notify all registered listeners that new data is available
     */
    private void notifyListeners() {
        for (UpdateListener l : mListeners) {
            l.update();
        }
    }


    /**
     * Format a given DateTime for the date column of the database
     * @param dt The DateTime object
     * @return The String representation of the day
     */
    private String formatDate(DateTime dt) {
        return dt.toString(DateTimeFormat.forPattern(FORMAT_DATE));
    }


    /**
     * Format a given DateTime for the time column of the database
     * @param dt The DateTime Object
     * @return The String representation of the time
     */
    private String formatTime(DateTime dt) {
        return dt.toString(DateTimeFormat.forPattern(FORMAT_TIME));
    }


    /**
     * Check if a certain DateTime is already present in the database
     * @param dt DateTime
     * @return The saved step count for that key, if present, or -1 if no step count was found
     */
    private int isInDatabase(DateTime dt) {
        int rv = -1;
        Cursor c = mDatabaseBinder.query(StepLoggingContract.StepCountLog.TABLE_NAME,
                new String[]{StepLoggingContract.StepCountLog.COLUMN_VALUE},
                StepLoggingContract.StepCountLog.COLUMN_DATE + " = '" + formatDate(dt) + "' AND " +
                        StepLoggingContract.StepCountLog.COLUMN_TIME + " = '" + formatTime(dt) + "'",
                null,
                null,
                null,
                null);
        if (c.getCount() != 0) {
            c.moveToFirst();
            rv = c.getInt(c.getColumnIndexOrThrow(StepLoggingContract.StepCountLog.COLUMN_VALUE));
        }
        c.close();
        return rv;
    }

    /**
     * Load the public key from the shared Preferences
     * @return The PublicKey object from the shared preferences
     */
    private PublicKey loadPubkey() {
        // Retrieve encoded pubkey from the SharedPreferences
        String pubkey = getSharedPreferences(getString(R.string.preferences_keystore), Context.MODE_PRIVATE).getString(getString(R.string.preferences_keystore_rsapub), null);
        // Check if we actually got a pubkey
        if (pubkey == null) return null;
        // Decode the encoded pubkey into an actual pubkey object
        return RSA.decodePublicKey(pubkey);
    }


    /**
     * Retrieve the secret key of the pedometer service from the database and return it
     * @return The PrivateKey, if available. Otherwise, null
     */
    private PrivateKey loadPrivateKey() {
        if (mDatabaseBinder == null || !mDatabaseAvailable) {
            Log.e(TAG, "loadSecretKey: Database unavailable");
            return null;
        }
        // Query the database for the private key
        Cursor c = mDatabaseBinder.query(VaultContract.KeyStore.TABLE_NAME,
                new String[] {VaultContract.KeyStore.COLUMN_KEY_BYTES},
                VaultContract.KeyStore.COLUMN_KEY_NAME + " = '" + VaultContract.KeyStore.NAME_PEDOMETER_PRIVATE + "'",
                null,
                null,
                null,
                null);
        if (c.getCount() == 0) {
            Log.e(TAG, "loadSecretKey: no secret key found, aborting");
            return null;
        }
        // Retrieve the encoded string
        c.moveToFirst();
        String encoded = c.getString(
                c.getColumnIndexOrThrow(VaultContract.KeyStore.COLUMN_KEY_BYTES)
        );
        // Close the cursor
        c.close();
        // Decode and return the PrivateKey
        PrivateKey pk = RSA.decodePrivateKey(encoded);
        try {
            if (!Arrays.equals(RSA.decryptRSA(RSA.encryptRSA(new byte[]{0x00}, mPubkey), pk), new byte[] {0x00})) {
                Log.e(TAG, "loadPrivateKey: Verification failed");
                return null;
            } else {
                Log.d(TAG, "loadPrivateKey: Verification successful");
                return pk;
            }
        } catch (IllegalBlockSizeException e) {
            Log.e(TAG, "loadPrivateKey: IllegalBlockSizeException while verifying keypair", e);
            return null;
        } catch (BadPaddingException e) {
            Log.e(TAG, "loadPrivateKey: BadPaddingException while verifying keypair", e);
            return null;
        }
    }


    /**
     * Load the highest seen sequence number from the database
     * @return The sequence number, or -1 if an error occured
     */
    private int getMaxSequenceNumber() {
        if (mDatabaseBinder == null || !mDatabaseAvailable) {
            Log.e(TAG, "getMaxSequenceNumber: Database unavailable");
            return -1;
        }
        Cursor c = mDatabaseBinder.query(VaultContract.SequenceNumberStore.TABLE_NAME,
                new String[]{VaultContract.SequenceNumberStore.COLUMN_SNR_VALUE},
                VaultContract.SequenceNumberStore.COLUMN_SNR_TYPE + " LIKE ?",
                new String[]{VaultContract.SequenceNumberStore.TYPE_PEDOMETER},
                null,
                null,
                null);
        if (c.getCount() == 0) {
            c.close();
            return -1;
        } else {
            c.moveToFirst();
            int res = c.getInt(c.getColumnIndexOrThrow(VaultContract.SequenceNumberStore.COLUMN_SNR_VALUE));
            c.close();
            return res;
        }
    }


    /**
     * Load todays sum of steps
     */
    private void loadHistoryToday() {
        if (!mDatabaseAvailable || mDatabaseBinder == null) {
            Log.e(TAG, "loadHistoryToday: Database unavailable");
            return;
        }
        String date = formatDate(getTimestamp());
        Cursor c = mDatabaseBinder.query(StepLoggingContract.StepCountLog.TABLE_NAME,
                new String[] {StepLoggingContract.StepCountLog.COLUMN_TIME, StepLoggingContract.StepCountLog.COLUMN_VALUE},
                StepLoggingContract.StepCountLog.COLUMN_DATE + " LIKE ?",
                new String[] {date},
                null, // groupby
                null, // having
                null); // orderby
        if (c.getCount() > 0) {
            while (c.moveToNext()) {
                long value = c.getInt(c.getColumnIndexOrThrow(StepLoggingContract.StepCountLog.COLUMN_VALUE));
                String time = c.getString(c.getColumnIndexOrThrow(StepLoggingContract.StepCountLog.COLUMN_TIME));
                DateTime timestamp = DateTime.parse(date + " " + time, DateTimeFormat.forPattern(FORMAT_DATE + " " + FORMAT_TIME));
                if (!mToday.contains(timestamp) || mToday.get(timestamp) < value) {
                    mToday.put(timestamp, value);
                }
            }
            Log.d(TAG, "loadHistoryToday: Got a result");
        } else {
            Log.d(TAG, "loadHistoryToday: Nothing in the database for today");
        }
        c.close();
        updateTodaySum();
        notifyListeners();
    }

    /**
     * Store an updated maximum seen sequence number
     * @param seqnr The sequence number
     */
    private void storeSequenceNumber(int seqnr) {
        if (!mDatabaseAvailable || mDatabaseBinder == null) {
            Log.e(TAG, "storeSequenceNumber: Database unavailable");
        }
        int cSeqNr = getMaxSequenceNumber();
        if (cSeqNr == -1) {
            Log.d(TAG, "storeSequenceNumber: Inserting new value");
            ContentValues insert = new ContentValues();
            insert.put(VaultContract.SequenceNumberStore.COLUMN_SNR_TYPE, VaultContract.SequenceNumberStore.TYPE_PEDOMETER);
            insert.put(VaultContract.SequenceNumberStore.COLUMN_SNR_VALUE, seqnr);
            mDatabaseBinder.insert(VaultContract.SequenceNumberStore.TABLE_NAME, null, insert);
        } else {
            Log.d(TAG, "storeSequenceNumber: Updating value");
            ContentValues values = new ContentValues();
            values.put(VaultContract.SequenceNumberStore.COLUMN_SNR_VALUE, seqnr);
            mDatabaseBinder.update(VaultContract.SequenceNumberStore.TABLE_NAME,
                    values,
                    VaultContract.SequenceNumberStore.COLUMN_SNR_TYPE + " LIKE ?",
                    new String[]{VaultContract.SequenceNumberStore.TYPE_PEDOMETER}
            );
        }
    }


    /**
     * Serialize the hashtable into a byte[]
     * @param ht The hashtable
     * @return The serialized hashtable, or null if serialization failed
     */
    private byte[] serializeToByteArray(Hashtable ht) {
        // Get ourselves set up for serialization
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream();
             ObjectOutput out = new ObjectOutputStream(bos)) {
            // Pass the object into the serialization pipeline
            out.writeObject(ht);
            // Return the result
            return bos.toByteArray();
        } catch (IOException e) {
            Log.e(TAG, "serializeToByteArray: An IOException occured, aborting");
            return null;
        }
    }


    /**
     * Deserialize an object from a byte[]
     * @param serialized The serialized object as a byte[]
     * @return The deserialized Hashtable<DateTime, Long>, or null if an error occured
     */
    @SuppressWarnings("unchecked")
    private Hashtable<DateTime, Long> deserializeFromByteArray(byte[] serialized) {
        // Set up deserialization chain
        try (ByteArrayInputStream bis = new ByteArrayInputStream(serialized);
             ObjectInput in = new ObjectInputStream(bis)) {
            // Deserialize the object and return it
            return (Hashtable<DateTime, Long>) in.readObject();
        } catch (IOException e) {
            Log.e(TAG, "deserializeFromByteArray: Encountered IOException, aborting");
            return null;
        } catch (ClassNotFoundException e) {
            Log.e(TAG, "deserializeFromByteArray: Encountered ClassNotFoundException, aborting");
            return null;
        }
    }


    /**
     * AsyncTask to load data from the encrypted cache in the background
     */
    private class CacheReintegrationTask extends AsyncTask<Void, Void, Hashtable<DateTime, Long>> {
        private static final String TAG = "CacheReintegrationTask";

        @Override
        protected Hashtable<DateTime, Long> doInBackground(Void... v) {
            // Create a copy of the current hashtable to perform work on
            Hashtable<DateTime, Long> result = null;
            // Load private key
            PrivateKey pk = loadPrivateKey();
            if (pk == null) {
                Log.e(TAG, "doInBackground: Private key retrieval failed, aborting");
                return null;
            }

            // Create variable to save number of files into
            int highestSeenFile;
            // Detect if session cache exists
            File file = new File(getFilesDir(), "pedometer-session-crash.cache");
            if (file.exists()) {
                // Read in the file
                byte[] filebytes = readFileRaw(file);
                if (filebytes == null) {
                    // Something went wrong while reading the file, abort
                    Log.e(TAG, "doInBackground: Error reading session cache, skipping");
                } else {
                    // File read successfully
                    try {
                        // Decrypt data from file
                        byte[] plaintext = Hybrid.decryptHybrid(filebytes, pk, mSeqNr);
                        if (plaintext != null && plaintext.length >= 32) {
                            // Plaintext seems sane, deserialize data
                            Hashtable<DateTime, Long> oldHashtable = deserializeFromByteArray(Arrays.copyOfRange(plaintext, 32, plaintext.length));
                            // Merge into result
                            result = new Hashtable<>(mCache);
                            result = merge(result, oldHashtable);
                            Log.i(TAG, "doInBackground: Successfully loaded Session cache file");
                        } else {
                            // Plaintext has a weird format, ignore it
                            Log.e(TAG, "doInBackground: Decryption produced nonsense, skipping session file");
                        }
                    } catch (BadPaddingException e) {
                        // Something went wrong during decryption, ignore
                        Log.e(TAG, "doInBackground: Bad Padding Exception, skipping session cache file");
                    }
                }
                // Delete file
                FileOperation.secureDelete(file);
            }

            // Detect if cache files exist
            file = new File(getFilesDir(), "pedometer.cache");
            int i = 0;
            if (file.exists()) {
                Log.d(TAG, "doInBackground: got pk");
                // Load the highest seen sequence number
                int seqnr = getMaxSequenceNumber();
                if (seqnr == -1) {
                    Log.w(TAG, "doInBackground: No sequence number found in database. Accepting all sequence numbers (saw " + seqnr + ")");
                }
                // Determine which files exist
                while (file.exists()) {
                    i += 1;
                    file = new java.io.File(getFilesDir(), "pedometer-" + i + ".cache");
                }
                Log.d(TAG, "doInBackground: Found " + i + " cache files");
                // Set some variables for later
                highestSeenFile = i-1;
                if (seqnr + (i-1) != (mSeqNr - 1)) {
                    Log.w(TAG, "doInBackground: Something's fishy, sequence numbers aren't adding up. Continuing for now (snr " + seqnr + ", mSnr: " + mSeqNr + ")");
                }
                // Get the current time (for reboot detection)
                long currentSystemWallTime = mStartTimeWall;
                // As the files are created in order, the oldest file is pedometer.cache, followed by pedometer-1.cache, and so on
                // We will be working backwards in time
                for (i -= 1; i >= 0; i--) {
                    Log.i(TAG, "doInBackground: Processing file " + i);

                    // Get file pointer
                    if (i > 0) {
                        file = new File(getFilesDir(), "pedometer-" + i + ".cache");
                    } else {
                        file = new File(getFilesDir(), "pedometer.cache");
                    }
                    byte[] filebytes = readFileRaw(file);
                    if (filebytes == null) {
                        Log.e(TAG, "doInBackground: Loading of file failed, skipping");
                        continue;
                    }

                    // filebytes now contains the bytes saved in the file. Let's decrypt them.
                    byte[] plaintext;
                    try {
                        if (seqnr != -1) {
                            plaintext = Hybrid.decryptHybrid(filebytes, pk, seqnr + i);
                        } else {
                            // No known good sequence number available, skip sequence number verification
                            plaintext = Hybrid.decryptHybrid(filebytes, pk, -1);
                        }
                    } catch (BadPaddingException e) {
                        Log.e(TAG, "doInBackground: BadPaddingException - Skipping file", e);
                        continue;
                    }
                    // Check if the decryption worked - result should have at least 32 bytes due to our header
                    if (plaintext == null || plaintext.length < 32) {
                        Log.e(TAG, "doInBackground: Decryption failed, skipping file");
                        continue;
                    }

                    // Load the headers
                    ByteBuffer buf = ByteBuffer.wrap(Arrays.copyOfRange(plaintext, 0, 32));
                    long startTimeSystem = buf.getLong();
                    long startTimeWall   = buf.getLong();
                    long stopTimeSystem  = buf.getLong();
                    long stopTimeWall    = buf.getLong();
                    // See if times check out
                    if (!timeDifferencesSane(startTimeSystem, startTimeWall, stopTimeSystem, stopTimeWall, currentSystemWallTime)) {
                        Log.e(TAG, "doInBackground: Time differences are not sane, skipping file");
                        continue;
                    }

                    // De-Serialize saved Hashtable
                    Hashtable<DateTime, Long> oldHashtable = deserializeFromByteArray(Arrays.copyOfRange(plaintext, 32, plaintext.length));
                    if (oldHashtable == null ) {
                        Log.e(TAG, "doInBackground: loaded hashtable is null, skipping");
                        currentSystemWallTime = startTimeWall;
                        continue;
                    }

                    // Merge the two Hashtables
                    if (result == null) {
                        result = new Hashtable<>(mCache);
                    }
                    result = merge(result, oldHashtable);
                    Log.i(TAG, "doInBackground: merge complete");

                    // At this point, we have successfully merged the two Hashtables into one
                    // Let's continue with the next one. For that, we need to update the current timestamp
                    // to the timestamp at the beginning of the older service start
                    currentSystemWallTime = startTimeWall;
                }
                // Delete all cache files
                for (i=highestSeenFile; i >= 0; i--) {
                    if (i==0) {
                        java.io.File f = new java.io.File(getFilesDir(), "pedometer.cache");
                        FileOperation.secureDelete(f);
                    } else {
                        java.io.File f = new java.io.File(getFilesDir(), "pedometer-" + i + ".cache");
                        FileOperation.secureDelete(f);
                    }
                }
                storeSequenceNumber(mSeqNr);
            } else {
                // If this statement is reached, no cache files have been detected. Return null
                Log.i(TAG, "doInBackground: No cache files found");
            }
            Log.i(TAG, "doInBackground: Finished, returning result");
            return result;
        }

        @Override
        protected void onPostExecute(Hashtable<DateTime, Long> ht) {
            if (ht == null) {
                Log.i(TAG, "onPostExecute: ht == null");
            } else {
                // Replace the cache Hashtable with our merged one
                mCache = ht;
                Log.d(TAG, "onPostExecute: Hashtable replaced");
            }
            saveToDatabase();
        }

        /**
         * Check if the time differences are sane
         * @param startTimeSystem Start time of the service in the cache file, as ms since boot
         * @param startTimeWall Start time of the service in the cache file, as ms since epoch
         * @param stopTimeSystem Stop time of the service in the cache file, as ms since boot
         * @param stopTimeWall Stop time of the service in the cache file, as ms since epoch
         * @param currentSystemWallTime Current system time, as ms since epoch
         * @return True if the values are sane, false otherwise
         */
        private boolean timeDifferencesSane(long startTimeSystem, long startTimeWall,
                                            long stopTimeSystem, long stopTimeWall,
                                            long currentSystemWallTime) {
            if (startTimeSystem > stopTimeSystem) {
                Log.e(TAG, "timeDifferencesSane: service stopped before it was started - relative");
                return false;
            } else if (startTimeWall > stopTimeWall) {
                Log.e(TAG, "timeDifferencesSane: service stopped before it was started - absolute");
                return false;
            } else if (stopTimeWall > currentSystemWallTime) {
                Log.e(TAG, "timeDifferencesSane: Service stop lies in the future");
                return false;
            }
            return true;
        }


        /**
         * Read the raw bytes from a file and return them
         * @param file The file to read
         * @return The raw bytes, or null if an error occured
         */
        private byte[] readFileRaw(File file) {
            byte[] filebytes = null;

            try {
                RandomAccessFile fobj = new RandomAccessFile(file, "r");
                filebytes = new byte[(int) fobj.length()];
                fobj.read(filebytes);
                fobj.close();
            }
            catch (IOException e) {
                Log.e(TAG, "readFileRaw: Error during read: ", e);
            }
            return filebytes;
        }


        /**
         * Merge two hashtables
         * @param base The base hashtable
         * @param newvalues The new hashtable
         * @return The merged hashtable
         */
        private Hashtable<DateTime, Long> merge(Hashtable<DateTime,Long> base, Hashtable<DateTime, Long> newvalues) {
            // Take the intersection of both sets
            Set<DateTime> intersect = new HashSet<>(base.keySet());
            Hashtable<DateTime, Long> result = new Hashtable<>();
            intersect.retainAll(newvalues.keySet());
            if (intersect.isEmpty()) {
                // If the intersection is empty, we can just add all elements from the old hashtable to the current one,
                // without fear of overwriting important data
                result.putAll(newvalues);
            } else {
                for (DateTime t : newvalues.keySet()) {
                    if (base.containsKey(t)) {
                        // There is already a value under this key, Add the step counts
                        long oldval = base.get(t);
                        oldval += newvalues.get(t);
                        result.put(t, oldval);
                    } else {
                        // No collision, transfer value
                        result.put(t, newvalues.get(t));
                    }
                }
            }
            return result;
        }
    }


    public class ShutdownReceiver extends BroadcastReceiver {
        private final String TAG = "ShutdownReceiver";

        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(Intent.ACTION_SHUTDOWN) || intent.getAction().equals(Intent.ACTION_REBOOT)) {
                Log.i(TAG, "onReceive: Got shutdown / reboot broadcast, stopping service");
                stopSelf();
            }
        }
    }


    /**
     * Private binder class
     */
    private class MyPedometerBinder extends Binder implements PedometerServiceBinder {

        /**
         * Get todays sum of steps
         * @return Todays sum of steps
         */
        @Override
        public int getSumToday() {
            return mTodaySum;
        }


        /**
         * Get todays event list
         * @return Todays event list
         */
        @Override
        public Hashtable<DateTime, Long> getToday() {
            return mToday;
        }


        @Override
        public void addUpdateListener(UpdateListener listener) {
            Log.d(TAG, "addUpdateListener: Update listener received");
            mListeners.add(listener);
        }


        @Override
        public void removeUpdateListener(UpdateListener listener) {
            if (mListeners.contains(listener)) {
                Log.d(TAG, "removeUpdateListener: Listener removed");
                mListeners.remove(listener);
            } else {
                Log.w(TAG, "removeUpdateListener: Listener was not registered, doing nothing");
            }
        }
    }


    /**
     * Check if an instance of this service is running
     * @param ctx Context of the calling activity
     * @return True if the service is running, false otherwise
     */
    public static boolean isRunning(Context ctx) {
        ActivityManager manager = (ActivityManager) ctx.getSystemService(Activity.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if ("de.velcommuta.denul.service.PedometerService".equals(service.service.getClassName())) {
                return true; // Package name matches, our service is running
            }
        }
        return false; // No matching package name found => Our service is not running
    }
}
