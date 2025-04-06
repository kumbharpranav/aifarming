package com.farming.ai.utils;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.farming.ai.R;
import com.farming.ai.ui.notifications.NotificationsActivity;
import com.farming.ai.receivers.NotificationReceiver;

import org.json.JSONObject;

import java.util.concurrent.atomic.AtomicInteger;

public class NotificationUtils {

    private static final String CHANNEL_ALERTS_ID = "alerts_channel";
    private static final String CHANNEL_GENERAL_ID = "general_channel";
    private static final String CHANNEL_ID = "crop_notifications";
    private static final AtomicInteger notificationId = new AtomicInteger(0);

    public static void createNotificationChannels(Context context) {
        // Create notification channels for Android O and above
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Create the Alert notification channel
            NotificationChannel alertChannel = new NotificationChannel(
                    CHANNEL_ALERTS_ID,
                    "Crop Alerts",
                    NotificationManager.IMPORTANCE_HIGH
            );
            alertChannel.setDescription("Notifications for critical crop alerts");
            
            // Create the General notification channel
            NotificationChannel generalChannel = new NotificationChannel(
                    CHANNEL_GENERAL_ID,
                    "General Notifications",
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            generalChannel.setDescription("General app notifications");
            
            // Create the Crop notification channel
            NotificationChannel cropChannel = new NotificationChannel(
                    CHANNEL_ID,
                    "Crop Notifications",
                    NotificationManager.IMPORTANCE_HIGH
            );
            cropChannel.setDescription("Notifications about crop watering and maintenance");
            
            // Register the channels with the system
            NotificationManager notificationManager = context.getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(alertChannel);
            notificationManager.createNotificationChannel(generalChannel);
            notificationManager.createNotificationChannel(cropChannel);
        }
    }
    
    public static void sendCropAlert(Context context, String title, String message, int notificationId) {
        sendNotification(context, title, message, notificationId, CHANNEL_ALERTS_ID, true);
    }
    
    public static void sendGeneralNotification(Context context, String title, String message, int notificationId) {
        sendNotification(context, title, message, notificationId, CHANNEL_GENERAL_ID, false);
    }
    
    private static void sendNotification(Context context, String title, String message, 
                                         int notificationId, String channelId, boolean highPriority) {
        // Create an intent to open the NotificationsActivity when the notification is tapped
        Intent intent = new Intent(context, NotificationsActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                context, 0, intent, PendingIntent.FLAG_IMMUTABLE);
        
        // Build the notification
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, channelId)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(title)
                .setContentText(message)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(message))
                .setPriority(highPriority ? NotificationCompat.PRIORITY_HIGH : NotificationCompat.PRIORITY_DEFAULT)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true);
        
        // Send the notification
        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
        try {
            notificationManager.notify(notificationId, builder.build());
        } catch (SecurityException e) {
            // Handle when notification permission is not granted
            e.printStackTrace();
        }
    }

    public static void showWateringNotification(Context context, String cropName) {
        Intent okIntent = new Intent(context, NotificationReceiver.class)
            .setAction("ACTION_WATER_OK")
            .putExtra("cropName", cropName);
            
        Intent dismissIntent = new Intent(context, NotificationReceiver.class)
            .setAction("ACTION_WATER_DISMISS")
            .putExtra("cropName", cropName);

        PendingIntent okPendingIntent = PendingIntent.getBroadcast(
            context, 0, okIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
            
        PendingIntent dismissPendingIntent = PendingIntent.getBroadcast(
            context, 1, dismissIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("Time to Water Crop")
            .setContentText("Your " + cropName + " needs watering!")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .addAction(R.drawable.ic_check, "OK", okPendingIntent)
            .addAction(R.drawable.ic_close, "Dismiss", dismissPendingIntent);

        NotificationManager manager = context.getSystemService(NotificationManager.class);
        manager.notify(notificationId.incrementAndGet(), builder.build());
    }

    public static void showGratitudeNotification(Context context) {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_GENERAL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("Thank You!")
            .setContentText("Thank you for allowing notifications. We'll keep you updated about your crops.")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .addAction(R.drawable.ic_close, "Dismiss", null);

        NotificationManagerCompat.from(context).notify(notificationId.incrementAndGet(), builder.build());
    }

    public static void showFertilizerNotification(Context context, String cropName, JSONObject fertilizerData) {
        try {
            String title = "Fertilizer Reminder";
            String message = String.format("Time to apply fertilizer to your %s\nRecommended: %s", 
                cropName, fertilizerData.getJSONArray("types").getString(0));

            NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(title)
                .setContentText(message)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(message))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true);

            NotificationManagerCompat.from(context)
                .notify(notificationId.incrementAndGet(), builder.build());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}