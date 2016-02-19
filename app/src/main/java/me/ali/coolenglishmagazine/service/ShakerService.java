package me.ali.coolenglishmagazine.service;

import android.app.Service;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.os.IBinder;

import me.ali.coolenglishmagazine.util.Shaker;

public class ShakerService extends Service implements Shaker.OnShakeListener {


    private Shaker mShaker;
    private SensorManager mSensorManager;
    private Sensor mAccelerometer;

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    public void onCreate() {

        super.onCreate();
        this.mSensorManager = ((SensorManager) getSystemService(SENSOR_SERVICE));
        this.mAccelerometer = this.mSensorManager.getDefaultSensor(1);
        mShaker = new Shaker(this);
        mShaker.setOnShakeListener(this);
    }

    @Override
    public void onShake() {
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);
        return START_STICKY;
    }
}