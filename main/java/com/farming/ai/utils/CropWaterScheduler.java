package com.farming.ai.utils;

import android.content.Context;
import androidx.work.*;
import java.util.concurrent.TimeUnit;
import com.farming.ai.workers.CropWateringWorker;

public class CropWaterScheduler {
    public static void scheduleCropWatering(Context context, String cropId, int intervalHours) {
        Data inputData = new Data.Builder()
            .putString("cropId", cropId)
            .build();

        PeriodicWorkRequest wateringWork = new PeriodicWorkRequest.Builder(
                CropWateringWorker.class,
                intervalHours,
                TimeUnit.HOURS)
            .setInputData(inputData)
            .build();

        WorkManager.getInstance(context)
            .enqueueUniquePeriodicWork(
                "watering_" + cropId,
                ExistingPeriodicWorkPolicy.REPLACE,
                wateringWork
            );
    }
}
