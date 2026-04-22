package com.example.gitlinked.models;

import java.util.ArrayList;
import java.util.List;

public class MatchResult {
    private User user;
    private int matchPercentage;
    private List<String> commonLanguages;
    private List<String> commonInterests;

    public MatchResult() {
        this.commonLanguages = new ArrayList<>();
        this.commonInterests = new ArrayList<>();
    }

    public MatchResult(User user, int matchPercentage, List<String> commonLanguages, List<String> commonInterests) {
        this.user = user;
        this.matchPercentage = matchPercentage;
        this.commonLanguages = commonLanguages;
        this.commonInterests = commonInterests;
    }

    // Getters and Setters
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public int getMatchPercentage() { return matchPercentage; }
    public void setMatchPercentage(int matchPercentage) { this.matchPercentage = matchPercentage; }

    public List<String> getCommonLanguages() { return commonLanguages; }
    public void setCommonLanguages(List<String> commonLanguages) { this.commonLanguages = commonLanguages; }

    public List<String> getCommonInterests() { return commonInterests; }
    public void setCommonInterests(List<String> commonInterests) { this.commonInterests = commonInterests; }

    public String getMatchSummary() {
        StringBuilder sb = new StringBuilder();
        if (!commonLanguages.isEmpty()) {
            sb.append("Both use ");
            sb.append(String.join(", ", commonLanguages));
        }
        if (!commonInterests.isEmpty()) {
            if (sb.length() > 0) sb.append(" · ");
            sb.append("Share interest in ");
            sb.append(String.join(", ", commonInterests));
        }
        return sb.toString();
    }
}
