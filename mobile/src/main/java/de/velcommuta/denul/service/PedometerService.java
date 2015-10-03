package de.velcommuta.denul.service;

import android.app.Service;
import android.content.ComponentName;
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
import android.util.Log;

import net.sqlcipher.Cursor;

import org.joda.time.DateTime;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.Hashtable;

import de.greenrobot.event.EventBus;
import de.velcommuta.denul.R;
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

    ///// Service lifecycle management
    /**
     * Required (empty) constructor
     */
    public PedometerService() {
    }


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // TODO This may be called multiple times - ensure that this would not break stuff
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
        edit.putInt(getString(R.string.preferences_keystore_seqnr), mSeqNr+1);
        edit.apply();


        // initialize data structure
        // TODO Implement loading if database is available
        // TODO Deal with the situation when users reboot the device and the counter resets
        //      within the same hour, leading to old values being overwritten
        // TODO Deal with missing sequence number values in some way (caching seen values in the database?)
        // if (!loadState()) {
        //     mHistory = new Hashtable<>();
        // }
        mHistory = new Hashtable<>();

        // Set up the pedometer
        // Get Sensor Manager
        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        // Get Sensor
        Sensor stepCountSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER);
        Log.d(TAG, "run: Registering with step count sensor");
        // Register us as a listener for that sensor
        mSensorManager.registerListener(this, stepCountSensor, SensorManager.SENSOR_DELAY_NORMAL);

        return Service.START_STICKY;
    }


    @Override
    public void onDestroy() {
        mSensorManager.unregisterListener(this);
        saveState();
        mEventBus.unregister(this);
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
        DateTime timestamp = new DateTime().withMillis(0).withSecondOfMinute(0).withMinuteOfHour(0);
        mHistory.put(timestamp, (long) event.values[0]);
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
     * Save the state of the service into an encrypted file
     */
    private void saveState() {
        // Serialize the state
        byte[] state = serializeToByteArray(mHistory);
        if (state == null) {
            Log.e(TAG, "saveState: Something went wrong during serialization, aborting");
            return;
        }
        // Encrypt the state
        byte[] cipheredState = Crypto.encryptHybrid(state, mPubkey, mSeqNr);
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
        try (FileOutputStream fos = new FileOutputStream(file)) {
            fos.write(cipheredState);
        } catch (IOException e) {
            Log.e(TAG, "saveState: Encountered IOException during write, aborting.", e);
            return;
        }
        Log.d(TAG, "saveState: Successfully wrote state to file");
    }


    /**
     * Load the saved state from Disk
     * TODO Move to AsyncTask to avoid blocking main thread
     */
    public void loadSavedState() {
        if (!mDatabaseBinder.isDatabaseOpen()) {
            Log.e(TAG, "loadSavedState: Database is NOT open, aborting");
            return;
        }
        new CacheReintegrationTask().execute();
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
        return Crypto.decodePrivateKey(encoded);
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

        @Override
        protected Hashtable<DateTime, Long> doInBackground(Void... v) {
            PrivateKey pk = loadPrivateKey();
            if (pk == null) {
                Log.e(TAG, "CacheReintegrationTask:doInBackground: Private key retrieval failed, aborting");
                return null;
            }
            Log.d(TAG, "CacheReintegrationTask:doInBackground: got pk");
            // TODO implement retrieval, decoding, merge of caches
            return null;
        }

        @Override
        protected void onPostExecute(Hashtable<DateTime, Long> ht) {
            if (ht == null) {
                Log.e(TAG, "CacheReintegrationTask:onPostExecute: ht == null, aborting");
                return;
            }
            // TODO Implement merge into current cache and database save
        }
    }
}
