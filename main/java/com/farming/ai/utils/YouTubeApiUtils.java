package com.farming.ai.utils;

import android.os.Handler;
import android.os.Looper;
import com.farming.ai.models.VideoItem;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class YouTubeApiUtils {
    private static final String API_KEY = "youtubeapi";
    private static final String SEARCH_URL = "https://www.googleapis.com/youtube/v3/search";
    private static final OkHttpClient client = new OkHttpClient();
    private static final Handler mainHandler = new Handler(Looper.getMainLooper());


    public interface YouTubeVideosCallback {
        void onVideosReceived(List<VideoItem> videos, List<VideoItem> shorts);
    }

    public static void fetchAgricultureVideos(YouTubeVideosCallback callback) {
        String videoUrl = SEARCH_URL + "?part=snippet" +
                "&q=modern+agriculture+techniques+india" +
                "&type=video" +
                "&videoDuration=long" +  // Only long videos
                "&maxResults=5" +
                "&key=" + API_KEY;

        String shortsUrl = SEARCH_URL + "?part=snippet" +
                "&q=modern+agriculture+techniques+india" +
                "&type=video" +
                "&videoDuration=short" +  // Only shorts
                "&maxResults=5" +
                "&key=" + API_KEY;

        fetchVideos(videoUrl, shortsUrl, callback);
    }

    private static void fetchVideos(String videoUrl, String shortsUrl, YouTubeVideosCallback callback) {
        List<VideoItem> videos = new ArrayList<>();
        List<VideoItem> shorts = new ArrayList<>();
        final int[] completedRequests = {0};

        // Fetch regular videos
        fetchVideoList(videoUrl, videoList -> {
            videos.addAll(videoList);
            completedRequests[0]++;
            if (completedRequests[0] == 2) {
                mainHandler.post(() -> callback.onVideosReceived(videos, shorts));
            }
        });

        // Fetch shorts
        fetchVideoList(shortsUrl, shortsList -> {
            shorts.addAll(shortsList);
            completedRequests[0]++;
            if (completedRequests[0] == 2) {
                mainHandler.post(() -> callback.onVideosReceived(videos, shorts));
            }
        });
    }

    private static void fetchVideoList(String url, VideoListCallback callback) {
        Request request = new Request.Builder().url(url).build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                mainHandler.post(() -> callback.onVideoListReceived(new ArrayList<>()));
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                try {
                    String jsonData = response.body().string();
                    JSONObject jsonObject = new JSONObject(jsonData);
                    JSONArray items = jsonObject.getJSONArray("items");

                    List<VideoItem> videos = new ArrayList<>();
                    for (int i = 0; i < items.length(); i++) {
                        JSONObject item = items.getJSONObject(i);
                        JSONObject snippet = item.getJSONObject("snippet");
                        String videoId = item.getJSONObject("id").getString("videoId");
                        String title = snippet.getString("title");
                        String thumbnailUrl = snippet.getJSONObject("thumbnails")
                                .getJSONObject("medium").getString("url");

                        videos.add(new VideoItem(title, videoId, thumbnailUrl));
                    }

                    mainHandler.post(() -> callback.onVideoListReceived(videos));
                } catch (Exception e) {
                    mainHandler.post(() -> callback.onVideoListReceived(new ArrayList<>()));
                }
            }
        });
    }

    public static void fetchNextPage(String pageToken, VideoListCallback callback) {
        String url = SEARCH_URL + "?part=snippet" +
                "&q=modern+agriculture+techniques+india" +
                "&type=video" +
                "&maxResults=10" +
                "&pageToken=" + pageToken +
                "&key=" + API_KEY;

        fetchVideoList(url, videos -> {
            callback.onVideoListReceived(videos);
        });
    }

    public static void searchVideos(String query, YouTubeVideosCallback callback) {
        String url = SEARCH_URL + "?part=snippet" +
                "&q=" + query +
                "&type=video" +
                "&maxResults=50" +
                "&key=" + API_KEY;

        fetchVideoList(url, videos -> callback.onVideosReceived(videos, null));
    }

    public static void fetchShortVideos(YouTubeVideosCallback callback) {
        String url = SEARCH_URL + "?part=snippet,contentDetails" +
                "&q=farming+shorts,agriculture+shorts" +
                "&type=video" +
                "&videoDuration=short" +  // Only request short videos
                "&maxResults=50" +
                "&key=" + API_KEY;

        fetchVideoList(url, videos -> {
            // All videos from this query are already shorts since we use videoDuration=short
            callback.onVideosReceived(new ArrayList<>(), videos);
        });
    }

    public static void fetchVideoDetails(List<String> videoIds, VideoListCallback callback) {
        String ids = String.join(",", videoIds);
        String url = "https://www.googleapis.com/youtube/v3/videos?part=snippet&id=" + ids + "&key=" + API_KEY;
        fetchVideoList(url, callback);
    }

    public interface VideoListCallback {
        void onVideoListReceived(List<VideoItem> videoList);
    }
}
