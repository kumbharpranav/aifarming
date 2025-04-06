package com.farming.ai;

import android.app.Application;
import android.os.StrictMode;
import com.farming.ai.utils.NotificationUtils;
import com.google.firebase.FirebaseApp;

public class AIFarmingApp extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        
        // Initialize Firebase
        FirebaseApp.initializeApp(this);
        
        // Initialize notification channels
        NotificationUtils.createNotificationChannels(this);
        
        // Thread policy for network operations
        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder()
                .permitAll()
                .build();
        StrictMode.setThreadPolicy(policy);
    }
}