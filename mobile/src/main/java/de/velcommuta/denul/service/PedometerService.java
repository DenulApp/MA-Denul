package de.velcommuta.denul.service;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.IBinder;
import android.util.Log;

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
import java.security.PublicKey;
import java.util.Hashtable;

import de.velcommuta.denul.R;
import de.velcommuta.denul.util.Crypto;
/**
 * Pedometer service for step counting using the built-in pedometer, if available
 */
public class PedometerService extends Service implements SensorEventListener {
    public static final String TAG = "PedometerService";

    private SensorManager mSensorManager;
    private PublicKey mPubkey;
    private int mSeqNr;

    private Hashtable<DateTime, Long> mHistory;

    ///// Service lifecycle management
    /**
     * Required (empty) constructor
     */
    public PedometerService() {
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // Called when the service is started
        // Check if a Step count sensor even exists
        if (!(getPackageManager().hasSystemFeature(PackageManager.FEATURE_SENSOR_STEP_COUNTER))) {
            // No step counter sensor available :(
            Log.e(TAG, "onStartCommand: No step count sensor available, stopping service");
            stopSelf();
            return Service.START_STICKY;
        }

        // Load the public key that is used to safely preserve service results if the database
        // is not open. This could happen, for example, if the device shuts down: The database will
        // be locked, but data will be lost unless it is preserved on disk. But since we don't want
        // unencrypted data lying around, we have to encrypt it in some way.
        // The corresponding private key is saved in the SQLite vault and only available if the
        // database is unlocked
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


        // initialize data structure
        // TODO Deal with the situation when users reboot the device and the counter resets
        //      within the same hour, leading to old values being overwritten
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
}
