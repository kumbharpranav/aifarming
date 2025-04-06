package com.farming.ai.ui.pests;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.os.Parcelable;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.farming.ai.R;
import com.farming.ai.adapters.CropAdapter;
import com.farming.ai.models.Crop;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class PestDetectionActivity extends AppCompatActivity {

    private static final String TAG = "PestDetectionActivity";
    private RecyclerView rvCrops;
    private CropAdapter cropAdapter;
    private List<Crop> cropList;
    private TextView tvNoCrops;
    private DatabaseReference databaseReference;
    private SharedPreferences sharedPreferences;
    private String userEmail;

    private Crop selectedCrop;
    private Uri imageUri; // To store the URI from camera

    // ActivityResultLaunchers for Camera and Gallery
    private ActivityResultLauncher<Intent> galleryLauncher;
    private ActivityResultLauncher<Uri> cameraLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pest_detection);

        Toolbar toolbar = findViewById(R.id.toolbar_pest_detection);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> onBackPressed());

        rvCrops = findViewById(R.id.rv_pest_crops);
        tvNoCrops = findViewById(R.id.tv_no_crops_pest);
        rvCrops.setLayoutManager(new LinearLayoutManager(this));
        cropList = new ArrayList<>();
        // Use the version of CropAdapter that doesn't show the image in the list
        // Pass context, list, and the listener method reference
        cropAdapter = new CropAdapter(this, cropList, this::onCropSelected);
        rvCrops.setAdapter(cropAdapter);

        sharedPreferences = getSharedPreferences("AIFarming", Context.MODE_PRIVATE);
        userEmail = sharedPreferences.getString("user_email", null);

        if (userEmail == null) {
            Log.e(TAG, "User email is null. Cannot fetch crops.");
            Toast.makeText(this, "Error: User not logged in.", Toast.LENGTH_LONG).show();
            tvNoCrops.setVisibility(View.VISIBLE);
            rvCrops.setVisibility(View.GONE);
            // Optionally, redirect to login or finish activity
             finish();
            return;
        }

        String userPath = userEmail.replace(".", ",");
        databaseReference = FirebaseDatabase.getInstance().getReference("crops").child(userPath);

        setupResultLaunchers();
        fetchCrops();
    }

    private void setupResultLaunchers() {
        galleryLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        Uri selectedImageUri = result.getData().getData();
                        if (selectedImageUri != null && selectedCrop != null) {
                            startAnalysisActivity(selectedCrop, selectedImageUri);
                        }
                    }
                });

        cameraLauncher = registerForActivityResult(
                new ActivityResultContracts.TakePicture(),
                success -> {
                    if (success && imageUri != null && selectedCrop != null) {
                        // Image captured successfully
                        startAnalysisActivity(selectedCrop, imageUri);
                    } else {
                        // Handle failure or cancellation
                         Toast.makeText(this, "Failed to capture image.", Toast.LENGTH_SHORT).show();
                         imageUri = null; // Reset Uri if capture failed
                         selectedCrop = null; // Reset selected crop
                    }
                });
    }

    private void fetchCrops() {
        databaseReference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                cropList.clear();
                if (dataSnapshot.exists()) {
                    for (DataSnapshot cropSnapshot : dataSnapshot.getChildren()) {
                        try {
                            Crop crop = mapSnapshotToCrop(cropSnapshot);
                            if (crop != null && crop.getName() != null) {
                                cropList.add(crop);
                                Log.d(TAG, "Added crop: " + crop.getName());
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "Error mapping snapshot to Crop, key: " + cropSnapshot.getKey(), e);
                        }
                    }
                    cropAdapter.notifyDataSetChanged();
                    Log.d(TAG, "Finished processing snapshots. Total crops: " + cropList.size());
                } else {
                    Log.d(TAG, "No crops found for user: " + userEmail);
                }

                if (cropList.isEmpty()) {
                    tvNoCrops.setVisibility(View.VISIBLE);
                    rvCrops.setVisibility(View.GONE);
                } else {
                    tvNoCrops.setVisibility(View.GONE);
                    rvCrops.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.e(TAG, "Database error: " + databaseError.getMessage());
                Toast.makeText(PestDetectionActivity.this, "Failed to load crops: " + databaseError.getMessage(), Toast.LENGTH_SHORT).show();
                tvNoCrops.setVisibility(View.VISIBLE);
                rvCrops.setVisibility(View.GONE);
            }
        });
    }

    // Re-implement manual mapping similar to CropMonitoringActivity for consistency
    private Crop mapSnapshotToCrop(DataSnapshot cropSnapshot) {
         if (cropSnapshot == null) return null;
         Crop crop = new Crop();

         // Set basic String fields
         crop.setId(cropSnapshot.getKey()); // Store the Firebase key as ID
         crop.setName(cropSnapshot.child("name").getValue(String.class));
         crop.setVariety(cropSnapshot.child("variety").getValue(String.class));
         crop.setGrowthStage(cropSnapshot.child("growthStage").getValue(String.class));
         crop.setNotes(cropSnapshot.child("notes").getValue(String.class));
         crop.setImageUrl(cropSnapshot.child("imageUrl").getValue(String.class)); // Still get image URL for details

         // Handle 'expectedHarvestDate' (String or Long timestamp)
         Object harvestDateObj = cropSnapshot.child("expectedHarvestDate").getValue();
         String harvestDateStr = null;
         if (harvestDateObj instanceof String) {
             harvestDateStr = (String) harvestDateObj;
         } else if (harvestDateObj instanceof Long) {
             // Convert timestamp (Long) to "yyyy-MM-dd" String
             try {
                 SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                 harvestDateStr = sdf.format(new Date((Long) harvestDateObj));
             } catch (Exception e) {
                 Log.e(TAG, "Error parsing harvest date timestamp", e);
             }
         }
         crop.setExpectedHarvestDate(harvestDateStr);

         // Handle 'age' (Long to String)
         Object ageObj = cropSnapshot.child("age").getValue();
         String ageStr = "0"; // Default value
         if (ageObj instanceof Long) {
             ageStr = String.valueOf(ageObj);
         } else if (ageObj instanceof String) {
             ageStr = (String) ageObj;
         }
         crop.setAge(ageStr);

         // Handle 'plantingDate' (Long/String to formatted String)
         Object plantingDateObj = cropSnapshot.child("plantingDate").getValue();
         String plantingDateStr = "N/A";
         SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
         if (plantingDateObj instanceof Long) {
             plantingDateStr = sdf.format(new Date((Long) plantingDateObj));
         } else if (plantingDateObj instanceof String) {
             plantingDateStr = (String) plantingDateObj; // Assume it's already formatted or handle specific format
         }
         crop.setPlantingDate(plantingDateStr);

         // Handle 'lastWateredTime' (Long/String to formatted String)
         Object lastWateredTimeObj = cropSnapshot.child("lastWateredTime").getValue();
         String lastWateredTimeStr = "N/A";
         SimpleDateFormat timeSdf = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
         if (lastWateredTimeObj instanceof Long) {
             long timestamp = (Long) lastWateredTimeObj;
             if (timestamp > 0) {
                 lastWateredTimeStr = timeSdf.format(new Date(timestamp));
             } else {
                  lastWateredTimeStr = "Never";
             }
         } else if (lastWateredTimeObj instanceof String) {
             lastWateredTimeStr = (String) lastWateredTimeObj;
         }
         crop.setLastWateredTime(lastWateredTimeStr);

         // Handle 'timestamp' (Long)
         Long timestamp = cropSnapshot.child("timestamp").getValue(Long.class);
         crop.setTimestamp(timestamp != null ? timestamp : 0L); // Provide default

         return crop;
    }

    // Method called by CropAdapter when an item is clicked
    private void onCropSelected(Crop crop) {
        Log.d(TAG, "Crop selected: " + crop.getName());
        this.selectedCrop = crop; // Store the selected crop
        showImageSourceDialog();
    }

    private void showImageSourceDialog() {
        if (selectedCrop == null) return; // Should not happen, but safety check

        CharSequence[] items = {"Take Photo", "Choose from Gallery", "Cancel"};
        new MaterialAlertDialogBuilder(this)
                .setTitle("Select Image Source for " + selectedCrop.getName())
                .setItems(items, (dialog, item) -> {
                    if (items[item].equals("Take Photo")) {
                        openCamera();
                    } else if (items[item].equals("Choose from Gallery")) {
                        openGallery();
                    } else if (items[item].equals("Cancel")) {
                        dialog.dismiss();
                        this.selectedCrop = null; // Clear selection on cancel
                    }
                })
                 .setOnCancelListener(dialog -> {
                     this.selectedCrop = null; // Clear selection if dialog is cancelled
                 })
                .show();
    }

    private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        galleryLauncher.launch(intent);
    }

    private void openCamera() {
        ContentValues values = new ContentValues();
        values.put(MediaStore.Images.Media.TITLE, "New Pest Picture");
        values.put(MediaStore.Images.Media.DESCRIPTION, "From the Camera for Pest Detection");
        imageUri = getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);

        if (imageUri != null) {
            cameraLauncher.launch(imageUri);
        } else {
            Toast.makeText(this, "Failed to prepare camera.", Toast.LENGTH_SHORT).show();
        }
    }

    // New method to start the analysis activity
    private void startAnalysisActivity(Crop crop, Uri imageUri) {
        Log.d(TAG, "Starting PestAnalysisActivity for crop: " + crop.getName() + " with image: " + imageUri.toString());
        Intent intent = new Intent(this, PestAnalysisActivity.class);
        intent.putExtra("SELECTED_CROP", (Parcelable) crop); // Explicit cast
        intent.putExtra("IMAGE_URI", imageUri.toString());
        startActivity(intent);

        // Reset selections after launching
        this.selectedCrop = null;
        this.imageUri = null;
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}
