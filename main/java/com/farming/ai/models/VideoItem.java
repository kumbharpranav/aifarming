package com.farming.ai.models;

public class VideoItem {
    private String title;
    private String videoId;
    private String thumbnailUrl;
    private int duration; // Add duration field

    public VideoItem(String title, String videoId, String thumbnailUrl) {
        this.title = title;
        this.videoId = videoId;
        this.thumbnailUrl = thumbnailUrl;
        this.duration = 0; // Default duration
    }

    public VideoItem(String title, String videoId, String thumbnailUrl, int duration) {
        this.title = title;
        this.videoId = videoId;
        this.thumbnailUrl = thumbnailUrl;
        this.duration = duration;
    }

    public String getTitle() { return title; }
    public String getVideoId() { return videoId; }
    public String getThumbnailUrl() { return thumbnailUrl; }
    public int getDuration() { return duration; }
    public void setDuration(int duration) { this.duration = duration; }
}
