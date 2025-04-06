package com.farming.ai.utils;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.farming.ai.R;

import java.util.ArrayList;
import java.util.List;

public class PermissionUtils {

    // List of required permissions
    private static final String[] REQUIRED_PERMISSIONS = new String[]{
            // Location permissions
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            
            // Camera permissions
            Manifest.permission.CAMERA,
            
            // Storage permissions for Android 10 and below
            Build.VERSION.SDK_INT <= Build.VERSION_CODES.S_V2 
                    ? Manifest.permission.READ_EXTERNAL_STORAGE : null,
            
            // Media permissions for Android 13+
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                    ? Manifest.permission.READ_MEDIA_IMAGES : null,
            
            // Notifications for Android 13+
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                    ? Manifest.permission.POST_NOTIFICATIONS : null
    };
    
    /**
     * Check if all required permissions are granted
     * @param context the context
     * @return true if all permissions are granted, false otherwise
     */
    public static boolean allPermissionsGranted(Context context) {
        for (String permission : REQUIRED_PERMISSIONS) {
            if (permission == null) continue;
            if (ContextCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }
    
    /**
     * Request all required permissions
     * @param activity the activity
     */
    public static void requestAllPermissions(Activity activity) {
        List<String> permissionsToRequest = new ArrayList<>();
        
        for (String permission : REQUIRED_PERMISSIONS) {
            if (permission == null) continue;
            if (ContextCompat.checkSelfPermission(activity, permission) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(permission);
            }
        }
        
        if (!permissionsToRequest.isEmpty()) {
            ActivityCompat.requestPermissions(activity, 
                    permissionsToRequest.toArray(new String[0]), 100);
        }
    }
    
    /**
     * Request only non-storage related permissions
     * @param activity the activity
     */
    public static void requestNonStoragePermissions(Activity activity) {
        List<String> permissionsToRequest = new ArrayList<>();
        
        for (String permission : REQUIRED_PERMISSIONS) {
            if (permission == null) continue;
            
            // Skip storage-related permissions
            if (permission.equals(Manifest.permission.READ_EXTERNAL_STORAGE) ||
                permission.equals(Manifest.permission.WRITE_EXTERNAL_STORAGE) ||
                permission.equals(Manifest.permission.READ_MEDIA_IMAGES)) {
                continue;
            }
            
            if (ContextCompat.checkSelfPermission(activity, permission) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(permission);
            }
        }
        
        if (!permissionsToRequest.isEmpty()) {
            ActivityCompat.requestPermissions(activity, 
                    permissionsToRequest.toArray(new String[0]), 101);
        }
    }
    
    /**
     * Register permission result handlers
     * @param activity the activity
     * @return activity result launcher
     */
    public static ActivityResultLauncher<String[]> registerForPermissionResult(AppCompatActivity activity) {
        return activity.registerForActivityResult(
                new ActivityResultContracts.RequestMultiplePermissions(),
                results -> {
                    boolean allGranted = true;
                    for (Boolean result : results.values()) {
                        allGranted = allGranted && result;
                    }
                    
                    if (!allGranted) {
                        showPermissionExplanationDialog(activity);
                    }
                });
    }
    
    /**
     * Show a dialog explaining why permissions are needed
     * @param context the context
     */
    public static void showPermissionExplanationDialog(Context context) {
        new AlertDialog.Builder(context)
                .setTitle(R.string.permission_required)
                .setMessage("This app requires camera, location, and storage permissions to function properly.")
                .setPositiveButton(R.string.grant_permissions, (dialog, which) -> {
                    // Open app settings
                    Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                    Uri uri = Uri.fromParts("package", context.getPackageName(), null);
                    intent.setData(uri);
                    context.startActivity(intent);
                })
                .setNegativeButton(android.R.string.cancel, null)
                .setCancelable(false)
                .show();
    }
} 