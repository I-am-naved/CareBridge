package com.example.carebridge;

public class CareTask {
    private String id;
    private String userId;
    private String title;
    private String description;
    private String reason;
    private String assignedBy;
    private String appointmentId;
    private String dueDate;
    private String status; // "PENDING", "COMPLETED", "AWAITING_REVIEW"
    private String completionNotes;
    private long timestamp;

    public CareTask() {
        // Default constructor for Firebase
    }

    public CareTask(String id, String userId, String title, String description, String reason, String assignedBy, String appointmentId, String dueDate, String status, long timestamp) {
        this.id = id;
        this.userId = userId;
        this.title = title;
        this.description = description;
        this.reason = reason;
        this.assignedBy = assignedBy;
        this.appointmentId = appointmentId;
        this.dueDate = dueDate;
        this.status = status;
        this.timestamp = timestamp;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }

    public String getAssignedBy() { return assignedBy; }
    public void setAssignedBy(String assignedBy) { this.assignedBy = assignedBy; }

    public String getAppointmentId() { return appointmentId; }
    public void setAppointmentId(String appointmentId) { this.appointmentId = appointmentId; }

    public String getDueDate() { return dueDate; }
    public void setDueDate(String dueDate) { this.dueDate = dueDate; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getCompletionNotes() { return completionNotes; }
    public void setCompletionNotes(String completionNotes) { this.completionNotes = completionNotes; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
}
