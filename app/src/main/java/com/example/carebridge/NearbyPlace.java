package com.example.carebridge;

public class NearbyPlace {
    private String id;
    private String name;
    private String address;
    private String category; // "hospital", "clinic", "pharmacy"
    private double latitude;
    private double longitude;
    private double distanceKm;

    public NearbyPlace(String id, String name, String address, String category, double latitude, double longitude, double distanceKm) {
        this.id = id;
        this.name = name;
        this.address = address;
        this.category = category;
        this.latitude = latitude;
        this.longitude = longitude;
        this.distanceKm = distanceKm;
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public String getAddress() { return address; }
    public String getCategory() { return category; }
    public double getLatitude() { return latitude; }
    public double getLongitude() { return longitude; }
    public double getDistanceKm() { return distanceKm; }
    public void setDistanceKm(double distanceKm) { this.distanceKm = distanceKm; }
}
