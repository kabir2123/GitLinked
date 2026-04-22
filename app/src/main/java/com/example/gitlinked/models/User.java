package com.example.gitlinked.models;

import java.util.ArrayList;
import java.util.List;

public class User {
    private String id;
    private String username;
    private String avatarUrl;
    private String bio;
    private String email;
    private List<String> languages;
    private List<String> interests;
    private List<Repository> topRepos;
    private String bleDeviceAddress;
    private double latitude;
    private double longitude;
    private boolean isOnline;

    public User() {
        this.languages = new ArrayList<>();
        this.interests = new ArrayList<>();
        this.topRepos = new ArrayList<>();
    }

    public User(String id, String username, String avatarUrl, String bio) {
        this();
        this.id = id;
        this.username = username;
        this.avatarUrl = avatarUrl;
        this.bio = bio;
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getAvatarUrl() { return avatarUrl; }
    public void setAvatarUrl(String avatarUrl) { this.avatarUrl = avatarUrl; }

    public String getBio() { return bio; }
    public void setBio(String bio) { this.bio = bio; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public List<String> getLanguages() { return languages; }
    public void setLanguages(List<String> languages) { this.languages = languages; }

    public List<String> getInterests() { return interests; }
    public void setInterests(List<String> interests) { this.interests = interests; }

    public List<Repository> getTopRepos() { return topRepos; }
    public void setTopRepos(List<Repository> topRepos) { this.topRepos = topRepos; }

    public String getBleDeviceAddress() { return bleDeviceAddress; }
    public void setBleDeviceAddress(String bleDeviceAddress) { this.bleDeviceAddress = bleDeviceAddress; }

    public double getLatitude() { return latitude; }
    public void setLatitude(double latitude) { this.latitude = latitude; }

    public double getLongitude() { return longitude; }
    public void setLongitude(double longitude) { this.longitude = longitude; }

    public boolean isOnline() { return isOnline; }
    public void setOnline(boolean online) { isOnline = online; }
}
