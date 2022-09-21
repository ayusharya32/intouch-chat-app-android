package com.easycodingg.intouch.application;

import static com.easycodingg.intouch.utils.Constants.NOTIFICATION_CHANNEL_HIGH;
import static com.easycodingg.intouch.utils.Constants.NOTIFICATION_CHANNEL_LOW;

import android.app.Application;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.DefaultLifecycleObserver;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.ProcessLifecycleOwner;

import com.easycodingg.intouch.services.IntouchService;

public class IntouchApplication extends Application implements DefaultLifecycleObserver {
    private static final String TAG = "IntouchApplicationyy";

    @Override
    public void onCreate() {
        super.onCreate();

        createNotificationChannel();
        ProcessLifecycleOwner.get().getLifecycle().addObserver(this);
    }

    @Override
    public void onStart(@NonNull LifecycleOwner owner) {
        DefaultLifecycleObserver.super.onStart(owner);
        Log.d(TAG, "onStart: Called App In Foreground - Service Running - " + IntouchService.isRunning);

        if(IntouchService.isRunning) {
            // User is logged in
            sendActionAppInForegroundToService();
        }
    }

    @Override
    public void onStop(@NonNull LifecycleOwner owner) {
        DefaultLifecycleObserver.super.onStop(owner);
        Log.d(TAG, "onStop: Called App In Background - Service Running - " + IntouchService.isRunning);

        if(IntouchService.isRunning) {
            // User is logged in
            sendActionAppInBackgroundToService();
        }
    }

    private void sendActionAppInForegroundToService() {
        Intent intent = new Intent(this, IntouchService.class);
        intent.setAction(IntouchService.ACTION_FOR_APP_COMES_FOREGROUND);
        startService(intent);
    }

    private void sendActionAppInBackgroundToService() {
        Intent intent = new Intent(this, IntouchService.class);
        intent.setAction(IntouchService.ACTION_FOR_APP_GOES_BACKGROUND);
        startService(intent);
    }

    private void createNotificationChannel() {
        NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channelLow = new NotificationChannel(
                    NOTIFICATION_CHANNEL_LOW,
                    "Channel Low",
                    NotificationManager.IMPORTANCE_LOW
            );

            NotificationChannel channelHigh = new NotificationChannel(
                    NOTIFICATION_CHANNEL_HIGH,
                    "Channel High",
                    NotificationManager.IMPORTANCE_HIGH
            );

            manager.createNotificationChannel(channelLow);
            manager.createNotificationChannel(channelHigh);
        }
    }

}
