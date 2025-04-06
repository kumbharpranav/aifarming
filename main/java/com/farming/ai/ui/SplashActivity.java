package com.farming.ai.ui;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.WindowManager;

import androidx.appcompat.app.AppCompatActivity;

import com.farming.ai.MainActivity;
import com.farming.ai.R;
import com.farming.ai.ui.auth.WelcomeActivity;

public class SplashActivity extends AppCompatActivity {

    private static final long SPLASH_DISPLAY_TIME = 2000; // 2 seconds
    private SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Set fullscreen
        getWindow().setFlags(
                WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN
        );
        
        setContentView(R.layout.activity_splash);
        
        prefs = getSharedPreferences("AIFarming", MODE_PRIVATE);

        // Delay and then check login state
        new Handler(Looper.getMainLooper()).postDelayed(this::checkLoginState, SPLASH_DISPLAY_TIME);
    }
    
    private void checkLoginState() {
        // Check if user is logged in from SharedPreferences
        boolean isLoggedIn = prefs.getBoolean("is_logged_in", false);
        
        Intent intent;
        if (isLoggedIn) {
            // User is logged in, go to MainActivity
            intent = new Intent(SplashActivity.this, MainActivity.class);
        } else {
            // User is not logged in, go to WelcomeActivity
            intent = new Intent(SplashActivity.this, WelcomeActivity.class);
        }
        
        startActivity(intent);
        finish();
    }
}