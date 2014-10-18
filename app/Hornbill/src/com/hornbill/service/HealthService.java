package com.hornbill.service;

import com.hornbill.stepcounter.StepManager;

import android.app.Service;
import android.content.Intent;
import android.content.IntentFilter;
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

    private StepManager mStepManager;

    @Override 
    public void onCreate() {
        Log.d(TAG, "onCreate");
        /* 目前暂不启动此功能。
        // 启动守护Receiver, 死后重启。
        IntentFilter guardFilter = new IntentFilter(Intent.ACTION_TIME_TICK);
        HealthServiceGuradReceiver guardReceiver = new HealthServiceGuradReceiver();
        registerReceiver(guardReceiver, guardFilter); // */

        mStepManager = new StepManager(this);
    }

    @Override 
    public void onDestroy() {
        Log.d(TAG, "onDestroy");
        mStepManager.stopWork();
        mStepManager.destroy();
        /* 目前不需要退出后再重启。
        Intent intent = new Intent(this, HealthService.class);
        startService(intent);//*/
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
    public StepManager getStepManager() {
        return mStepManager;
    }
}