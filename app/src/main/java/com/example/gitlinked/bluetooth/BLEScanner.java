package com.example.gitlinked.bluetooth;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.os.ParcelUuid;
import android.util.Log;

import com.example.gitlinked.utils.Constants;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * BLE Scanner for discovering nearby GitLinked users.
 * Scans for devices advertising the GitLinked service UUID.
 * If filtered scan fails, falls back to unfiltered scan.
 */
public class BLEScanner {

    private static final String TAG = "BLEScanner";
    private final Context context;
    private final BluetoothAdapter bluetoothAdapter;
    private BluetoothLeScanner leScanner;
    private boolean isScanning = false;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final List<ScanResult> discoveredDevices = new ArrayList<>();

    public interface ScanListener {
        void onDeviceFound(ScanResult result, String userId);
        void onScanComplete(List<ScanResult> results);
        void onScanError(String error);
    }

    private ScanListener listener;

    public BLEScanner(Context context, BluetoothAdapter adapter) {
        this.context = context;
        this.bluetoothAdapter = adapter;
        if (adapter != null) {
            this.leScanner = adapter.getBluetoothLeScanner();
        }
    }

    public void setScanListener(ScanListener listener) {
        this.listener = listener;
    }

    /**
     * Start BLE scan with timeout.
     * First tries filtered scan for GitLinked UUID, falls back to unfiltered.
     */
    public void startScan() {
        if (isScanning) {
            Log.w(TAG, "Already scanning");
            return;
        }

        if (leScanner == null) {
            // Try to re-acquire scanner
            if (bluetoothAdapter != null && bluetoothAdapter.isEnabled()) {
                leScanner = bluetoothAdapter.getBluetoothLeScanner();
            }
            if (leScanner == null) {
                if (listener != null) {
                    listener.onScanError("BLE Scanner not available. Is Bluetooth enabled?");
                }
                return;
            }
        }

        discoveredDevices.clear();
        isScanning = true;

        // Set up scan filter for GitLinked service UUID
        ScanFilter filter = new ScanFilter.Builder()
                .setServiceUuid(new ParcelUuid(Constants.GITLINKED_SERVICE_UUID))
                .build();

        ScanSettings settings = new ScanSettings.Builder()
                .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                .setReportDelay(0)
                .build();

        try {
            leScanner.startScan(
                    Collections.singletonList(filter),
                    settings,
                    scanCallback
            );
            Log.d(TAG, "BLE filtered scan started (UUID: " + Constants.GITLINKED_SERVICE_UUID + ")");

            // Auto-stop after timeout
            handler.postDelayed(this::stopScan, Constants.BLE_SCAN_DURATION_MS);
        } catch (SecurityException e) {
            Log.e(TAG, "Permission denied for BLE scan", e);
            isScanning = false;
            if (listener != null) {
                listener.onScanError("Bluetooth permission denied. Please grant permissions in Settings.");
            }
        } catch (Exception e) {
            Log.e(TAG, "Scan start failed, trying unfiltered scan", e);
            startUnfilteredScan(settings);
        }
    }

    /**
     * Fallback: start unfiltered scan if filtered scan fails.
     */
    private void startUnfilteredScan(ScanSettings settings) {
        try {
            leScanner.startScan(null, settings, scanCallback);
            Log.d(TAG, "BLE unfiltered scan started (fallback)");
            handler.postDelayed(this::stopScan, Constants.BLE_SCAN_DURATION_MS);
        } catch (SecurityException e) {
            Log.e(TAG, "Permission denied for unfiltered scan", e);
            isScanning = false;
            if (listener != null) {
                listener.onScanError("Bluetooth permission denied");
            }
        }
    }

    /**
     * Stop BLE scan.
     */
    public void stopScan() {
        if (!isScanning) return;

        isScanning = false;
        handler.removeCallbacksAndMessages(null);

        try {
            if (leScanner != null) {
                leScanner.stopScan(scanCallback);
            }
        } catch (SecurityException e) {
            Log.e(TAG, "Permission denied stopping scan", e);
        } catch (Exception e) {
            Log.e(TAG, "Error stopping scan", e);
        }

        Log.d(TAG, "BLE scan stopped. Found " + discoveredDevices.size() + " devices");

        if (listener != null) {
            listener.onScanComplete(new ArrayList<>(discoveredDevices));
        }
    }

    public boolean isScanning() {
        return isScanning;
    }

    public List<ScanResult> getDiscoveredDevices() {
        return new ArrayList<>(discoveredDevices);
    }

    private final ScanCallback scanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            super.onScanResult(callbackType, result);

            String foundUserId = null;
            if (result.getScanRecord() != null) {
                byte[] serviceData = result.getScanRecord().getServiceData(
                        new ParcelUuid(Constants.GITLINKED_SERVICE_UUID));
                if (serviceData != null) {
                    foundUserId = new String(serviceData, StandardCharsets.UTF_8).trim();
                }
            }

            if (foundUserId == null) return;

            // Avoid duplicates by BLE address
            boolean alreadyFound = false;
            for (ScanResult existing : discoveredDevices) {
                if (existing.getDevice().getAddress().equals(result.getDevice().getAddress())) {
                    alreadyFound = true;
                    break;
                }
            }

            if (!alreadyFound) {
                discoveredDevices.add(result);
                Log.d(TAG, "GitLinked device found: " + result.getDevice().getAddress()
                        + " UserID: " + foundUserId + " RSSI: " + result.getRssi());

                if (listener != null) {
                    listener.onDeviceFound(result, foundUserId);
                }
            }
        }

        @Override
        public void onScanFailed(int errorCode) {
            super.onScanFailed(errorCode);
            isScanning = false;

            String errorMsg;
            switch (errorCode) {
                case SCAN_FAILED_ALREADY_STARTED:
                    errorMsg = "Scan already in progress";
                    break;
                case SCAN_FAILED_APPLICATION_REGISTRATION_FAILED:
                    errorMsg = "BLE app registration failed. Try toggling Bluetooth.";
                    break;
                case SCAN_FAILED_FEATURE_UNSUPPORTED:
                    errorMsg = "BLE scanning not supported on this device";
                    break;
                case SCAN_FAILED_INTERNAL_ERROR:
                    errorMsg = "BLE internal error. Try toggling Bluetooth.";
                    break;
                default:
                    errorMsg = "Scan failed (code: " + errorCode + ")";
            }

            Log.e(TAG, errorMsg);
            if (listener != null) {
                listener.onScanError(errorMsg);
            }
        }
    };
}
