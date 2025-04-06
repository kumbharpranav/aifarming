package com.farming.ai.utils;

import com.farming.ai.R;

public class WeatherIconMapper {
    public static int getWeatherIcon(String condition) {
        condition = condition.toLowerCase();
        if (condition.contains("clear") || condition.contains("sunny")) {
            return R.drawable.ic_weather_sunny;
        } else if (condition.contains("rain") || condition.contains("drizzle")) {
            return R.drawable.ic_weather_rainy;
        } else if (condition.contains("cloud") || condition.contains("overcast")) {
            return R.drawable.ic_weather_cloudy;
        } else if (condition.contains("thunder") || condition.contains("storm")) {
            return R.drawable.ic_weather_storm;
        }
        return R.drawable.ic_weather_default;
    }

    public static int getWeatherDescription(String condition) {
        condition = condition.toLowerCase();
        if (condition.contains("rain") || condition.contains("drizzle")) {
            return R.string.weather_rainy;
        } else if (condition.contains("cloud") || condition.contains("overcast")) {
            return R.string.weather_cloudy;
        } else if (condition.contains("thunder") || condition.contains("storm")) {
            return R.string.weather_stormy;
        }
        return R.string.weather_sunny;
    }
}
