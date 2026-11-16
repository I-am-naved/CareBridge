package com.example.carebridge;

public class NearbyPlace {
    private String id;
    private String name;
    private String address;
    private String category; // "hospital", "clinic", "pharmacy", "laboratory"
    private double latitude;
    private double longitude;
    private double distanceKm;
    private String phoneNumber;
    private String openingHours;
    private String websiteUrl;
    private double rating;

    public NearbyPlace(String id, String name, String address, String category, double latitude, double longitude, double distanceKm) {
        this(id, name, address, category, latitude, longitude, distanceKm, null, null, null, 0.0);
    }

    public NearbyPlace(String id, String name, String address, String category, double latitude, double longitude, double distanceKm, String phoneNumber, String openingHours, String websiteUrl, double rating) {
        this.id = id;
        this.name = name;
        this.address = address;
        this.category = category;
        this.latitude = latitude;
        this.longitude = longitude;
        this.distanceKm = distanceKm;
        this.phoneNumber = phoneNumber;
        this.openingHours = openingHours;
        this.websiteUrl = websiteUrl;
        this.rating = rating;
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public String getAddress() { return address; }
    public String getCategory() { return category; }
    public double getLatitude() { return latitude; }
    public double getLongitude() { return longitude; }
    public double getDistanceKm() { return distanceKm; }
    public void setDistanceKm(double distanceKm) { this.distanceKm = distanceKm; }

    public String getPhoneNumber() { return phoneNumber; }
    public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }

    public String getOpeningHours() { return openingHours; }
    public void setOpeningHours(String openingHours) { this.openingHours = openingHours; }

    public String getWebsiteUrl() { return websiteUrl; }
    public void setWebsiteUrl(String websiteUrl) { this.websiteUrl = websiteUrl; }

    public double getRating() { return rating; }
    public void setRating(double rating) { this.rating = rating; }
}
