package com.farming.ai.ui.crops;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import com.farming.ai.R;
import com.farming.ai.models.Crop;

public class CropDetailsActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_crop_detail);
        
        Crop crop = (Crop) getIntent().getSerializableExtra("crop");
        if (crop != null) {
            setupViews(crop);
        }
    }
    
    private void setupViews(Crop crop) {
        // TODO: Initialize and populate views
    }
}
