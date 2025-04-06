package com.farming.ai.models;

public class WeatherData {
    private double temperature;
    private String condition;
    private int humidity;
    private double windSpeed;
    private String location;

    public WeatherData(double temperature, String condition, int humidity, double windSpeed, String location) {
        this.temperature = temperature;
        this.condition = condition;
        this.humidity = humidity;
        this.windSpeed = windSpeed;
        this.location = location;
    }

    public double getTemperature() { return temperature; }
    public String getCondition() { return condition; }
    public int getHumidity() { return humidity; }
    public double getWindSpeed() { return windSpeed; }
    public String getLocation() { return location; }
}