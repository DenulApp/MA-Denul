package de.velcommuta.denul.service;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import de.greenrobot.event.EventBus;

public class GPSTrackingService extends Service {
    public static final String TAG = "GPSTrackingService";
    private Thread mRunningThread;

    public GPSTrackingService() {
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // EventBus.getDefault().register(this);

        Runnable r = new Runnable() {
            @Override
            public void run() {
                Log.d(TAG, "Runnable: Thread running");
                // Register with EventBus
                // EventBus.getDefault().register(this);

                // TODO Do stuff


                // Thread is terminating, unregister from EventBus
                // EventBus.getDefault().unregister(this);
                Log.d(TAG, "Runnable: Thread stopped");
                stopSelf();
            }
        };

        mRunningThread = new Thread(r);
        mRunningThread.start();

        return Service.START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        // No binding necessary, service is controlled via EventBus
        return null;
    }

    @Override
    public void onDestroy() {
        // Unregister from EventBus
        mRunningThread.interrupt();
        Log.d(TAG, "onDestroy: Service stopped");
        // EventBus.getDefault().unregister(this);
    }
}
