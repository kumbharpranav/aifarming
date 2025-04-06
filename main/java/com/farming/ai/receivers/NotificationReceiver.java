package com.farming.ai.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import com.farming.ai.utils.NotificationUtils;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;
import androidx.annotation.NonNull;
import androidx.work.ExistingWorkPolicy;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;
import androidx.work.Data;

import java.util.Random;
import java.util.concurrent.TimeUnit;

import com.farming.ai.workers.CropWateringWorker;

public class NotificationReceiver extends BroadcastReceiver {
    private static final int DISMISS_DELAY = 5 * 60 * 1000; // 5 minutes

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        String cropId = intent.getStringExtra("cropId");
        String cropName = intent.getStringExtra("cropName");

        switch (action) {
            case "ACTION_WATER_OK":
                handleWateringConfirmation(context, cropId, cropName);
                break;
            case "ACTION_WATER_DISMISS":
                scheduleWateringReminder(context, cropId, cropName);
                break;
            case "ACTION_FERTILIZE":
                handleFertilizing(context, cropId, cropName);
                break;
        }
    }

    private void handleWateringConfirmation(Context context, String cropId, String cropName) {
        updateCropWateringStatus(context, cropId, true);
        NotificationUtils.sendGeneralNotification(
            context,
            "Watering Confirmed",
            "Great! You've watered your " + cropName,
            new Random().nextInt()
        );
    }

    private void scheduleWateringReminder(Context context, String cropId, String cropName) {
        OneTimeWorkRequest wateringWork = new OneTimeWorkRequest.Builder(CropWateringWorker.class)
            .setInitialDelay(30, TimeUnit.MINUTES)
            .setInputData(new Data.Builder()
                .putString("cropId", cropId)
                .putString("cropName", cropName)
                .build())
            .build();

        WorkManager.getInstance(context)
            .enqueueUniqueWork(
                "watering_reminder_" + cropId,
                ExistingWorkPolicy.REPLACE,
                wateringWork
            );
    }

    private void updateCropWateringStatus(Context context, String cropId, boolean isWatered) {
        SharedPreferences prefs = context.getSharedPreferences("AIFarming", Context.MODE_PRIVATE);
        String userId = prefs.getString("user_id", null);
        DatabaseReference cropsRef = FirebaseDatabase.getInstance().getReference("crops")
            .child(userId);

        cropsRef.orderByChild("id").equalTo(cropId).addListenerForSingleValueEvent(
            new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    for (DataSnapshot cropSnapshot : snapshot.getChildren()) {
                        cropSnapshot.getRef().child("isWatered").setValue(isWatered);
                        cropSnapshot.getRef().child("lastWateredTime")
                            .setValue(System.currentTimeMillis());
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    // Handle error
                }
            });
    }

    private void handleFertilizing(Context context, String cropId, String cropName) {
        // Implement fertilizing logic here
    }
}
