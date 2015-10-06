package de.velcommuta.denul.service;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

/**
 * Broadcast receiver for BOOT_COMPLETED Broadcasts
 */
public class OnBootReceiver extends BroadcastReceiver {
    private static final String TAG = "OnBootReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction().equals(Intent.ACTION_BOOT_COMPLETED)) {
            Log.i(TAG, "onReceive: Got BOOT_COMPLETED");
            Intent pedometerService = new Intent(context, PedometerService.class);
            context.startService(pedometerService);
        } else {
            Log.e(TAG, "onReceive: Got intent which I did not filter for - " + intent.getAction());
        }
    }
}
