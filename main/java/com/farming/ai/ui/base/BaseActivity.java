package com.farming.ai.ui.base;

import android.content.Context;
import android.content.res.Configuration;
import android.os.Bundle;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import com.farming.ai.utils.LocaleHelper;

public abstract class BaseActivity extends AppCompatActivity {
    
    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(LocaleHelper.updateResources(base, LocaleHelper.getLanguage(base)));
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        LocaleHelper.updateResources(this, LocaleHelper.getLanguage(this));
    }
}
