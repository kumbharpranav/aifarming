package com.farming.ai.ui.videos;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.farming.ai.R;
import com.farming.ai.utils.YouTubeApiUtils;

public class VideoSearchActivity extends AppCompatActivity {
    private RecyclerView rvResults;
    private View progressBar;
    private SearchView searchView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video_search);

        // Setup toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Search Videos");
        }

        // Initialize views
        rvResults = findViewById(R.id.rv_search_results);
        progressBar = findViewById(R.id.progress_bar);
        searchView = findViewById(R.id.search_view);

        // Setup RecyclerView
        rvResults.setLayoutManager(new LinearLayoutManager(this));

        // Setup search
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                performSearch(query);
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                return false;
            }
        });
    }

    private void performSearch(String query) {
        progressBar.setVisibility(View.VISIBLE);
        YouTubeApiUtils.searchVideos(query, (videos, nextPageToken) -> {
            progressBar.setVisibility(View.GONE);
            VideoLibraryAdapter adapter = new VideoLibraryAdapter(videos, videoId -> {
                // Handle video selection
                finish();
                Intent intent = new Intent(this, VideoLibraryActivity.class);
                intent.putExtra("videoId", videoId);
                startActivity(intent);
            });
            rvResults.setAdapter(adapter);
        });
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
