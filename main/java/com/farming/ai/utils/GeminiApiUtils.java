package com.farming.ai.utils;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.provider.MediaStore;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.content.FileProvider;

import com.google.ai.client.generativeai.GenerativeModel;
import com.google.ai.client.generativeai.type.Content;
import com.google.ai.client.generativeai.type.GenerateContentResponse;
import com.google.ai.client.generativeai.type.GenerationConfig;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import kotlin.coroutines.Continuation;
import kotlin.coroutines.EmptyCoroutineContext;
import org.json.JSONObject;

public class GeminiApiUtils {
    private static final String API_KEY = "geminiapi"; // Use your free API key
    private static final Handler mainHandler = new Handler(Looper.getMainLooper());
    private static final Executor executor = Executors.newSingleThreadExecutor();

    public interface GeminiCallback {
        void onSuccess(String result);
        void onError(String error);
    }

    public static void analyzeCropData(Context context, String prompt, GeminiCallback callback) {
        executor.execute(() -> {
            try {
                GenerativeModel model = new GenerativeModel("gemini-2.0-flash", API_KEY);
                Content[] contents = new Content[] {
                    new Content.Builder().addText(prompt).build()
                };
                
                model.generateContent(contents, createGeminiContinuation(callback));
            } catch (Exception e) {
                mainHandler.post(() -> callback.onError(e.getMessage()));
            }
        });
    }

    public static void getFertilizerRecommendations(Context context, String cropType, GeminiCallback callback) {
        executor.execute(() -> {
            try {
                GenerativeModel model = new GenerativeModel("gemini-2.0-flash", API_KEY);
                String prompt = String.format(
                    "Provide short and practical fertilizer recommendations for %s. " +
                    "Include NPK ratio, application frequency, and at least one tip.", cropType);

                Content[] contents = new Content[] {
                    new Content.Builder().addText(prompt).build()
                };
                
                model.generateContent(contents, createGeminiContinuation(callback));
            } catch (Exception e) {
                mainHandler.post(() -> callback.onError(e.getMessage()));
            }
        });
    }

    public static void analyzePestIssue(Context context, String cropType, String description, GeminiCallback callback) {
        executor.execute(() -> {
            try {
                GenerativeModel model = new GenerativeModel("gemini-2.0-flash", API_KEY);
                String prompt = String.format(
                    "You are an agricultural expert. Analyze this pest/disease issue in %s crop.\n" +
                    "Issue description: %s\n" +
                    "Return analysis in JSON format.",
                    cropType, description
                );

                Content[] contents = new Content[] {
                    new Content.Builder().addText(prompt).build()
                };
                
                model.generateContent(contents, createGeminiContinuation(callback));
            } catch (Exception e) {
                mainHandler.post(() -> callback.onError("Error: " + e.getMessage()));
            }
        });
    }

    public static void analyzePestImage(Context context, String imagePath, GeminiCallback callback) {
        executor.execute(() -> {
            try {
                GenerativeModel model = new GenerativeModel("gemini-2.0-flash", API_KEY);
                Bitmap bitmap = loadImageSafely(context, imagePath);
                
                if (bitmap == null) {
                    mainHandler.post(() -> callback.onError("Failed to load image"));
                    return;
                }

                // Scale bitmap if too large
                if (bitmap.getWidth() > 2048 || bitmap.getHeight() > 2048) {
                    float scale = Math.min(2048f / bitmap.getWidth(), 2048f / bitmap.getHeight());
                    int newWidth = Math.round(bitmap.getWidth() * scale);
                    int newHeight = Math.round(bitmap.getHeight() * scale);
                    bitmap = Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true);
                }

                Content[] contents = new Content[] {
                    new Content.Builder()
                        .addImage(bitmap)
                        .addText("Identify any pests or diseases in this crop image. " +
                               "If you see signs of pests or disease, describe what you see and " +
                               "provide a short recommendation for treatment.")
                        .build()
                };
                
                model.generateContent(contents, createGeminiContinuation(callback));
            } catch (Exception e) {
                mainHandler.post(() -> callback.onError(e.getMessage()));
            }
        });
    }

    public static void analyzeCropImage(Context context, String imagePath, GeminiCallback callback) {
        executor.execute(() -> {
            try {
                GenerativeModel model = new GenerativeModel("gemini-pro-vision", API_KEY);
                Bitmap bitmap = loadImageSafely(context, imagePath);
                
                if (bitmap == null) {
                    mainHandler.post(() -> callback.onError("Failed to load image"));
                    return;
                }

                if (bitmap.getWidth() > 2048 || bitmap.getHeight() > 2048) {
                    float scale = Math.min(2048f / bitmap.getWidth(), 2048f / bitmap.getHeight());
                    int newWidth = Math.round(bitmap.getWidth() * scale);
                    int newHeight = Math.round(bitmap.getHeight() * scale);
                    bitmap = Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true);
                }

                String prompt = "Analyze this crop image and identify the following:\n" +
                              "1. What is the crop name?\n" +
                              "2. What variety does it appear to be?\n" +
                              "3. What growth stage is it in?\n" +
                              "4. What is its health status?\n" +
                              "Please format the response exactly as:\n" +
                              "Crop Name: [answer]\n" +
                              "Variety: [answer]\n" +
                              "Growth Stage: [answer]\n" +
                              "Health Status: [answer]";

                Content[] contents = new Content[] {
                    new Content.Builder()
                        .addImage(bitmap)
                        .addText(prompt)
                        .build()
                };

                model.generateContent(contents, createGeminiContinuation(callback));

            } catch (Exception e) {
                mainHandler.post(() -> callback.onError("Analysis error: " + e.getMessage()));
            }
        });
    }

    /**
     * Safely loads an image from a file path using multiple methods to handle MIUI security restrictions
     * @param context The application context
     * @param imagePath The path to the image file
     * @return A bitmap or null if loading failed
     */
    private static Bitmap loadImageSafely(Context context, String imagePath) {
        Bitmap bitmap = null;
        
        try {
            // Try to create a content URI using FileProvider (safer for MIUI)
            File imageFile = new File(imagePath);
            if (imageFile.exists()) {
                try {
                    Uri imageUri = FileProvider.getUriForFile(
                        context,
                        context.getPackageName() + ".fileprovider",
                        imageFile
                    );
                    
                    // Try loading using ContentResolver
                    bitmap = MediaStore.Images.Media.getBitmap(context.getContentResolver(), imageUri);
                } catch (Exception e) {
                    Log.e("GeminiApiUtils", "Error loading with ContentResolver: " + e.getMessage());
                }
            }
            
            // If content URI method failed, try direct file loading as fallback
            if (bitmap == null) {
                bitmap = BitmapFactory.decodeFile(imagePath);
            }
        } catch (Exception e) {
            Log.e("GeminiApiUtils", "Error loading image: " + e.getMessage());
        }
        
        return bitmap;
    }
    
    private static com.google.ai.client.generativeai.type.GenerationConfig createConfig() {
        return new com.google.ai.client.generativeai.type.GenerationConfig.Builder()
            .build();
    }

    private static Continuation<com.google.ai.client.generativeai.type.GenerateContentResponse> createGeminiContinuation(GeminiCallback callback) {
        return new Continuation<com.google.ai.client.generativeai.type.GenerateContentResponse>() {
            @Override
            public EmptyCoroutineContext getContext() {
                return EmptyCoroutineContext.INSTANCE;
            }

            @Override
            public void resumeWith(Object result) {
                try {
                    if (result instanceof com.google.ai.client.generativeai.type.GenerateContentResponse) {
                        com.google.ai.client.generativeai.type.GenerateContentResponse response = (com.google.ai.client.generativeai.type.GenerateContentResponse) result;
                        String textResponse = response.getText().trim();
                        mainHandler.post(() -> callback.onSuccess(textResponse));
                    } else {
                        mainHandler.post(() -> callback.onError("Invalid response type"));
                    }
                } catch (Exception e) {
                    mainHandler.post(() -> callback.onError(e.getMessage()));
                }
            }
        };
    }

    private static void handleGeminiResponse(Object result, GeminiCallback callback) {
        try {
            if (result instanceof com.google.ai.client.generativeai.type.GenerateContentResponse) {
                com.google.ai.client.generativeai.type.GenerateContentResponse response = (com.google.ai.client.generativeai.type.GenerateContentResponse) result;
                String textResponse = response.getText().trim();
                mainHandler.post(() -> callback.onSuccess(textResponse));
            } else {
                mainHandler.post(() -> callback.onError("Invalid response type"));
            }
        } catch (Exception e) {
            mainHandler.post(() -> callback.onError(e.getMessage()));
        }
    }
}
