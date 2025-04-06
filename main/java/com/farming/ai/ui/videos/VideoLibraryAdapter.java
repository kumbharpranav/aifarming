package com.farming.ai.ui.videos;

import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.farming.ai.R;
import com.farming.ai.models.VideoItem;
import com.google.android.material.card.MaterialCardView;
import java.util.ArrayList;
import java.util.List;

public class VideoLibraryAdapter extends RecyclerView.Adapter<VideoLibraryAdapter.VideoViewHolder> {
    
    private static final String TAG = "VideoLibraryAdapter";
    private List<VideoItem> videos;
    private final OnVideoClickListener clickListener;

    public interface OnVideoClickListener {
        void onVideoClick(String videoId);
    }

    public VideoLibraryAdapter(List<VideoItem> videos, OnVideoClickListener clickListener) {
        this.videos = videos != null ? videos : new ArrayList<>();
        this.clickListener = clickListener;
    }

    @NonNull
    @Override
    public VideoViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_video, parent, false);
        return new VideoViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull VideoViewHolder holder, int position) {
        // Validate position
        if (position < 0 || position >= videos.size()) {
            return;
        }

        VideoItem video = videos.get(position);
        // Guard against null video object
        if (video == null) {
            return;
        }

        // Set title
        String title = video.getTitle();
        holder.tvTitle.setText(title != null ? title : "");

        // Load thumbnail
        String thumbnailUrl = video.getThumbnailUrl();
        if (thumbnailUrl != null && !thumbnailUrl.isEmpty()) {
            Glide.with(holder.itemView.getContext())
                .load(thumbnailUrl)
                .placeholder(R.drawable.ic_notification) // Add a placeholder image
                .error(R.drawable.ic_notification) // Add an error image
                .into(holder.ivThumbnail);
        } else {
            holder.ivThumbnail.setImageResource(R.drawable.ic_notification);
        }

        // Handle video click
        String videoId = video.getVideoId();
        holder.cardView.setOnClickListener(v -> {
            if (videoId != null && !videoId.isEmpty() && clickListener != null) {
                clickListener.onVideoClick(videoId);
            }
        });

        // Set up like button
        Context context = holder.itemView.getContext();
        setupLikeButton(holder, videoId, context);

        // Set up share button
        setupShareButton(holder, videoId, context);
    }

    private void setupLikeButton(VideoViewHolder holder, String videoId, Context context) {
        // Initialize like button state regardless of video ID
        holder.btnLike.setSelected(false);
        holder.btnLike.setColorFilter(null);
        holder.btnLike.setEnabled(true);

        Log.d(TAG, "setupLikeButton called for videoId: " + videoId);

        if (videoId != null && !videoId.isEmpty() && context != null) {
            try {
                boolean isLiked = context.getSharedPreferences("video_likes", Context.MODE_PRIVATE)
                    .getBoolean("video_" + videoId, false);
                
                Log.d(TAG, "isLiked state for videoId " + videoId + ": " + isLiked);
                
                holder.btnLike.setSelected(isLiked);
                if (isLiked) {
                    holder.btnLike.setColorFilter(context.getColor(R.color.light_red));
                }

                holder.btnLike.setOnClickListener(v -> {
                    boolean newLikeState = !holder.btnLike.isSelected();
                    holder.btnLike.setSelected(newLikeState);
                    holder.btnLike.setColorFilter(newLikeState ? 
                        context.getColor(R.color.light_red) : null);
                    
                    context.getSharedPreferences("video_likes", Context.MODE_PRIVATE)
                        .edit()
                        .putBoolean("video_" + videoId, newLikeState)
                        .apply();
                });
            } catch (Exception e) {
                // If any error occurs, just disable the like button
                holder.btnLike.setEnabled(false);
            }
        } else {
            holder.btnLike.setEnabled(false);
        }
    }

    private void setupShareButton(VideoViewHolder holder, String videoId, Context context) {
        if (videoId != null && !videoId.isEmpty()) {
            holder.btnShare.setOnClickListener(v -> {
                Intent shareIntent = new Intent(Intent.ACTION_SEND);
                shareIntent.setType("text/plain");
                shareIntent.putExtra(Intent.EXTRA_TEXT, 
                    "Check out this farming video: https://youtu.be/" + videoId);
                context.startActivity(Intent.createChooser(shareIntent, "Share video"));
            });
            holder.btnShare.setEnabled(true);
        } else {
            holder.btnShare.setEnabled(false);
        }
    }

    @Override
    public int getItemCount() {
        return videos.size();
    }

    public void addVideos(List<VideoItem> newVideos) {
        int startPosition = videos.size();
        videos.addAll(newVideos);
        notifyItemRangeInserted(startPosition, newVideos.size());
    }

    public void clearAndAddVideos(List<VideoItem> newVideos) {
        videos.clear();
        videos.addAll(newVideos);
        notifyDataSetChanged();
    }

    static class VideoViewHolder extends RecyclerView.ViewHolder {
        MaterialCardView cardView;
        ImageView ivThumbnail;
        TextView tvTitle;
        ImageView btnLike;
        ImageView btnShare;

        VideoViewHolder(@NonNull View itemView) {
            super(itemView);
            cardView = (MaterialCardView) itemView;
            ivThumbnail = itemView.findViewById(R.id.iv_thumbnail);
            tvTitle = itemView.findViewById(R.id.tv_title);
            btnLike = itemView.findViewById(R.id.btn_like);
            btnShare = itemView.findViewById(R.id.btn_share);
        }
    }
}
