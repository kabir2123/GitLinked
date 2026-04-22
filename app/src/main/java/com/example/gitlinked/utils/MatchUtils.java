package com.example.gitlinked.utils;

import com.example.gitlinked.models.MatchResult;
import com.example.gitlinked.models.User;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Match algorithm:
 * match = (common_languages / total_unique_languages) * 70
 *       + (common_interests / total_unique_interests) * 30
 */
public class MatchUtils {

    /**
     * Calculate match percentage between two users.
     */
    public static MatchResult calculateMatch(User currentUser, User otherUser) {
        // Calculate common languages
        List<String> commonLangs = new ArrayList<>();
        Set<String> allLangs = new HashSet<>();

        if (currentUser.getLanguages() != null && otherUser.getLanguages() != null) {
            allLangs.addAll(currentUser.getLanguages());
            allLangs.addAll(otherUser.getLanguages());

            for (String lang : currentUser.getLanguages()) {
                if (otherUser.getLanguages().contains(lang)) {
                    commonLangs.add(lang);
                }
            }
        }

        // Calculate common interests
        List<String> commonInterests = new ArrayList<>();
        Set<String> allInterests = new HashSet<>();

        if (currentUser.getInterests() != null && otherUser.getInterests() != null) {
            allInterests.addAll(currentUser.getInterests());
            allInterests.addAll(otherUser.getInterests());

            for (String interest : currentUser.getInterests()) {
                if (otherUser.getInterests().contains(interest)) {
                    commonInterests.add(interest);
                }
            }
        }

        // Formula: (common_lang / total_lang) * 70 + (common_interest / total_interest) * 30
        double langScore = allLangs.size() > 0
                ? (double) commonLangs.size() / allLangs.size() * 70.0
                : 0;
        double interestScore = allInterests.size() > 0
                ? (double) commonInterests.size() / allInterests.size() * 30.0
                : 0;

        int matchPercentage = (int) Math.round(langScore + interestScore);
        matchPercentage = Math.min(100, Math.max(0, matchPercentage)); // Clamp 0-100

        MatchResult result = new MatchResult();
        result.setUser(otherUser);
        result.setMatchPercentage(matchPercentage);
        result.setCommonLanguages(commonLangs);
        result.setCommonInterests(commonInterests);

        return result;
    }

    /**
     * Get a color resource based on match percentage.
     * High (>= 70): green, Medium (>= 40): orange, Low: red
     */
    public static String getMatchColor(int percentage) {
        if (percentage >= 70) return "#FF3FB950";      // Green
        else if (percentage >= 40) return "#FFD29922";  // Orange
        else return "#FFFF7B72";                        // Red
    }
}
