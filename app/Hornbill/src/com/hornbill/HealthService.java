package com.hornbill;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

public class HealthService extends Service {
    private static final String TAG = "HealthService";


    private final IBinder mBinder = new LocalBinder();

    public class LocalBinder extends Binder {
        public LocalBinder() {
            Log.d(TAG, "LocalBinder construct");
        }

        public HealthService getService() {
            Log.d(TAG, "get Service called.");
            return HealthService.this;
        }
    }

    @Override 
    public void onCreate() {
        Log.d(TAG, "onCreate");
    }

    @Override 
    public void onDestroy() {
        Log.d(TAG, "onDestroy");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "onStartCommand");
        return 0;
    }

    @Override 
    public IBinder onBind(Intent intent) {
        Log.d(TAG, "onBind");
        return mBinder; 
    }

    @Override
    public boolean onUnbind(Intent intent) {
        Log.d(TAG, "onUnBind");
        return true;
    }

    //==========================================================================
    public NetworkManager getNetworkManager() {
        return mNetworkManager;
    }

    public ImManager getImManager() {
        return mImManager;
    }
}