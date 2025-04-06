package com.farming.ai.ui.videos;

import android.os.Bundle;
import android.view.View;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.farming.ai.R;
import com.farming.ai.utils.PreferencesHelper;
import java.util.ArrayList;
import java.util.Set;  // Add this import

public class LikedVideosActivity extends AppCompatActivity {
    private RecyclerView rvLikedVideos;
    private View noVideosView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_liked_videos);

        rvLikedVideos = findViewById(R.id.rv_liked_videos);
        noVideosView = findViewById(R.id.no_videos_view);

        rvLikedVideos.setLayoutManager(new LinearLayoutManager(this));
        loadLikedVideos();
    }

    private void loadLikedVideos() {
        Set<String> likedVideoIds = PreferencesHelper.getLikedVideos(this);
        if (likedVideoIds.isEmpty()) {
            noVideosView.setVisibility(View.VISIBLE);
            return;
        }

        // Fetch video details for liked videos
        // Implementation here using YouTubeApiUtils
    }
}
