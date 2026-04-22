package com.example.gitlinked.activities;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.example.gitlinked.R;
import com.example.gitlinked.api.GitHubService;
import com.example.gitlinked.database.ConnectionDao;
import com.example.gitlinked.database.UserDao;
import com.example.gitlinked.models.ConnectionRequest;
import com.example.gitlinked.models.MatchResult;
import com.example.gitlinked.models.Repository;
import com.example.gitlinked.models.User;
import com.example.gitlinked.utils.Constants;
import com.example.gitlinked.utils.MatchUtils;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;

/**
 * Displays a developer's profile: avatar, bio, languages, repos, and match score.
 * Fetches real repos from GitHub API when possible.
 */
public class ProfileActivity extends AppCompatActivity {

    private static final String TAG = "ProfileActivity";

    private CircleImageView imgAvatar;
    private TextView tvUsername, tvBio, tvMatchPercentage, tvMatchDetail;
    private ChipGroup chipGroupLanguages;
    private LinearLayout layoutRepos, layoutActions, layoutMatch;
    private MaterialButton btnConnect, btnMessage, btnViewGitHub;

    private String userId, username, avatarUrl;
    private User profileUser;
    private UserDao userDao;
    private ConnectionDao connectionDao;
    private String currentUserId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        userDao = new UserDao(this);
        connectionDao = new ConnectionDao(this);

        // Get intent extras
        userId = getIntent().getStringExtra(Constants.EXTRA_USER_ID);
        username = getIntent().getStringExtra(Constants.EXTRA_USERNAME);
        avatarUrl = getIntent().getStringExtra(Constants.EXTRA_AVATAR_URL);

        SharedPreferences prefs = getSharedPreferences(Constants.PREF_NAME, MODE_PRIVATE);
        currentUserId = prefs.getString(Constants.PREF_USER_ID, "");

        initViews();
        loadProfile();
    }

    private void initViews() {
        imgAvatar = findViewById(R.id.img_profile_avatar);
        tvUsername = findViewById(R.id.tv_profile_username);
        tvBio = findViewById(R.id.tv_profile_bio);
        tvMatchPercentage = findViewById(R.id.tv_match_percentage);
        tvMatchDetail = findViewById(R.id.tv_match_detail);
        chipGroupLanguages = findViewById(R.id.chip_group_languages);
        layoutRepos = findViewById(R.id.layout_repos);
        layoutActions = findViewById(R.id.layout_actions);
        layoutMatch = findViewById(R.id.layout_match);
        btnConnect = findViewById(R.id.btn_connect);
        btnMessage = findViewById(R.id.btn_message);
        btnViewGitHub = findViewById(R.id.btn_view_github);

        // Back button
        findViewById(R.id.btn_back).setOnClickListener(v -> finish());

        // Connect / Send Invite
        btnConnect.setOnClickListener(v -> handleConnectClick());

        // Message (only if connected)
        btnMessage.setOnClickListener(v -> handleMessageClick());

        // View on GitHub
        btnViewGitHub.setOnClickListener(v -> {
            String url = Constants.GITHUB_BASE_URL + username;
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
        });
    }

    private void handleConnectClick() {
        ConnectionRequest existing = connectionDao.getConnectionBetween(currentUserId, userId);

        if (existing == null) {
            // Send invite
            connectionDao.sendRequest(currentUserId, userId);
            Toast.makeText(this, "Invite sent to " + username + "! 📩", Toast.LENGTH_SHORT).show();
            btnConnect.setText("📩 Invite Sent");
            btnConnect.setEnabled(false);
        } else if (existing.isPending() && existing.getToUserId().equals(currentUserId)) {
            // We received this invite — accept it
            connectionDao.acceptRequest(existing.getId());
            Toast.makeText(this, "Connected with " + username + "! 🎉", Toast.LENGTH_SHORT).show();
            btnConnect.setText("✓ Connected");
            btnConnect.setEnabled(false);
            btnMessage.setEnabled(true);
            btnMessage.setAlpha(1f);
        }
    }

    private void handleMessageClick() {
        boolean connected = connectionDao.areConnected(currentUserId, userId);
        if (connected) {
            Intent intent = new Intent(this, ChatActivity.class);
            intent.putExtra(Constants.EXTRA_USER_ID, userId);
            intent.putExtra(Constants.EXTRA_USERNAME, username);
            intent.putExtra(Constants.EXTRA_AVATAR_URL, avatarUrl);
            startActivity(intent);
        } else {
            Toast.makeText(this, "Send an invite first to start chatting", Toast.LENGTH_SHORT).show();
        }
    }

    private void loadProfile() {
        // Load avatar
        if (avatarUrl != null && !avatarUrl.isEmpty()) {
            Glide.with(this).load(avatarUrl).circleCrop().into(imgAvatar);
        }

        tvUsername.setText(username != null ? username : "Unknown");

        // Try to load from database
        profileUser = userDao.getUserById(userId);

        if (profileUser != null) {
            tvBio.setText(profileUser.getBio() != null ? profileUser.getBio() : "Developer");
            displayLanguages(profileUser.getLanguages());
            calculateAndShowMatch();
        } else {
            tvBio.setText("Developer on GitLinked");
            displayLanguages(new ArrayList<>());
        }

        // Fetch real repos from GitHub
        fetchAndDisplayRepos();

        // Check if viewing own profile
        boolean isOwnProfile = userId != null && userId.equals(currentUserId);
        if (isOwnProfile) {
            layoutActions.setVisibility(View.GONE);
            layoutMatch.setVisibility(View.GONE);
        } else {
            // Update button states based on connection status
            updateConnectionButtons();
        }
    }

    private void updateConnectionButtons() {
        ConnectionRequest existing = connectionDao.getConnectionBetween(currentUserId, userId);

        if (existing == null) {
            // No connection — show "Send Invite"
            btnConnect.setText("Send Invite");
            btnConnect.setEnabled(true);
            btnMessage.setEnabled(false);
            btnMessage.setAlpha(0.5f);
        } else if (existing.isPending()) {
            if (existing.getFromUserId().equals(currentUserId)) {
                // We sent the invite
                btnConnect.setText("📩 Invite Sent");
                btnConnect.setEnabled(false);
                btnMessage.setEnabled(false);
                btnMessage.setAlpha(0.5f);
            } else {
                // We received the invite — show "Accept Invite"
                btnConnect.setText("Accept Invite");
                btnConnect.setEnabled(true);
                btnMessage.setEnabled(false);
                btnMessage.setAlpha(0.5f);
            }
        } else if (existing.isAccepted()) {
            btnConnect.setText("✓ Connected");
            btnConnect.setEnabled(false);
            btnMessage.setEnabled(true);
            btnMessage.setAlpha(1f);
        }
    }

    private void calculateAndShowMatch() {
        // Build current user from prefs
        SharedPreferences prefs = getSharedPreferences(Constants.PREF_NAME, MODE_PRIVATE);
        User currentUser = new User();
        String langsStr = prefs.getString(Constants.PREF_LANGUAGES, "Java,Python,JavaScript");
        currentUser.setLanguages(new ArrayList<>(Arrays.asList(langsStr.split(","))));
        currentUser.setInterests(new ArrayList<>(Arrays.asList("Android", "Web Dev", "Open Source", "AI/ML")));

        if (profileUser != null) {
            MatchResult match = MatchUtils.calculateMatch(currentUser, profileUser);
            tvMatchPercentage.setText(match.getMatchPercentage() + "%");
            tvMatchPercentage.setTextColor(Color.parseColor(
                    MatchUtils.getMatchColor(match.getMatchPercentage())));
            tvMatchDetail.setText(match.getMatchSummary());
        }
    }

    private void displayLanguages(List<String> languages) {
        chipGroupLanguages.removeAllViews();
        if (languages == null || languages.isEmpty()) {
            // Show placeholder
            Chip chip = new Chip(this);
            chip.setText("Loading...");
            chip.setChipBackgroundColorResource(R.color.chipBackground);
            chip.setTextColor(getResources().getColor(R.color.textSecondary, null));
            chip.setClickable(false);
            chipGroupLanguages.addView(chip);
            return;
        }

        for (String lang : languages) {
            if (lang.trim().isEmpty()) continue;
            Chip chip = new Chip(this);
            chip.setText(lang.trim());
            chip.setChipBackgroundColorResource(R.color.chipBackground);
            chip.setTextColor(getResources().getColor(R.color.chipText, null));
            chip.setClickable(false);
            chipGroupLanguages.addView(chip);
        }
    }

    /**
     * Fetch real repos from GitHub API.
     * For the logged-in user, use authenticated endpoint.
     * For other users, use public endpoint.
     */
    private void fetchAndDisplayRepos() {
        GitHubService gitHubService = new GitHubService(this);

        // Check if this is a mock user
        if (userId != null && userId.startsWith("mock_")) {
            displayMockRepos();
            return;
        }

        // Check if this is the demo user
        if ("demo_user".equals(userId)) {
            displayMockRepos();
            return;
        }

        boolean isOwnProfile = userId != null && userId.equals(currentUserId);

        if (isOwnProfile && gitHubService.getSavedToken() != null) {
            // Fetch own repos with auth token
            gitHubService.fetchRepos(gitHubService.getSavedToken(), new GitHubService.OnReposLoaded() {
                @Override
                public void onSuccess(List<Repository> repos) {
                    runOnUiThread(() -> {
                        displayRepos(repos);
                        // Also update languages from repos
                        updateLanguagesFromRepos(repos);
                    });
                }

                @Override
                public void onError(String error) {
                    Log.w(TAG, "Failed to fetch own repos: " + error);
                    runOnUiThread(() -> displayMockRepos());
                }
            });
        } else if (username != null && !username.isEmpty()) {
            // Fetch public repos
            gitHubService.fetchPublicRepos(username, new GitHubService.OnReposLoaded() {
                @Override
                public void onSuccess(List<Repository> repos) {
                    runOnUiThread(() -> {
                        displayRepos(repos);
                        updateLanguagesFromRepos(repos);
                    });
                }

                @Override
                public void onError(String error) {
                    Log.w(TAG, "Failed to fetch public repos for " + username + ": " + error);
                    runOnUiThread(() -> displayMockRepos());
                }
            });
        } else {
            displayMockRepos();
        }
    }

    private void updateLanguagesFromRepos(List<Repository> repos) {
        List<String> languages = new ArrayList<>();
        for (Repository repo : repos) {
            if (repo.getLanguage() != null && !repo.getLanguage().isEmpty()
                    && !languages.contains(repo.getLanguage())) {
                languages.add(repo.getLanguage());
            }
        }
        if (!languages.isEmpty()) {
            displayLanguages(languages);
        }
    }

    /**
     * Display real repositories fetched from GitHub.
     */
    private void displayRepos(List<Repository> repos) {
        layoutRepos.removeAllViews();

        if (repos.isEmpty()) {
            TextView empty = new TextView(this);
            empty.setText("No public repositories found");
            empty.setTextColor(getResources().getColor(R.color.textSecondary, null));
            empty.setTextSize(13);
            layoutRepos.addView(empty);
            return;
        }

        // Show up to 6 repos
        int limit = Math.min(repos.size(), 6);
        for (int i = 0; i < limit; i++) {
            Repository repo = repos.get(i);
            addRepoView(repo);
        }
    }

    /**
     * Display mock repositories for demo/mock users.
     */
    private void displayMockRepos() {
        layoutRepos.removeAllViews();

        List<Repository> repos = new ArrayList<>();
        if (username != null) {
            switch (username) {
                case "priya_codes":
                    repos.add(new Repository("react-dashboard", "Admin dashboard with React + TypeScript", "TypeScript", 42, 12, ""));
                    repos.add(new Repository("node-api-starter", "REST API boilerplate with Express", "JavaScript", 28, 8, ""));
                    repos.add(new Repository("ml-image-classifier", "CNN-based image classifier", "Python", 67, 15, ""));
                    break;
                case "arjun_dev":
                    repos.add(new Repository("compose-weather", "Weather app built with Jetpack Compose", "Kotlin", 89, 23, ""));
                    repos.add(new Repository("flutter-todo", "Cross-platform todo app", "Dart", 34, 9, ""));
                    break;
                case "sneha_ml":
                    repos.add(new Repository("sentiment-analysis", "NLP sentiment analyzer using BERT", "Python", 120, 34, ""));
                    repos.add(new Repository("data-viz-toolkit", "Interactive data visualization library", "Python", 56, 14, ""));
                    repos.add(new Repository("recommendation-engine", "Collaborative filtering recommender", "Python", 78, 19, ""));
                    break;
                case "demo_dev":
                    repos.add(new Repository("gitlinked-app", "Location-aware developer networking app", "Java", 25, 7, ""));
                    repos.add(new Repository("python-utils", "Collection of Python utility scripts", "Python", 15, 3, ""));
                    repos.add(new Repository("web-portfolio", "Personal portfolio website", "JavaScript", 10, 2, ""));
                    break;
                default:
                    repos.add(new Repository("awesome-project", "A cool project", "Java", 15, 3, ""));
                    repos.add(new Repository("hello-world", "Getting started repo", "Python", 5, 1, ""));
                    break;
            }
        }

        for (Repository repo : repos) {
            addRepoView(repo);
        }
    }

    private void addRepoView(Repository repo) {
        View repoView = LayoutInflater.from(this).inflate(android.R.layout.simple_list_item_2, layoutRepos, false);

        TextView title = repoView.findViewById(android.R.id.text1);
        TextView subtitle = repoView.findViewById(android.R.id.text2);

        title.setText("📦 " + repo.getName());
        title.setTextColor(getResources().getColor(R.color.accentCyan, null));
        title.setTextSize(15);

        String desc = (repo.getLanguage() != null ? repo.getLanguage() + " · " : "")
                + "⭐ " + repo.getStars() + "  🍴 " + repo.getForks();
        if (repo.getDescription() != null && !repo.getDescription().isEmpty()) {
            desc = repo.getDescription() + "\n" + desc;
        }
        subtitle.setText(desc);
        subtitle.setTextColor(getResources().getColor(R.color.textSecondary, null));
        subtitle.setTextSize(12);

        // Click to open in browser
        if (repo.getUrl() != null && !repo.getUrl().isEmpty()) {
            repoView.setOnClickListener(v ->
                    startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(repo.getUrl()))));
        }

        repoView.setPadding(0, 12, 0, 12);
        layoutRepos.addView(repoView);

        // Divider
        View divider = new View(this);
        divider.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 1));
        divider.setBackgroundColor(getResources().getColor(R.color.divider, null));
        layoutRepos.addView(divider);
    }
}
