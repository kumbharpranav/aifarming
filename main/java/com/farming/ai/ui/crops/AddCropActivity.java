package com.farming.ai.ui.crops;

import android.Manifest;
import android.app.DatePickerDialog;
import android.app.ProgressDialog;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Parcelable;
import android.provider.MediaStore;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.appcompat.widget.SwitchCompat;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import com.bumptech.glide.Glide;
import com.farming.ai.R;
import com.farming.ai.models.Crop;
import com.farming.ai.utils.GeminiVisionApiHelper;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.android.gms.tasks.CancellationTokenSource;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnTokenCanceledListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ServerValue;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLEncoder;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

// Interface defined outside the class
interface OnLocationReceivedListener {
    void onLocationReceived(@Nullable Location location);
}

public class AddCropActivity extends AppCompatActivity implements OnLocationReceivedListener {
    private EditText etCropName, etVariety, etGrowthStage, etPlantingDate, etNotes, etAge, etExpectedHarvestDate, etFarmArea, etLastWateredTime, etWateringIntervalHours, etSoilType;
    private ImageView ivCropImage;
    private Button btnSubmit, btnAnalyzeWithGemini;
    private GeminiVisionApiHelper geminiHelper;
    private String imagePath;
    private Uri imageUri;
    private Bitmap imageBitmap;
    private DatabaseReference mDatabase;
    private FusedLocationProviderClient fusedLocationClient;
    private ProgressBar progressBar;
    private Toolbar toolbar;
    private double currentLatitude = 0.0;
    private double currentLongitude = 0.0;
    private String pendingImageUrl = null;
    private String userEmail;
    private SwitchCompat swIsWatered, swNeedsAttention;

    private static final int REQUEST_STORAGE_PERMISSION = 100;
    private static final int REQUEST_CAMERA_PERMISSION = 101;
    private static final int REQUEST_PICK_IMAGE = 102;
    private static final int REQUEST_IMAGE_CAPTURE = 103;

    private String encodeEmail(String email) {
        if (email == null) return null;
        return email.replace('.', ',');
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_crop);

        mDatabase = FirebaseDatabase.getInstance().getReference();
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        geminiHelper = new GeminiVisionApiHelper();

        SharedPreferences prefs = getSharedPreferences("AIFarming", MODE_PRIVATE);
        userEmail = prefs.getString("user_email", null);

        if (userEmail == null) {
            Log.e("AddCropActivity", "User email not found in SharedPreferences. Redirecting to login or showing error.");
            Toast.makeText(this, "User session not found. Please log in again.", Toast.LENGTH_LONG).show();
        }

        initViews();
        setupToolbar();

        handleIncomingIntent(getIntent());

        checkAndRequestStoragePermission();
    }

    private void initViews() {
        etCropName = findViewById(R.id.et_crop_name);
        etVariety = findViewById(R.id.et_variety);
        etGrowthStage = findViewById(R.id.et_growth_stage);
        etPlantingDate = findViewById(R.id.et_planting_date);
        etNotes = findViewById(R.id.et_notes);
        etAge = findViewById(R.id.et_age);
        etExpectedHarvestDate = findViewById(R.id.et_expected_harvest_date);
        etFarmArea = findViewById(R.id.et_farm_area);
        etLastWateredTime = findViewById(R.id.et_last_watered_time);
        etWateringIntervalHours = findViewById(R.id.et_watering_interval_hours);
        etSoilType = findViewById(R.id.et_soil_type);
        swIsWatered = (SwitchCompat) findViewById(R.id.sw_is_watered);
        swNeedsAttention = (SwitchCompat) findViewById(R.id.sw_needs_attention);
        ivCropImage = findViewById(R.id.iv_crop_image);
        btnSubmit = findViewById(R.id.btn_submit);
        btnAnalyzeWithGemini = findViewById(R.id.btn_analyze_with_gemini);

        progressBar = findViewById(R.id.progressBar);

        ivCropImage.setOnClickListener(v -> showImageSourceDialog());

        btnSubmit.setOnClickListener(v -> saveCrop());
        btnAnalyzeWithGemini.setOnClickListener(v -> analyzeImageWithGemini());
    }

    private void setupToolbar() {
        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Add New Crop");
        }
    }

    private void handleIncomingIntent(Intent intent) {
        if (intent != null) {
            Uri intentUri = null;
            if (Intent.ACTION_SEND.equals(intent.getAction()) && intent.getType() != null) {
                if (intent.getType().startsWith("image/")) {
                    intentUri = intent.getParcelableExtra(Intent.EXTRA_STREAM);
                }
            } else if (intent.getData() != null) {
                intentUri = intent.getData();
            } else if (intent.hasExtra("imageUri")) {
                intentUri = intent.getParcelableExtra("imageUri");
            }

            if (intentUri != null) {
                Log.d("AddCropActivity", "Received image URI from intent: " + intentUri);
                imageUri = intentUri;
                loadImageFromUri();
                new Handler().postDelayed(() -> {
                    if (imageBitmap != null) {
                        analyzeImageWithGemini();
                    } else {
                        Log.e("AddCropActivity", "Failed to load bitmap from intent URI, cannot analyze.");
                        Toast.makeText(this, "Failed to load image from source.", Toast.LENGTH_SHORT).show();
                    }
                }, 100);
            }
        }
    }

    private void showImageSourceDialog() {
        boolean hasCameraPermission = checkSelfPermission(Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED;

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Select Image Source");
        builder.setItems(new CharSequence[]{"Camera", "Gallery"}, (dialog, which) -> {
            if (which == 0) {
                checkAndRequestCameraPermission();
            } else {
                dispatchPickPictureIntent();
            }
        });
        builder.show();
    }

    private void dispatchTakePictureIntent() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            File photoFile = null;
            try {
                photoFile = createImageFile();
            } catch (IOException ex) {
                Log.e("AddCropActivity", "Error creating image file", ex);
                Toast.makeText(this, "Error creating image file", Toast.LENGTH_SHORT).show();
                return;
            }
            if (photoFile != null) {
                imageUri = FileProvider.getUriForFile(this,
                        getPackageName() + ".fileprovider",
                        photoFile);
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri);
                startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
            }
        }
    }

    private File createImageFile() throws IOException {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(
                imageFileName,  /* prefix */
                ".jpg",         /* suffix */
                storageDir      /* directory */
        );

        return image;
    }

    private void dispatchPickPictureIntent() {
        Intent pickPhotoIntent = new Intent(Intent.ACTION_GET_CONTENT);
        pickPhotoIntent.setType("image/*");
        startActivityForResult(pickPhotoIntent, REQUEST_PICK_IMAGE);
    }

    private void loadImageFromUri() {
        try {
            InputStream imageStream = getContentResolver().openInputStream(imageUri);
            imageBitmap = BitmapFactory.decodeStream(imageStream);
            if (imageBitmap != null) {
                Glide.with(this).load(imageBitmap).into(ivCropImage);
                btnAnalyzeWithGemini.setEnabled(true);
            } else {
                Log.e("AddCropActivity", "Failed to decode bitmap from URI");
                Toast.makeText(this, "Failed to load image.", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Log.e("AddCropActivity", "Error loading image from URI", e);
            Toast.makeText(this, "Error loading image.", Toast.LENGTH_SHORT).show();
        }
    }

    private void analyzeImageWithGemini() {
        if (imageUri == null) {
            Toast.makeText(this, "Please select an image first", Toast.LENGTH_SHORT).show();
            return;
        }

        showLoading("Analyzing image...");

        String prompt = "Analyze the provided crop image. Extract the following details and return ONLY a valid JSON object. " +
                "Keys: name (string), variety (string), age (string, e.g., '3 weeks', 'Mature'), growthStage (string, e.g., 'Seedling', 'Vegetative'), " +
                "plantingDate (string, YYYY-MM-DD), expectedHarvestDate (string, YYYY-MM-DD), farmArea (string, e.g., '2 acres'), " +
                "isWatered (boolean, based on visual health), lastWateredTime (string, YYYY-MM-DD HH:MM, if estimable), " +
                "needsAttention (boolean, if visible stress/disease/pests), wateringIntervalHours (integer), soilType (string), notes (string, leave empty). " +
                "If a value cannot be determined from the image, use an empty string \"\" for strings, 0 for integers, and false for booleans.";

        Bitmap resizedBitmap = resizeBitmap(imageBitmap, 1024);

        Log.d("AddCropActivity", "Resized Bitmap Dimensions: " + resizedBitmap.getWidth() + "x" + resizedBitmap.getHeight());

        geminiHelper.sendGeminiRequest(resizedBitmap, prompt, new GeminiVisionApiHelper.GeminiApiCallback() {
            @Override
            public void onSuccess(String result) {
                runOnUiThread(() -> {
                    hideLoading();
                    Log.d("AddCropActivity", "Gemini Success Result (Raw): " + result);
                    String cleanedResult = result.replace("`", ""); // Basic cleaning

                    // Remove "json" prefix if present
                    if (cleanedResult.startsWith("json")) {
                        cleanedResult = cleanedResult.substring(4).trim();
                    }

                    if (cleanedResult.startsWith("```json")) {
                        cleanedResult = cleanedResult.substring(7);
                    }
                    if (cleanedResult.endsWith("```")) {
                        cleanedResult = cleanedResult.substring(0, cleanedResult.length() - 3);
                    }
                    cleanedResult = cleanedResult.trim();

                    Log.d("AddCropActivity", "Gemini Success Result (Cleaned JSON): " + cleanedResult);

                    try {
                        JSONObject jsonResult = new JSONObject(cleanedResult);
                        Log.d("AddCropActivity", "Parsed JSON object: " + jsonResult.toString());

                        String cropName = jsonResult.optString("name", "");
                        String variety = jsonResult.optString("variety", "");
                        String growthStage = jsonResult.optString("growthStage", "");
                        String age = jsonResult.optString("age", "");
                        String plantingDate = jsonResult.optString("plantingDate", "");
                        String expectedHarvestDate = jsonResult.optString("expectedHarvestDate", "");
                        String farmArea = jsonResult.optString("farmArea", "");
                        boolean isWatered = jsonResult.optBoolean("isWatered", false);
                        String lastWateredTime = jsonResult.optString("lastWateredTime", "");
                        boolean needsAttention = jsonResult.optBoolean("needsAttention", false);
                        int wateringIntervalHours = jsonResult.optInt("wateringIntervalHours", 0);
                        String soilType = jsonResult.optString("soilType", "");
                        String notes = jsonResult.optString("notes", "");

                        Log.d("AddCropActivity", "Parsed Values - Name: " + cropName + ", Variety: " + variety + ", Stage: " + growthStage + ", Age: " + age);
                        Log.d("AddCropActivity", "Parsed Values - Planting: " + plantingDate + ", Harvest: " + expectedHarvestDate + ", Area: " + farmArea);
                        Log.d("AddCropActivity", "Parsed Values - Watered: " + isWatered + ", LastWater: " + lastWateredTime + ", Attention: " + needsAttention);
                        Log.d("AddCropActivity", "Parsed Values - Interval: " + wateringIntervalHours + ", Soil: " + soilType + ", Notes: " + notes);

                        etCropName.setText(cropName);
                        etVariety.setText(variety);
                        etGrowthStage.setText(growthStage);
                        etAge.setText(age);
                        etPlantingDate.setText(plantingDate);
                        etExpectedHarvestDate.setText(expectedHarvestDate);
                        etFarmArea.setText(farmArea);
                        swIsWatered.setChecked(isWatered);
                        etLastWateredTime.setText(lastWateredTime);
                        swNeedsAttention.setChecked(needsAttention);
                        etWateringIntervalHours.setText(wateringIntervalHours > 0 ? String.valueOf(wateringIntervalHours) : "");
                        etSoilType.setText(soilType);
                        etNotes.setText(notes);

                        Toast.makeText(AddCropActivity.this, "Analysis complete!", Toast.LENGTH_SHORT).show();

                    } catch (JSONException e) {
                        Log.e("AddCropActivity", "Error parsing Gemini JSON response: " + cleanedResult, e);
                        Toast.makeText(AddCropActivity.this, "AI analysis succeeded, but failed to parse results.", Toast.LENGTH_LONG).show();
                    }
                });
            }

            @Override
            public void onFailure(Exception e) {
                runOnUiThread(() -> {
                    hideLoading();
                    Log.e("AddCropActivity", "Gemini API Error", e);
                    String errorMessage = "Failed to analyze image.";
                    if (e instanceof com.google.ai.client.generativeai.type.ServerException) {
                        errorMessage = "AI analysis failed due to server overload. Please try again later.";
                    } else if (e.getMessage() != null) {
                        errorMessage += " Error: " + e.getMessage();
                    }
                    Toast.makeText(AddCropActivity.this, errorMessage, Toast.LENGTH_LONG).show();
                });
            }
        });
    }

    private void saveCrop() {
        if (imageUri == null) {
            Toast.makeText(this, "Please select an image", Toast.LENGTH_SHORT).show();
            return;
        }

        if (userEmail == null) {
            Toast.makeText(this, "User session error. Cannot save crop.", Toast.LENGTH_SHORT).show();
            Log.e("AddCropActivity", "Cannot save crop, userEmail is null.");
            return;
        }

        showLoading("Saving crop data...");
        getCurrentLocation(location -> {
            String locationString = "Unknown";
            if (location != null) {
                locationString = getAddressFromLocation(location.getLatitude(), location.getLongitude());
            }
            saveCropData(null, locationString);
        });
    }

    private void saveCropData(String imageUrl, String locationString) {
        if (userEmail == null) {
            SharedPreferences prefs = getSharedPreferences("AIFarming", MODE_PRIVATE);
            userEmail = prefs.getString("user_email", null);
            if (userEmail == null) {
                Toast.makeText(this, "User session invalid. Cannot save crop.", Toast.LENGTH_SHORT).show();
                Log.e("AddCropActivity", "User Email is null. Cannot save crop data.");
                hideLoading();
                return;
            }
        }

        String encodedEmail = encodeEmail(userEmail);
        if (encodedEmail == null) {
            Toast.makeText(this, "Error encoding user email.", Toast.LENGTH_SHORT).show();
            Log.e("AddCropActivity", "Encoded email is null in saveCropData.");
            hideLoading();
            return;
        }

        String cropName = etCropName.getText().toString().trim();
        String variety = etVariety.getText().toString().trim();
        String growthStage = etGrowthStage.getText().toString().trim();
        String plantingDateStr = etPlantingDate.getText().toString().trim();
        String notes = etNotes.getText().toString().trim();
        String age = etAge.getText().toString().trim();
        String expectedHarvestDateStr = etExpectedHarvestDate.getText().toString().trim();
        String farmAreaStr = etFarmArea.getText().toString().trim();
        boolean isWatered = swIsWatered.isChecked();
        String lastWateredTimeStr = etLastWateredTime.getText().toString().trim();
        boolean needsAttention = swNeedsAttention.isChecked();
        String wateringIntervalStr = etWateringIntervalHours.getText().toString().trim();
        String soilType = etSoilType.getText().toString().trim();

        int wateringIntervalHours = 0;
        try {
            if (!wateringIntervalStr.isEmpty()) {
                wateringIntervalHours = Integer.parseInt(wateringIntervalStr);
            }
        } catch (NumberFormatException e) {
            Log.w("AddCropActivity", "Invalid number format for watering interval: " + wateringIntervalStr);
        }

        String cropId = mDatabase.child("crops").child(encodedEmail).push().getKey();

        if (cropId == null) {
            Toast.makeText(this, "Failed to create crop ID", Toast.LENGTH_SHORT).show();
            hideLoading();
            return;
        }

        String cropImageUrl = imageUrl;
        if (cropImageUrl == null || cropImageUrl.isEmpty()) {
            try {
                String encodedName = URLEncoder.encode(cropName + ", plant", "UTF-8");
                cropImageUrl = "https://source.unsplash.com/500x300/?" + encodedName;
                Log.d("AddCropActivity", "Generated Unsplash URL: " + cropImageUrl);
            } catch (Exception e) {
                Log.e("AddCropActivity", "Error encoding crop name for Unsplash URL", e);
                cropImageUrl = null; // Fallback or set a default placeholder URL
            }
        }

        Crop newCrop = new Crop(
                cropId,
                encodedEmail,
                cropName,
                variety,
                growthStage,
                plantingDateStr,
                locationString,
                notes,
                cropImageUrl,
                null, // Use null and handle in toMap()
                age,
                expectedHarvestDateStr,
                farmAreaStr,
                isWatered,
                lastWateredTimeStr,
                needsAttention,
                wateringIntervalHours,
                soilType
        );

        Map<String, Object> cropValues = newCrop.toMap();

        mDatabase.child("crops").child(encodedEmail).child(cropId).setValue(cropValues)
                .addOnSuccessListener(aVoid -> {
                    hideLoading();
                    Toast.makeText(AddCropActivity.this, "Crop added successfully!", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    hideLoading();
                    Toast.makeText(AddCropActivity.this, "Failed to add crop: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private long convertDateStringToMillis(String dateStr) {
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
        try {
            Date date = sdf.parse(dateStr);
            return date != null ? date.getTime() : 0;
        } catch (ParseException e) {
            Log.e("AddCropActivity", "Error parsing date string: " + dateStr, e);
            return 0; // Or handle error appropriately
        }
    }

    private void getCurrentLocation(OnLocationReceivedListener listener) {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider requesting permissions here instead of just logging
            Log.w("AddCropActivity", "Location permission not granted.");
            Toast.makeText(this, "Location permission required to tag crop location.", Toast.LENGTH_SHORT).show();
            listener.onLocationReceived(null); // Pass null location
            return;
        }

        // Use CancellationTokenSource for managing cancellation
        CancellationTokenSource cancellationTokenSource = new CancellationTokenSource();

        // Request current location
        fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, cancellationTokenSource.getToken())
                .addOnCompleteListener(this, new OnCompleteListener<Location>() {
                    @Override
                    public void onComplete(@NonNull Task<Location> task) {
                        if (task.isSuccessful() && task.getResult() != null) {
                            Location location = task.getResult();
                            currentLatitude = location.getLatitude();
                            currentLongitude = location.getLongitude();
                            Log.d("AddCropActivity", "Location fetched: " + currentLatitude + ", " + currentLongitude);
                            listener.onLocationReceived(location);
                        } else {
                            Log.w("AddCropActivity", "Failed to get current location.", task.getException());
                            listener.onLocationReceived(null); // Pass null location on failure
                        }
                    }
                });

        // Optional: Add cancellation logic if needed, e.g., cancel after a timeout
        // handler.postDelayed(() -> cancellationTokenSource.cancel(), TIMEOUT_DURATION);
    }

    private String getAddressFromLocation(double latitude, double longitude) {
        Geocoder geocoder = new Geocoder(this, Locale.getDefault());
        String addressText = "Lat: " + latitude + ", Lon: " + longitude;
        try {
            List<Address> addresses = geocoder.getFromLocation(latitude, longitude, 1);
            if (addresses != null && !addresses.isEmpty()) {
                Address address = addresses.get(0);
                StringBuilder sb = new StringBuilder();
                if (address.getLocality() != null) sb.append(address.getLocality()).append(", ");
                if (address.getAdminArea() != null) sb.append(address.getAdminArea());
                if (sb.length() > 0) {
                    addressText = sb.toString();
                } else if (address.getCountryName() != null) {
                    addressText = address.getCountryName();
                }
            }
        } catch (IOException e) {
            Log.e("AddCropActivity", "Geocoder error", e);
        }
        return addressText;
    }

    @Override
    public void onLocationReceived(@Nullable Location location) {
        hideLoading();
        if (location != null) {
            this.currentLatitude = location.getLatitude();
            this.currentLongitude = location.getLongitude();
            String locationString = "Lat: " + currentLatitude + ", Lon: " + currentLongitude;
            Log.d("AddCropActivity", "Location fetched: " + locationString);

            if (pendingImageUrl != null) {
                saveCropData(pendingImageUrl, locationString);
                pendingImageUrl = null;
            }
        } else {
            Log.w("AddCropActivity", "Failed to get location, saving without precise coordinates.");
            if (pendingImageUrl != null) {
                saveCropData(pendingImageUrl, "Not Available");
                pendingImageUrl = null;
            }
        }
    }

    private void showLoading(String message) {
        if (progressBar != null) {
            progressBar.setVisibility(View.VISIBLE);
        }
    }

    private void hideLoading() {
        if (progressBar != null) {
            progressBar.setVisibility(View.GONE);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private Bitmap resizeBitmap(Bitmap source, int maxDimension) {
        int originalWidth = source.getWidth();
        int originalHeight = source.getHeight();
        int resizedWidth = originalWidth;
        int resizedHeight = originalHeight;

        if (originalHeight > maxDimension || originalWidth > maxDimension) {
            if (originalWidth > originalHeight) {
                resizedWidth = maxDimension;
                resizedHeight = (resizedWidth * originalHeight) / originalWidth;
            } else {
                resizedHeight = maxDimension;
                resizedWidth = (resizedHeight * originalWidth) / originalHeight;
            }
        }

        if (resizedWidth == originalWidth && resizedHeight == originalHeight) {
            return source;
        } else {
            return Bitmap.createScaledBitmap(source, resizedWidth, resizedHeight, true);
        }
    }

    private void checkAndRequestStoragePermission() {
        String permissionNeeded;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            permissionNeeded = Manifest.permission.READ_MEDIA_IMAGES;
        } else {
            permissionNeeded = Manifest.permission.READ_EXTERNAL_STORAGE;
        }

        if (checkSelfPermission(permissionNeeded) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{permissionNeeded}, REQUEST_STORAGE_PERMISSION);
        }
    }

    private void checkAndRequestCameraPermission() {
        if (checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.CAMERA}, REQUEST_CAMERA_PERMISSION);
        } else {
            dispatchTakePictureIntent();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case REQUEST_STORAGE_PERMISSION:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(this, "Storage Permission Granted", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, "Storage permission is required to select images.", Toast.LENGTH_LONG).show();
                }
                break;
            case REQUEST_CAMERA_PERMISSION:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(this, "Camera Permission Granted", Toast.LENGTH_SHORT).show();
                    dispatchTakePictureIntent();
                } else {
                    Toast.makeText(this, "Camera permission is required to take pictures.", Toast.LENGTH_LONG).show();
                }
                break;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            if (requestCode == REQUEST_PICK_IMAGE && data != null && data.getData() != null) {
                imageUri = data.getData();
                loadImageFromUri();
            } else if (requestCode == REQUEST_IMAGE_CAPTURE && imageUri != null) {
                // Image URI is already set from createImageFile
                loadImageFromUri();
            }
        }
    }

    private void showImagePickDialog() {
        // Check for permissions first (Storage/Camera)
        if (!hasStoragePermission()) {
            requestStoragePermission();
            return;
        }
        // Optionally add camera permission check if offering camera
        // if (!hasCameraPermission()) {
        //    requestCameraPermission();
        //    return;
        // }

        // Intent to pick image from gallery
        Intent galleryIntent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        galleryIntent.setType("image/*");
        
        // Optionally, add Camera Intent
        // Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        // File photoFile = null;
        // try {
        //     photoFile = createImageFile(); // Need createImageFile() helper method
        // } catch (IOException ex) {
        //     Log.e("AddCropActivity", "Error creating image file", ex);
        // }
        // if (photoFile != null) {
        //     Uri photoURI = FileProvider.getUriForFile(this,
        //             "com.your.package.fileprovider", // Replace with your FileProvider authority
        //             photoFile);
        //     cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
        // }

        // Create Chooser Intent
        Intent chooserIntent = Intent.createChooser(galleryIntent, "Select Image");
        // Optionally add camera intent to chooser
        // chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, new Intent[] {cameraIntent});

        startActivityForResult(chooserIntent, REQUEST_PICK_IMAGE); // Use existing PICK_IMAGE_REQUEST code
    }

    // Reuse existing permission check/request logic if available
    // Example placeholders (replace with your actual implementations):
    private boolean hasStoragePermission() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
    }

    private void requestStoragePermission() {
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, REQUEST_STORAGE_PERMISSION);
    }

    // Add hasCameraPermission, requestCameraPermission, createImageFile if using camera
    // ... (potentially reuse code from showImageSourceDialog or similar)
}