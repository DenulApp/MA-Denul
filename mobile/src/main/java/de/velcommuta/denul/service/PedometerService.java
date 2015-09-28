package de.velcommuta.denul.service;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.IBinder;
import android.util.Log;

import com.facebook.android.crypto.keychain.SharedPrefsBackedKeyChain;
import com.facebook.crypto.Crypto;
import com.facebook.crypto.Entity;
import com.facebook.crypto.util.SystemNativeCryptoLibrary;

import org.joda.time.DateTime;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.util.Hashtable;

/**
 * Pedometer service for step counting using the built-in pedometer, if available
 */
public class PedometerService extends Service implements SensorEventListener {
    public static final String TAG = "PedometerService";

    private final String ENTITY = "pedometer-history";

    private SensorManager mSensorManager;

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


        // initialize data structure
        // TODO Deal with the situation when users reboot the device and the counter resets
        //      within the same hour, leading to old values being overwritten
        if (!loadState()) {
            mHistory = new Hashtable<>();
        }

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
     * Save the data of the service in case it is terminated while the database is closed
     */
    private void saveState() {
        Crypto crypto = new Crypto(
                new SharedPrefsBackedKeyChain(this),
                new SystemNativeCryptoLibrary());

        if (!crypto.isAvailable()) {
            Log.e(TAG, "saveState: Something went wrong while loading Conceal. Nothing we can do :(");
            return;
        }

        try {
            OutputStream fileStream = new BufferedOutputStream(openFileOutput("pedometer.cache", MODE_PRIVATE));
            OutputStream cipheringStream = crypto.getCipherOutputStream(
                    fileStream, new Entity(ENTITY)
            );
            ObjectOutputStream fos = new ObjectOutputStream(cipheringStream);
            fos.writeObject(mHistory);
            fos.close();
        } catch (Exception e) {
            Log.e(TAG, "saveState: Exception occured: ", e);
            e.printStackTrace();
        }
    }

    /**
     * Load state saved by saveState.
     * @return True if the state was successfully loaded, false otherwise
     */
    private boolean loadState() {
        File f = new File(getFilesDir() + "/pedometer.cache");
        if (!f.exists() || f.isDirectory()) {
            Log.d(TAG, "loadState: No saved state found.");
            return false;
        }

        Crypto crypto = new Crypto(
                new SharedPrefsBackedKeyChain(this),
                new SystemNativeCryptoLibrary());

        if (!crypto.isAvailable()) {
            Log.e(TAG, "loadState: Something went wrong while loading Conceal. Nothing we can do :(");
            return false;
        }

        try {
            InputStream decipheringStream = crypto.getCipherInputStream(openFileInput("pedometer.cache"), new Entity(ENTITY));
            BufferedInputStream bstream = new BufferedInputStream(decipheringStream);
            ObjectInputStream objIn = new ObjectInputStream(bstream);

            //noinspection unchecked
            mHistory = (Hashtable<DateTime, Long>) objIn.readObject();
            objIn.close();
            Log.d(TAG, "loadState: Successfully reloaded state from disk - got " + mHistory.size() + " entries.");
            //noinspection ResultOfMethodCallIgnored
            f.delete();
            return true;
        } catch (Exception e) {
            Log.e(TAG, "loadState: Exception occured: ", e);
            e.printStackTrace();
            return false;
        }
    }
}
