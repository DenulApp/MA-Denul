package de.velcommuta.denul.service;

import android.app.PendingIntent;
import android.app.Service;
import android.app.TaskStackBuilder;
import android.content.Intent;
import android.location.Location;
import android.os.Bundle;
import android.os.IBinder;
import android.os.SystemClock;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationListener;

import java.util.LinkedList;
import java.util.List;
import java.util.Observable;

import de.greenrobot.event.EventBus;
import de.velcommuta.denul.R;
import de.velcommuta.denul.event.GPSLocationEvent;
import de.velcommuta.denul.ui.MainActivity;

/**
 * Service to perform GPS tracking, using the Google Apps location API
 */
// TODO Rebuild this without threads, as the callbacks are called on the main thread anyway
public class GPSTrackingService extends Service {
    public static final String TAG = "GPSTrackingService";
    private Thread mRunningThread;


    /**
     * Required empty constructor
     */
    public GPSTrackingService() {
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // EventBus.getDefault().register(this);
        // Prepare persistent Notification
        NotificationCompat.Builder nBuilder = new NotificationCompat.Builder(this)
                .setSmallIcon(R.drawable.ic_running)
                .setContentTitle(getString(R.string.location_tracking_notify))
                .setContentText(getString(R.string.location_tracking_details, getString(R.string.app_name)));
        // Create intent for the application
        Intent appIntent = new Intent(this, MainActivity.class);
        appIntent.setAction(MainActivity.INTENT_GPS_TRACK);

        TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);
        stackBuilder.addParentStack(MainActivity.class);
        stackBuilder.addNextIntent(appIntent);
        PendingIntent resultIntent = stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);
        // Set the intent of the notification
        nBuilder.setContentIntent(resultIntent);
        // Start the persistent service
        startForeground(42, nBuilder.build());

        Runnable r = new GPSTrackingRunnable();

        mRunningThread = new Thread(r);
        mRunningThread.start();

        return Service.START_STICKY;
    }


    /**
     * Runnable to perform the GPS tracking in its own thread to free up the main thread.
     */
    private class GPSTrackingRunnable extends Observable implements
            Runnable,
            GoogleApiClient.ConnectionCallbacks,
            GoogleApiClient.OnConnectionFailedListener,
            LocationListener {
        // The code in this runnable is largely based on the LocationUpdates sample by Google
        // https://github.com/googlesamples/android-play-location

        protected GoogleApiClient mGoogleApiClient;
        protected LocationRequest mLocationRequest;

        // Data structure that will be used to store the LatLng values
        protected List<Location> mPoints = new LinkedList<>();


        // Request updates every 2 seconds
        public static final long UPDATE_INTERVAL_IN_MILLISECONDS = 2000;

        /**
         * The fastest rate for active location updates. Exact. Updates will never be more frequent
         * than this value.
         */
        public static final long FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS =
                UPDATE_INTERVAL_IN_MILLISECONDS / 2;

        private long mChronometerBase;
        /**
         * Build the Google API client that is used for location requests
         */
        private synchronized void buildGoogleApiClient() {
            Log.i(TAG, "Building GoogleApiClient");
            mGoogleApiClient = new GoogleApiClient.Builder(getBaseContext())
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .addApi(LocationServices.API)
                    .build();
            createLocationRequest();
        }

        /**
         * Sets up the location request. Android has two location request settings:
         * {@code ACCESS_COARSE_LOCATION} and {@code ACCESS_FINE_LOCATION}. These settings control
         * the accuracy of the current location. This sample uses ACCESS_FINE_LOCATION, as defined in
         * the AndroidManifest.xml.
         * <p/>
         * When the ACCESS_FINE_LOCATION setting is specified, combined with a fast update
         * interval (5 seconds), the Fused Location Provider API returns location updates that are
         * accurate to within a few feet.
         * <p/>
         * These settings are appropriate for mapping applications that show real-time location
         * updates.
         */
        protected void createLocationRequest() {
            mLocationRequest = new LocationRequest();

            // Sets the desired interval for active location updates. This interval is
            // inexact. You may not receive updates at all if no location sources are available, or
            // you may receive them slower than requested. You may also receive updates faster than
            // requested if other applications are requesting location at a faster interval.
            mLocationRequest.setInterval(UPDATE_INTERVAL_IN_MILLISECONDS);

            // Sets the fastest rate for active location updates. This interval is exact, and your
            // application will never receive updates faster than this value.
            mLocationRequest.setFastestInterval(FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS);

            mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        }

        /**
         * Requests location updates from the FusedLocationApi.
         */
        protected void startLocationUpdates() {
            // The final argument to {@code requestLocationUpdates()} is a LocationListener
            // (http://developer.android.com/reference/com/google/android/gms/location/LocationListener.html).
            LocationServices.FusedLocationApi.requestLocationUpdates(
                    mGoogleApiClient, mLocationRequest, this);
        }

        /**
         * Removes location updates from the FusedLocationApi.
         */
        protected void stopLocationUpdates() {
            // It is a good practice to remove location requests when the activity is in a paused or
            // stopped state. Doing so helps battery performance and is especially
            // recommended in applications that request frequent location updates.

            // The final argument to {@code requestLocationUpdates()} is a LocationListener
            // (http://developer.android.com/reference/com/google/android/gms/location/LocationListener.html).
            LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, this);
        }

        @Override
        /**
         * Main loop of the thread. Sets up everything and then loops, waiting for a kill signal
         */
        public void run() {
            Log.d(TAG, "Runnable: Thread running");

            buildGoogleApiClient();
            mGoogleApiClient.connect();

            // TODO Check if the loop can be avoided somehow - it's not very nice
            try {
                while (!Thread.currentThread().isInterrupted()) {
                    Thread.sleep(1000);
                }
            } catch (InterruptedException e) {
                // Do nothing
            }

            Log.i(TAG, "run: Received Interrupt, Thread stopping");
            stopLocationUpdates();
            mGoogleApiClient.disconnect();
            stopForeground(true);
            stopSelf();
        }

        @Override
        /**
         * Called when the Google API client has successfully connected
         */
        public void onConnected(Bundle bundle) {
            startLocationUpdates();
            Log.i(TAG, "onConnected: Connected to API");
            Location cLoc = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
            mChronometerBase = SystemClock.elapsedRealtime();
            addLocationAndNotify(cLoc);
        }

        @Override
        /**
         * Called when the Google API client was suspended for some reason.
         */
        public void onConnectionSuspended(int i) {
            Log.w(TAG, "onConnectionSuspended: Connection lost, attempting to reconnect");
            mGoogleApiClient.connect();
        }

        @Override
        /**
         * Called when the connection to the API client has failed
         */
        public void onConnectionFailed(ConnectionResult connectionResult) {
            Log.e(TAG, "onConnectionFailed: Connection to API failed. Quitting.");
            // TODO Notify main thread
            stopSelf();
        }

        @Override
        /**
         * Called when new location data is available
         */
        public void onLocationChanged(Location location) {
            addLocationAndNotify(location);
        }

        /**
         * Called to notify the main thread about the new location.
         * @param location The location that was just reported by the API
         */
        private void addLocationAndNotify(Location location) {
            Log.d(TAG, "addLocationAndNotify: Sending new location to UI Thread");
            mPoints.add(location);
            EventBus.getDefault().postSticky(new GPSLocationEvent(mPoints, mChronometerBase));
        }


    }
    @Override
    /**
     * Unused function required by interface (this service is not bindable)
     */
    public IBinder onBind(Intent intent) {
        // No binding necessary
        return null;
    }

    @Override
    /**
     * Called when the Service is torn down. Clean up everything
     */
    public void onDestroy() {
        mRunningThread.interrupt();
        Log.d(TAG, "onDestroy: Service stopped");
    }
}
