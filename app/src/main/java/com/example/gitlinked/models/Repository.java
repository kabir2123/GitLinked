package com.example.gitlinked.models;

import com.google.gson.annotations.SerializedName;

public class Repository {
    @SerializedName("name")
    private String name;

    @SerializedName("description")
    private String description;

    @SerializedName("language")
    private String language;

    @SerializedName("stargazers_count")
    private int stars;

    @SerializedName("forks_count")
    private int forks;

    @SerializedName("html_url")
    private String url;

    @SerializedName("fork")
    private boolean isFork;

    public Repository() {}

    public Repository(String name, String description, String language, int stars, int forks, String url) {
        this.name = name;
        this.description = description;
        this.language = language;
        this.stars = stars;
        this.forks = forks;
        this.url = url;
    }

    // Getters and Setters
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getLanguage() { return language; }
    public void setLanguage(String language) { this.language = language; }

    public int getStars() { return stars; }
    public void setStars(int stars) { this.stars = stars; }

    public int getForks() { return forks; }
    public void setForks(int forks) { this.forks = forks; }

    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }

    public boolean isFork() { return isFork; }
    public void setFork(boolean fork) { isFork = fork; }
}
