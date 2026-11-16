package com.example.carebridge;

public class User {
    private String fullName;
    private String userId;
    private String email;
    private String role; // "patient" or "admin"
    private long createdAt;

    public User() {
        // Default constructor required for Firebase DataSnapshot.getValue(User.class)
    }

    public User(String fullName, String userId, String email, String role, long createdAt) {
        this.fullName = fullName;
        this.userId = userId;
        this.email = email;
        this.role = role;
        this.createdAt = createdAt;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }
}
