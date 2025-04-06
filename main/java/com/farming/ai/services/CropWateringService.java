package com.farming.ai.services;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.IBinder;
import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import com.farming.ai.R;
import com.farming.ai.models.Crop;
import com.farming.ai.utils.NotificationUtils;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class CropWateringService extends Service {
    private static final int NOTIFICATION_INTERVAL = 5 * 60 * 1000; // 5 minutes
    private String userId;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        checkCropsWateringStatus();
        return START_STICKY;
    }

    private void checkCropsWateringStatus() {
        SharedPreferences prefs = getSharedPreferences("AIFarming", MODE_PRIVATE);
        userId = prefs.getString("user_id", null);

        if (userId != null) {
            DatabaseReference cropsRef = FirebaseDatabase.getInstance()
                .getReference("crops")
                .child(userId);

            cropsRef.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    for (DataSnapshot cropSnapshot : snapshot.getChildren()) {
                        Crop crop = cropSnapshot.getValue(Crop.class);
                        if (crop != null && !crop.isWatered()) {
                            NotificationUtils.showWateringNotification(
                                getApplicationContext(), 
                                crop.getName()
                            );
                        }
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    // Handle error
                }
            });
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
