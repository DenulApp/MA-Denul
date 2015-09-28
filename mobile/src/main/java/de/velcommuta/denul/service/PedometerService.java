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
import android.util.Base64;
import android.util.Log;

import org.joda.time.DateTime;
import org.spongycastle.jce.provider.BouncyCastleProvider;

import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.Hashtable;

import de.velcommuta.denul.R;

/**
 * Pedometer service for step counting using the built-in pedometer, if available
 */
public class PedometerService extends Service implements SensorEventListener {
    public static final String TAG = "PedometerService";

    private SensorManager mSensorManager;
    private PublicKey mPubkey;

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
            Log.e(TAG, "onStartCommand: Pubkey is null, aborting");
            return Service.START_STICKY;
        }
        Log.d(TAG, "onStartCommand: Successfully loaded Pubkey");


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

        return Service.START_STICKY;
    }

    @Override
    public void onDestroy() {
        mSensorManager.unregisterListener(this);
        // TODO Save state?
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
     * Load the public key saved in the SharedPreferences
     * @return The public key from the SharedPreferences
     */
    private PublicKey loadPubkey() {
        // Retrieve encoded pubkey from the SharedPreferences
        String pubkey = getSharedPreferences(getString(R.string.preferences_keystore), Context.MODE_PRIVATE).getString(getString(R.string.preferences_keystore_rsapub), null);
        // Check if we actually got a pubkey
        if (pubkey == null) return null;
        // Decode the encoded pubkey into an actual pubkey object
        try {
            KeyFactory kFactory = KeyFactory.getInstance("RSA", new BouncyCastleProvider());
            byte[] encoded = Base64.decode(pubkey, Base64.NO_WRAP);
            X509EncodedKeySpec keySpec = new X509EncodedKeySpec(encoded);
            return kFactory.generatePublic(keySpec);
        } catch (Exception e) {
            Log.e(TAG, "loadPubkey: Encountered exception during key retrieval: ", e);
            e.printStackTrace();
            stopSelf();
            return null;
        }
    }
}
