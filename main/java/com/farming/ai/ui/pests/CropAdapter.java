package com.farming.ai.ui.pests;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.farming.ai.R;
import com.farming.ai.models.Crop;
import java.util.List;

public class CropAdapter extends RecyclerView.Adapter<CropAdapter.CropViewHolder> {
    private final List<Crop> crops;
    private final OnCropClickListener listener;

    public interface OnCropClickListener {
        void onCropClick(Crop crop);
    }

    public CropAdapter(List<Crop> crops, OnCropClickListener listener) {
        this.crops = crops;
        this.listener = listener;
    }

    @NonNull
    @Override
    public CropViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_crop, parent, false);
        return new CropViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CropViewHolder holder, int position) {
        Crop crop = crops.get(position);
        holder.tvCropName.setText(crop.getName());
        holder.tvCropVariety.setText(crop.getVariety());
        
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onCropClick(crop);
            }
        });
    }

    @Override
    public int getItemCount() {
        return crops.size();
    }

    static class CropViewHolder extends RecyclerView.ViewHolder {
        TextView tvCropName;
        TextView tvCropVariety;

        CropViewHolder(View view) {
            super(view);
            tvCropName = view.findViewById(R.id.tv_crop_name);
            tvCropVariety = view.findViewById(R.id.tv_crop_variety);
        }
    }
}
