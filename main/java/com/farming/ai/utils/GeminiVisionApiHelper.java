package com.farming.ai.utils;

import android.graphics.Bitmap;
import android.util.Log;

import androidx.annotation.NonNull;

import com.farming.ai.BuildConfig; // Import BuildConfig
import com.google.ai.client.generativeai.GenerativeModel;
import com.google.ai.client.generativeai.java.GenerativeModelFutures;
import com.google.ai.client.generativeai.type.BlockThreshold;
import com.google.ai.client.generativeai.type.Content;
import com.google.ai.client.generativeai.type.GenerateContentResponse;
import com.google.ai.client.generativeai.type.GenerationConfig;
import com.google.ai.client.generativeai.type.HarmCategory;
import com.google.ai.client.generativeai.type.SafetySetting;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;

import java.io.IOException;
import java.util.Collections;
import java.util.concurrent.Executors;

public class GeminiVisionApiHelper {

    // API Key loaded from BuildConfig
    private static final String API_KEY = BuildConfig.GEMINI_API_KEY;
    private static final String TAG = "GeminiVisionApiHelper";

    private GenerativeModelFutures generativeModel;
    private final ListeningExecutorService executor = MoreExecutors.listeningDecorator(Executors.newSingleThreadExecutor());

    public GeminiVisionApiHelper() {
        // Initialize the Generative Model
        // Use gemini-1.5-pro for image analysis
        GenerativeModel model = new GenerativeModel(
                "gemini-2.0-flash", // Updated model name to latest Pro
                API_KEY,
                buildGenerationConfig(),
                buildSafetySettings()
        );
        generativeModel = GenerativeModelFutures.from(model);
    }

    /**
     * Sends an image and a text prompt to the Gemini API.
     *
     * @param image   The input Bitmap image.
     * @param prompt  The text prompt to accompany the image.
     * @param callback Callback to handle the async response.
     */
    @SuppressWarnings("UnstableApiUsage") // For Futures.addCallback
    public void sendGeminiRequest(Bitmap image, String prompt, final GeminiApiCallback callback) {
        try {
            // Construct the content object with text and image
            Content content = new Content.Builder()
                    .addText(prompt)
                    .addImage(image)
                    .build();

            // Send the request using the SDK
            ListenableFuture<GenerateContentResponse> response = generativeModel.generateContent(content);

            Futures.addCallback(response, new FutureCallback<GenerateContentResponse>() {
                @Override
                public void onSuccess(GenerateContentResponse result) {
                    try {
                        // Extract the text, expecting JSON as configured
                        String jsonText = result.getText();
                        if (jsonText != null) {
                            callback.onSuccess(jsonText);
                        } else {
                            Log.e(TAG, "Gemini response text was null");
                            callback.onFailure(new IOException("Received null text response from Gemini"));
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error processing Gemini success response", e);
                        callback.onFailure(e);
                    }
                }

                @Override
                public void onFailure(@NonNull Throwable t) {
                    Log.e(TAG, "Gemini request failed", t);
                    callback.onFailure(new Exception("Gemini API request failed", t));
                }
            }, executor); // Use the executor for the callback

        } catch (Exception e) {
            Log.e(TAG, "Error sending Gemini request", e);
            callback.onFailure(e);
        }
    }

    // Helper to build Generation Config (request JSON output)
    private GenerationConfig buildGenerationConfig() {
        return new GenerationConfig.Builder()
                // Add other config like temperature, topP, topK if needed
                .build();
    }

    // Helper to build Safety Settings (adjust blocking thresholds as needed)
    private java.util.List<SafetySetting> buildSafetySettings() {
        // Example: Block medium and high probability unsafe content
        return Collections.singletonList(
                new SafetySetting(HarmCategory.HARASSMENT, BlockThreshold.MEDIUM_AND_ABOVE)
                // Add more settings for other categories if desired
        );
    }

    /**
     * Callback interface for handling asynchronous responses.
     */
    public interface GeminiApiCallback {
        void onSuccess(String result);
        void onFailure(Exception e);
    }
}
