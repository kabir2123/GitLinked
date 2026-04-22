package com.example.gitlinked.services;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;

import com.example.gitlinked.api.GitHubService;
import com.example.gitlinked.database.UserDao;
import com.example.gitlinked.database.JobDao;
import com.example.gitlinked.models.Repository;
import com.example.gitlinked.models.User;
import com.example.gitlinked.utils.Constants;

import java.util.Arrays;

import java.util.ArrayList;
import java.util.List;

/**
 * Background service that syncs cached data.
 * - Refreshes user profile and repos from GitHub
 * - Seeds mock data on first run
 * - Caches user data for offline mode
 */
public class SyncService extends Service {

    private static final String TAG = "SyncService";

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "SyncService started");

        // Seed mock data
        JobDao jobDao = new JobDao(this);
        jobDao.seedMockData();

        // Seed mock users for demo
        seedMockUsers();

        // Sync GitHub profile and repos if logged in
        syncGitHubProfile();

        stopSelf();
        return START_NOT_STICKY;
    }

    private void syncGitHubProfile() {
        GitHubService gitHubService = new GitHubService(this);
        String token = gitHubService.getSavedToken();

        if (token != null) {
            // Fetch user profile
            gitHubService.fetchUser(token, new GitHubService.OnUserLoaded() {
                @Override
                public void onSuccess(User user) {
                    UserDao userDao = new UserDao(SyncService.this);
                    userDao.insertOrUpdate(user);
                    Log.d(TAG, "User profile synced: " + user.getUsername());
                }

                @Override
                public void onError(String error) {
                    Log.e(TAG, "Failed to sync profile: " + error);
                }
            });

            // Also fetch repos to update languages
            gitHubService.fetchRepos(token, new GitHubService.OnReposLoaded() {
                @Override
                public void onSuccess(List<Repository> repos) {
                    Log.d(TAG, "Repos synced: " + repos.size() + " repos");
                }

                @Override
                public void onError(String error) {
                    Log.e(TAG, "Failed to sync repos: " + error);
                }
            });
        }
    }

    /**
     * Seed mock developer profiles for demo/testing if BLE devices are not available.
     */
    private void seedMockUsers() {
        UserDao userDao = new UserDao(this);
        List<User> existing = userDao.getAllUsers();

        // Skip if we already have mock users (check for more than just the current user)
        if (existing.size() > 3) return;

        // Mock developer 1
        User dev1 = new User("mock_1", "priya_codes",
                "https://avatars.githubusercontent.com/u/1?v=4",
                "Full-stack developer | React & Node.js enthusiast");
        dev1.setLanguages(new ArrayList<>(Arrays.asList("Java", "JavaScript", "Python", "TypeScript")));
        dev1.setInterests(new ArrayList<>(Arrays.asList("Android", "Web Dev", "Open Source", "AI/ML")));
        dev1.setLatitude(12.9716);
        dev1.setLongitude(77.5946);
        dev1.setOnline(true);

        // Mock developer 2
        User dev2 = new User("mock_2", "arjun_dev",
                "https://avatars.githubusercontent.com/u/2?v=4",
                "Android developer @ Google | Kotlin lover");
        dev2.setLanguages(new ArrayList<>(Arrays.asList("Kotlin", "Java", "Dart")));
        dev2.setInterests(new ArrayList<>(Arrays.asList("Android", "Flutter", "Mobile Dev")));
        dev2.setLatitude(12.9720);
        dev2.setLongitude(77.5950);
        dev2.setOnline(true);

        // Mock developer 3
        User dev3 = new User("mock_3", "sneha_ml",
                "https://avatars.githubusercontent.com/u/3?v=4",
                "ML Engineer | Data Science | Python aficionado");
        dev3.setLanguages(new ArrayList<>(Arrays.asList("Python", "R", "Java", "SQL")));
        dev3.setInterests(new ArrayList<>(Arrays.asList("Machine Learning", "Data Science", "AI/ML", "Open Source")));
        dev3.setLatitude(12.9700);
        dev3.setLongitude(77.5940);
        dev3.setOnline(true);

        // Mock developer 4
        User dev4 = new User("mock_4", "rahul_cloud",
                "https://avatars.githubusercontent.com/u/4?v=4",
                "DevOps & Cloud Architect | AWS Certified");
        dev4.setLanguages(new ArrayList<>(Arrays.asList("Go", "Python", "Shell", "Terraform")));
        dev4.setInterests(new ArrayList<>(Arrays.asList("Cloud", "DevOps", "Kubernetes", "Open Source")));
        dev4.setLatitude(12.9710);
        dev4.setLongitude(77.5960);
        dev4.setOnline(false);

        // Mock developer 5
        User dev5 = new User("mock_5", "ananya_web",
                "https://avatars.githubusercontent.com/u/5?v=4",
                "Frontend dev | Vue.js & React | Design enthusiast");
        dev5.setLanguages(new ArrayList<>(Arrays.asList("JavaScript", "TypeScript", "CSS", "HTML")));
        dev5.setInterests(new ArrayList<>(Arrays.asList("Web Dev", "UI/UX", "Open Source")));
        dev5.setLatitude(12.9730);
        dev5.setLongitude(77.5935);
        dev5.setOnline(true);

        // Mock developer 6
        User dev6 = new User("mock_6", "vikram_sys",
                "https://avatars.githubusercontent.com/u/6?v=4",
                "Systems programmer | Rust & C++ | Performance optimization");
        dev6.setLanguages(new ArrayList<>(Arrays.asList("Rust", "C++", "C", "Java")));
        dev6.setInterests(new ArrayList<>(Arrays.asList("Systems Programming", "Embedded", "Open Source")));
        dev6.setLatitude(12.9695);
        dev6.setLongitude(77.5955);
        dev6.setOnline(true);

        userDao.insertOrUpdate(dev1);
        userDao.insertOrUpdate(dev2);
        userDao.insertOrUpdate(dev3);
        userDao.insertOrUpdate(dev4);
        userDao.insertOrUpdate(dev5);
        userDao.insertOrUpdate(dev6);

        Log.d(TAG, "Mock users seeded");
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
