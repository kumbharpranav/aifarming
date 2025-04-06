package com.farming.ai.utils;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.os.Handler;
import android.os.Looper;

import androidx.core.app.ActivityCompat;

import com.farming.ai.models.WeatherData;
import com.farming.ai.R;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.Calendar;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class WeatherUtils {

    private static final String API_KEY = "weatherapi";
    private static final String BASE_URL = "https://api.weatherapi.com/v1/current.json";
    private static final OkHttpClient client = new OkHttpClient();
    private static final Executor executor = Executors.newSingleThreadExecutor();
    private static final Handler mainHandler = new Handler(Looper.getMainLooper());

    /**
     * Interface to handle weather data callback
     */
    public interface WeatherDataCallback {
        void onWeatherDataReceived(WeatherData weatherData);
    }

    /**
     * Get weather data for the current location
     * @param context The context
     * @param callback The callback to receive weather data
     */
    public static void getWeatherData(Context context, WeatherDataCallback callback) {
        // Check location permission
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            callback.onWeatherDataReceived(null);
            return;
        }

        // Get current location
        LocationManager locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        if (locationManager != null) {
            try {
                Location lastKnownLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                if (lastKnownLocation == null) {
                    lastKnownLocation = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
                }

                if (lastKnownLocation != null) {
                    // Call weather API with location
                    fetchWeatherData(lastKnownLocation.getLatitude(), lastKnownLocation.getLongitude(), callback);
                } else {
                    // Use default location if no location available
                    fetchWeatherData(0, 0, callback);
                }
            } catch (Exception e) {
                e.printStackTrace();
                callback.onWeatherDataReceived(null);
            }
        } else {
            callback.onWeatherDataReceived(null);
        }
    }

    /**
     * Fetch weather data from the API
     * @param latitude The latitude
     * @param longitude The longitude
     * @param callback The callback to receive weather data
     */
    private static void fetchWeatherData(double latitude, double longitude, WeatherDataCallback callback) {
        String url = BASE_URL + "?key=" + API_KEY + "&q=" + latitude + "," + longitude;

        Request request = new Request.Builder()
                .url(url)
                .build();

        // Make async HTTP request
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                e.printStackTrace();
                executor.execute(() -> mainHandler.post(() -> callback.onWeatherDataReceived(null)));
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful() && response.body() != null) {
                    try {
                        JSONObject jsonObject = new JSONObject(response.body().string());
                        JSONObject current = jsonObject.getJSONObject("current");
                        JSONObject location = jsonObject.getJSONObject("location");
                        
                        WeatherData weatherData = new WeatherData(
                                current.getDouble("temp_c"),
                                current.getJSONObject("condition").getString("text"),
                                current.getInt("humidity"),
                                current.getDouble("wind_kph"),
                                location.getString("name")
                        );
                        
                        executor.execute(() -> mainHandler.post(() -> callback.onWeatherDataReceived(weatherData)));
                    } catch (JSONException e) {
                        e.printStackTrace();
                        executor.execute(() -> mainHandler.post(() -> callback.onWeatherDataReceived(null)));
                    }
                } else {
                    executor.execute(() -> mainHandler.post(() -> callback.onWeatherDataReceived(null)));
                }
            }
        });
    }

    public static boolean isNightTime() {
        Calendar c = Calendar.getInstance();
        int hour = c.get(Calendar.HOUR_OF_DAY);
        return hour >= 19 || hour < 5; // Between 7 PM and 5 AM
    }

    public static int getWeatherIcon(String condition) {
        boolean isNight = isNightTime();
        
        if (condition == null) {
            return R.drawable.ic_unknown;
        }
        
        condition = condition.toLowerCase();
        
        if (condition.contains("clear") || condition.contains("sunny")) {
            return isNight ? R.drawable.ic_night : R.drawable.ic_sunny;
        }
            
        if (condition.contains("cloud")) {
            return isNight ? R.drawable.ic_cloudy_night : R.drawable.ic_cloudy;
        }
            
        if (condition.contains("rain")) {
            return isNight ? R.drawable.ic_rain_night : R.drawable.ic_rain;
        }
            
        if (condition.contains("storm")) {
            return R.drawable.ic_storm;
        }
            
        if (condition.contains("snow")) {
            return isNight ? R.drawable.ic_snow_night : R.drawable.ic_snow;
        }
            
        return R.drawable.ic_unknown;
    }
}