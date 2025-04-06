package com.farming.ai.ui.auth;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.widget.Button;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.farming.ai.R;
import com.farming.ai.utils.PermissionUtils;

import java.io.File;

public class WelcomeActivity extends AppCompatActivity {

    private static final int PERMISSION_REQUEST_CODE = 100;
    
    // Create the permission launcher
    private final ActivityResultLauncher<String[]> requestPermissionLauncher =
        registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), permissions -> {
            boolean allStoragePermissionsGranted = true;
            
            for (String key : permissions.keySet()) {
                if (isStoragePermission(key) && !permissions.get(key)) {
                    allStoragePermissionsGranted = false;
                    break;
                }
            }
            
            if (!allStoragePermissionsGranted) {
                showStoragePermissionExplanationDialog();
            }
        });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_welcome);

        // Request storage permissions
        requestStoragePermissions();

        // Set up login button
        Button btnLogin = findViewById(R.id.btn_login);
        btnLogin.setOnClickListener(view -> {
            Intent intent = new Intent(WelcomeActivity.this, LoginActivity.class);
            startActivity(intent);
        });

        // Set up register button
        Button btnRegister = findViewById(R.id.btn_register);
        btnRegister.setOnClickListener(view -> {
            Intent intent = new Intent(WelcomeActivity.this, RegisterActivity.class);
            startActivity(intent);
        });
    }
    
    /**
     * Check if the given permission is a storage-related permission
     */
    private boolean isStoragePermission(String permission) {
        return permission.equals(Manifest.permission.READ_EXTERNAL_STORAGE) ||
               permission.equals(Manifest.permission.WRITE_EXTERNAL_STORAGE) ||
               permission.equals(Manifest.permission.READ_MEDIA_IMAGES);
    }
    
    /**
     * Request storage permissions based on Android version
     */
    private void requestStoragePermissions() {
        // Skip requesting altogether if we detect the app already has storage access
        // This direct check helps prevent unnecessary permission requests on some devices
        if (hasEffectiveStoragePermission()) {
            // Just request other non-storage permissions
            PermissionUtils.requestNonStoragePermissions(this);
            return;
        }
        
        // Determine which storage permissions to request based on API level
        String[] storagePermissions;
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Android 13+ uses the more granular READ_MEDIA_IMAGES permission
            storagePermissions = new String[] {Manifest.permission.READ_MEDIA_IMAGES};
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // Android 11+ (API 30+) has scoped storage, only needs READ permission
            storagePermissions = new String[] {Manifest.permission.READ_EXTERNAL_STORAGE};
        } else {
            // Android 10 and below need both READ and WRITE permissions
            storagePermissions = new String[] {
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            };
        }
        
        // Check if any needed permissions remain to be granted
        boolean needToRequest = false;
        for (String permission : storagePermissions) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                needToRequest = true;
                break;
            }
        }
        
        // If permissions need to be requested, request them
        if (needToRequest) {
            requestPermissionLauncher.launch(storagePermissions);
        }
        
        // Also request other permissions using the utility
        PermissionUtils.requestNonStoragePermissions(this);
    }
    
    /**
     * Check if the app effectively has storage access permission already
     * This is a practical check that tests if the app can actually access storage
     */
    private boolean hasEffectiveStoragePermission() {
        // Test directories that should be accessible if storage permission is granted
        File[] testDirs = new File[] {
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM),
            getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        };
        
        // Check if at least one of these directories is accessible
        for (File dir : testDirs) {
            if (dir != null && dir.exists() && dir.canRead()) {
                return true;
            }
        }
        
        // Also check directly with the PackageManager for the most critical permission
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_IMAGES) 
                == PackageManager.PERMISSION_GRANTED;
        } else {
            return ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) 
                == PackageManager.PERMISSION_GRANTED;
        }
    }
    
    /**
     * Show a dialog explaining why storage permissions are needed
     */
    private void showStoragePermissionExplanationDialog() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.permission_required)
                .setMessage("Storage permission is required to access and analyze crop images with AI. " +
                           "Without this permission, the image analysis features will not work properly.")
                .setPositiveButton("Ask Again", (dialog, which) -> {
                    requestStoragePermissions();
                })
                .setNegativeButton("Cancel", (dialog, which) -> {
                    Toast.makeText(this, "Some features may not work without storage permission", 
                              Toast.LENGTH_LONG).show();
                })
                .setCancelable(true)
                .show();
    }
    
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        
        if (requestCode == PERMISSION_REQUEST_CODE) {
            boolean allGranted = true;
            boolean anyStorageDenied = false;
            
            for (int i = 0; i < permissions.length; i++) {
                if (grantResults[i] != PackageManager.PERMISSION_GRANTED) {
                    allGranted = false;
                    
                    if (isStoragePermission(permissions[i])) {
                        anyStorageDenied = true;
                    }
                }
            }
            
            if (!allGranted && anyStorageDenied) {
                // Only show the dialog for storage permissions
                showStoragePermissionExplanationDialog();
            }
        }
    }

    @Override
    public void onBackPressed() {
        finishAffinity();
    }
}