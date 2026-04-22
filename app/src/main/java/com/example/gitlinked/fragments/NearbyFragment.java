package com.example.gitlinked.fragments;

import android.bluetooth.le.ScanResult;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.example.gitlinked.R;
import com.example.gitlinked.activities.ChatActivity;
import com.example.gitlinked.activities.ProfileActivity;
import com.example.gitlinked.adapters.DeveloperAdapter;
import com.example.gitlinked.bluetooth.BLEManager;
import com.example.gitlinked.bluetooth.BLEScanner;
import com.example.gitlinked.database.ConnectionDao;
import com.example.gitlinked.database.UserDao;
import com.example.gitlinked.models.ConnectionRequest;
import com.example.gitlinked.models.MatchResult;
import com.example.gitlinked.models.User;
import com.example.gitlinked.realtime.FirebaseRealtimeRepository;
import com.example.gitlinked.utils.Constants;
import com.example.gitlinked.utils.MatchUtils;
import com.example.gitlinked.views.RadarView;
import com.google.firebase.database.ChildEventListener;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Fragment showing nearby developers with a proximity radar visualization.
 * The radar shows dots for discovered developers, positioned by estimated distance.
 * Below the radar is the traditional developer list with match scores.
 */
public class NearbyFragment extends Fragment {

    private RecyclerView recyclerDevelopers;
    private DeveloperAdapter adapter;
    private List<MatchResult> matchResults = new ArrayList<>();
    private UserDao userDao;
    private ConnectionDao connectionDao;
    private FirebaseRealtimeRepository firebaseRepo;
    private ChildEventListener connectionListener;
    private String currentUserId;
    private RadarView radarView;
    private BLEScanner bleScanner;
    private Set<String> discoveredUserIds = new HashSet<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.activity_nearby, container, false);

        userDao = new UserDao(requireContext());
        connectionDao = new ConnectionDao(requireContext());
        firebaseRepo = new FirebaseRealtimeRepository(requireContext());

        SharedPreferences prefs = requireContext().getSharedPreferences(
                Constants.PREF_NAME, requireContext().MODE_PRIVATE);
        currentUserId = prefs.getString(Constants.PREF_USER_ID, "");

        // Radar view
        radarView = view.findViewById(R.id.radar_view);

        // Developer list
        recyclerDevelopers = view.findViewById(R.id.recycler_developers);
        TextView tvScanStatus = view.findViewById(R.id.tv_scan_status);
        SwipeRefreshLayout swipeRefresh = view.findViewById(R.id.swipe_refresh);

        swipeRefresh.setColorSchemeResources(R.color.accentCyan);
        swipeRefresh.setProgressBackgroundColorSchemeResource(R.color.surfaceDark);

        adapter = new DeveloperAdapter(requireContext(), matchResults);
        recyclerDevelopers.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerDevelopers.setAdapter(adapter);

        adapter.setOnDeveloperClickListener(new DeveloperAdapter.OnDeveloperClickListener() {
            @Override
            public void onDeveloperClick(MatchResult match, int position) {
                Intent intent = new Intent(requireContext(), ProfileActivity.class);
                intent.putExtra(Constants.EXTRA_USER_ID, match.getUser().getId());
                intent.putExtra(Constants.EXTRA_USERNAME, match.getUser().getUsername());
                intent.putExtra(Constants.EXTRA_AVATAR_URL, match.getUser().getAvatarUrl());
                startActivity(intent);
            }

            @Override
            public void onConnectClick(MatchResult match, int position) {
                handleInviteClick(match, position);
            }
        });

        // Initialize BLE Scanner
        BLEManager bleManager = BLEManager.getInstance(requireContext());
        bleScanner = bleManager.getScanner();
        if (bleScanner != null) {
            bleScanner.setScanListener(new BLEScanner.ScanListener() {
                @Override
                public void onDeviceFound(ScanResult result, String userId) {
                    handleBleDeviceFound(result, userId);
                }

                @Override
                public void onScanComplete(List<ScanResult> results) {
                    if (swipeRefresh != null) swipeRefresh.setRefreshing(false);
                }

                @Override
                public void onScanError(String error) {
                    if (swipeRefresh != null) swipeRefresh.setRefreshing(false);
                    Toast.makeText(getContext(), error, Toast.LENGTH_SHORT).show();
                }
            });
        }

        // Load data immediately
        loadDevelopers(tvScanStatus, swipeRefresh);

        swipeRefresh.setOnRefreshListener(() -> {
            discoveredUserIds.clear();
            startBleScan();
            loadDevelopers(tvScanStatus, swipeRefresh);
        });

        View fab = view.findViewById(R.id.fab_scan);
        if (fab != null) {
            fab.setOnClickListener(v -> {
                discoveredUserIds.clear();
                startBleScan();
                loadDevelopers(tvScanStatus, swipeRefresh);
            });
        }

        setupFirebaseSync();

        return view;
    }

    private void handleBleDeviceFound(ScanResult result, String userId) {
        if (userId == null || userId.equals(currentUserId)) return;

        discoveredUserIds.add(userId);

        User user = userDao.getUserById(userId);
        if (user == null) {
            user = new User();
            user.setId(userId);
            user.setUsername(userId); // Fallback to ID as username initially
            user.setBio("Discovered via Radar");
        }
        user.setBleDeviceAddress(result.getDevice().getAddress());
        user.setOnline(true);
        userDao.insertOrUpdate(user);

        // Refresh UI on main thread
        if (getActivity() != null) {
            getActivity().runOnUiThread(() -> {
                View view = getView();
                if (view != null) {
                    loadDevelopers(view.findViewById(R.id.tv_scan_status),
                            view.findViewById(R.id.swipe_refresh));
                }
            });
        }
    }

    private void startBleScan() {
        if (bleScanner != null) {
            bleScanner.startScan();
        }
    }

    private void setupFirebaseSync() {
        if (firebaseRepo.isAvailable()) {
            connectionListener = firebaseRepo.observeConnectionsForUser(currentUserId, request -> {
                connectionDao.upsertFromRemote(request.getFromUserId(), request.getToUserId(),
                        request.getStatus(), request.getTimestamp());
                
                // Refresh adapter if currently showing
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        View view = getView();
                        if (view != null) {
                            loadDevelopers(view.findViewById(R.id.tv_scan_status),
                                    view.findViewById(R.id.swipe_refresh));
                        }
                    });
                }
            });
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        startBleScan();
        // Refresh data when returning from profile/chat
        if (recyclerDevelopers != null) {
            View view = getView();
            if (view != null) {
                TextView tvScanStatus = view.findViewById(R.id.tv_scan_status);
                SwipeRefreshLayout swipeRefresh = view.findViewById(R.id.swipe_refresh);
                loadDevelopers(tvScanStatus, swipeRefresh);
            }
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (firebaseRepo != null && connectionListener != null) {
            firebaseRepo.removeConnectionsListener(connectionListener);
        }
        if (bleScanner != null) {
            bleScanner.stopScan();
        }
    }

    private void handleInviteClick(MatchResult match, int position) {
        String otherUserId = match.getUser().getId();
        ConnectionRequest existing = connectionDao.getConnectionBetween(currentUserId, otherUserId);

        if (existing == null) {
            // Local update
            connectionDao.sendRequest(currentUserId, otherUserId);
            // Firebase update
            firebaseRepo.sendConnectionRequest(currentUserId, otherUserId, (success, error) -> {
                if (!success) {
                    Log.e("NearbyFragment", "Firebase invite failed: " + error);
                }
            });
            
            Toast.makeText(requireContext(),
                    "Invite sent to " + match.getUser().getUsername() + "! 📩",
                    Toast.LENGTH_SHORT).show();
            adapter.notifyItemChanged(position);
        } else if (existing.isAccepted()) {
            Intent intent = new Intent(requireContext(), ChatActivity.class);
            intent.putExtra(Constants.EXTRA_USER_ID, match.getUser().getId());
            intent.putExtra(Constants.EXTRA_USERNAME, match.getUser().getUsername());
            intent.putExtra(Constants.EXTRA_AVATAR_URL, match.getUser().getAvatarUrl());
            startActivity(intent);
        } else if (existing.isPending() && existing.getToUserId().equals(currentUserId)) {
            // Local update
            connectionDao.acceptRequest(existing.getId());
            // Firebase update
            firebaseRepo.updateConnectionStatusByUsers(existing.getFromUserId(), existing.getToUserId(),
                    ConnectionRequest.STATUS_ACCEPTED, (success, error) -> {
                        if (!success) {
                            Log.e("NearbyFragment", "Firebase accept failed: " + error);
                        }
                    });

            Toast.makeText(requireContext(),
                    "Connected with " + match.getUser().getUsername() + "! 🎉",
                    Toast.LENGTH_SHORT).show();
            adapter.notifyItemChanged(position);
        } else {
            Toast.makeText(requireContext(),
                    "Invite already sent — waiting for response ⏳",
                    Toast.LENGTH_SHORT).show();
        }
    }

    private void loadDevelopers(TextView tvStatus, SwipeRefreshLayout swipeRefresh) {
        if (getContext() == null) return;
        
        SharedPreferences prefs = requireContext().getSharedPreferences(
                Constants.PREF_NAME, requireContext().MODE_PRIVATE);

        // Use real languages from prefs
        String langsStr = prefs.getString(Constants.PREF_LANGUAGES, "Java,Python,JavaScript");
        User currentUser = new User();
        currentUser.setLanguages(new ArrayList<>(Arrays.asList(langsStr.split(","))));
        currentUser.setInterests(new ArrayList<>(Arrays.asList("Android", "Web Dev", "Open Source", "AI/ML")));

        List<User> allUsers = userDao.getAllUsers();
        matchResults.clear();

        // If we have discovered users via radar, prioritize showing them
        if (!discoveredUserIds.isEmpty()) {
            for (String userId : discoveredUserIds) {
                User user = userDao.getUserById(userId);
                if (user != null) {
                    matchResults.add(MatchUtils.calculateMatch(currentUser, user));
                }
            }
        } else {
            // Fallback to all users if nothing discovered yet (or for mock demo)
            for (User user : allUsers) {
                if (user.getId().equals(currentUserId)) continue;
                matchResults.add(MatchUtils.calculateMatch(currentUser, user));
            }
        }

        Collections.sort(matchResults, (a, b) ->
                Integer.compare(b.getMatchPercentage(), a.getMatchPercentage()));

        adapter.updateData(matchResults);

        // Update radar with developer dots
        updateRadar(matchResults);

        if (tvStatus != null) {
            tvStatus.setText(matchResults.size() + " developers found");
        }
        if (swipeRefresh != null) {
            swipeRefresh.setRefreshing(false);
        }

        // Show/hide empty state
        View view = getView();
        if (view != null) {
            View layoutEmpty = view.findViewById(R.id.layout_empty);
            if (layoutEmpty != null) {
                layoutEmpty.setVisibility(matchResults.isEmpty() ? View.VISIBLE : View.GONE);
            }
        }
    }

    /**
     * Update the radar visualization with discovered developers.
     * Each developer gets a dot on the radar.
     */
    private void updateRadar(List<MatchResult> matches) {
        if (radarView == null) return;

        List<RadarView.RadarDot> dots = new ArrayList<>();

        for (int i = 0; i < matches.size(); i++) {
            MatchResult match = matches.get(i);
            User user = match.getUser();

            // If we have a real BLE address, use a more realistic RSSI
            // Otherwise simulate RSSI based on index (spread developers around)
            int simulatedRssi;
            if (user.getBleDeviceAddress() != null && !user.getBleDeviceAddress().isEmpty()) {
                // For now, if we don't have the real-time RSSI here, we use a default
                simulatedRssi = -60;
            } else {
                // Mock data — spread evenly across the radar
                simulatedRssi = -45 - (i * 12);
            }

            // Clamp RSSI to reasonable BLE range
            simulatedRssi = Math.max(-90, Math.min(-30, simulatedRssi));

            RadarView.RadarDot dot = new RadarView.RadarDot(
                    user.getUsername(), simulatedRssi);
            dot.isOnline = user.isOnline();
            dots.add(dot);
        }

        radarView.setDevelopers(dots);
        radarView.setActive(!matches.isEmpty());
    }
}
