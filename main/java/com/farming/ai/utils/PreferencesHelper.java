package com.farming.ai.utils;

import android.content.Context;
import android.content.SharedPreferences;
import java.util.Set;
import java.util.HashSet;

public class PreferencesHelper {
    private static final String PREF_NAME = "AIFarming";
    private static final String LIKED_VIDEOS = "liked_videos";

    public static void saveLikedVideo(Context context, String videoId) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        Set<String> likedVideos = new HashSet<>(prefs.getStringSet(LIKED_VIDEOS, new HashSet<>()));
        likedVideos.add(videoId);
        prefs.edit().putStringSet(LIKED_VIDEOS, likedVideos).apply();
    }

    public static Set<String> getLikedVideos(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        return prefs.getStringSet(LIKED_VIDEOS, new HashSet<>());
    }
}
