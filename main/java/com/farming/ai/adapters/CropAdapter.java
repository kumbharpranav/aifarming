package com.farming.ai.adapters;

import android.content.Context;
import android.content.Intent;
import android.os.Parcelable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.farming.ai.R;
import com.farming.ai.models.Crop;
import com.farming.ai.ui.crops.CropDetailActivity;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class CropAdapter extends RecyclerView.Adapter<CropAdapter.CropViewHolder> {

    private Context context;
    private List<Crop> cropList;
    private SimpleDateFormat displayDateFormat;
    private SimpleDateFormat parseDateFormat;
    private OnItemClickListener listener;

    // Interface for click events
    public interface OnItemClickListener {
        void onItemClick(Crop crop);
    }

    // Constructor accepting the listener
    public CropAdapter(Context context, List<Crop> cropList, OnItemClickListener listener) {
        this.context = context;
        this.cropList = cropList;
        this.listener = listener;
        this.displayDateFormat = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());
        this.parseDateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
    }

    @NonNull
    @Override
    public CropViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_crop, parent, false);
        return new CropViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CropViewHolder holder, int position) {
        Crop crop = cropList.get(position);
        
        holder.tvCropName.setText(crop.getName());
        holder.tvCropVariety.setText(crop.getVariety());
        
        // Set planting and harvest dates
        String plantingDateStr = crop.getPlantingDate();
        if (plantingDateStr != null && !plantingDateStr.isEmpty()) {
            holder.tvPlantingDate.setText(formatDateStringForDisplay(plantingDateStr));
        } else {
            holder.tvPlantingDate.setText("Not set");
        }
        
        String harvestDateStr = crop.getExpectedHarvestDate();
        if (harvestDateStr != null && !harvestDateStr.isEmpty()) {
            holder.tvHarvestDate.setText(formatDateStringForDisplay(harvestDateStr));
        } else {
            holder.tvHarvestDate.setText("Not set");
        }
        
        // Set crop status indicator color
        if (crop.isNeedsAttention()) {
            holder.statusIndicator.setBackgroundColor(
                    ContextCompat.getColor(context, R.color.light_red));
        } else {
            holder.statusIndicator.setBackgroundColor(
                    ContextCompat.getColor(context, R.color.light_green));
        }
        
        // Load crop image
        if (holder.ivCropImage != null) { 
            if (crop.getImageUrl() != null && !crop.getImageUrl().isEmpty()) {
                Glide.with(holder.itemView.getContext())
                     .load(crop.getImageUrl())
                     .placeholder(R.drawable.ic_notification)
                     .error(R.drawable.ic_notification)
                     .centerCrop()
                     .into(holder.ivCropImage);
            } else {
                // Set placeholder if no image URL
                holder.ivCropImage.setImageResource(R.drawable.ic_notification);
            }
        } else {
            // Log.e("CropAdapter", "ivCropImage is null for position " + position + ". Check item_crop.xml layout.");
        }
        
        // Set click listener
        holder.cardView.setOnClickListener(view -> {
            if (listener != null) {
                // Use the listener interface for clicks
                listener.onItemClick(crop);
            } else {
                // Fallback: Default behavior (open DetailActivity) if no listener provided
                Intent intent = new Intent(context, CropDetailActivity.class);
                // Explicitly cast Crop to Parcelable to resolve ambiguity
                intent.putExtra("crop", (Parcelable) crop);
                context.startActivity(intent);
            }
        });
    }

    // Helper method to format date string (optional, adjust as needed)
    private String formatDateStringForDisplay(String dateStr) {
        try {
            Date date = parseDateFormat.parse(dateStr);
            if (date != null) {
                return displayDateFormat.format(date);
            }
        } catch (ParseException e) {
            // Fallback to displaying the raw string if parsing fails
        }
        return dateStr; // Return original string if parsing fails or format is different
    }

    @Override
    public int getItemCount() {
        return cropList.size();
    }

    public class CropViewHolder extends RecyclerView.ViewHolder {
        
        CardView cardView;
        ImageView ivCropImage;
        TextView tvCropName, tvCropVariety, tvPlantingDate, tvHarvestDate;
        View statusIndicator;
        
        public CropViewHolder(@NonNull View itemView) {
            super(itemView);
            
            cardView = itemView.findViewById(R.id.card_view);
            ivCropImage = itemView.findViewById(R.id.iv_crop_image);
            tvCropName = itemView.findViewById(R.id.tv_crop_name);
            tvCropVariety = itemView.findViewById(R.id.tv_crop_variety);
            tvPlantingDate = itemView.findViewById(R.id.tv_planting_date);
            tvHarvestDate = itemView.findViewById(R.id.tv_harvest_date);
            statusIndicator = itemView.findViewById(R.id.status_indicator);
        }
    }
}