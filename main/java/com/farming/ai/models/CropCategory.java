package com.farming.ai.models;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CropCategory {
    // Change to public for access
    public static final Map<String, List<String>> CROP_VARIETIES = createCropVarietiesMap();

    private static Map<String, List<String>> createCropVarietiesMap() {
        Map<String, List<String>> map = new HashMap<>();
        
        // Food Grains
        map.put("Rice", Arrays.asList("Basmati", "IR8", "Sona Masuri", "Ponni"));
        map.put("Wheat", Arrays.asList("Common Wheat", "Durum", "Emmer", "Einkorn"));
        map.put("Maize", Arrays.asList("Sweet Corn", "Popcorn", "Dent Corn", "Flint Corn"));
        map.put("Barley", Arrays.asList("Two-Row", "Six-Row", "Hull-less"));
        map.put("Sorghum", Arrays.asList("Grain Sorghum", "Sweet Sorghum", "Forage Sorghum"));
        map.put("Pearl Millet", Arrays.asList("Hybrid", "Local", "Improved"));
        map.put("Finger Millet", Arrays.asList("GPU 28", "Indaf 15", "PR 202"));

        // Pulses
        map.put("Chickpea", Arrays.asList("Desi", "Kabuli"));
        map.put("Pigeon Pea", Arrays.asList("Early", "Medium", "Late Duration"));
        map.put("Green Gram", Arrays.asList("Pusa Vishal", "Pusa Baisakhi"));
        map.put("Black Gram", Arrays.asList("T-9", "Pant U-30"));
        map.put("Lentils", Arrays.asList("Red", "Green", "Brown"));
        map.put("Horse Gram", Arrays.asList("PHG-9", "VLG-1"));

        // Oilseeds
        map.put("Mustard", Arrays.asList("Yellow", "Brown", "Black"));
        map.put("Groundnut", Arrays.asList("Virginia", "Spanish", "Valencia"));
        map.put("Soybean", Arrays.asList("Punjab-1", "JS-335"));
        map.put("Sunflower", Arrays.asList("KBSH-44", "DRSH-1"));
        map.put("Sesame", Arrays.asList("White", "Black", "Brown"));
        map.put("Linseed", Arrays.asList("LC-185", "Padmini"));
        map.put("Castor", Arrays.asList("DCH-177", "GCH-4"));

        // Cash Crops
        map.put("Cotton", Arrays.asList("Upland", "Egyptian", "Pima"));
        map.put("Sugarcane", Arrays.asList("Early", "Mid-Late", "Late"));
        map.put("Jute", Arrays.asList("White", "Tossa"));
        map.put("Tobacco", Arrays.asList("Flue-Cured", "Air-Cured"));
        map.put("Rubber", Arrays.asList("RRII 105", "RRIM 600"));
        map.put("Tea", Arrays.asList("Assam", "Chinese", "Cambod"));
        map.put("Coffee", Arrays.asList("Arabica", "Robusta"));

        // Horticulture - Fruits
        map.put("Mango", Arrays.asList("Alphonso", "Dasheri", "Langra"));
        map.put("Banana", Arrays.asList("Cavendish", "Red Banana", "Nendran"));
        map.put("Apple", Arrays.asList("Red Delicious", "Golden Delicious", "McIntosh"));
        map.put("Grapes", Arrays.asList("Thompson Seedless", "Flame Seedless"));
        map.put("Orange", Arrays.asList("Valencia", "Nagpur", "Kinnow"));

        // Horticulture - Vegetables
        map.put("Tomato", Arrays.asList("Roma", "Beefsteak", "Cherry"));
        map.put("Potato", Arrays.asList("Kufri Jyoti", "Kufri Pukhraj"));
        map.put("Onion", Arrays.asList("Red", "White", "Yellow"));
        map.put("Brinjal", Arrays.asList("Long Purple", "Round Black"));
        map.put("Okra", Arrays.asList("Pusa Sawani", "Arka Anamika"));

        // Spices
        map.put("Turmeric", Arrays.asList("Alleppey", "Salem", "Madras"));
        map.put("Chili", Arrays.asList("Kashmiri", "Guntur", "Bhut Jolokia"));
        map.put("Coriander", Arrays.asList("Local", "CS-6", "GC-1"));
        map.put("Cumin", Arrays.asList("GC-1", "GC-2", "RZ-19"));

        // Medicinal Plants
        map.put("Tulsi", Arrays.asList("Krishna", "Rama", "Vana"));
        map.put("Ashwagandha", Arrays.asList("JA-20", "JA-134"));
        map.put("Aloe Vera", Arrays.asList("Indian", "Cape"));

        return map;
    }

    public static String detectVariety(String cropType, Map<String, Object> visionResults) {
        List<String> varieties = CROP_VARIETIES.get(cropType);
        if (varieties == null || varieties.isEmpty()) {
            return "Common";
        }
        return varieties.get(0);
    }

    public static List<String> getVarieties(String cropType) {
        return CROP_VARIETIES.getOrDefault(cropType, Arrays.asList("Common"));
    }
}
