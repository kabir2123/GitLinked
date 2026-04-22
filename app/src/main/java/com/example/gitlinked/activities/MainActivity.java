package com.example.gitlinked.activities;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.example.gitlinked.R;
import com.example.gitlinked.api.GitHubService;
import com.example.gitlinked.database.JobDao;
import com.example.gitlinked.database.UserDao;
import com.example.gitlinked.fragments.NearbyFragment;
import com.example.gitlinked.models.User;
import com.example.gitlinked.services.SyncService;
import com.example.gitlinked.utils.Constants;
import com.example.gitlinked.utils.LocationUtils;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Main activity with bottom navigation.
 * Houses fragments for Nearby, Jobs, Events, and Chats tabs.
 */
public class MainActivity extends AppCompatActivity {

    private BottomNavigationView bottomNav;
    private MaterialToolbar toolbar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        toolbar = findViewById(R.id.toolbar);
        bottomNav = findViewById(R.id.bottom_navigation);

        // Request permissions
        if (!LocationUtils.hasBlePermissions(this)) {
            LocationUtils.requestAllPermissions(this);
        }

        // *** CRITICAL: Seed mock data SYNCHRONOUSLY before loading any fragment ***
        // This ensures mock developers are available in the DB when
        // NearbyFragment queries for them.
        seedMockDataSync();

        // Start SyncService for GitHub profile sync (async, runs in background)
        startService(new Intent(this, SyncService.class));

        // Start BLE Service — this starts:
        //   1. BLE Advertising: broadcasts our GitHub username so others can discover us
        //   2. BLE Scanning: discovers other GitLinked users nearby
        // Without this, peer-to-peer discovery WILL NOT work.
        if (LocationUtils.hasBlePermissions(this)) {
            Intent bleIntent = new Intent(this, com.example.gitlinked.services.BLEService.class);
            bleIntent.setAction(com.example.gitlinked.services.BLEService.ACTION_START);
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                startForegroundService(bleIntent);
            } else {
                startService(bleIntent);
            }
        }

        // Toolbar menu clicks
        toolbar.setOnMenuItemClickListener(item -> {
            int id = item.getItemId();
            if (id == R.id.action_profile) {
                openOwnProfile();
                return true;
            } else if (id == R.id.action_logout) {
                logout();
                return true;
            }
            return false;
        });

        // Bottom navigation
        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_nearby) {
                startActivity(new Intent(this, NearbyActivity.class));
                return true;
            } else if (id == R.id.nav_jobs) {
                startActivity(new Intent(this, JobsActivity.class));
                return true;
            } else if (id == R.id.nav_events) {
                startActivity(new Intent(this, EventsActivity.class));
                return true;
            } else if (id == R.id.nav_chats) {
                startActivity(new Intent(this, ChatsActivity.class));
                return true;
            }
            return false;
        });

        // Default: load Nearby fragment (now mock data is already in DB)
        if (savedInstanceState == null) {
            loadFragment(new NearbyFragment());
        }
    }

    /**
     * Seed all mock data synchronously on the main thread.
     * This MUST happen before loading any fragment that queries the database.
     */
    private void seedMockDataSync() {
        // Seed mock jobs + events
        JobDao jobDao = new JobDao(this);
        jobDao.seedMockData();

        // Seed mock developers
        UserDao userDao = new UserDao(this);
        List<User> existing = userDao.getAllUsers();

        // Only seed if we don't have enough users yet
        // (skip current user, check for at least 3 mock devs)
        int mockCount = 0;
        for (User u : existing) {
            if (u.getId() != null && u.getId().startsWith("mock_")) mockCount++;
        }

        if (mockCount < 6) {
            User dev1 = new User("mock_1", "priya_codes",
                    "https://avatars.githubusercontent.com/u/1?v=4",
                    "Full-stack developer | React & Node.js enthusiast");
            dev1.setLanguages(new ArrayList<>(Arrays.asList("Java", "JavaScript", "Python", "TypeScript")));
            dev1.setInterests(new ArrayList<>(Arrays.asList("Android", "Web Dev", "Open Source", "AI/ML")));
            dev1.setLatitude(12.9716);
            dev1.setLongitude(77.5946);
            dev1.setOnline(true);

            User dev2 = new User("mock_2", "arjun_dev",
                    "https://avatars.githubusercontent.com/u/2?v=4",
                    "Android developer @ Google | Kotlin lover");
            dev2.setLanguages(new ArrayList<>(Arrays.asList("Kotlin", "Java", "Dart")));
            dev2.setInterests(new ArrayList<>(Arrays.asList("Android", "Flutter", "Mobile Dev")));
            dev2.setLatitude(12.9720);
            dev2.setLongitude(77.5950);
            dev2.setOnline(true);

            User dev3 = new User("mock_3", "sneha_ml",
                    "https://avatars.githubusercontent.com/u/3?v=4",
                    "ML Engineer | Data Science | Python aficionado");
            dev3.setLanguages(new ArrayList<>(Arrays.asList("Python", "R", "Java", "SQL")));
            dev3.setInterests(new ArrayList<>(Arrays.asList("Machine Learning", "Data Science", "AI/ML", "Open Source")));
            dev3.setLatitude(12.9700);
            dev3.setLongitude(77.5940);
            dev3.setOnline(true);

            User dev4 = new User("mock_4", "rahul_cloud",
                    "https://avatars.githubusercontent.com/u/4?v=4",
                    "DevOps & Cloud Architect | AWS Certified");
            dev4.setLanguages(new ArrayList<>(Arrays.asList("Go", "Python", "Shell", "Terraform")));
            dev4.setInterests(new ArrayList<>(Arrays.asList("Cloud", "DevOps", "Kubernetes", "Open Source")));
            dev4.setLatitude(12.9710);
            dev4.setLongitude(77.5960);
            dev4.setOnline(false);

            User dev5 = new User("mock_5", "ananya_web",
                    "https://avatars.githubusercontent.com/u/5?v=4",
                    "Frontend dev | Vue.js & React | Design enthusiast");
            dev5.setLanguages(new ArrayList<>(Arrays.asList("JavaScript", "TypeScript", "CSS", "HTML")));
            dev5.setInterests(new ArrayList<>(Arrays.asList("Web Dev", "UI/UX", "Open Source")));
            dev5.setLatitude(12.9730);
            dev5.setLongitude(77.5935);
            dev5.setOnline(true);

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
        }
    }

    private void loadFragment(Fragment fragment) {
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.frame_content, fragment)
                .commit();
    }

    private void openOwnProfile() {
        SharedPreferences prefs = getSharedPreferences(Constants.PREF_NAME, MODE_PRIVATE);
        Intent intent = new Intent(this, ProfileActivity.class);
        intent.putExtra(Constants.EXTRA_USER_ID, prefs.getString(Constants.PREF_USER_ID, ""));
        intent.putExtra(Constants.EXTRA_USERNAME, prefs.getString(Constants.PREF_USERNAME, ""));
        intent.putExtra(Constants.EXTRA_AVATAR_URL, prefs.getString(Constants.PREF_AVATAR_URL, ""));
        startActivity(intent);
    }

    private void logout() {
        GitHubService gitHubService = new GitHubService(this);
        gitHubService.logout();
        startActivity(new Intent(this, LoginActivity.class));
        finish();
    }
}
