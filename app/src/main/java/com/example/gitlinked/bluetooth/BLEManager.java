package com.example.gitlinked.bluetooth;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.pm.PackageManager;
import android.util.Log;

/**
 * Central manager for BLE operations.
 * Handles initialization, capability checks, and provides adapter access.
 */
public class BLEManager {

    private static final String TAG = "BLEManager";
    private final Context context;
    private BluetoothAdapter bluetoothAdapter;
    private BLEScanner scanner;
    private BLEAdvertiser advertiser;
    private static BLEManager instance;

    private BLEManager(Context context) {
        this.context = context.getApplicationContext();
        initBluetooth();
    }

    public static synchronized BLEManager getInstance(Context context) {
        if (instance == null) {
            instance = new BLEManager(context);
        }
        return instance;
    }

    private void initBluetooth() {
        BluetoothManager bluetoothManager =
                (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);
        if (bluetoothManager != null) {
            bluetoothAdapter = bluetoothManager.getAdapter();
        }
    }

    /**
     * Check if the device supports BLE.
     */
    public boolean isBleSupported() {
        return context.getPackageManager()
                .hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE);
    }

    /**
     * Check if Bluetooth is enabled.
     */
    public boolean isBluetoothEnabled() {
        return bluetoothAdapter != null && bluetoothAdapter.isEnabled();
    }

    /**
     * Check if BLE advertising is supported (for peripheral mode).
     */
    public boolean isAdvertisingSupported() {
        return bluetoothAdapter != null
                && bluetoothAdapter.getBluetoothLeAdvertiser() != null;
    }

    /**
     * Get the BLE Scanner instance.
     */
    public BLEScanner getScanner() {
        if (scanner == null && bluetoothAdapter != null) {
            scanner = new BLEScanner(context, bluetoothAdapter);
        }
        return scanner;
    }

    /**
     * Get the BLE Advertiser instance.
     */
    public BLEAdvertiser getAdvertiser() {
        if (advertiser == null && bluetoothAdapter != null) {
            advertiser = new BLEAdvertiser(context, bluetoothAdapter);
        }
        return advertiser;
    }

    /**
     * Get the underlying BluetoothAdapter.
     */
    public BluetoothAdapter getBluetoothAdapter() {
        return bluetoothAdapter;
    }

    /**
     * Clean up resources.
     */
    public void cleanup() {
        if (scanner != null) {
            scanner.stopScan();
        }
        if (advertiser != null) {
            advertiser.stopAdvertising();
        }
    }
}
