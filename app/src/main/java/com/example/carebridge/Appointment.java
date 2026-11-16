package com.example.carebridge;

public class Appointment {
    private String id;
    private String userId;
    private String providerName;
    private String department;
    private String location;
    private String date;
    private String time;
    private String status; // e.g. "Confirmed", "Available", "Completed"
    private String notes;
    private long timestamp;

    public Appointment() {
        // Default constructor required for Firebase DataSnapshot.getValue(Appointment.class)
    }

    public Appointment(String id, String userId, String providerName, String department, String location, String date, String time, String status, String notes, long timestamp) {
        this.id = id;
        this.userId = userId;
        this.providerName = providerName;
        this.department = department;
        this.location = location;
        this.date = date;
        this.time = time;
        this.status = status;
        this.notes = notes;
        this.timestamp = timestamp;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getProviderName() { return providerName; }
    public void setProviderName(String providerName) { this.providerName = providerName; }

    public String getDepartment() { return department; }
    public void setDepartment(String department) { this.department = department; }

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }

    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }

    public String getTime() { return time; }
    public void setTime(String time) { this.time = time; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
}
