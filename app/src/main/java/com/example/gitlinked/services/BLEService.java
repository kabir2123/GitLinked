package com.example.gitlinked.services;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.example.gitlinked.bluetooth.BLEManager;
import com.example.gitlinked.utils.Constants;

/**
 * Foreground service for continuous BLE scanning and advertising.
 * Runs in background to discover nearby developers even when app is not in foreground.
 */
public class BLEService extends Service {

    private static final String TAG = "BLEService";
    private BLEManager bleManager;

    public static final String ACTION_START = "com.example.gitlinked.START_BLE";
    public static final String ACTION_STOP = "com.example.gitlinked.STOP_BLE";

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
        bleManager = BLEManager.getInstance(this);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && ACTION_STOP.equals(intent.getAction())) {
            stopBleOperations();
            stopForeground(true);
            stopSelf();
            return START_NOT_STICKY;
        }

        // Start as foreground service
        Notification notification = buildNotification();
        startForeground(Constants.BLE_NOTIFICATION_ID, notification);

        // Start BLE operations
        startBleOperations();

        return START_STICKY;
    }

    private void startBleOperations() {
        if (bleManager.isBleSupported() && bleManager.isBluetoothEnabled()) {
            // Start advertising
            if (bleManager.isAdvertisingSupported()) {
                bleManager.getAdvertiser().startAdvertising();
                Log.d(TAG, "BLE advertising started");
            }

            // Start scanning
            if (bleManager.getScanner() != null) {
                bleManager.getScanner().startScan();
                Log.d(TAG, "BLE scanning started");
            }
        } else {
            Log.w(TAG, "BLE not supported or Bluetooth disabled");
        }
    }

    private void stopBleOperations() {
        if (bleManager != null) {
            bleManager.cleanup();
        }
    }

    private Notification buildNotification() {
        return new NotificationCompat.Builder(this, Constants.BLE_CHANNEL_ID)
                .setContentTitle("GitLinked Active")
                .setContentText("Discovering nearby developers...")
                .setSmallIcon(android.R.drawable.ic_menu_mylocation)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setOngoing(true)
                .build();
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    Constants.BLE_CHANNEL_ID,
                    Constants.BLE_CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_LOW
            );
            channel.setDescription("Used for BLE discovery service");
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        stopBleOperations();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
