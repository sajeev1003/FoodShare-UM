package com.example.foodshare;

import android.net.Uri;
import com.google.firebase.firestore.Exclude;

public class FoodItem {
    private String id;
    private String title;
    private String description;
    private String location; // Human readable address
    private double latitude; // For Map
    private double longitude; // For Map
    private String imageUriString;
    private Uri imageUri;
    private int imageResId;
    private String timestamp;
    private String donorName;
    private boolean isHalal;
    private boolean isVegetarian;
    private boolean isVegan;
    private boolean isNutFree;
    private boolean isDairyFree;
    private int quantity;
    private String pickupTime;
    private String donorId;
    
    // Very New Field
    private boolean isFree; // True if free, False if discounted
    private double price;   // 0.0 if free
    private Long pickupStartTimestamp = 0L; // Default 0 to prevent NPE
    private Long expiryTimestamp = 0L;      // Default 0 to prevent NPE
    private String status; // "Available", "Expired", "Claimed"
    private boolean isDonorVerified; // Verification Badge Logic

    public FoodItem() {}

    // Constructor for user-created items
    public FoodItem(String id, String title, String description, String location, double latitude, double longitude, Uri imageUri, String timestamp, String donorName, int quantity, String pickupTime, boolean isHalal, boolean isVegetarian, boolean isVegan, boolean isNutFree, boolean isDairyFree, String donorId, boolean isFree, double price, long pickupStartTimestamp, long expiryTimestamp, boolean isDonorVerified) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.location = location;
        this.latitude = latitude;
        this.longitude = longitude;
        this.imageUri = imageUri;
        if (imageUri != null) this.imageUriString = imageUri.toString();
        this.timestamp = timestamp;
        this.donorName = donorName;
        this.quantity = quantity;
        this.pickupTime = pickupTime;
        this.isHalal = isHalal;
        this.isVegetarian = isVegetarian;
        this.isVegan = isVegan;
        this.isNutFree = isNutFree;
        this.isDairyFree = isDairyFree;
        this.donorId = donorId;
        this.isFree = isFree;
        this.price = price;
        this.pickupStartTimestamp = pickupStartTimestamp;
        this.expiryTimestamp = expiryTimestamp;
        this.status = "Available"; // Default
        this.isDonorVerified = isDonorVerified;
    }
    
    // Legacy Constructor for backward compatibility (defaults verified to false)
    public FoodItem(String id, String title, String description, String location, int imageResId, String timestamp, String donorName, boolean isHalal, boolean isVegetarian, boolean isVegan, boolean isNutFree, boolean isDairyFree, int quantity, String pickupTime, String donorId) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.location = location;
        this.imageUri = null;
        this.imageResId = imageResId;
        this.timestamp = timestamp;
        this.donorName = donorName;
        this.isHalal = isHalal;
        this.isVegetarian = isVegetarian;
        this.isVegan = isVegan;
        this.isNutFree = isNutFree;
        this.isDairyFree = isDairyFree;
        this.quantity = quantity;
        this.pickupTime = pickupTime;
        this.donorId = donorId;
        this.isFree = true; // Default to free for legacy items
        this.price = 0.0;
        this.pickupStartTimestamp = System.currentTimeMillis(); // Default to now
        this.expiryTimestamp = System.currentTimeMillis() + 86400000; // Default 24h from now
        this.status = "Available";
        this.isDonorVerified = false;
    }

    // Getters and Setters for existing fields...
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    
    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }
    
    public String getTimestamp() { return timestamp; }
    public void setTimestamp(String timestamp) { this.timestamp = timestamp; }
    
    public String getDonorName() { return donorName; }
    public void setDonorName(String donorName) { this.donorName = donorName; }
    
    public String getDonorId() { return donorId; }
    public void setDonorId(String donorId) { this.donorId = donorId; }
    
    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }
    
    public String getPickupTime() { return pickupTime; }
    public void setPickupTime(String pickupTime) { this.pickupTime = pickupTime; }
    
    public boolean isHalal() { return isHalal; }
    public void setHalal(boolean halal) { isHalal = halal; }
    
    public boolean isVegetarian() { return isVegetarian; }
    public void setVegetarian(boolean vegetarian) { isVegetarian = vegetarian; }
    
    public boolean isVegan() { return isVegan; }
    public void setVegan(boolean vegan) { isVegan = vegan; }
    
    public boolean isNutFree() { return isNutFree; }
    public void setNutFree(boolean nutFree) { isNutFree = nutFree; }
    
    public boolean isDairyFree() { return isDairyFree; }
    public void setDairyFree(boolean dairyFree) { isDairyFree = dairyFree; }
    
    @Exclude public Uri getImageUri() {
        if (imageUri == null && imageUriString != null) {
             try {
                 return Uri.parse(imageUriString);
             } catch (Exception e) {
                 return null;
             }
        }
        return imageUri;
    }
    public void setImageUri(Uri imageUri) {
        this.imageUri = imageUri;
        if(imageUri != null) this.imageUriString = imageUri.toString();
    }
    public String getImageUriString() { return imageUriString; }
    public void setImageUriString(String imageUriString) { this.imageUriString = imageUriString; }
    
    public int getImageResId() { return imageResId; }
    public void setImageResId(int imageResId) { this.imageResId = imageResId; }

    // New Getters/Setters
    public double getLatitude() { return latitude; }
    public void setLatitude(double latitude) { this.latitude = latitude; }
    
    public double getLongitude() { return longitude; }
    public void setLongitude(double longitude) { this.longitude = longitude; }

    public boolean isFree() { return isFree; }
    public void setFree(boolean free) { isFree = free; }

    public double getPrice() { return price; }
    public void setPrice(double price) { this.price = price; }

    // Use Long wrapper in setter to prevent unboxing NPEs if data is missing/null in DB
    public long getPickupStartTimestamp() { return pickupStartTimestamp != null ? pickupStartTimestamp : 0L; }
    public void setPickupStartTimestamp(Long pickupStartTimestamp) { 
        this.pickupStartTimestamp = pickupStartTimestamp != null ? pickupStartTimestamp : 0L; 
    }

    public long getExpiryTimestamp() { return expiryTimestamp != null ? expiryTimestamp : 0L; }
    public void setExpiryTimestamp(Long expiryTimestamp) { 
        this.expiryTimestamp = expiryTimestamp != null ? expiryTimestamp : 0L; 
    }
    
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public boolean isDonorVerified() { return isDonorVerified; }
    public void setDonorVerified(boolean donorVerified) { isDonorVerified = donorVerified; }
}
