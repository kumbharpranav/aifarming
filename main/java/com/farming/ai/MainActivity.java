package com.farming.ai;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.Toolbar;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.farming.ai.models.VideoItem;
import com.farming.ai.ui.auth.WelcomeActivity;
import com.farming.ai.ui.crops.AddCropActivity;
import com.farming.ai.ui.crops.CropMonitoringActivity;
import com.farming.ai.ui.notifications.NotificationsActivity;
import com.farming.ai.ui.pests.PestDetectionActivity;
import com.farming.ai.ui.profile.UserProfileActivity;
import com.farming.ai.ui.resources.ResourceOptimizationActivity;
import com.farming.ai.ui.settings.SettingsActivity;
import com.farming.ai.ui.videos.VideoLibraryActivity;
import com.farming.ai.utils.NotificationUtils;
import com.farming.ai.utils.WeatherUtils;
import com.farming.ai.utils.YouTubeApiUtils;
import com.farming.ai.utils.GeminiApiUtils;
import com.google.android.material.card.MaterialCardView;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.List;

import androidx.dynamicanimation.animation.SpringAnimation;
import androidx.dynamicanimation.animation.SpringForce;
import com.farming.ai.ui.videos.VideoLibraryAdapter; 
import com.farming.ai.ui.base.BaseActivity;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import org.json.JSONObject; 
import com.farming.ai.utils.WeatherIconMapper;
import androidx.core.content.ContextCompat;
import android.provider.MediaStore;
import android.net.Uri;
import android.widget.Toast;
import androidx.core.content.FileProvider;
import java.io.File;
import java.io.IOException;
import android.os.Environment;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import android.app.ProgressDialog;

public class MainActivity extends BaseActivity {

    private TextView tvWeather, tvCropStatus, tvTips;
    private CardView cropMonitoringCard, pestDetectionCard, resourceOptimizationCard, 
                   videoLibraryCard, settingsCard, cardDashboard;
    private View cropStatusIndicator;
    private DatabaseReference mDatabase;
    private SharedPreferences prefs;
    private String userId, userEmail;
    private RecyclerView rvVideos;
    private View videoLoadingProgress;
    private TextView tvNoVideos;
    private Button btnRefreshTips;
    private FloatingActionButton fabAddCrop;
    private static final int REQUEST_IMAGE_CAPTURE = 1;
    private static final int REQUEST_PICK_IMAGE = 2;
    private String currentPhotoPath;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize SharedPreferences
        prefs = getSharedPreferences("AIFarming", MODE_PRIVATE);

        // Check if session exists
        if (!isLoggedIn()) {
            startActivity(new Intent(this, WelcomeActivity.class));
            finish();
            return;
        }

        // Get user details from session
        userId = prefs.getString("user_id", null);
        userEmail = prefs.getString("user_email", null);

        // Set up toolbar with proper theme
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(R.string.app_name);
        }
        
        // Initialize Firebase Database
        mDatabase = FirebaseDatabase.getInstance().getReference();
        
        // Initialize UI elements
        initUI();
        
        // Load weather data
        loadWeatherData();
        
        // Load crop status
        loadCropStatus();
        
        // Initialize video views
        rvVideos = findViewById(R.id.rv_videos);
        videoLoadingProgress = findViewById(R.id.video_loading_progress);
        tvNoVideos = findViewById(R.id.tv_no_videos);
        
        // Load videos
        loadYouTubeVideos();
        
        // Add smooth animations to cards
        setupCardAnimations();
        
        setupBottomNavigation();
        setupFab();
        
        // Check crop status
        checkCropStatus(userEmail);
        
        cardDashboard = findViewById(R.id.card_dashboard);
        cardDashboard.setOnClickListener(v -> {
            // Handle click event
        });
    }
    
    private boolean isLoggedIn() {
        return prefs.getBoolean("is_logged_in", false) && 
               prefs.getString("user_id", null) != null;
    }
    
    private void logout() {
        // Clear session
        prefs.edit()
            .remove("user_id")
            .remove("user_email")
            .remove("user_name")
            .remove("is_logged_in")
            .apply();
        
        // Go to Welcome screen
        startActivity(new Intent(MainActivity.this, WelcomeActivity.class));
        finish();
    }
    
    private void initUI() {
        // Initialize TextViews
        tvWeather = findViewById(R.id.tv_weather);
        tvCropStatus = findViewById(R.id.tv_crop_status);
        cropStatusIndicator = findViewById(R.id.crop_status_indicator);
        
        // Initialize Cards
        cropMonitoringCard = findViewById(R.id.card_crop_monitoring);
        pestDetectionCard = findViewById(R.id.card_pest_detection);
        resourceOptimizationCard = findViewById(R.id.card_resource_optimization);
        videoLibraryCard = findViewById(R.id.card_video_library);
        settingsCard = findViewById(R.id.card_settings);
        cardDashboard = findViewById(R.id.card_dashboard);

        // Set up card click listeners
        cropMonitoringCard.setOnClickListener(v -> startActivity(
                new Intent(MainActivity.this, CropMonitoringActivity.class)));
        
        pestDetectionCard.setOnClickListener(v -> startActivity(
                new Intent(MainActivity.this, PestDetectionActivity.class)));
        
        resourceOptimizationCard.setOnClickListener(v -> startActivity(
                new Intent(MainActivity.this, ResourceOptimizationActivity.class)));
        
        videoLibraryCard.setOnClickListener(v -> startActivity(
                new Intent(MainActivity.this, VideoLibraryActivity.class)));
        
        settingsCard.setOnClickListener(v -> startActivity(
                new Intent(MainActivity.this, SettingsActivity.class)));

        cardDashboard.setOnClickListener(v -> {
            // Handle click event for dashboard
        });
        
        tvTips = findViewById(R.id.tv_tips);
        btnRefreshTips = findViewById(R.id.btn_refresh_tips);
        btnRefreshTips.setOnClickListener(v -> loadAgricultureTips());
        
        // Load initial tips
        loadAgricultureTips();
    }
    
    private void loadWeatherData() {
        WeatherUtils.getWeatherData(this, data -> {
            if (data != null) {
                tvWeather.setText(data.getTemperature() + "°C | " + data.getCondition());
                
                // Set weather icon
                ImageView weatherIcon = findViewById(R.id.iv_weather);
                weatherIcon.setImageResource(WeatherIconMapper.getWeatherIcon(data.getCondition()));
            } else {
                tvWeather.setText("Weather data unavailable");
            }
        });
    }
    
    private void loadCropStatus() {
        if (userId == null) return;
        
        mDatabase.child("crops").child(userId).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists() && dataSnapshot.getChildrenCount() > 0) {
                    // Check if any crops need attention
                    boolean needsAttention = false;
                    for (DataSnapshot cropSnapshot : dataSnapshot.getChildren()) {
                        if (cropSnapshot.child("needsAttention").exists() && 
                                (boolean) cropSnapshot.child("needsAttention").getValue()) {
                            needsAttention = true;
                            break;
                        }
                    }
                    
                    // Update UI based on crop status
                    if (needsAttention) {
                        cropStatusIndicator.setBackgroundColor(getResources().getColor(R.color.light_red, null));
                        tvCropStatus.setText("Some crops need attention!");
                        // Send notification
                        NotificationUtils.sendCropAlert(MainActivity.this, 
                                "Crop Alert", "Some of your crops need attention!", 1);
                    } else {
                        cropStatusIndicator.setBackgroundColor(getResources().getColor(R.color.light_green, null));
                        tvCropStatus.setText("All crops are doing well");
                    }
                } else {
                    tvCropStatus.setText("No crops added yet");
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                tvCropStatus.setText("Failed to load crop status");
            }
        });
    }
    
    private void loadYouTubeVideos() {
        videoLoadingProgress.setVisibility(View.VISIBLE);
        tvNoVideos.setVisibility(View.GONE);
        
        YouTubeApiUtils.fetchAgricultureVideos((videos, shorts) -> {
            videoLoadingProgress.setVisibility(View.GONE);
            
            if (videos.isEmpty() && shorts.isEmpty()) {
                tvNoVideos.setVisibility(View.VISIBLE);
                return;
            }

            // Setup videos section with proper adapter
            RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(this);
            rvVideos.setLayoutManager(layoutManager);
            rvVideos.setAdapter(new VideoLibraryAdapter(videos, videoId -> {
                Intent intent = new Intent(MainActivity.this, VideoLibraryActivity.class);
                intent.putExtra("videoId", videoId);
                startActivity(intent);
            }));
        });
    }
    
    private void loadAgricultureTips() {
        if (userId != null) {
            mDatabase.child("crops").child(userId).get().addOnSuccessListener(snapshot -> {
                StringBuilder cropTypes = new StringBuilder();
                for (DataSnapshot crop : snapshot.getChildren()) {
                    cropTypes.append(crop.child("name").getValue(String.class)).append(",");
                }
                
                String prompt = cropTypes.length() > 0 
                    ? "Give me a practical farming tip for growing " + cropTypes.toString()
                    : "Give me a general farming tip";
                
                GeminiApiUtils.analyzeCropData(this, prompt, new GeminiApiUtils.GeminiCallback() {
                    @Override
                    public void onSuccess(String result) {
                        tvTips.setText(result);
                    }

                    @Override
                    public void onError(String error) {
                        tvTips.setText("Could not load tips. Try again later.");
                    }
                });
            });
        }
    }
    
    private void setupCardAnimations() {
        CardView[] cards = {
            cropMonitoringCard,
            pestDetectionCard,
            resourceOptimizationCard,
            videoLibraryCard,
            settingsCard
        };

        for (CardView card : cards) {
            // Scale animation
            SpringAnimation scaleX = new SpringAnimation(card, SpringAnimation.SCALE_X, 1f);
            SpringAnimation scaleY = new SpringAnimation(card, SpringAnimation.SCALE_Y, 1f);
            
            SpringForce springForce = new SpringForce()
                .setStiffness(SpringForce.STIFFNESS_MEDIUM)
                .setDampingRatio(SpringForce.DAMPING_RATIO_MEDIUM_BOUNCY)
                .setFinalPosition(1f);
            
            scaleX.setSpring(springForce);
            scaleY.setSpring(springForce);
            
            card.setOnTouchListener((v, event) -> {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        card.setPressed(true);
                        scaleX.animateToFinalPosition(0.95f);
                        scaleY.animateToFinalPosition(0.95f);
                        break;
                    case MotionEvent.ACTION_UP:
                    case MotionEvent.ACTION_CANCEL:
                        card.setPressed(false);
                        scaleX.animateToFinalPosition(1f);
                        scaleY.animateToFinalPosition(1f);
                        break;
                }
                return false;
            });
        }
    }
    
    // Video Adapter class
    private class VideoAdapter extends RecyclerView.Adapter<VideoAdapter.VideoViewHolder> {
        private final List<VideoItem> videos;

        VideoAdapter(List<VideoItem> videos) {
            this.videos = videos;
        }

        @NonNull
        @Override
        public VideoViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = getLayoutInflater().inflate(R.layout.item_video, parent, false);
            return new VideoViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull VideoViewHolder holder, int position) {
            VideoItem video = videos.get(position);
            holder.tvTitle.setText(video.getTitle());
            Glide.with(MainActivity.this)
                    .load(video.getThumbnailUrl())
                    .into(holder.ivThumbnail);
            
            holder.cardView.setOnClickListener(v -> {
                Intent intent = new Intent(MainActivity.this, VideoLibraryActivity.class);
                intent.putExtra("videoId", video.getVideoId());
                startActivity(intent);
            });
        }

        @Override
        public int getItemCount() {
            return videos.size();
        }

        class VideoViewHolder extends RecyclerView.ViewHolder {
            MaterialCardView cardView;
            ImageView ivThumbnail;
            TextView tvTitle;

            VideoViewHolder(@NonNull View itemView) {
                super(itemView);
                cardView = (MaterialCardView) itemView;
                ivThumbnail = itemView.findViewById(R.id.iv_thumbnail);
                tvTitle = itemView.findViewById(R.id.tv_title);
            }
        }
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        
        if (id == R.id.action_notifications) {
            startActivity(new Intent(MainActivity.this, NotificationsActivity.class));
            return true;
        } else if (id == R.id.action_profile) {
            startActivity(new Intent(MainActivity.this, UserProfileActivity.class));
            return true;
        } else if (id == R.id.action_logout) {
            logout();
            return true;
        }
        
        return super.onOptionsItemSelected(item);
    }

    private void setupFab() {
        fabAddCrop = findViewById(R.id.fab);
        fabAddCrop.setOnClickListener(v -> startActivity(new Intent(MainActivity.this, AddCropActivity.class)));
    }

    private void setupBottomNavigation() {
        BottomNavigationView bottomNav = findViewById(R.id.bottom_navigation);
        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_home) {
                return true;
            } else if (id == R.id.nav_videos) {
                startActivity(new Intent(this, VideoLibraryActivity.class));
                return true;
            }
            return false;
        });
    }

    private void showAddCropDialog() {
        String[] options = {"Take Photo", "Choose from Gallery", "Cancel"};
        
        new androidx.appcompat.app.AlertDialog.Builder(this)
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
        // Show loading dialog
        ProgressDialog progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Analyzing crop image...");
        progressDialog.show();

        GeminiApiUtils.analyzeCropImage(this, imagePath, new GeminiApiUtils.GeminiCallback() {
            @Override
            public void onSuccess(String result) {
                progressDialog.dismiss();
                Intent addCropIntent = new Intent(MainActivity.this, AddCropActivity.class);
                addCropIntent.putExtra("analysis_result", result);
                addCropIntent.putExtra("image_path", imagePath);
                startActivity(addCropIntent);
            }

            @Override
            public void onError(String error) {
                progressDialog.dismiss();
                Toast.makeText(MainActivity.this, "Failed to analyze image: " + error, 
                    Toast.LENGTH_SHORT).show();
                Intent addCropIntent = new Intent(MainActivity.this, AddCropActivity.class);
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

    private void checkCropStatus(String userEmail) {
        if (userEmail == null || userEmail.isEmpty()) {
            tvCropStatus.setText(R.string.crop_status_login_required);
            cropStatusIndicator.setBackgroundColor(ContextCompat.getColor(this, android.R.color.darker_gray));
            return;
        }

        // Encode email for Firebase path
        String encodedEmail = userEmail.replace(".", ",");
        DatabaseReference cropsRef = FirebaseDatabase.getInstance()
                .getReference("crops")
                .child(encodedEmail);

        tvCropStatus.setText(R.string.crop_status_loading);

        cropsRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists() && dataSnapshot.hasChildren()) {
                    // Check if any crops need attention
                    boolean needsAttention = false;
                    for (DataSnapshot cropSnapshot : dataSnapshot.getChildren()) {
                        Boolean attention = cropSnapshot.child("needsAttention").getValue(Boolean.class);
                        if (attention != null && attention) {
                            needsAttention = true;
                            break;
                        }
                    }

                    if (needsAttention) {
                        tvCropStatus.setText(R.string.crop_status_attention);
                        cropStatusIndicator.setBackgroundColor(getResources().getColor(R.color.light_red, null));
                    } else {
                        tvCropStatus.setText(R.string.crop_status_ok);
                        cropStatusIndicator.setBackgroundColor(getResources().getColor(R.color.light_green, null));
                    }
                } else {
                    tvCropStatus.setText(R.string.crop_status_no_crops);
                    cropStatusIndicator.setBackgroundColor(ContextCompat.getColor(MainActivity.this, android.R.color.darker_gray));
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                // Log.e(TAG, "Failed to read crop status.", databaseError.toException());
                tvCropStatus.setText(R.string.crop_status_error);
                cropStatusIndicator.setBackgroundColor(ContextCompat.getColor(MainActivity.this, android.R.color.holo_red_dark));
            }
        });
    }
}