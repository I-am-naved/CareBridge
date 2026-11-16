package com.example.carebridge;

public class JourneyItem {
    private String id;
    private String userId;
    private String stepNumber; // e.g. "01", "02"
    private String category;   // e.g. "Doctor visit", "Referral", "Follow-up", "Appointment"
    private String title;
    private String description;
    private String date;
    private long timestamp;

    public JourneyItem() {
        // Default constructor for Firebase
    }

    public JourneyItem(String id, String userId, String stepNumber, String category, String title, String description, String date, long timestamp) {
        this.id = id;
        this.userId = userId;
        this.stepNumber = stepNumber;
        this.category = category;
        this.title = title;
        this.description = description;
        this.date = date;
        this.timestamp = timestamp;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getStepNumber() { return stepNumber; }
    public void setStepNumber(String stepNumber) { this.stepNumber = stepNumber; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
}
