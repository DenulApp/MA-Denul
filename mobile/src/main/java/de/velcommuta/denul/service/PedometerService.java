package de.velcommuta.denul.service;

import android.app.Service;
import android.content.ComponentName;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.AsyncTask;
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
import java.io.FileNotFoundException;
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
import java.util.Set;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;

import de.greenrobot.event.EventBus;
import de.velcommuta.denul.R;
import de.velcommuta.denul.db.StepLoggingContract;
import de.velcommuta.denul.db.VaultContract;
import de.velcommuta.denul.event.DatabaseAvailabilityEvent;
import de.velcommuta.denul.util.Crypto;

/**
 * Pedometer service for step counting using the built-in pedometer, if available
 */
public class PedometerService extends Service implements SensorEventListener, ServiceConnection {
    public static final String TAG = "PedometerService";

    private SensorManager mSensorManager;
    private PublicKey mPubkey;
    private int mSeqNr;
    private EventBus mEventBus;

    private boolean mDatabaseAvailable = false;
    private DatabaseServiceBinder mDatabaseBinder = null;

    private Hashtable<DateTime, Long> mHistory;
    // TODO Think about replacing this with a Hashtable<DateTime, Int>

    private long mStartTimeWall = System.currentTimeMillis();
    private long mStartTimeSystem = SystemClock.elapsedRealtime();

    ///// Service lifecycle management
    /**
     * Required (empty) constructor
     */
    public PedometerService() {
    }


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

            // Register with EventBus
            mEventBus = EventBus.getDefault();
            mEventBus.register(this);

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
            // Increment sequence number
            SharedPreferences.Editor edit = getSharedPreferences(getString(R.string.preferences_keystore), Context.MODE_PRIVATE).edit();
            edit.putInt(getString(R.string.preferences_keystore_seqnr), mSeqNr + 1);
            edit.apply();


            // initialize data structure
            mHistory = new Hashtable<>();

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
            unbindService(this);
        }
        saveState();
        Log.d(TAG, "onDestroy: Shutting down");
    }


    @Override
    public IBinder onBind(Intent intent) {
        // Service is not bindable
        return null;
    }


    ///// Sensor Callbacks
    @Override
    public void onSensorChanged(SensorEvent event) {
        DateTime timestamp = getTimestamp();
        // We are saving step counts per hour. Thus, the exact value of the event is not interesting
        // to us. Instead, we use the fact that we will get one event per step, and can thus simply
        // increment the step counter, disregarding the actual value of the event
        Long cvalue = mHistory.get(timestamp);
        if (cvalue != null) {
            mHistory.put(timestamp, cvalue + (long) 1);
        } else {
            // We have just rolled over to a new hour
            mHistory.put(timestamp, (long) 1);
            if (mDatabaseAvailable) {
                // If the database is currently available, this is a good time to save our state to it
                saveToDatabase();
            }
        }
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
    public void requestDatabaseBinder() {
        Log.d(TAG, "requestDatabaseBinder: Requesting binding");
        Intent intent = new Intent(this, DatabaseService.class);
        bindService(intent, this, 0);
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
     * Save the state of the service into an encrypted file
     */
    private void saveState() {
        // Serialize the state
        byte[] state = serializeToByteArray(mHistory);
        if (state == null) {
            Log.e(TAG, "saveState: Something went wrong during serialization, aborting");
            return;
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
        byte[] cipheredState = Crypto.encryptHybrid(plaintext, mPubkey, mSeqNr);
        if (cipheredState == null) {
            Log.e(TAG, "saveState: Something went wrong during state encryption, aborting");
            return;
        }
        // Write to file, taking care not to overwrite existing state
        int i = 0;
        File file = new File(getFilesDir(), "pedometer.cache");
        while (file.exists()) {
            i += 1;
            file = new File(getFilesDir(), "pedometer-" + i + ".cache");
        }
        try {
            FileOutputStream fos = new FileOutputStream(file);
            fos.write(cipheredState);
            fos.close();
        } catch (IOException e) {
            Log.e(TAG, "saveState: Encountered IOException during write, aborting.", e);
            return;
        }
        Log.d(TAG, "saveState: Successfully wrote state to file");
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
        for (DateTime ts : mHistory.keySet()) {
            int c = isInDatabase(ts);
            if (c != -1) {
                if (c < mHistory.get(ts)) {
                    ContentValues update = new ContentValues();
                    update.put(StepLoggingContract.StepCountLog.COLUMN_VALUE, mHistory.get(ts));
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
                entry.put(StepLoggingContract.StepCountLog.COLUMN_VALUE, mHistory.get(ts));
                mDatabaseBinder.insert(StepLoggingContract.StepCountLog.TABLE_NAME, null, entry);
            }
        }
        Log.d(TAG, "saveToDatabase: All values saved, committing");
        mDatabaseBinder.commit();
        Log.d(TAG, "saveToDatabase: Removing saved values");
        for (DateTime ts : mHistory.keySet()) {
            if (!ts.equals(currentTimestamp)) {
                mHistory.remove(ts);
            }
        }
    }


    /**
     * Format a given DateTime for the date column of the database
     * @param dt The DateTime object
     * @return The String representation of the day
     */
    private String formatDate(DateTime dt) {
        return dt.toString(DateTimeFormat.forPattern("dd/MM/yyyy"));
    }


    /**
     * Format a given DateTime for the time column of the database
     * @param dt The DateTime Object
     * @return The String representation of the time
     */
    private String formatTime(DateTime dt) {
        return dt.toString(DateTimeFormat.forPattern("HH:mm"));
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
        return Crypto.decodePublicKey(pubkey);
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
        PrivateKey pk = Crypto.decodePrivateKey(encoded);
        try {
            if (!Arrays.equals(Crypto.decryptRSA(Crypto.encryptRSA(new byte[] {0x00}, mPubkey), pk), new byte[] {0x00})) {
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


    /**
     * AsyncTask to load data from the encrypted cache in the background
     */
    private class CacheReintegrationTask extends AsyncTask<Void, Void, Hashtable<DateTime, Long>> {
        private static final String TAG = "CacheReintegrationTask";

        @Override
        protected Hashtable<DateTime, Long> doInBackground(Void... v) {
            // Create a copy of the current hashtable to perform work on
            Hashtable<DateTime, Long> result = new Hashtable<>(mHistory);
            // Create variable to save number of files into
            int highestSeenFile;
            // Detect if cache files exist
            File file = new File(getFilesDir(), "pedometer.cache");
            int i = 0;
            if (file.exists()) {
                // We have at least one cache file, load the private key from the database
                PrivateKey pk = loadPrivateKey();
                if (pk == null) {
                    Log.e(TAG, "doInBackground: Private key retrieval failed, aborting");
                    return null;
                }
                Log.d(TAG, "doInBackground: got pk");
                // Determine which files exist
                while (file.exists()) {
                    i += 1;
                    file = new File(getFilesDir(), "pedometer-" + i + ".cache");
                }
                Log.d(TAG, "doInBackground: Found " + i + " cache files");
                highestSeenFile = i-1;
                // Get the current time (for reboot detection)
                long currentSystemWallTime = mStartTimeWall;
                long currentSystemUptime   = mStartTimeSystem;
                // As the files are created in order, the oldest file is pedometer.cache, followed by pedometer-1.cache, and so on
                // We will be working backwards in time
                if (i > 0) {
                    for (i -= 1; i >= 0; i--) {
                        Log.i(TAG, "doInBackground: Processing file " + i);
                        byte[] filebytes;
                        try {
                            // Get file pointer
                            if (i > 0) {
                                file = new File(getFilesDir(), "pedometer-" + i + ".cache");
                            } else {
                                file = new File(getFilesDir(), "pedometer.cache");
                            }
                            RandomAccessFile fobj = new RandomAccessFile(file, "r");
                            filebytes = new byte[(int) fobj.length()];
                            fobj.read(filebytes);
                            fobj.close();
                        } catch (FileNotFoundException e) {
                            Log.e(TAG, "doInBackground: File not found - wtf", e);
                            continue;
                        } catch (IOException e) {
                            Log.e(TAG, "doInBackground: IOException: ", e);
                            continue;
                        }

                        // filebytes now contains the bytes saved in the file. Let's decrypt them.
                        byte[] plaintext;
                        try {
                            plaintext = Crypto.decryptHybrid(filebytes, pk, -1); // TODO Implement sequence number verification
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
                        if (!timeDifferencesSane(startTimeSystem, startTimeWall, stopTimeSystem, stopTimeWall, currentSystemUptime, currentSystemWallTime)) {
                            Log.e(TAG, "doInBackground: Time differences are not sane, skipping file");
                            continue;
                        }

                        // De-Serialize saved Hashtable
                        Hashtable<DateTime, Long> oldHashtable = deserializeFromByteArray(Arrays.copyOfRange(plaintext, 32, plaintext.length));
                        if (oldHashtable == null ) {
                            Log.e(TAG, "doInBackground: loaded hashtable is null, skipping");
                            currentSystemUptime = startTimeSystem;
                            currentSystemWallTime = startTimeWall;
                            continue;
                        }

                        // Merge the two Hashtables
                        // Take the intersection of both sets
                        Set<DateTime> intersect = new HashSet<>(result.keySet());
                        intersect.retainAll(oldHashtable.keySet());
                        if (intersect.isEmpty()) {
                            // If the intersection is empty, we can just add all elements from the old hashtable to the current one,
                            // without fear of overwriting important data
                            result.putAll(oldHashtable);
                        } else {
                            for (DateTime t : oldHashtable.keySet()) {
                                if (result.containsKey(t)) {
                                    // There is already a value under this key, Add the step counts
                                    long oldval = result.get(t);
                                    oldval += oldHashtable.get(t);
                                    result.put(t, oldval);
                                } else {
                                    // No collision, transfer value
                                    result.put(t, oldHashtable.get(t));
                                }
                            }
                        }
                        Log.i(TAG, "doInBackground: merge complete");

                        // At this point, we have successfully merged the two Hashtables into one
                        // Let's continue with the next one. For that, we need to update the current timestamp
                        // to the timestamp at the beginning of the older service start
                        currentSystemUptime = startTimeSystem;
                        currentSystemWallTime = startTimeWall;
                    }
                    // Delete all cache files
                    for (i=highestSeenFile; i >= 0; i--) {
                        if (i==0) {
                            File f = new File(getFilesDir(), "pedometer.cache");
                            Crypto.secureDelete(f);
                        } else {
                            File f = new File(getFilesDir(), "pedometer-" + i + ".cache");
                            Crypto.secureDelete(f);
                        }
                    }
                }
            } else {
                // If this statement is reached, no cache files have been detected. Return null
                Log.i(TAG, "doInBackground: No cache files found");
                return null;
            }
            Log.i(TAG, "doInBackground: Finished, returning result");
            return result;
        }

        @Override
        protected void onPostExecute(Hashtable<DateTime, Long> ht) {
            if (ht == null) {
                Log.w(TAG, "onPostExecute: ht == null, aborting");
                return;
            }
            // Replace the cache Hashtable with our merged one
            mHistory = ht;
            Log.d(TAG, "onPostExecute: Hashtable replaced, saving to Database");
            saveToDatabase();
        }

        /**
         * Check if the time differences are sane
         * @param startTimeSystem Start time of the service in the cache file, as ms since boot
         * @param startTimeWall Start time of the service in the cache file, as ms since epoch
         * @param stopTimeSystem Stop time of the service in the cache file, as ms since boot
         * @param stopTimeWall Stop time of the service in the cache file, as ms since epoch
         * @param currentSystemUptime Current system time, as ms since boot
         * @param currentSystemWallTime Current system time, as ms since epoch
         * @return True if the values are sane, false otherwise
         */
        private boolean timeDifferencesSane(long startTimeSystem, long startTimeWall,
                                            long stopTimeSystem, long stopTimeWall,
                                            long currentSystemUptime, long currentSystemWallTime) {
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
    }
}
