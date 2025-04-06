package com.farming.ai.ui.crops;

import android.app.AlertDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.bumptech.glide.Glide;
import com.farming.ai.R;
import com.farming.ai.models.Crop;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class CropDetailActivity extends AppCompatActivity {

    private Crop crop;
    private ImageView ivCropImage;
    private TextView tvCropName, tvCropVariety, tvPlantingDate, tvHarvestDate;
    private EditText etNotes;
    private Button btnSaveNotes, btnDeleteCrop;
    
    private DatabaseReference mDatabase;
    private String userId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_crop_detail);
        
        // Initialize SharedPreferences
        SharedPreferences prefs = getSharedPreferences("AIFarming", MODE_PRIVATE);
        userId = prefs.getString("user_id", null);
        
        // Initialize Firebase Database reference
        mDatabase = FirebaseDatabase.getInstance().getReference();
        
        // Set up toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle("Crop Details");
        
        // Initialize UI components
        ivCropImage = findViewById(R.id.ivCropImage);
        tvCropName = findViewById(R.id.tvCropName);
        tvCropVariety = findViewById(R.id.tvCropVariety);
        tvPlantingDate = findViewById(R.id.tvPlantingDate);
        tvHarvestDate = findViewById(R.id.tvHarvestDate);
        etNotes = findViewById(R.id.etNotes);
        btnSaveNotes = findViewById(R.id.btnSaveNotes);
        btnDeleteCrop = findViewById(R.id.btnDeleteCrop);
        
        // Get crop from intent
        if (getIntent().hasExtra("crop")) {
            crop = (Crop) getIntent().getSerializableExtra("crop");
            displayCropDetails();
        } else {
            Toast.makeText(this, "Error loading crop details", Toast.LENGTH_SHORT).show();
            finish();
        }
        
        // Set up button click listeners
        btnSaveNotes.setOnClickListener(view -> saveNotes());
        btnDeleteCrop.setOnClickListener(view -> showDeleteConfirmDialog());
    }
    
    private void displayCropDetails() {
        // Set crop name and variety
        tvCropName.setText(crop.getName() != null ? crop.getName() : "N/A");
        tvCropVariety.setText(crop.getVariety());
        
        // Set planting and harvest dates
        SimpleDateFormat parseFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        SimpleDateFormat displayFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());

        String plantingDateStr = crop.getPlantingDate();
        if (plantingDateStr != null && !plantingDateStr.equals("N/A")) {
            try {
                Date plantingDate = parseFormat.parse(plantingDateStr);
                tvPlantingDate.setText(displayFormat.format(plantingDate));
            } catch (ParseException e) {
                tvPlantingDate.setText("Invalid Date"); // Handle parsing error
                // Log.e("CropDetailActivity", "Error parsing planting date: " + plantingDateStr, e);
            }
        } else {
            tvPlantingDate.setText("Not set");
        }

        String harvestDateStr = crop.getExpectedHarvestDate();
        if (harvestDateStr != null && !harvestDateStr.isEmpty() && !harvestDateStr.equals("N/A")) { 
            try {
                Date harvestDate = parseFormat.parse(harvestDateStr);
                tvHarvestDate.setText(displayFormat.format(harvestDate));
            } catch (ParseException e) {
                tvHarvestDate.setText("Invalid Date"); // Handle parsing error
                // Log.e("CropDetailActivity", "Error parsing harvest date: " + harvestDateStr, e);
            }
        } else {
            tvHarvestDate.setText("Not set");
        }

        // Load crop image using Glide
        if (crop.getImageUrl() != null && !crop.getImageUrl().isEmpty()) {
            Glide.with(this)
                    .load(crop.getImageUrl())
                    .placeholder(R.drawable.ic_crop_placeholder)
                    .error(R.drawable.ic_crop_placeholder)
                    .into(ivCropImage);
        } else {
            ivCropImage.setImageResource(R.drawable.ic_crop_placeholder);
        }
    }
    
    private void saveNotes() {
        if (userId == null || crop == null) {
            return;
        }
        
        String notes = etNotes.getText().toString().trim();
        
        // Update notes in the database
        mDatabase.child("crops").child(userId).child(crop.getId()).child("notes").setValue(notes)
                .addOnSuccessListener(aVoid -> 
                    Toast.makeText(this, "Notes saved successfully", Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e -> 
                    Toast.makeText(this, "Failed to save notes: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }
    
    private void showDeleteConfirmDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Delete Crop")
                .setMessage("Are you sure you want to delete this crop? This action cannot be undone.")
                .setPositiveButton("Delete", (dialog, which) -> deleteCrop())
                .setNegativeButton("Cancel", null)
                .show();
    }
    
    private void deleteCrop() {
        if (userId == null || crop == null) {
            return;
        }
        
        // Delete crop from database
        mDatabase.child("crops").child(userId).child(crop.getId()).removeValue()
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Crop deleted successfully", Toast.LENGTH_SHORT).show();
                    finish(); // Close the activity after deletion
                })
                .addOnFailureListener(e -> 
                    Toast.makeText(this, "Failed to delete crop: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}