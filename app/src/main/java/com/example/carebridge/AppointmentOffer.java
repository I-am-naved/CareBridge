package com.example.carebridge;

import java.util.List;

public class AppointmentOffer {
    private String id;
    private String userId;
    private String patientName;
    private String doctorId;
    private String providerName;
    private String department;
    private String location;
    private String status; // "PENDING", "CONFIRMED"
    private String bookedSlot;
    private List<String> availableSlots;
    private long timestamp;
    private long confirmedAt;

    public AppointmentOffer() {
        // Default constructor for Firebase
    }

    public AppointmentOffer(String id, String userId, String patientName, String doctorId, String providerName, String department, String location, String status, List<String> availableSlots, long timestamp) {
        this.id = id;
        this.userId = userId;
        this.patientName = patientName;
        this.doctorId = doctorId;
        this.providerName = providerName;
        this.department = department;
        this.location = location;
        this.status = status;
        this.availableSlots = availableSlots;
        this.timestamp = timestamp;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getPatientName() { return patientName; }
    public void setPatientName(String patientName) { this.patientName = patientName; }

    public String getDoctorId() { return doctorId; }
    public void setDoctorId(String doctorId) { this.doctorId = doctorId; }

    public String getProviderName() { return providerName; }
    public void setProviderName(String providerName) { this.providerName = providerName; }

    public String getDepartment() { return department; }
    public void setDepartment(String department) { this.department = department; }

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getBookedSlot() { return bookedSlot; }
    public void setBookedSlot(String bookedSlot) { this.bookedSlot = bookedSlot; }

    public List<String> getAvailableSlots() { return availableSlots; }
    public void setAvailableSlots(List<String> availableSlots) { this.availableSlots = availableSlots; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }

    public long getConfirmedAt() { return confirmedAt; }
    public void setConfirmedAt(long confirmedAt) { this.confirmedAt = confirmedAt; }
}
