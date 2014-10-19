
package com.hornbill;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;

import com.hornbill.service.HealthService;
import com.hornbill.stepcounter.StepCounter.StepListener;
import com.hornbill.stepcounter.StepManager;

public class MainActivity extends Activity implements StepListener, ServiceConnection, OnClickListener {
    private StepManager mStepManager;
    private Button mBindBtn;
    private Button mReadBtn;
    private TextView mStepCountText;
    private int mBindState;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        bindService(new Intent(this, HealthService.class), this, Context.BIND_AUTO_CREATE);

        mStepCountText = (TextView) findViewById(R.id.step_count_text);
        mBindBtn = (Button) findViewById(R.id.btn_bind);
        mBindBtn.setOnClickListener(this);
        findViewById(R.id.btn_clear_step).setOnClickListener(this);
        mBindState = 0;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        unbindService(this);
    }

    @Override
    public void onStep(int stepCount) {
        mStepCountText.setText(String.valueOf(stepCount));
    }

    @Override
    public void onServiceConnected(ComponentName name, IBinder service) {
        HealthService healthService = ((HealthService.LocalBinder)service).getService();
        mStepManager = healthService.getStepManager();
    }

    @Override
    public void onServiceDisconnected(ComponentName name) {
        mStepManager = null;
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        switch (id) {
        case R.id.btn_bind: {
            if (mBindState == 0) {
                mStepManager.registListener(this);
                mStepManager.startWork();
                mBindBtn.setText(this.getResources().getString(R.string.step_count_stop));
                mBindState = 1;
            } else {
                mStepManager.unregistListener(this);
                mStepManager.stopWork();
                mBindBtn.setText(this.getResources().getString(R.string.step_count_begin));
                mBindState = 0;
            }
        }
            break;

        case R.id.btn_clear_step:
            mStepManager.resetWork();
            break;
        }
    }
}
