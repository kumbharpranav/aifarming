package com.farming.ai.ui.profile;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import com.farming.ai.R;

public class UserProfileActivity extends AppCompatActivity {
    private String userId;
    private String userEmail;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_profile);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle("User Profile");

        // Initialize SharedPreferences
        SharedPreferences prefs = getSharedPreferences("AIFarming", MODE_PRIVATE);
        userId = prefs.getString("user_id", null);
        userEmail = prefs.getString("user_email", null);

        if (userId != null) {
            loadUserProfile(userId);
        } else {
            // Handle case where user is not logged in (although MainActivity should prevent this)
            // Toast.makeText(this, "User not logged in.", Toast.LENGTH_SHORT).show();
        }
    }

    private void loadUserProfile(String userId) {
        // TODO: Load user profile data using userId
    }
}
