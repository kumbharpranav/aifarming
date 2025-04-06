package com.farming.ai.ui.videos;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.farming.ai.R;
import com.farming.ai.utils.YouTubeApiUtils;
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.YouTubePlayer;
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.listeners.AbstractYouTubePlayerListener;
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.views.YouTubePlayerView;
import com.farming.ai.utils.UltraSmoothScroller;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.android.material.search.SearchView;
import com.farming.ai.ui.auth.LoginActivity;  // Add this import
import com.google.android.material.bottomnavigation.BottomNavigationView;
import android.content.ActivityNotFoundException;  // Add this import

public class VideoLibraryActivity extends AppCompatActivity {
    private YouTubePlayerView youTubePlayerView;
    private RecyclerView rvVideos;
    private RecyclerView rvShorts;
    private View loadingProgress;
    private View noVideosView;
    private boolean isLoading = false;
    private String nextPageToken = null;
    private DatabaseReference mDatabase;
    private String userId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video_library);

        // Check session instead of Firebase auth
        SharedPreferences prefs = getSharedPreferences("AIFarming", MODE_PRIVATE);
        if (!prefs.getBoolean("is_logged_in", false)) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        userId = prefs.getString("user_id", null);

        // Initialize Firebase
        mDatabase = FirebaseDatabase.getInstance().getReference();

        // Setup toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Video Library");
        }

        // Initialize views
        youTubePlayerView = findViewById(R.id.youtube_player_view);
        rvVideos = findViewById(R.id.rv_videos);
        rvShorts = findViewById(R.id.rv_shorts);
        loadingProgress = findViewById(R.id.loading_progress);
        noVideosView = findViewById(R.id.no_videos_view);

        // Add lifecycle observer
        if (youTubePlayerView != null) {
            getLifecycle().addObserver(youTubePlayerView);
        }

        // Setup RecyclerViews
        initializeRecyclerViews();

        // Load videos
        loadVideos();

        // Check if video ID was passed
        String videoId = getIntent().getStringExtra("videoId");
        if (videoId != null && youTubePlayerView != null) {
            playVideo(videoId);
        }

        // Setup search functionality
        setupSearch();

        String searchQuery = getIntent().getStringExtra("search_query");
        if (searchQuery != null) {
            performSearch(searchQuery);
        }

        // Setup bottom navigation
        BottomNavigationView bottomNav = findViewById(R.id.bottom_navigation);
        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_featured) {
                loadVideos();
                return true;
            } else if (id == R.id.nav_search) {
                try {
                    startActivity(new Intent(this, VideoSearchActivity.class));
                } catch (ActivityNotFoundException e) {
                    Toast.makeText(this, "Search activity not found. Please check the manifest.", Toast.LENGTH_SHORT).show();
                }
                return true;
            } else if (id == R.id.nav_shorts) {
                loadShorts();
                return true;
            }
            return false;
        });
    }

    private void initializeRecyclerViews() {
        if (rvVideos != null) {
            LinearLayoutManager layoutManager = new LinearLayoutManager(this);
            rvVideos.setLayoutManager(layoutManager);
            
            // Add scroll listener for infinite scroll
            rvVideos.addOnScrollListener(new RecyclerView.OnScrollListener() {
                @Override
                public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                    super.onScrolled(recyclerView, dx, dy);
                    
                    int visibleItemCount = layoutManager.getChildCount();
                    int totalItemCount = layoutManager.getItemCount();
                    int firstVisibleItemPosition = layoutManager.findFirstVisibleItemPosition();

                    if (!isLoading && nextPageToken != null) {
                        if ((visibleItemCount + firstVisibleItemPosition) >= totalItemCount
                                && firstVisibleItemPosition >= 0) {
                            loadMoreVideos();
                        }
                    }
                }
            });
        }
        
        if (rvShorts != null) {
            rvShorts.setLayoutManager(new LinearLayoutManager(this, 
                LinearLayoutManager.HORIZONTAL, false));
            rvShorts.setNestedScrollingEnabled(false);
        }
    }

    private void loadVideos() {
        loadingProgress.setVisibility(View.VISIBLE);
        noVideosView.setVisibility(View.GONE);

        YouTubeApiUtils.fetchAgricultureVideos((videos, shorts) -> {
            loadingProgress.setVisibility(View.GONE);

            if (videos.isEmpty() && shorts.isEmpty()) {
                noVideosView.setVisibility(View.VISIBLE);
                return;
            }

            // Setup videos section
            VideoLibraryAdapter videosAdapter = new VideoLibraryAdapter(videos, this::playVideo);
            rvVideos.setAdapter(videosAdapter);

            // Setup shorts section
            VideoLibraryAdapter shortsAdapter = new VideoLibraryAdapter(shorts, this::playVideo);
            rvShorts.setAdapter(shortsAdapter);
        });
    }

    private void loadMoreVideos() {
        isLoading = true;
        loadingProgress.setVisibility(View.VISIBLE);
        
        if (userId != null) {
            mDatabase.child("crops").child(userId).get().addOnSuccessListener(snapshot -> {
                StringBuilder cropTypes = new StringBuilder();
                for (DataSnapshot crop : snapshot.getChildren()) {
                    cropTypes.append(crop.child("name").getValue(String.class)).append(" farming,");
                }
                
                String searchQuery = cropTypes.length() > 0 ? 
                    cropTypes.toString() : "modern agriculture techniques";
                    
                YouTubeApiUtils.searchVideos(searchQuery, (videos, nextPageToken) -> {
                    isLoading = false;
                    loadingProgress.setVisibility(View.GONE);
                    
                    VideoLibraryAdapter adapter = (VideoLibraryAdapter) rvVideos.getAdapter();
                    if (adapter != null) {
                        adapter.addVideos(videos);
                    }
                });
            });
        }
    }

    private void playVideo(String videoId) {
        if (youTubePlayerView == null) return;
        
        youTubePlayerView.addYouTubePlayerListener(new AbstractYouTubePlayerListener() {
            @Override
            public void onReady(@NonNull YouTubePlayer youTubePlayer) {
                youTubePlayer.loadVideo(videoId, 0);
                youTubePlayer.play();
            }
        });
        
        // Scroll to player
        youTubePlayerView.getParent().requestChildFocus(youTubePlayerView, youTubePlayerView);
    }

    private void initUI() {
        // Add smooth scrolling configuration
        RecyclerView.OnFlingListener smoothScroller = new RecyclerView.OnFlingListener() {
            @Override
            public boolean onFling(int velocityX, int velocityY) {
                rvVideos.smoothScrollBy(0, velocityY / 2);
                return true;
            }
        };
        
        // Custom LayoutManager with smooth scrolling
        LinearLayoutManager videosLayoutManager = new LinearLayoutManager(this) {
            @Override
            public void smoothScrollToPosition(RecyclerView recyclerView, RecyclerView.State state, int position) {
                UltraSmoothScroller smoothScroller = new UltraSmoothScroller(recyclerView.getContext());
                smoothScroller.setTargetPosition(position);
                startSmoothScroll(smoothScroller);
            }
        };
        
        LinearLayoutManager shortsLayoutManager = new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false) {
            @Override
            public void smoothScrollToPosition(RecyclerView recyclerView, RecyclerView.State state, int position) {
                UltraSmoothScroller smoothScroller = new UltraSmoothScroller(recyclerView.getContext());
                smoothScroller.setTargetPosition(position);
                startSmoothScroll(smoothScroller);
            }
        };

        rvVideos.setLayoutManager(videosLayoutManager);
        rvShorts.setLayoutManager(shortsLayoutManager);
        
        rvVideos.setOnFlingListener(smoothScroller);
        rvShorts.setOnFlingListener(smoothScroller);

        // Enable nested scrolling
        rvVideos.setNestedScrollingEnabled(true);
        rvShorts.setNestedScrollingEnabled(true);
    }

    private void setupInfiniteScroll() {
        rvVideos.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                LinearLayoutManager layoutManager = (LinearLayoutManager) recyclerView.getLayoutManager();
                if (layoutManager != null && !isLoading) {
                    int lastVisible = layoutManager.findLastVisibleItemPosition();
                    int totalItemCount = layoutManager.getItemCount();
                    
                    if (lastVisible == totalItemCount - 1) {
                        loadMoreVideos();
                    }
                }
            }
        });
    }

    // Add loadShorts method
    private void loadShorts() {
        loadingProgress.setVisibility(View.VISIBLE);
        noVideosView.setVisibility(View.GONE);

        YouTubeApiUtils.fetchShortVideos((videos, shorts) -> {
            loadingProgress.setVisibility(View.GONE);

            if (shorts.isEmpty()) {
                noVideosView.setVisibility(View.VISIBLE);
                return;
            }

            VideoLibraryAdapter shortsAdapter = new VideoLibraryAdapter(shorts, this::playVideo);
            rvShorts.setAdapter(shortsAdapter);
        });
    }

    // Remove setupSearch method since we're using VideoSearchActivity
    private void setupSearch() {
        // We no longer need this method since search is handled by VideoSearchActivity
    }

    private void performSearch(String query) {
        loadingProgress.setVisibility(View.VISIBLE);
        YouTubeApiUtils.searchVideos(query, (videos, shorts) -> {
            loadingProgress.setVisibility(View.GONE);
            VideoLibraryAdapter adapter = (VideoLibraryAdapter) rvVideos.getAdapter();
            if (adapter != null) {
                adapter.clearAndAddVideos(videos);
            }
        });
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (youTubePlayerView != null) {
            youTubePlayerView.release();
        }
    }
}
