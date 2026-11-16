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
    private String assignedBy;
    private String reason;
    private String status;     // e.g. "PENDING", "AWAITING_REVIEW", "COMPLETED"

    public JourneyItem() {
        // Default constructor for Firebase
    }

    public JourneyItem(String id, String userId, String stepNumber, String category, String title, String description, String date, long timestamp) {
        this(id, userId, stepNumber, category, title, description, date, timestamp, null, null, null);
    }

    public JourneyItem(String id, String userId, String stepNumber, String category, String title, String description, String date, long timestamp, String assignedBy, String reason, String status) {
        this.id = id;
        this.userId = userId;
        this.stepNumber = stepNumber;
        this.category = category;
        this.title = title;
        this.description = description;
        this.date = date;
        this.timestamp = timestamp;
        this.assignedBy = assignedBy;
        this.reason = reason;
        this.status = status;
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

    public String getAssignedBy() { return assignedBy; }
    public void setAssignedBy(String assignedBy) { this.assignedBy = assignedBy; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
