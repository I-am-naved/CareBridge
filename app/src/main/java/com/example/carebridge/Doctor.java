package com.example.carebridge;

public class Doctor {
    private String id;
    private String name;
    private String specialty;
    private String location;
    private boolean available;
    private String scheduleSummary;

    public Doctor() {
        // Default constructor for Firebase
    }

    public Doctor(String id, String name, String specialty, String location, boolean available, String scheduleSummary) {
        this.id = id;
        this.name = name;
        this.specialty = specialty;
        this.location = location;
        this.available = available;
        this.scheduleSummary = scheduleSummary;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getSpecialty() { return specialty; }
    public void setSpecialty(String specialty) { this.specialty = specialty; }

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }

    public boolean isAvailable() { return available; }
    public void setAvailable(boolean available) { this.available = available; }

    public String getScheduleSummary() { return scheduleSummary; }
    public void setScheduleSummary(String scheduleSummary) { this.scheduleSummary = scheduleSummary; }
}
