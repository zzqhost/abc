package com.hornbill.stepcounter;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.util.Log;

public class StepSaver {
    private static final String STEP_SETTING_FILE = "StepSaverSetting";
    private SharedPreferences mSp;
    private String mKey;

    public StepSaver(Context context, String key) {
        mKey = key;
        mSp = context.getSharedPreferences(STEP_SETTING_FILE, Context.MODE_PRIVATE);
    }

    public int readStep() {
        Log.d("jtx", " service readStep() key: " + mKey);
        if (mSp != null) {
            int count = mSp.getInt(mKey, 0);
            return count;
        } else {
            return 0;
        }
    }

    public void saveStep(int count) {
        Log.d("jtx", " service saveStep() key: " + mKey);
        if (mSp != null) {
            Editor editor = mSp.edit();
            editor.putInt(mKey, count);
            editor.commit();
        }
    }
}
