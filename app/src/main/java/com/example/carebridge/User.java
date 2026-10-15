package com.example.carebridge;

public class User {
    private String fullName;
    private String userId;
    private String email;
    private String password;
    private long createdAt;

    public User() {
        // Default constructor required for Firebase DataSnapshot.getValue(User.class)
    }

    public User(String fullName, String userId, String email, String password, long createdAt) {
        this.fullName = fullName;
        this.userId = userId;
        this.email = email;
        this.password = password;
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

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }
}
