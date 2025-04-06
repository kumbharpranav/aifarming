package com.farming.ai.utils;

import android.content.Context;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.Interpolator;
import androidx.recyclerview.widget.LinearSmoothScroller;

public class UltraSmoothScroller extends LinearSmoothScroller {
    private static final float MILLISECONDS_PER_INCH = 150f;
    private final Interpolator interpolator = new DecelerateInterpolator(1.5f);

    public UltraSmoothScroller(Context context) {
        super(context);
    }

    @Override
    protected float calculateSpeedPerPixel(android.util.DisplayMetrics displayMetrics) {
        return MILLISECONDS_PER_INCH / displayMetrics.densityDpi;
    }

    @Override
    protected int calculateTimeForScrolling(int dx) {
        int time = super.calculateTimeForScrolling(dx);
        float value = interpolator.getInterpolation(Math.min(1f, dx / 1000f));
        return (int) (time * value);
    }
}
