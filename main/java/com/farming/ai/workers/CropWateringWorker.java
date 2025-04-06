package com.farming.ai.workers;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;
import androidx.work.Data;
import com.farming.ai.utils.CropWaterScheduler;
import com.farming.ai.utils.NotificationUtils;
import com.farming.ai.models.Crop;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import android.util.Log;

public class CropWateringWorker extends Worker {

    // Define the expected date format from Firebase/Gemini
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());

    public CropWateringWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    @Override
    public Result doWork() {
        String cropId = getInputData().getString("cropId");
        if (cropId == null) return Result.failure();

        DatabaseReference cropRef = FirebaseDatabase.getInstance()
            .getReference("crops")
            .child(cropId);

        cropRef.get().addOnSuccessListener(snapshot -> {
            Crop crop = snapshot.getValue(Crop.class);
            if (crop != null) {
                long currentTime = System.currentTimeMillis();
                String lastWateredStr = crop.getLastWateredTime();
                int intervalHours = crop.getWateringIntervalHours();
                
                long lastWateredMillis = 0;
                if (lastWateredStr != null && !lastWateredStr.isEmpty()) {
                    try {
                        Date lastWateredDate = DATE_FORMAT.parse(lastWateredStr);
                        if (lastWateredDate != null) {
                            lastWateredMillis = lastWateredDate.getTime();
                        }
                    } catch (ParseException e) {
                        Log.e("CropWateringWorker", "Failed to parse last watered time: " + lastWateredStr, e);
                        // Handle error - maybe skip notification or use a default?
                        return; // Exit if date cannot be parsed
                    }
                } else {
                     Log.w("CropWateringWorker", "Last watered time is missing for crop: " + cropId);
                     // Decide how to handle missing last watered time
                     // Maybe assume it needs watering? Or skip?
                     return; // Example: Skip if time is missing
                }

                if (intervalHours <= 0) {
                    Log.w("CropWateringWorker", "Watering interval is invalid for crop: " + cropId);
                    return; // Skip if interval is invalid
                }
                
                // Check if watering is needed
                if (lastWateredMillis > 0 && (currentTime - lastWateredMillis >= intervalHours * 3600000L)) {
                    NotificationUtils.showWateringNotification(
                        getApplicationContext(),
                        crop.getName()
                    );
                    
                    // Schedule next notification
                    CropWaterScheduler.scheduleCropWatering(
                        getApplicationContext(),
                        cropId,
                        intervalHours
                    );
                }
            }
        });

        return Result.success();
    }
}
