package com.farming.ai.ui.pests;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.farming.ai.R;
import com.farming.ai.adapters.VideoAdapter;
import com.farming.ai.models.Crop;
import com.farming.ai.utils.GeminiVisionApiHelper;
import com.farming.ai.utils.YouTubeApiUtils;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PestAnalysisActivity extends AppCompatActivity {

    private static final String TAG = "PestAnalysisActivity";

    private ImageView ivPestImage;
    private TextView tvSelectedCropName;
    private ProgressBar pbLoading;
    private TextView tvAnalysisResult;
    private TextView tvVideosHeading;
    private FrameLayout youtubePlayerContainer;
    private Button btnSaveNote;
    private LinearLayout youtubeLinksContainer;
    private RecyclerView youtubeCarousel;

    private Crop selectedCrop;
    private Uri imageUri;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pest_analysis);

        Toolbar toolbar = findViewById(R.id.toolbar_pest_analysis);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }

        ivPestImage = findViewById(R.id.iv_pest_image);
        tvSelectedCropName = findViewById(R.id.tv_selected_crop_name);
        pbLoading = findViewById(R.id.pb_analysis_loading);
        tvAnalysisResult = findViewById(R.id.tv_pest_analysis_result);
        tvVideosHeading = findViewById(R.id.tv_suggested_videos_heading);

        btnSaveNote = findViewById(R.id.btn_save_note);
        youtubeLinksContainer = findViewById(R.id.youtube_links_container);
        youtubeCarousel = findViewById(R.id.youtube_carousel);

        // Set font sizes
        tvAnalysisResult.setTextSize(18);
        tvVideosHeading.setTextSize(18);

        // Get data from Intent
        if (getIntent() != null) {
            selectedCrop = getIntent().getParcelableExtra("SELECTED_CROP");
            String imageUriString = getIntent().getStringExtra("IMAGE_URI");
            if (imageUriString != null) {
                imageUri = Uri.parse(imageUriString);
            }
        }

        if (selectedCrop != null && imageUri != null) {
            // Display the selected image and crop name
            ivPestImage.setImageURI(imageUri);
            tvSelectedCropName.setText(getString(R.string.selected_crop_dynamic, selectedCrop.getName()));
            // Start Gemini analysis
            startGeminiAnalysis(selectedCrop, imageUri);
        } else {
            // Handle error: missing data
            tvAnalysisResult.setText("Error: Could not load crop or image data.");
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed(); // Go back to the previous activity
        return true;
    }

    private void startGeminiAnalysis(Crop crop, Uri imageUri) {
        pbLoading.setVisibility(View.VISIBLE);
        // Convert Uri to Bitmap
        android.graphics.Bitmap bitmap = null;
        try {
            bitmap = android.provider.MediaStore.Images.Media.getBitmap(this.getContentResolver(), imageUri);
        } catch (IOException e) {
            pbLoading.setVisibility(View.GONE);
            tvAnalysisResult.setText("Failed to load image: " + e.getMessage());
            return;
        }
        // Assuming GeminiVisionApiHelper is a utility class for making API calls
        GeminiVisionApiHelper geminiHelper = new GeminiVisionApiHelper();
        String prompt = "Analyze this crop image for pests, suggest pesticides, fertilizers, homemade remedies, and provide at least 5 YouTube links for further information.";
        geminiHelper.sendGeminiRequest(bitmap, prompt, new GeminiVisionApiHelper.GeminiApiCallback() {
            @Override
            public void onSuccess(String analysisResult) {
                runOnUiThread(() -> {
                    pbLoading.setVisibility(View.GONE);
                    // Process and display the analysis result
                    String formattedResult = formatAnalysisResult(analysisResult);
                    tvAnalysisResult.setText(formattedResult);
                    // Check for YouTube links
                    List<String> youtubeLinks = extractYouTubeLinks(formattedResult);
                    if (!youtubeLinks.isEmpty()) {
                        tvVideosHeading.setVisibility(View.VISIBLE);
                        displayYouTubeLinks(youtubeLinks);
                    }
                    // Add a button to save the analysis as a note
                    btnSaveNote.setVisibility(View.VISIBLE);
                    btnSaveNote.setOnClickListener(v -> saveNoteToDatabase(formattedResult));
                });
            }

            @Override
            public void onFailure(Exception e) {
                runOnUiThread(() -> {
                    pbLoading.setVisibility(View.GONE);
                    tvAnalysisResult.setText("Failed to analyze image: " + e.getMessage());
                });
            }
        });
    }

    private void displayYouTubeLinks(List<String> youtubeLinks) {
        List<String> videoIds = new ArrayList<>();
        for (String link : youtubeLinks) {
            String videoId = extractVideoId(link);
            if (videoId != null) {
                videoIds.add(videoId);
            }
        }
        if (!videoIds.isEmpty()) {
            if(videoIds.size() > 5) {
                videoIds = videoIds.subList(0, 5);
            }
            fetchYouTubeVideos(videoIds);
        } else {
            runOnUiThread(() -> {
                youtubeCarousel.setVisibility(View.GONE);
                tvVideosHeading.setText("No valid YouTube videos found.");
            });
        }
    }

    private String extractVideoId(String url) {
        String regex = "(?:youtube\\.com/(?:[^/]+/.+/|(?:v|e(?:mbed)?)?/|.*[?&]v=)|youtu\\.be/)([^&%?\n]+)";
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(url);
        return matcher.find() ? matcher.group(1) : null;
    }

    private void fetchYouTubeVideos(List<String> videoIds) {
        YouTubeApiUtils.fetchVideoDetails(videoIds, videos -> {
            runOnUiThread(() -> {
                if (!videos.isEmpty()) {
                    youtubeCarousel.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
                    VideoAdapter adapter = new VideoAdapter(videos, PestAnalysisActivity.this);
                    youtubeCarousel.setAdapter(adapter);
                    youtubeCarousel.setVisibility(View.VISIBLE);
                } else {
                    youtubeCarousel.setVisibility(View.GONE);
                    tvVideosHeading.setText("Failed to fetch video details.");
                }
            });
        });
    }

    private void openYouTubeLink(String url) {
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
        intent.putExtra("force_fullscreen", true);
        startActivity(intent);
    }

    private void saveNoteToDatabase(String note) {
        // Logic to save the note to the database under crops/email/cropId
        String userEmail = getSharedPreferences("user_prefs", MODE_PRIVATE).getString("user_email", "");
        String cropId = selectedCrop.getId();
        DatabaseReference cropRef = FirebaseDatabase.getInstance().getReference("crops").child(userEmail).child(cropId);
        cropRef.child("notes").push().setValue(note).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                Toast.makeText(this, "Note saved successfully.", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Failed to save note.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private String formatAnalysisResult(String result) {
        // Remove asterisks and format the result
        return result.replace("*", "");
    }

    private List<String> extractYouTubeLinks(String text) {
        List<String> links = new ArrayList<>();
        // Simple regex to find YouTube links
        Pattern pattern = Pattern.compile("https?://(?:www\\.)?youtube\\.com/watch\\?v=[\\w-]+|https?://youtu\\.be/[\\w-]+", Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(text);
        while (matcher.find()) {
            links.add(matcher.group());
        }
        return links;
    }

    private void initializeYouTubePlayer(String videoUrl) {
        // TODO: Implement YouTube player initialization
        // This is a placeholder for where you'd integrate the YouTube API
    }
}
