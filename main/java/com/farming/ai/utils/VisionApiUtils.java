package com.farming.ai.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.Looper;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.label.ImageLabel;
import com.google.mlkit.vision.label.ImageLabeler;
import com.google.mlkit.vision.label.ImageLabeling;
import com.google.mlkit.vision.label.defaults.ImageLabelerOptions;
import com.google.mlkit.vision.text.Text;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;
import com.google.android.gms.tasks.Task;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.io.File;
import android.net.Uri;
import com.farming.ai.models.CropCategory;

public class VisionApiUtils {
    private static final Handler mainHandler = new Handler(Looper.getMainLooper());
    private static final Executor executor = Executors.newSingleThreadExecutor();
    
    public interface CropDetectionCallback {
        void onCropDetected(String cropType, String soilType, float confidence);
        void onError(String error);
    }

    public interface PestDetectionCallback {
        void onPestDetected(String pestType, double confidence);
        void onError(String error);
    }

    public static void analyzeCropImage(Context context, String imagePath, CropDetectionCallback callback) {
        try {
            Bitmap bitmap = BitmapFactory.decodeFile(imagePath);
            if (bitmap == null) {
                callback.onError("Failed to load image");
                return;
            }

            ImageLabeler labeler = ImageLabeling.getClient(new ImageLabelerOptions.Builder()
                .setConfidenceThreshold(0.7f)
                .build());

            InputImage image = InputImage.fromBitmap(bitmap, 0);
            labeler.process(image)
                .addOnSuccessListener(labels -> {
                    String cropType = "Unknown";
                    String soilType = "Unknown";
                    float maxConfidence = 0f;

                    for (ImageLabel label : labels) {
                        String text = label.getText().toLowerCase();
                        float confidence = label.getConfidence();

                        // Check against known crop types
                        if (CropCategory.getVarieties(text) != null && confidence > maxConfidence) {
                            cropType = text;
                            maxConfidence = confidence;
                        }

                        if (text.contains("soil")) {
                            soilType = determineSoilType(text);
                        }
                    }

                    callback.onCropDetected(cropType, soilType, maxConfidence);
                })
                .addOnFailureListener(e -> callback.onError(e.getMessage()));

        } catch (Exception e) {
            callback.onError(e.getMessage());
        }
    }

    public static void analyzePestImage(Context context, String imagePath, PestDetectionCallback callback) {
        try {
            Bitmap bitmap = BitmapFactory.decodeFile(imagePath);
            if (bitmap == null) {
                callback.onPestDetected("Error loading image", 0.0);
                return;
            }

            InputImage image = InputImage.fromBitmap(bitmap, 0);
            ImageLabeler labeler = ImageLabeling.getClient(ImageLabelerOptions.DEFAULT_OPTIONS);

            labeler.process(image).addOnSuccessListener(labels -> {
                double maxConfidence = 0;
                String detectedPest = null;

                // Check for pest/disease related keywords
                for (ImageLabel label : labels) {
                    String text = label.getText().toLowerCase();
                    float confidence = label.getConfidence();

                    if ((text.contains("pest") || text.contains("disease") || 
                         text.contains("blight") || text.contains("leaf spot") ||
                         text.contains("mold")) && confidence > maxConfidence) {
                        detectedPest = text;
                        maxConfidence = confidence;
                    }
                }

                if (detectedPest != null) {
                    callback.onPestDetected(detectedPest, maxConfidence);
                } else {
                    // Run additional analysis for non-obvious symptoms
                    analyzeLeafCondition(bitmap, callback);
                }
            });
        } catch (Exception e) {
            callback.onPestDetected("Error analyzing image", 0.0);
        }
    }

    private static void analyzeLeafCondition(Bitmap bitmap, PestDetectionCallback callback) {
        try {
            InputImage image = InputImage.fromBitmap(bitmap, 0);
            
            // Rest of your leaf analysis code...
            int[] pixels = new int[bitmap.getWidth() * bitmap.getHeight()];
            bitmap.getPixels(pixels, 0, bitmap.getWidth(), 0, 0, 
                bitmap.getWidth(), bitmap.getHeight());

            int yellowishPixels = 0;
            int brownPixels = 0;

            for (int pixel : pixels) {
                int r = (pixel >> 16) & 0xff;
                int g = (pixel >> 8) & 0xff;
                int b = pixel & 0xff;

                if (r > 200 && g > 200 && b < 100) yellowishPixels++;
                if (r > 100 && r < 200 && g < 150 && b < 100) brownPixels++;
            }

            double yellowRatio = (double) yellowishPixels / pixels.length;
            double brownRatio = (double) brownPixels / pixels.length;

            if (yellowRatio > 0.3) {
                callback.onPestDetected("Potential nutrient deficiency", yellowRatio);
            } else if (brownRatio > 0.3) {
                callback.onPestDetected("Possible fungal infection", brownRatio);
            } else {
                callback.onPestDetected("No obvious issues detected", 0.95);
            }
        } catch (Exception e) {
            callback.onError("Error analyzing leaf condition: " + e.getMessage());
        }
    }

    public static void analyzePestImage(String imagePath, PestDetectionCallback callback) {
        try {
            Bitmap bitmap = BitmapFactory.decodeFile(imagePath);
            if (bitmap == null) {
                callback.onPestDetected("Error loading image", 0.0);
                return;
            }

            InputImage image = InputImage.fromBitmap(bitmap, 0);
            ImageLabeler labeler = ImageLabeling.getClient(ImageLabelerOptions.DEFAULT_OPTIONS);

            labeler.process(image).addOnSuccessListener(labels -> {
                String detectedIssue = null;
                double maxConfidence = 0;

                for (ImageLabel label : labels) {
                    String text = label.getText().toLowerCase();
                    float confidence = label.getConfidence();

                    if ((text.contains("pest") || text.contains("disease") || 
                         text.contains("blight") || text.contains("spot") ||
                         text.contains("rot") || text.contains("mold")) && 
                        confidence > maxConfidence) {
                        detectedIssue = text;
                        maxConfidence = confidence;
                    }
                }

                if (detectedIssue != null) {
                    callback.onPestDetected(detectedIssue, maxConfidence);
                } else {
                    analyzeLeafCondition(bitmap, callback);
                }
            });
        } catch (Exception e) {
            callback.onPestDetected("Error analyzing image", 0.0);
        }
    }

    private static Bitmap toBitmap(InputImage image) {
        return BitmapFactory.decodeFile(image.getMediaImage().toString());
    }

    private static String determineSoilType(String text) {
        if (text.contains("sandy")) return "Sandy";
        if (text.contains("clay")) return "Clay";
        if (text.contains("loam")) return "Loam";
        if (text.contains("silt")) return "Silty";
        return "Normal";
    }

    private static String analyzeSoilCondition(String labelText, float confidence) {
        if (labelText.contains("dry") && confidence > 0.7) {
            return "dry";
        } else if (labelText.contains("wet") && confidence > 0.7) {
            return "wet";
        } else if (labelText.contains("fertile") && confidence > 0.7) {
            return "fertile";
        }
        return "normal";
    }
}