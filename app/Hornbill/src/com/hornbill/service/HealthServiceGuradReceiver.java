package com.hornbill.service;

import android.app.ActivityManager;
import android.app.ActivityManager.RunningServiceInfo;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;


public class HealthServiceGuradReceiver extends BroadcastReceiver {
    private static String TAG = "HealthServiceGuradReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        boolean isServiceRunning = false;

        if (intent.getAction().equals(Intent.ACTION_TIME_TICK)) {
            Log.d(TAG, "service HealthServiceGuradReceiver::onReceive()");
            ActivityManager manager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
            for (RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
                if (HealthService.class.getName().equals(service.service.getClassName())) {
                    isServiceRunning = true;
                }
            }
            Log.d(TAG, " service HealthServiceGuradReceiver::onReceive(), isServiceRunning = " + isServiceRunning);
            if (!isServiceRunning) {
                Intent i = new Intent(context, HealthService.class);
                context.startService(i);
            }
        }
    }
}
