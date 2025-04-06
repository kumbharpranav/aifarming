package com.farming.ai.models;

import com.google.firebase.database.Exclude;
import com.google.firebase.database.ServerValue;

import android.os.Parcel;
import android.os.Parcelable;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

public class Crop implements Parcelable, Serializable {

    private String id;
    private String userEmail; // Store encoded email
    private String name;
    private String variety;
    private String growthStage;
    private String plantingDate; // Keep as String for simplicity, or use Long for timestamp
    private String location;
    private String notes;
    private String imageUrl;
    private Long timestamp; // Use Long for reading/writing timestamps

    // New Fields
    private Object age; // Use Object for flexibility (String or Number)
    private String expectedHarvestDate;
    private String farmArea;
    private boolean isWatered;
    private String lastWateredTime;
    private boolean needsAttention;
    private int wateringIntervalHours;
    private String soilType;

    // Default constructor required for Firebase
    public Crop() {
    }

    // Constructor with all fields
    public Crop(String id, String userEmail, String name, String variety, String growthStage,
                String plantingDate, String location, String notes, String imageUrl, Long timestamp,
                Object age, String expectedHarvestDate, String farmArea, boolean isWatered,
                String lastWateredTime, boolean needsAttention, int wateringIntervalHours,
                String soilType) {
        this.id = id;
        this.userEmail = userEmail;
        this.name = name;
        this.variety = variety;
        this.growthStage = growthStage;
        this.plantingDate = plantingDate;
        this.location = location;
        this.notes = notes;
        this.imageUrl = imageUrl;
        this.timestamp = timestamp;
        this.age = age;
        this.expectedHarvestDate = expectedHarvestDate;
        this.farmArea = farmArea;
        this.isWatered = isWatered;
        this.lastWateredTime = lastWateredTime;
        this.needsAttention = needsAttention;
        this.wateringIntervalHours = wateringIntervalHours;
        this.soilType = soilType;
    }

    // Parcelable implementation
    protected Crop(Parcel in) {
        id = in.readString();
        userEmail = in.readString();
        name = in.readString();
        variety = in.readString();
        growthStage = in.readString();
        plantingDate = in.readString();
        location = in.readString();
        notes = in.readString();
        imageUrl = in.readString();
        timestamp = in.readLong();
        age = in.readString();
        expectedHarvestDate = in.readString();
        farmArea = in.readString();
        isWatered = in.readByte() != 0;
        lastWateredTime = in.readString();
        needsAttention = in.readByte() != 0;
        wateringIntervalHours = in.readInt();
        soilType = in.readString();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(id);
        dest.writeString(userEmail);
        dest.writeString(name);
        dest.writeString(variety);
        dest.writeString(growthStage);
        dest.writeString(plantingDate);
        dest.writeString(location);
        dest.writeString(notes);
        dest.writeString(imageUrl);
        dest.writeLong(timestamp);
        dest.writeString(String.valueOf(age));
        dest.writeString(expectedHarvestDate);
        dest.writeString(farmArea);
        dest.writeByte((byte) (isWatered ? 1 : 0));
        dest.writeString(lastWateredTime);
        dest.writeByte((byte) (needsAttention ? 1 : 0));
        dest.writeInt(wateringIntervalHours);
        dest.writeString(soilType);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<Crop> CREATOR = new Creator<Crop>() {
        @Override
        public Crop createFromParcel(Parcel in) {
            return new Crop(in);
        }

        @Override
        public Crop[] newArray(int size) {
            return new Crop[size];
        }
    };

    @Exclude
    public String getId() { return id; }
    public String getUserEmail() { return userEmail; }
    public String getName() { return name; }
    public String getVariety() { return variety; }
    public String getGrowthStage() { return growthStage; }
    public String getPlantingDate() { return plantingDate; }
    public String getLocation() { return location; }
    public String getNotes() { return notes; }
    public String getImageUrl() { return imageUrl; }
    public Long getTimestamp() { return timestamp; } // Changed return type
    public Object getAge() { return age; } // Changed return type
    public String getExpectedHarvestDate() { return expectedHarvestDate; }
    public String getFarmArea() { return farmArea; }
    public boolean isWatered() { return isWatered; } // Use isWatered for boolean getter
    public String getLastWateredTime() { return lastWateredTime; }
    public boolean isNeedsAttention() { return needsAttention; } // Use isNeedsAttention for boolean getter
    public int getWateringIntervalHours() { return wateringIntervalHours; }
    public String getSoilType() { return soilType; }

    public void setId(String id) { this.id = id; }
    public void setUserEmail(String userEmail) { this.userEmail = userEmail; }
    public void setName(String name) { this.name = name; }
    public void setVariety(String variety) { this.variety = variety; }
    public void setGrowthStage(String growthStage) { this.growthStage = growthStage; }
    public void setPlantingDate(String plantingDate) { this.plantingDate = plantingDate; }
    public void setLocation(String location) { this.location = location; }
    public void setNotes(String notes) { this.notes = notes; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
    public void setTimestamp(Long timestamp) { this.timestamp = timestamp; } // Changed parameter type
    public void setAge(Object age) { this.age = age; } // Changed parameter type
    public void setExpectedHarvestDate(String expectedHarvestDate) { this.expectedHarvestDate = expectedHarvestDate; }
    public void setFarmArea(String farmArea) { this.farmArea = farmArea; }
    public void setWatered(boolean watered) { isWatered = watered; }
    public void setLastWateredTime(String lastWateredTime) { this.lastWateredTime = lastWateredTime; }
    public void setNeedsAttention(boolean needsAttention) { this.needsAttention = needsAttention; }
    public void setWateringIntervalHours(int wateringIntervalHours) { this.wateringIntervalHours = wateringIntervalHours; }
    public void setSoilType(String soilType) { this.soilType = soilType; }

    @Exclude
    public Map<String, Object> toMap() {
        HashMap<String, Object> result = new HashMap<>();
        result.put("id", id);
        result.put("userEmail", userEmail);
        result.put("name", name);
        result.put("variety", variety);
        result.put("growthStage", growthStage);
        result.put("plantingDate", plantingDate);
        result.put("location", location);
        result.put("notes", notes);
        result.put("imageUrl", imageUrl);
        result.put("timestamp", timestamp == null ? ServerValue.TIMESTAMP : timestamp); // Handle null for new crops

        // Add new fields
        result.put("age", age);
        result.put("expectedHarvestDate", expectedHarvestDate);
        result.put("farmArea", farmArea);
        result.put("isWatered", isWatered);
        result.put("lastWateredTime", lastWateredTime);
        result.put("needsAttention", needsAttention);
        result.put("wateringIntervalHours", wateringIntervalHours);
        result.put("soilType", soilType);

        return result;
    }
}