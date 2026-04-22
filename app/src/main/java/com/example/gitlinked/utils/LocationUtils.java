package com.example.gitlinked.utils;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Build;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.util.ArrayList;
import java.util.List;

/**
 * Utility class for location and Bluetooth permission handling.
 */
public class LocationUtils {

    public static final int PERMISSION_REQUEST_CODE = 1001;

    /**
     * Check if fine location permission is granted.
     */
    public static boolean hasLocationPermission(Context context) {
        return ContextCompat.checkSelfPermission(context,
                Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    /**
     * Check if all BLE-required permissions are granted.
     */
    public static boolean hasBlePermissions(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            return ContextCompat.checkSelfPermission(context,
                    Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED
                    && ContextCompat.checkSelfPermission(context,
                    Manifest.permission.BLUETOOTH_ADVERTISE) == PackageManager.PERMISSION_GRANTED
                    && ContextCompat.checkSelfPermission(context,
                    Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED;
        } else {
            return hasLocationPermission(context);
        }
    }

    /**
     * Request all required permissions for BLE and location.
     */
    public static void requestAllPermissions(Activity activity) {
        List<String> permissions = new ArrayList<>();
        permissions.add(Manifest.permission.ACCESS_FINE_LOCATION);
        permissions.add(Manifest.permission.ACCESS_COARSE_LOCATION);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            permissions.add(Manifest.permission.BLUETOOTH_SCAN);
            permissions.add(Manifest.permission.BLUETOOTH_ADVERTISE);
            permissions.add(Manifest.permission.BLUETOOTH_CONNECT);
        }

        // Android 13+ requires POST_NOTIFICATIONS for foreground service notification
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.POST_NOTIFICATIONS);
        }

        ActivityCompat.requestPermissions(activity,
                permissions.toArray(new String[0]),
                PERMISSION_REQUEST_CODE);
    }

    /**
     * Calculate distance between two coordinates in meters.
     */
    public static double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
        float[] results = new float[1];
        Location.distanceBetween(lat1, lon1, lat2, lon2, results);
        return results[0];
    }

    /**
     * Format distance for display.
     */
    public static String formatDistance(double meters) {
        if (meters < 1000) {
            return String.format("%.0fm away", meters);
        } else {
            return String.format("%.1fkm away", meters / 1000);
        }
    }
}
