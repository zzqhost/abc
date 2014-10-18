package com.hornbill.stepcounter;

import java.util.ArrayList;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.util.Log;

public class StepCounter implements SensorEventListener {
    public static final int TYPE_STEP_COUNTER = 19;
    public static final int TYPE_STEP_DETECTOR = 18;

    public static enum PeakType {
        DOWN, UP
    }

    public static enum ThresholdState {
        BETWEEN, UNDER_LOW, OVER_HIGH
    }

    public static class PeakInfo {
        int index = 0;
        boolean isCounted = false;
        public double magnitude;
        public StepCounter.PeakType peakType;
        public long time;

        void set(long aTime, StepCounter.PeakType aType, double aMagnitude, int aIndex) {
            this.time = aTime;
            this.peakType = aType;
            this.magnitude = aMagnitude;
            this.index = aIndex;
        }
    }


    private static final double ADAPTIVE_FACTOR = 0.4D;
    private static final long DOWN_UP_TIME_THRESHOLD = 750L;
    private static final double LOW_THRESHOLD_GAIN = 0.7D;
    private static final double MAX_MAGNITUDE_ABOVE_GRAVITY = 1.3D;
    private static final int NO_STEP_DETECTED_LIMIT_MILLISECS = 2000;
    private static final int REQUIRED_STEPS = 3;

    private double MINIMUM_HIGH_THRESHOLD = 1.1D;
    private double MINIMUM_LOW_THRESHOLD = 0.9D;
    private double adaptiveHighThreshold;
    private double adaptiveLowThreshold;
    private double meanHighThreshold;
    private PeakInfo currentPeak;
    private int stepCount;
    private ThresholdState thresholdState;
    private int numOfReadingsReceived;
    long lastDataTimeStamp;
    private ArrayList<PeakInfo> peaks;


    private Context mContext;

    public StepCounter(Context context) {
        mContext = context;
        init();
    }

    private void init() {
        this.currentPeak = new PeakInfo();
        this.peaks = new ArrayList<PeakInfo>();
        this.lastDataTimeStamp = 0L;
        this.thresholdState = ThresholdState.BETWEEN;
        this.adaptiveHighThreshold = this.MINIMUM_HIGH_THRESHOLD;
        this.adaptiveLowThreshold = this.MINIMUM_LOW_THRESHOLD;
        this.meanHighThreshold = this.MINIMUM_HIGH_THRESHOLD;
        this.stepCount = 0;
        this.numOfReadingsReceived = 0;
        mSensorManager = (SensorManager) mContext.getSystemService(Context.SENSOR_SERVICE);
    }

    private double getNewMeanHighThreshold() {
        double d = 0.0D;
        int i = 0;
        PeakInfo peakInfo = getLastUpPeak();
        if ((peakInfo != null) && (this.lastDataTimeStamp - peakInfo.time > NO_STEP_DETECTED_LIMIT_MILLISECS)) {
            return this.MINIMUM_HIGH_THRESHOLD;
        }
        for (int j = 0; j < this.peaks.size(); j++) {
            if (this.peaks.get(j).isCounted && this.peaks.get(j).peakType == PeakType.UP) {
                i++;
                d += this.peaks.get(j).magnitude;
            }
        }
        if (i > 0) {
            return d / i;
        }
        return this.meanHighThreshold;
    }

    private PeakInfo getLastUpPeak() {
        for (int i = this.peaks.size() - 1; i > 0; i--) {
            if (this.peaks.get(i).peakType == PeakType.UP)
                return this.peaks.get(i);
        }
        return null;
    }

    private void adaptThresholds() {
        this.meanHighThreshold = getNewMeanHighThreshold();
        if (this.meanHighThreshold >= 1.0D) {
            double d1 = this.meanHighThreshold - 1.0D;
            this.adaptiveHighThreshold = Math.max(this.MINIMUM_HIGH_THRESHOLD, this.meanHighThreshold - d1
                    * ADAPTIVE_FACTOR);
            double d2 = Math.min(1.0D, (this.adaptiveHighThreshold - this.MINIMUM_HIGH_THRESHOLD)
                    / MAX_MAGNITUDE_ABOVE_GRAVITY);
            this.adaptiveLowThreshold = (this.MINIMUM_LOW_THRESHOLD + d2 * LOW_THRESHOLD_GAIN);
            if (this.adaptiveHighThreshold <= this.adaptiveLowThreshold) {
                Log.e("larry", "Threshold error high<low");
            }
        }
    }

    private void addPeak(PeakInfo aPeakInfo) {
        int size = this.peaks.size();
        if (size > 0 && aPeakInfo.peakType == this.peaks.get(size - 1).peakType) {
            this.peaks.get(size - 1).set(aPeakInfo.time, aPeakInfo.peakType, aPeakInfo.magnitude, aPeakInfo.index);
            return;
        }
        PeakInfo peakInfo = new PeakInfo();
        peakInfo.set(aPeakInfo.time, aPeakInfo.peakType, aPeakInfo.magnitude, aPeakInfo.index);
        this.peaks.add(peakInfo);
        // if (this.peaks.size() > 10){
        if (this.peaks.size() > REQUIRED_STEPS * 2) {
            this.peaks.remove(0);
        }
    }

    private void detectPeaks(long aTime, double aMagnitude) {
        boolean shouldAdd = false;
        if (this.thresholdState == ThresholdState.BETWEEN) {
            if (aMagnitude > this.adaptiveHighThreshold) {
                this.thresholdState = ThresholdState.OVER_HIGH;
                this.currentPeak.set(aTime, PeakType.UP, aMagnitude, this.numOfReadingsReceived);
                shouldAdd = true;
            } else if (aMagnitude < this.adaptiveLowThreshold) {
                this.thresholdState = ThresholdState.UNDER_LOW;
                this.currentPeak.set(aTime, PeakType.DOWN, aMagnitude, this.numOfReadingsReceived);
                shouldAdd = true;
            }
        } else if (this.thresholdState == ThresholdState.OVER_HIGH) {
            if (aMagnitude > this.adaptiveHighThreshold) {
                if (aMagnitude > this.currentPeak.magnitude) {
                    shouldAdd = true;
                }
                // shouldAdd = true;
                this.currentPeak.set(aTime, PeakType.UP, aMagnitude, this.numOfReadingsReceived);
            } else if (aMagnitude < this.adaptiveLowThreshold) {
                this.thresholdState = ThresholdState.UNDER_LOW;
                this.currentPeak.set(aTime, PeakType.DOWN, aMagnitude, this.numOfReadingsReceived);
                shouldAdd = true;
            } else {
                this.thresholdState = ThresholdState.BETWEEN;
            }
        } else { // this.thresholdState == ThresholdState.UNDER_LOW
            if (aMagnitude < this.adaptiveLowThreshold) {
                if (aMagnitude < this.currentPeak.magnitude) {
                    shouldAdd = true;
                }
                // shouldAdd = true;
                this.currentPeak.set(aTime, PeakType.DOWN, aMagnitude, this.numOfReadingsReceived);
            } else if (aMagnitude > this.adaptiveHighThreshold) {
                this.thresholdState = ThresholdState.OVER_HIGH;
                this.currentPeak.set(aTime, PeakType.UP, aMagnitude, this.numOfReadingsReceived);
                shouldAdd = true;
            } else {
                this.thresholdState = ThresholdState.BETWEEN;
            }
        }
        if (shouldAdd == true) {
            addPeak(this.currentPeak);
        }
    }

    private int countBeginStart = 0;
    private int countUnconfirmed = 0;
    private PeakInfo lastPeak = new PeakInfo();

    private void detectSteps() {
        countBeginStart = stepCount;
        if (this.peaks.size() > 2) {
            PeakInfo peak1 = this.peaks.get(0);
            PeakInfo peak2 = this.peaks.get(1);
            if (peak1.peakType != peak2.peakType) {
                if (peak2.time - peak1.time < DOWN_UP_TIME_THRESHOLD) {
                    if (!peak1.isCounted && !peak2.isCounted) {
                        peak1.isCounted = true;
                        peak2.isCounted = true;
                        PeakInfo tempPeak;
                        if (peak2.peakType != PeakType.UP) {
                            tempPeak = peaks.get(2);
                        } else {
                            tempPeak = peak2;
                        }
                        long interval = Math.abs(tempPeak.time - lastPeak.time);
                        if (interval > 250l && interval < 1500l) {
                            this.stepCount++;
                            if (mStepListener != null) {
                                mStepListener.onStep(stepCount);
                            }
                        }
                        countUnconfirmed = stepCount - countBeginStart;
                        lastPeak = tempPeak;
                    }
                }
            }
        }
    }

    public void registerSensor() {
        Sensor stepDetectorSensor = mSensorManager.getDefaultSensor(TYPE_STEP_DETECTOR);
        if (stepDetectorSensor != null) {
            Log.d("jtx", " service registerSensor() use Detector");
            mRegistered = mSensorManager.registerListener(this, stepDetectorSensor, SensorManager.SENSOR_DELAY_FASTEST);

        } else {
            Log.d("jtx", " service registerSensor() use ACCELEROMETER");
            mRegistered = mSensorManager.registerListener(this,
                    mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER), SensorManager.SENSOR_DELAY_FASTEST);
        }

//        mRegistered = mSensorManager.registerListener(this,
//                mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER), SensorManager.SENSOR_DELAY_FASTEST);
    }

    public void unregisterSensor() {
        if (mRegistered) {
            mSensorManager.unregisterListener(this);
            mRegistered = false;
        }
    }

    private SensorManager mSensorManager;
    private boolean mRegistered;

    // ========================================================

    @Override
    public void onAccuracyChanged(Sensor arg0, int arg1) {
        // TODO Auto-generated method stub
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        // TODO Auto-generated method stub
        Log.d("jtx", " StepCounter onSensorChanged()");
        int sensorType = event.sensor.getType();
        if (sensorType == Sensor.TYPE_ACCELEROMETER) {
            // 获得三个轴向的数
            float x = event.values[0];
            float y = event.values[1];
            float z = event.values[2];

            this.lastDataTimeStamp = System.currentTimeMillis();
            double d = Math.sqrt(x * x + y * y + z * z) / 9.806650161743164D;
            this.numOfReadingsReceived = (1 + this.numOfReadingsReceived);
            detectPeaks(this.lastDataTimeStamp, d);
            detectSteps();
            adaptThresholds();
        } /*else if (sensorType == Sensor.TYPE_STEP_DETECTOR) { // TODO, 最新的API支持新的计步方式。
            this.stepCount++;
            if (mStepListener != null) {
                mStepListener.onStep(stepCount);
            }
        }//*/
    }

    public static interface StepListener {
        void onStep(int stepCount);
    }

    private StepListener mStepListener;

    public void setStepListener(StepListener l) {
        mStepListener = l;
    }

    public void setStepCount(int count) {
        stepCount = count;
    }

    public int getStepCount() {
        return stepCount;
    }
}
