package com.hornbill.stepcounter;

import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;

import android.content.BroadcastReceiver;
import android.content.Context;

import com.hornbill.stepcounter.StepCounter.StepListener;


public class StepManager {
    private Context mContext;
    private boolean mIsWorking;
    private StepCounter mStepCounter;
    private StepListener mStepListener;
    private String mCurrentDayStepCountKey;
    private BroadcastReceiver mScreenOffReceiver;
    private StepSaver mStepSaver;
    private HashSet<StepListener> mListeners;

    public StepManager(Context context) {
        mContext = context;
        mListeners = new HashSet<StepCounter.StepListener>();
        mIsWorking = false;
        init();
    }

    public void startWork() {
        mStepCounter.registerSensor();
        mStepCounter.setStepListener(mStepListener);
        int count = mStepSaver.readStep();
        mStepCounter.setStepCount(count);
        mIsWorking = true;

        // 屏幕锁屏，开屏应怎样处理？
        /*
        mScreenOffReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                Log.d("jtx", " mScreenOffReceiver onReceive() " + intent.getAction());
                if (!intent.getAction().equals(Intent.ACTION_SCREEN_OFF)) {
                    return;
                }
                stopWork();
            }
        };
        IntentFilter screenOffFilter = new IntentFilter(Intent.ACTION_SCREEN_ON);
        screenOffFilter.addAction(Intent.ACTION_SCREEN_OFF);
        mContext.registerReceiver(mScreenOffReceiver, screenOffFilter);
         //*/

        // 关闭自动锁屏? 
        /*
        PowerManager manager = (PowerManager) mContext.getSystemService(Context.POWER_SERVICE);
        PowerManager.WakeLock wakeLock = manager.newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK | PowerManager.ACQUIRE_CAUSES_WAKEUP, "");
        wakeLock.acquire();
        */
    }

    public void stopWork() {
        mStepSaver.saveStep(mStepCounter.getStepCount());
        mStepCounter.unregisterSensor();
        mIsWorking = false;

        /*
        PowerManager manager = (PowerManager) mContext.getSystemService(Context.POWER_SERVICE);
        PowerManager.WakeLock wakeLock = manager.newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK | PowerManager.ACQUIRE_CAUSES_WAKEUP, "");
        wakeLock.release(); //*/
    }

    // 清零计数器
    public void resetWork() {
        mStepSaver.saveStep(0);
        if (mIsWorking) {
            mStepCounter.setStepCount(0);
            notifyUser(0);
        }
    }

    public void destroy() {
        stopWork();
    }

    public void registListener(StepListener listener) {
        if (listener == null) {
            return;
        }
        mListeners.add(listener);
    }

    public void unregistListener(StepListener listener) {
        if (listener == null) {
            return;
        }
        if (!mListeners.contains(listener)) {
            return;
        }
        mListeners.remove(listener);
    }


    //
    private void init() {
        // 初始化 stepCounter.
        mStepCounter = new StepCounter(mContext);
        mStepListener = new MyStepListener();

        // 存储。
        mCurrentDayStepCountKey = getStepCountKey(null);
        mStepSaver = new StepSaver(mContext, mCurrentDayStepCountKey);
    }

    private String getStepCountKey(Date date) {
        String res = null;
        Calendar cl = Calendar.getInstance();
        if (date != null) {
            cl.setTime(date);
        }
        int year = cl.get(Calendar.YEAR);
        int month = cl.get(Calendar.MONTH) + 1;
        int day = cl.get(Calendar.DAY_OF_MONTH);
        res = String.format("%d_%d_%d", year, month, day);
        return res;
    }

    private class MyStepListener implements StepListener {
        @Override
        public void onStep(int stepCount) {
            mStepSaver.saveStep(stepCount);
            notifyUser(stepCount);
        }
    }

    private void notifyUser(int stepCount) {
        for (StepListener listener: mListeners) {
            if (listener != null) {
                listener.onStep(stepCount);
            }
        }
    }
}
