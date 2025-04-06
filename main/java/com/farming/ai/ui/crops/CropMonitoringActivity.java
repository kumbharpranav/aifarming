package com.farming.ai.ui.crops;

import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;  // Add this import
import android.os.Bundle;
import android.os.Environment;  // Add this import
import android.provider.MediaStore;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.farming.ai.R;
import com.farming.ai.adapters.CropAdapter;
import com.farming.ai.models.Crop;
import com.farming.ai.ui.auth.LoginActivity;  // Add this import
import com.farming.ai.utils.GeminiApiUtils;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class CropMonitoringActivity extends AppCompatActivity {

    private static final int REQUEST_IMAGE_CAPTURE = 1;
    private static final int REQUEST_PICK_IMAGE = 2;
    private String currentPhotoPath;

    private RecyclerView recyclerView;
    private TextView emptyView;
    private CropAdapter adapter;
    private List<Crop> cropList;
    private View progressBar;
    
    private SharedPreferences prefs;
    private String userId;
    private String email;
    private DatabaseReference mDatabase;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_crop_monitoring);
        
        // Set up toolbar with proper theme
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(R.string.crop_monitoring);
        }
        
        // Initialize SharedPreferences
        prefs = getSharedPreferences("AIFarming", MODE_PRIVATE);
        if (!prefs.getBoolean("is_logged_in", false)) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        userId = prefs.getString("user_id", null);
        email = prefs.getString("user_email", null);

        // Initialize Firebase Database
        mDatabase = FirebaseDatabase.getInstance().getReference();
        
        // Initialize UI components
        recyclerView = findViewById(R.id.recycler_view);
        emptyView = findViewById(R.id.tv_empty_list);
        progressBar = findViewById(R.id.progress_bar);
        
        // Initialize crop list and adapter
        cropList = new ArrayList<>();
        adapter = new CropAdapter(this, cropList, null);
        
        // Set up RecyclerView
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);
        
        // Initialize UI
        initUI();
        
        // Load crops from database
        loadCrops();
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        // Reload crops when activity is resumed
        loadCrops();
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
    
    private void loadCrops() {
        if (userId == null) return;
        if (email != null) {
            email = email.replace('.', ',');
        }
        
        DatabaseReference cropsRef = mDatabase.child("crops").child(email);
        
        progressBar.setVisibility(View.VISIBLE);
        cropsRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                cropList.clear();
                Log.d("CropMonitoring", "Data changed. Snapshot exists: " + dataSnapshot.exists());
                if (!dataSnapshot.exists()) {
                    Log.w("CropMonitoring", "No data found at path: " + cropsRef.toString());
                }
                for (DataSnapshot cropSnapshot : dataSnapshot.getChildren()) {
                    try {
                        // Manual mapping instead of cropSnapshot.getValue(Crop.class);
                        Crop crop = new Crop();
                        crop.setId(cropSnapshot.getKey());

                        // Safely get values, providing defaults or logging if necessary
                        crop.setUserEmail(cropSnapshot.child("userEmail").getValue(String.class));
                        crop.setName(cropSnapshot.child("name").getValue(String.class));
                        crop.setVariety(cropSnapshot.child("variety").getValue(String.class));
                        crop.setGrowthStage(cropSnapshot.child("growthStage").getValue(String.class));

                        // Define a date format
                        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());

                        // Handle plantingDate (might be Long or String)
                        Object plantingDateObj = cropSnapshot.child("plantingDate").getValue();
                        String plantingDateStr = "N/A";
                        if (plantingDateObj instanceof Long) {
                            // Convert Long timestamp to a readable date string
                            plantingDateStr = sdf.format(new Date((Long) plantingDateObj));
                        } else if (plantingDateObj instanceof String) {
                            plantingDateStr = (String) plantingDateObj;
                        }
                        crop.setPlantingDate(plantingDateStr);

                        // Handle age (might be Long or String/Object)
                        Object ageObj = cropSnapshot.child("age").getValue();
                        String ageStr = "N/A";
                        if (ageObj instanceof Long) {
                            ageStr = String.valueOf(ageObj);
                        } else if (ageObj instanceof String) {
                            ageStr = (String) ageObj;
                        } else if (ageObj != null) { // Handle other unexpected types if necessary
                            ageStr = ageObj.toString();
                        }
                        crop.setAge(ageStr);

                        crop.setLocation(cropSnapshot.child("location").getValue(String.class));
                        crop.setNotes(cropSnapshot.child("notes").getValue(String.class));
                        crop.setImageUrl(cropSnapshot.child("imageUrl").getValue(String.class));
                        crop.setTimestamp(cropSnapshot.child("timestamp").getValue(Long.class));

                        // Define a date format including time
                        SimpleDateFormat timeSdf = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());

                        // Handle lastWateredTime (might be Long or String)
                        Object lastWateredTimeObj = cropSnapshot.child("lastWateredTime").getValue();
                        String lastWateredTimeStr = "N/A";
                        if (lastWateredTimeObj instanceof Long) {
                            // Format timestamp including time
                            long timestamp = (Long) lastWateredTimeObj;
                            if (timestamp > 0) { // Avoid formatting '0'
                                lastWateredTimeStr = timeSdf.format(new Date(timestamp));
                            } else {
                                lastWateredTimeStr = "Never";
                            }
                        } else if (lastWateredTimeObj instanceof String) {
                            lastWateredTimeStr = (String) lastWateredTimeObj;
                        }
                        crop.setLastWateredTime(lastWateredTimeStr);

                        Boolean isWatered = cropSnapshot.child("isWatered").getValue(Boolean.class);
                        crop.setWatered(isWatered != null ? isWatered : false); // Handle potential null

                        Boolean needsAttention = cropSnapshot.child("needsAttention").getValue(Boolean.class);
                        crop.setNeedsAttention(needsAttention != null ? needsAttention : false); // Handle potential null

                        Integer wateringIntervalHours = cropSnapshot.child("wateringIntervalHours").getValue(Integer.class);
                        crop.setWateringIntervalHours(wateringIntervalHours != null ? wateringIntervalHours : 0); // Handle potential null

                        crop.setSoilType(cropSnapshot.child("soilType").getValue(String.class));

                        crop.setExpectedHarvestDate(cropSnapshot.child("expectedHarvestDate").getValue(String.class));
                        crop.setFarmArea(cropSnapshot.child("farmArea").getValue(String.class));

                        if (crop.getName() != null) { // Basic check if crop data is valid
                            cropList.add(crop);
                            Log.d("CropMonitoring", "Added crop: " + crop.getName());
                        } else {
                            Log.w("CropMonitoring", "Skipped crop with null name or critical field, key: " + cropSnapshot.getKey());
                        }
                    } catch (Exception e) {
                        // Log detailed error, avoiding direct getValue(Crop.class) which might trigger the exception seen before.
                        Log.e("CropMonitoring", "Manual mapping failed for snapshot key: " + cropSnapshot.getKey() + ", path: " + cropSnapshot.getRef().toString(), e);
                        // Log potentially problematic fields
                        Log.e("CropMonitoring", "Value for 'age': " + cropSnapshot.child("age").getValue() + " (Actual Type: " + (cropSnapshot.child("age").getValue() != null ? cropSnapshot.child("age").getValue().getClass().getSimpleName() : "null") + ")");
                        Log.e("CropMonitoring", "Value for 'plantingDate': " + cropSnapshot.child("plantingDate").getValue() + " (Actual Type: " + (cropSnapshot.child("plantingDate").getValue() != null ? cropSnapshot.child("plantingDate").getValue().getClass().getSimpleName() : "null") + ")");
                        Log.e("CropMonitoring", "Value for 'lastWateredTime': " + cropSnapshot.child("lastWateredTime").getValue() + " (Actual Type: " + (cropSnapshot.child("lastWateredTime").getValue() != null ? cropSnapshot.child("lastWateredTime").getValue().getClass().getSimpleName() : "null") + ")");
                        // Add logs for other string fields if needed
                    }
                }

                Log.d("CropMonitoring", "Finished processing snapshots. Total crops in list: " + cropList.size());

                progressBar.setVisibility(View.GONE);
                adapter.notifyDataSetChanged();
                
                // Update empty view visibility
                if (cropList.isEmpty()) {
                    recyclerView.setVisibility(View.GONE);
                    emptyView.setVisibility(View.VISIBLE);
                } else {
                    recyclerView.setVisibility(View.VISIBLE);
                    emptyView.setVisibility(View.GONE);
                }
            }
            
            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                progressBar.setVisibility(View.GONE);
                Snackbar.make(findViewById(android.R.id.content), 
                    "Error loading crops: " + databaseError.getMessage(), 
                    Snackbar.LENGTH_LONG).show();
            }
        });
    }

    private void initUI() {
        FloatingActionButton fabAddCrop = findViewById(R.id.fab_add_crop);
        fabAddCrop.setOnClickListener(v -> {
            Intent intent = new Intent(CropMonitoringActivity.this, AddCropActivity.class);
            startActivity(intent);
        });
    }

    private void showAddCropDialog() {
        String[] options = {"Take Photo", "Choose from Gallery", "Cancel"};
        
        new AlertDialog.Builder(this)
            .setTitle("Add New Crop")
            .setItems(options, (dialog, which) -> {
                switch (which) {
                    case 0: // Take Photo
                        try {
                            Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                            if (cameraIntent.resolveActivity(getPackageManager()) != null) {
                                File photoFile = createImageFile();
                                if (photoFile != null) {
                                    Uri photoUri = FileProvider.getUriForFile(this,
                                        getApplicationContext().getPackageName() + ".fileprovider",
                                        photoFile);
                                    cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoUri);
                                    startActivityForResult(cameraIntent, REQUEST_IMAGE_CAPTURE);
                                }
                            }
                        } catch (IOException e) {
                            Toast.makeText(this, "Error creating image file", Toast.LENGTH_SHORT).show();
                        }
                        break;
                    case 1: // Choose from Gallery
                        Intent galleryIntent = new Intent(Intent.ACTION_PICK,
                            MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                        startActivityForResult(galleryIntent, REQUEST_PICK_IMAGE);
                        break;
                    case 2: // Cancel
                        dialog.dismiss();
                        break;
                }
            })
            .show();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            String imagePath = null;
            if (requestCode == REQUEST_IMAGE_CAPTURE) {
                imagePath = currentPhotoPath;
            } else if (requestCode == REQUEST_PICK_IMAGE && data != null) {
                Uri selectedImage = data.getData();
                imagePath = getRealPathFromURI(selectedImage);
            }

            if (imagePath != null) {
                analyzeAndStartAddCrop(imagePath);
            }
        }
    }

    private void analyzeAndStartAddCrop(String imagePath) {
        ProgressDialog progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Analyzing image...");
        progressDialog.show();
        
        // Convert file path to content URI using FileProvider (safer for MIUI security)
        Uri imageUri = null;
        try {
            File imageFile = new File(imagePath);
            if (imageFile.exists()) {
                imageUri = FileProvider.getUriForFile(
                    this,
                    getPackageName() + ".fileprovider",
                    imageFile
                );
            }
        } catch (Exception e) {
            Log.e("CropMonitoring", "Error creating URI from file: " + e.getMessage());
        }
        
        // Store the URI for use in the callbacks
        final Uri finalImageUri = imageUri;
        
        GeminiApiUtils.analyzeCropImage(this, imagePath, new GeminiApiUtils.GeminiCallback() {
            @Override
            public void onSuccess(String result) {
                progressDialog.dismiss();
                Intent addCropIntent = new Intent(CropMonitoringActivity.this, AddCropActivity.class);
                addCropIntent.putExtra("analysis_result", result);
                
                // Pass both URI and file path, with URI as the preferred method
                if (finalImageUri != null) {
                    addCropIntent.putExtra("image_uri", finalImageUri.toString());
                    // Grant read permission to the receiving activity
                    addCropIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                }
                
                // Keep the image path as fallback
                addCropIntent.putExtra("image_path", imagePath);
                
                startActivity(addCropIntent);
            }

            @Override
            public void onError(String error) {
                progressDialog.dismiss();
                Toast.makeText(CropMonitoringActivity.this, "Failed to analyze image: " + error,
                    Toast.LENGTH_SHORT).show();
                Intent addCropIntent = new Intent(CropMonitoringActivity.this, AddCropActivity.class);
                
                // Pass both URI and file path, with URI as the preferred method
                if (finalImageUri != null) {
                    addCropIntent.putExtra("image_uri", finalImageUri.toString());
                    // Grant read permission to the receiving activity
                    addCropIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                }
                
                // Keep the image path as fallback
                addCropIntent.putExtra("image_path", imagePath);
                
                startActivity(addCropIntent);
            }
        });
    }

    private File createImageFile() throws IOException {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(imageFileName, ".jpg", storageDir);
        currentPhotoPath = image.getAbsolutePath();
        return image;
    }

    private String getRealPathFromURI(Uri uri) {
        String[] projection = {MediaStore.Images.Media.DATA};
        var cursor = getContentResolver().query(uri, projection, null, null, null);
        if (cursor == null) return null;
        
        int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
        cursor.moveToFirst();
        String path = cursor.getString(column_index);
        cursor.close();
        return path;
    }
}