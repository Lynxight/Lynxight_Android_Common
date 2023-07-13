package com.lynxight.common.utils;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.util.Log;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;


public class Permissions {
    private final static String TAG = Permissions.class.getSimpleName();

    public static void requestPermission(Activity activity,
                                         String permission, final int requestCode)
    {
        // Only ask for these permissions on runtime when running Android 6.0 or higher
        if (checkPermissionState(activity, permission)) {
            Log.i(TAG, "permission " + permission + ": PERMISSION_GRANTED");
        } else {
            Log.i(TAG, "permission " + permission + ": PERMISSION_DENIED");
            ActivityCompat.requestPermissions(activity, new String[] {permission}, requestCode);
            checkPermissionState(activity, permission);
        }
    }

    public static void requestInternetPermission(Activity activity) {
        requestPermission(activity,
                Manifest.permission.INTERNET, Constants.PERMISSION_INTERNET_REQUEST);
    }

    public static void requestWiFiStatePermission(Activity activity) {
        requestPermission(activity,
                Manifest.permission.ACCESS_WIFI_STATE, Constants.PERMISSION_WIFI_STATE_REQUEST);
    }

    public static void requestBluetoothPermission(Activity activity) {
        requestPermission(activity,
                Manifest.permission.BLUETOOTH, Constants.PERMISSION_BLUETOOTH_REQUEST);
    }

    public static void requestBluetoothAdminPermission(Activity activity) {
        requestPermission(activity,
                Manifest.permission.BLUETOOTH_ADMIN, Constants.PERMISSION_BLUETOOTH_ADMIN_REQUEST);
    }

    public static void requestSystemAlertWindowPermission(Activity activity) {
        requestPermission(activity,
                Manifest.permission.SYSTEM_ALERT_WINDOW,
                Constants.PERMISSION_SYSTEM_ALERT_WINDOW_REQUEST);
    }

    public static void requestAudioRecordPermission(Activity activity) {
        requestPermission(activity,
                Manifest.permission.RECORD_AUDIO,
                Constants.PERMISSION_AUDIO_RECORD_REQUEST);
    }

    public static boolean checkPermissionState(Activity activity, final String permission) {
        return ContextCompat.checkSelfPermission(activity, permission) ==
                PackageManager.PERMISSION_GRANTED;
    }
}
