package com.example.foodshare;

import com.google.firebase.firestore.DocumentId;

public class Reservation {

    // We use the name YOU want: documentId
    // The @DocumentId tag ensures Firestore autofills it (Fixes the "No ID" error)
    @DocumentId
    private String documentId;

    private String locationName;
    private String itemName;
    private String pickupTime;
    private String portion;
    private String status;
    private String pickupId;
    private String userId;
    private String donorId;
    private String foodId;

    public Reservation() {}

    public Reservation(String documentId, String locationName, String itemName, String pickupTime, String portion, String status, String pickupId, String userId, String donorId, String foodId) {
        this.documentId = documentId;
        this.locationName = locationName;
        this.itemName = itemName;
        this.pickupTime = pickupTime;
        this.portion = portion;
        this.status = status;
        this.pickupId = pickupId;
        this.userId = userId;
        this.donorId = donorId;
        this.foodId = foodId;
    }

    // --- RESTORED: The method your other files are looking for ---
    public String getDocumentId() {
        return documentId;
    }

    public void setDocumentId(String documentId) {
        this.documentId = documentId;
    }

    // --- SAFETY: Keep getId() too, just in case any file uses it ---
    public String getId() {
        return documentId;
    }

    public void setId(String id) {
        this.documentId = id;
    }

    // --- Standard Getters/Setters ---
    public String getLocationName() { return locationName; }
    public void setLocationName(String locationName) { this.locationName = locationName; }

    public String getItemName() { return itemName; }
    public void setItemName(String itemName) { this.itemName = itemName; }

    public String getPickupTime() { return pickupTime; }
    public void setPickupTime(String pickupTime) { this.pickupTime = pickupTime; }

    public String getPortion() { return portion; }
    public void setPortion(String portion) { this.portion = portion; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getPickupId() { return pickupId; }
    public void setPickupId(String pickupId) { this.pickupId = pickupId; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getDonorId() { return donorId; }
    public void setDonorId(String donorId) { this.donorId = donorId; }

    public String getFoodId() { return foodId; }
    public void setFoodId(String foodId) { this.foodId = foodId; }
}