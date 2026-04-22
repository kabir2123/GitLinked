package com.example.gitlinked.activities;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.example.gitlinked.R;
import com.example.gitlinked.adapters.DeveloperAdapter;
import com.example.gitlinked.api.GitHubService;
import com.example.gitlinked.bluetooth.BLEManager;
import com.example.gitlinked.bluetooth.BLEScanner;
import com.example.gitlinked.database.ConnectionDao;
import com.example.gitlinked.database.UserDao;
import com.example.gitlinked.models.ConnectionRequest;
import com.example.gitlinked.models.MatchResult;
import com.example.gitlinked.models.Repository;
import com.example.gitlinked.models.User;
import com.example.gitlinked.realtime.FirebaseRealtimeRepository;
import com.example.gitlinked.utils.Constants;
import com.example.gitlinked.utils.LocationUtils;
import com.example.gitlinked.utils.MatchUtils;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.example.gitlinked.views.RadarView;
import com.google.firebase.database.ChildEventListener;

/**
 * Discovers nearby developers via BLE.
 *
 * BLE Discovery Flow:
 * 1. Start BLE scan (filtered by GitLinked UUID, fallback to unfiltered)
 * 2. On device found: extract GitHub username from scan response service data
 * 3. Check if user exists in local DB — if not, fetch their public GitHub profile
 * 4. Calculate match score and display in the list
 * 5. User taps "Invite" → sends connection request
 * 6. Other user accepts → both can chat
 */
public class NearbyActivity extends AppCompatActivity {

    private static final String TAG = "NearbyActivity";

    private RecyclerView recyclerDevelopers;
    private SwipeRefreshLayout swipeRefresh;
    private com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton fabScan;
    private TextView tvScanStatus;
    private DeveloperAdapter adapter;
    private List<MatchResult> matchResults = new ArrayList<>();
    private UserDao userDao;
    private ConnectionDao connectionDao;
    private GitHubService gitHubService;
    private BLEManager bleManager;
    private FirebaseRealtimeRepository realtimeRepository;
    private ChildEventListener connectionsListener;
    private String currentUserId;
    private RadarView radarView;

    // Track users discovered in this BLE scan session
    private Set<String> bleDiscoveredUserIds = new HashSet<>();
    // Latest RSSI values for live radar positioning.
    private final Map<String, Integer> latestRssiByUserId = new HashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_nearby);

        userDao = new UserDao(this);
        connectionDao = new ConnectionDao(this);
        gitHubService = new GitHubService(this);
        bleManager = BLEManager.getInstance(this);
        realtimeRepository = new FirebaseRealtimeRepository(this);

        SharedPreferences prefs = getSharedPreferences(Constants.PREF_NAME, MODE_PRIVATE);
        currentUserId = prefs.getString(Constants.PREF_USER_ID, "");

        initViews();
        setupRecyclerView();

        // Load cached developers (mock + previously discovered)
        loadNearbyDevelopers();
    }

    @Override
    protected void onResume() {
        super.onResume();
        startRealtimeConnectionSync();
        // Refresh data and button states when returning from profile/chat
        loadNearbyDevelopers();
    }

    @Override
    protected void onPause() {
        super.onPause();
        stopRealtimeConnectionSync();
    }

    private void initViews() {
        recyclerDevelopers = findViewById(R.id.recycler_developers);
        swipeRefresh = findViewById(R.id.swipe_refresh);
        fabScan = findViewById(R.id.fab_scan);
        tvScanStatus = findViewById(R.id.tv_scan_status);
        radarView = findViewById(R.id.radar_view);

        swipeRefresh.setColorSchemeResources(R.color.accentCyan);
        swipeRefresh.setProgressBackgroundColorSchemeResource(R.color.surfaceDark);

        swipeRefresh.setOnRefreshListener(this::scanForDevelopers);
        fabScan.setOnClickListener(v -> scanForDevelopers());
    }

    private void setupRecyclerView() {
        adapter = new DeveloperAdapter(this, matchResults);
        recyclerDevelopers.setLayoutManager(new LinearLayoutManager(this));
        recyclerDevelopers.setAdapter(adapter);

        adapter.setOnDeveloperClickListener(new DeveloperAdapter.OnDeveloperClickListener() {
            @Override
            public void onDeveloperClick(MatchResult match, int position) {
                openProfile(match);
            }

            @Override
            public void onConnectClick(MatchResult match, int position) {
                handleInviteClick(match, position);
            }
        });
    }

    /**
     * Handle invite button click — sends invite, accepts, or opens chat.
     */
    private void handleInviteClick(MatchResult match, int position) {
        String otherUserId = match.getUser().getId();
        ConnectionRequest existing = connectionDao.getConnectionBetween(currentUserId, otherUserId);

        if (existing == null) {
            new AlertDialog.Builder(this)
                    .setTitle("Start conversation?")
                    .setMessage("Send a chat invite to @" + match.getUser().getUsername() + "?")
                    .setPositiveButton("Send Invite", (dialog, which) -> {
                        connectionDao.sendRequest(currentUserId, otherUserId);
                        if (realtimeRepository.isAvailable()) {
                            realtimeRepository.sendConnectionRequest(currentUserId, otherUserId, (success, error) -> {
                                if (!success) {
                                    Log.w(TAG, "Failed to sync invite to Firebase: " + error);
                                    runOnUiThread(() -> Toast.makeText(this,
                                            "Invite saved locally, but sync failed: " + error,
                                            Toast.LENGTH_LONG).show());
                                }
                            });
                        }
                        Toast.makeText(this, "Invite sent to " + match.getUser().getUsername() + "! 📩",
                                Toast.LENGTH_SHORT).show();
                        adapter.notifyItemChanged(position);
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
        } else if (existing.isAccepted()) {
            openChat(match);
        } else if (existing.isPending() && existing.getToUserId().equals(currentUserId)) {
            connectionDao.acceptRequest(existing.getId());
            if (realtimeRepository.isAvailable()) {
                realtimeRepository.updateConnectionStatusByUsers(
                        existing.getFromUserId(),
                        existing.getToUserId(),
                        ConnectionRequest.STATUS_ACCEPTED,
                        (success, error) -> {
                            if (!success) {
                                Log.w(TAG, "Failed to sync accept to Firebase: " + error);
                            }
                        });
            }
            Toast.makeText(this, "Connected with " + match.getUser().getUsername() + "! 🎉",
                    Toast.LENGTH_SHORT).show();
            // Once accepted, jump straight into chat.
            openChat(match);
        } else {
            Toast.makeText(this, "Invite already sent — waiting for response ⏳",
                    Toast.LENGTH_SHORT).show();
        }
    }

    private void startRealtimeConnectionSync() {
        if (!realtimeRepository.isAvailable() || connectionsListener != null) return;
        connectionsListener = realtimeRepository.observeConnectionsForUser(currentUserId, request -> {
            connectionDao.upsertFromRemote(
                    request.getFromUserId(),
                    request.getToUserId(),
                    request.getStatus(),
                    request.getTimestamp());
            runOnUiThread(this::loadNearbyDevelopers);
        });
    }

    private void stopRealtimeConnectionSync() {
        if (!realtimeRepository.isAvailable() || connectionsListener == null) return;
        realtimeRepository.removeConnectionsListener(connectionsListener);
        connectionsListener = null;
    }

    /**
     * Scan for developers using BLE. Falls back to showing cached data.
     */
    private void scanForDevelopers() {
        tvScanStatus.setText(R.string.scanning);
        fabScan.setEnabled(false);
        bleDiscoveredUserIds.clear();
        latestRssiByUserId.clear();
        if (radarView != null) {
            radarView.clearDevelopers();
            radarView.setActive(true);
        }

        if (bleManager.isBleSupported() && bleManager.isBluetoothEnabled()
                && LocationUtils.hasBlePermissions(this)) {
            startBleScan();
        } else {
            // No BLE — just show cached data
            tvScanStatus.setText("BLE unavailable — showing cached developers");
            loadNearbyDevelopers();
        }
    }

    /**
     * Start BLE scan with listener that processes discovered GitLinked devices.
     */
    private void startBleScan() {
        BLEScanner scanner = bleManager.getScanner();
        if (scanner == null) {
            loadNearbyDevelopers();
            return;
        }

        // Also start advertising our own presence so the other user can discover us
        if (bleManager.isAdvertisingSupported()) {
            bleManager.getAdvertiser().startAdvertising();
            Log.d(TAG, "Started advertising for mutual discovery");
        }

        scanner.setScanListener(new BLEScanner.ScanListener() {
            @Override
            public void onDeviceFound(android.bluetooth.le.ScanResult result) {
                processDiscoveredDevice(result);
                runOnUiThread(() -> {
                    tvScanStatus.setText("Scanning... Found " + bleDiscoveredUserIds.size() + " developers");
                    // Keep list in hard-sync with radar discoveries on every BLE callback.
                    loadNearbyDevelopers();
                });
            }

            @Override
            public void onScanComplete(List<android.bluetooth.le.ScanResult> results) {
                runOnUiThread(() -> {
                    Log.d(TAG, "BLE scan complete. Discovered " + bleDiscoveredUserIds.size() + " users");
                    loadNearbyDevelopers();
                });
            }

            @Override
            public void onScanError(String error) {
                runOnUiThread(() -> {
                    Toast.makeText(NearbyActivity.this, error, Toast.LENGTH_SHORT).show();
                    loadNearbyDevelopers();
                });
            }
        });

        scanner.startScan();
    }

    /**
     * Process a discovered BLE device:
     * 1. Extract the GitHub username from BLE service data
     * 2. If user not in local DB, fetch their public GitHub profile
     * 3. Save to DB for display
     */
    private void processDiscoveredDevice(android.bluetooth.le.ScanResult result) {
        if (result.getScanRecord() == null) return;

        // Extract user ID (GitHub username) from service data
        byte[] serviceData = result.getScanRecord().getServiceData(
                new android.os.ParcelUuid(Constants.GITLINKED_SERVICE_UUID));

        if (serviceData == null || serviceData.length == 0) return;

        String discoveredUserId = new String(serviceData, StandardCharsets.UTF_8).trim();
        int rssi = result.getRssi();

        // Don't add ourselves
        if (discoveredUserId.equals(currentUserId)) return;

        latestRssiByUserId.put(discoveredUserId, rssi);
        runOnUiThread(() -> updateRadarLiveDot(discoveredUserId, rssi));

        // Skip profile/bootstrap work if already processed in this scan session
        if (bleDiscoveredUserIds.contains(discoveredUserId)) return;
        bleDiscoveredUserIds.add(discoveredUserId);

        Log.d(TAG, "Discovered GitLinked user via BLE: " + discoveredUserId
                + " (RSSI: " + rssi + ")");

        // Check if user already exists in our local DB
        User existingUser = userDao.getUserById(discoveredUserId);

        if (existingUser != null) {
            // Update BLE address and mark as online
            existingUser.setBleDeviceAddress(result.getDevice().getAddress());
            existingUser.setOnline(true);
            userDao.insertOrUpdate(existingUser);
            Log.d(TAG, "Updated known user: " + existingUser.getUsername());
            runOnUiThread(this::loadNearbyDevelopers);
        } else {
            // NEW user: add a placeholder immediately so it appears in the UI now.
            User placeholderUser = new User();
            placeholderUser.setId(discoveredUserId);
            placeholderUser.setUsername(discoveredUserId);
            placeholderUser.setBio("Discovered via Bluetooth");
            placeholderUser.setBleDeviceAddress(result.getDevice().getAddress());
            placeholderUser.setOnline(true);
            userDao.insertOrUpdate(placeholderUser);
            runOnUiThread(this::loadNearbyDevelopers);

            // Then fetch richer profile data in background.
            fetchPublicGitHubProfile(discoveredUserId, result.getDevice().getAddress());
        }
    }

    /**
     * Fetch a discovered user's public GitHub profile and repos.
     * This is called when we discover a user via BLE who isn't in our local DB yet.
     *
     * The discovered user ID is their GitHub username (advertised via BLE).
     * We use the GitHub public API (no auth needed) to get their:
     *  - Avatar, Bio, Username
     *  - Public repos → extract languages
     */
    private void fetchPublicGitHubProfile(String githubUsername, String bleAddress) {
        Log.d(TAG, "Fetching public GitHub profile for: " + githubUsername);

        // Fetch public user profile (no auth required)
        gitHubService.fetchPublicUser(githubUsername, new GitHubService.OnUserLoaded() {
            @Override
            public void onSuccess(User user) {
                user.setBleDeviceAddress(bleAddress);
                user.setOnline(true);
                userDao.insertOrUpdate(user);
                Log.d(TAG, "Saved discovered user: " + user.getUsername()
                        + " (avatar: " + user.getAvatarUrl() + ")");

                // Also fetch their public repos to get languages
                gitHubService.fetchPublicRepos(githubUsername, new GitHubService.OnReposLoaded() {
                    @Override
                    public void onSuccess(List<Repository> repos) {
                        // Extract languages from repos
                        Set<String> languages = new HashSet<>();
                        for (Repository repo : repos) {
                            if (repo.getLanguage() != null && !repo.getLanguage().isEmpty()) {
                                languages.add(repo.getLanguage());
                            }
                        }
                        user.setLanguages(new ArrayList<>(languages));
                        userDao.insertOrUpdate(user);
                        Log.d(TAG, "Updated languages for " + githubUsername + ": " + languages);

                        // Refresh the UI
                        runOnUiThread(() -> loadNearbyDevelopers());
                    }

                    @Override
                    public void onError(String error) {
                        Log.w(TAG, "Failed to fetch repos for " + githubUsername + ": " + error);
                        // Still refresh UI with whatever data we have
                        runOnUiThread(() -> loadNearbyDevelopers());
                    }
                });
            }

            @Override
            public void onError(String error) {
                Log.w(TAG, "Failed to fetch profile for " + githubUsername + ": " + error);
                // Placeholder user was already inserted on discovery.
                runOnUiThread(() -> loadNearbyDevelopers());
            }
        });
    }

    /**
     * Load all developers from the local SQLite cache and calculate match scores.
     * Includes both BLE-discovered users and mock data.
     */
    private void loadNearbyDevelopers() {
        User currentUser = getCurrentUser();
        List<User> allUsers = userDao.getAllUsers();

        matchResults.clear();

        // Hard sync rule: any user shown on radar must also appear as a RecyclerView card.
        // Use union of explicit discovered IDs + live RSSI map keys.
        Set<String> radarDiscoveredUserIds = new HashSet<>(bleDiscoveredUserIds);
        radarDiscoveredUserIds.addAll(latestRssiByUserId.keySet());

        // If BLE/radar discovered users exist, show only those users in cards.
        if (!radarDiscoveredUserIds.isEmpty()) {
            for (String discoveredUserId : radarDiscoveredUserIds) {
                if (currentUserId.equals(discoveredUserId)) continue;

                User discoveredUser = userDao.getUserById(discoveredUserId);
                if (discoveredUser == null) {
                    // Guard: still show an invite-able card even if profile fetch hasn't completed.
                    discoveredUser = new User();
                    discoveredUser.setId(discoveredUserId);
                    discoveredUser.setUsername(discoveredUserId);
                    discoveredUser.setBio("Discovered via Bluetooth");
                    discoveredUser.setOnline(true);
                }

                MatchResult match = MatchUtils.calculateMatch(currentUser, discoveredUser);
                matchResults.add(match);
            }
        } else {
            for (User user : allUsers) {
                if (user.getId().equals(currentUserId)) continue;
                MatchResult match = MatchUtils.calculateMatch(currentUser, user);
                matchResults.add(match);
            }
        }

        // Sort by match percentage (highest first)
        Collections.sort(matchResults, (a, b) ->
                Integer.compare(b.getMatchPercentage(), a.getMatchPercentage()));

        adapter.updateData(matchResults);

        // Update radar visualization
        updateRadar(matchResults);

        tvScanStatus.setText(matchResults.size() + " developers found");
        fabScan.setEnabled(true);
        swipeRefresh.setRefreshing(false);

        // Show/hide empty state
        findViewById(R.id.layout_empty).setVisibility(
                matchResults.isEmpty() ? android.view.View.VISIBLE : android.view.View.GONE);
    }

    /**
     * Update the radar with developer dots positioned by RSSI.
     */
    private void updateRadar(List<MatchResult> matches) {
        if (radarView == null) return;

        // When BLE scan has live data, render only truly discovered users with real RSSI.
        if (!latestRssiByUserId.isEmpty()) {
            List<RadarView.RadarDot> dots = new ArrayList<>();
            for (Map.Entry<String, Integer> entry : latestRssiByUserId.entrySet()) {
                User user = userDao.getUserById(entry.getKey());
                String username = user != null && user.getUsername() != null && !user.getUsername().isEmpty()
                        ? user.getUsername() : entry.getKey();
                int rssi = Math.max(-95, Math.min(-25, entry.getValue()));
                RadarView.RadarDot dot = new RadarView.RadarDot(username, rssi);
                dot.isOnline = user == null || user.isOnline();
                dots.add(dot);
            }
            radarView.setDevelopers(dots);
            radarView.setActive(true);
            return;
        }

        // Fallback visualization for cached/mock users when no live BLE RSSI is available.
        List<RadarView.RadarDot> fallbackDots = new ArrayList<>();
        for (int i = 0; i < matches.size(); i++) {
            User user = matches.get(i).getUser();
            int simulatedRssi = (user.getBleDeviceAddress() != null && !user.getBleDeviceAddress().isEmpty())
                    ? -52 - (i * 7) : -46 - (i * 10);
            simulatedRssi = Math.max(-90, Math.min(-30, simulatedRssi));
            RadarView.RadarDot dot = new RadarView.RadarDot(user.getUsername(), simulatedRssi);
            dot.isOnline = user.isOnline();
            fallbackDots.add(dot);
        }
        radarView.setDevelopers(fallbackDots);
        radarView.setActive(!fallbackDots.isEmpty());
    }

    private void updateRadarLiveDot(String discoveredUserId, int rssi) {
        if (radarView == null) return;
        User user = userDao.getUserById(discoveredUserId);
        String username = user != null && user.getUsername() != null && !user.getUsername().isEmpty()
                ? user.getUsername() : discoveredUserId;
        radarView.addDeveloper(username, Math.max(-95, Math.min(-25, rssi)));
        radarView.setActive(true);
    }

    /**
     * Build a User object for the current logged-in user (for match calculation).
     */
    private User getCurrentUser() {
        SharedPreferences prefs = getSharedPreferences(Constants.PREF_NAME, MODE_PRIVATE);
        User user = new User();
        user.setId(prefs.getString(Constants.PREF_USER_ID, ""));
        user.setUsername(prefs.getString(Constants.PREF_USERNAME, ""));

        // Use real languages from prefs if available
        String langsStr = prefs.getString(Constants.PREF_LANGUAGES, "Java,Python,JavaScript");
        user.setLanguages(new ArrayList<>(Arrays.asList(langsStr.split(","))));
        user.setInterests(new ArrayList<>(Arrays.asList("Android", "Web Dev", "Open Source", "AI/ML")));

        return user;
    }

    private void openProfile(MatchResult match) {
        Intent intent = new Intent(this, ProfileActivity.class);
        intent.putExtra(Constants.EXTRA_USER_ID, match.getUser().getId());
        intent.putExtra(Constants.EXTRA_USERNAME, match.getUser().getUsername());
        intent.putExtra(Constants.EXTRA_AVATAR_URL, match.getUser().getAvatarUrl());
        startActivity(intent);
    }

    private void openChat(MatchResult match) {
        Intent intent = new Intent(this, ChatActivity.class);
        intent.putExtra(Constants.EXTRA_USER_ID, match.getUser().getId());
        intent.putExtra(Constants.EXTRA_USERNAME, match.getUser().getUsername());
        intent.putExtra(Constants.EXTRA_AVATAR_URL, match.getUser().getAvatarUrl());
        startActivity(intent);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Stop scanner when leaving
        if (bleManager.getScanner() != null && bleManager.getScanner().isScanning()) {
            bleManager.getScanner().stopScan();
        }
    }
}
