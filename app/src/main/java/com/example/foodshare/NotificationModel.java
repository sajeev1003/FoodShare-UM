package com.example.foodshare;

public class NotificationModel {
    private String id;
    private String title;
    private String message;
    private Long timestamp;
    private boolean isRead;
    private String type;
    private String targetId;

    public NotificationModel() {} // Required for Firestore

    public NotificationModel(String id, String title, String message, Long timestamp, boolean isRead, String type, String targetId) {
        this.id = id;
        this.title = title;
        this.message = message;
        this.timestamp = timestamp;
        this.isRead = isRead;
        this.type = type;
        this.targetId = targetId;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public Long getTimestamp() { return timestamp; }
    public void setTimestamp(Long timestamp) { this.timestamp = timestamp; }

    @com.google.firebase.firestore.PropertyName("isRead")
    public boolean isRead() { return isRead; }
    
    @com.google.firebase.firestore.PropertyName("isRead")
    public void setRead(boolean read) { isRead = read; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getTargetId() { return targetId; }
    public void setTargetId(String targetId) { this.targetId = targetId; }
}