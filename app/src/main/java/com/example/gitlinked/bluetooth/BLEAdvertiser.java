package com.example.gitlinked.bluetooth;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.le.AdvertiseCallback;
import android.bluetooth.le.AdvertiseData;
import android.bluetooth.le.AdvertiseSettings;
import android.bluetooth.le.BluetoothLeAdvertiser;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.ParcelUuid;
import android.util.Log;

import com.example.gitlinked.utils.Constants;

import java.nio.charset.StandardCharsets;

/**
 * BLE Advertiser that broadcasts user identity for discovery by nearby devices.
 */
public class BLEAdvertiser {

    private static final String TAG = "BLEAdvertiser";
    private final Context context;
    private final BluetoothAdapter bluetoothAdapter;
    private BluetoothLeAdvertiser leAdvertiser;
    private boolean isAdvertising = false;

    public BLEAdvertiser(Context context, BluetoothAdapter adapter) {
        this.context = context;
        this.bluetoothAdapter = adapter;
        if (adapter != null) {
            this.leAdvertiser = adapter.getBluetoothLeAdvertiser();
        }
    }

    /**
     * Start advertising this device as a GitLinked user.
     * Broadcasts the service UUID and user ID as service data.
     */
    public void startAdvertising() {
        if (isAdvertising) {
            Log.w(TAG, "Already advertising");
            return;
        }

        if (leAdvertiser == null) {
            Log.e(TAG, "BLE Advertiser not available");
            return;
        }

        // Get user ID from preferences
        SharedPreferences prefs = context.getSharedPreferences(
                Constants.PREF_NAME, Context.MODE_PRIVATE);
        String userId = prefs.getString(Constants.PREF_USER_ID, "unknown");

        AdvertiseSettings settings = new AdvertiseSettings.Builder()
                .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_BALANCED)
                .setConnectable(false)
                .setTimeout(0) // Advertise indefinitely
                .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_MEDIUM)
                .build();

        // Include service UUID for filtering
        AdvertiseData advertiseData = new AdvertiseData.Builder()
                .setIncludeDeviceName(false) // Save space
                .addServiceUuid(new ParcelUuid(Constants.GITLINKED_SERVICE_UUID))
                .build();

        // Include user ID in scan response
        byte[] userIdBytes = userId.getBytes(StandardCharsets.UTF_8);
        // Truncate to fit BLE payload limit (max ~20 bytes for service data)
        if (userIdBytes.length > 20) {
            byte[] truncated = new byte[20];
            System.arraycopy(userIdBytes, 0, truncated, 0, 20);
            userIdBytes = truncated;
        }

        AdvertiseData scanResponse = new AdvertiseData.Builder()
                .addServiceData(new ParcelUuid(Constants.GITLINKED_SERVICE_UUID), userIdBytes)
                .build();

        try {
            leAdvertiser.startAdvertising(settings, advertiseData, scanResponse, advertiseCallback);
        } catch (SecurityException e) {
            Log.e(TAG, "Permission denied for BLE advertising", e);
        }
    }

    /**
     * Stop advertising.
     */
    public void stopAdvertising() {
        if (!isAdvertising) return;

        try {
            if (leAdvertiser != null) {
                leAdvertiser.stopAdvertising(advertiseCallback);
            }
        } catch (SecurityException e) {
            Log.e(TAG, "Permission denied stopping advertising", e);
        }

        isAdvertising = false;
        Log.d(TAG, "BLE advertising stopped");
    }

    public boolean isAdvertising() {
        return isAdvertising;
    }

    private final AdvertiseCallback advertiseCallback = new AdvertiseCallback() {
        @Override
        public void onStartSuccess(AdvertiseSettings settingsInEffect) {
            super.onStartSuccess(settingsInEffect);
            isAdvertising = true;
            Log.d(TAG, "BLE advertising started successfully");
        }

        @Override
        public void onStartFailure(int errorCode) {
            super.onStartFailure(errorCode);
            isAdvertising = false;
            Log.e(TAG, "BLE advertising failed with error code: " + errorCode);
        }
    };
}
