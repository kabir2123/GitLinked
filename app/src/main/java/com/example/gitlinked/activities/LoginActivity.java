package com.example.gitlinked.activities;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.gitlinked.R;
import com.example.gitlinked.api.GitHubService;
import com.example.gitlinked.models.Repository;
import com.example.gitlinked.models.User;
import com.example.gitlinked.utils.Constants;
import com.google.android.material.button.MaterialButton;

import java.util.List;

/**
 * Login screen with GitHub OAuth flow.
 * Also provides a "Demo Mode" that skips OAuth and uses mock data.
 */
public class LoginActivity extends AppCompatActivity {

    private static final String TAG = "LoginActivity";
    private MaterialButton btnGitHubLogin;
    private TextView btnSkipLogin;
    private ProgressBar progressLogin;
    private GitHubService gitHubService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        gitHubService = new GitHubService(this);

        // Check if already logged in
        if (gitHubService.isLoggedIn()) {
            navigateToMain();
            return;
        }

        btnGitHubLogin = findViewById(R.id.btn_github_login);
        btnSkipLogin = findViewById(R.id.btn_skip_login);
        progressLogin = findViewById(R.id.progress_login);

        btnGitHubLogin.setOnClickListener(v -> startGitHubOAuth());
        btnSkipLogin.setOnClickListener(v -> enterDemoMode());
    }

    /**
     * Launch GitHub OAuth in browser.
     */
    private void startGitHubOAuth() {
        String authUrl = Constants.GITHUB_AUTH_URL
                + "?client_id=" + Constants.GITHUB_CLIENT_ID
                + "&redirect_uri=" + Constants.GITHUB_REDIRECT_URI
                + "&scope=" + Constants.GITHUB_SCOPE;

        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(authUrl));
        startActivity(intent);
    }

    /**
     * Handle OAuth callback when GitHub redirects back.
     */
    @Override
    protected void onResume() {
        super.onResume();

        Uri uri = getIntent().getData();
        if (uri != null && uri.toString().startsWith(Constants.GITHUB_REDIRECT_URI)) {
            String code = uri.getQueryParameter("code");
            if (code != null) {
                handleAuthCode(code);
            }
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
    }

    /**
     * Exchange authorization code for access token, then fetch user profile + repos.
     */
    private void handleAuthCode(String code) {
        showLoading(true);

        gitHubService.exchangeCodeForToken(code, new GitHubService.OnTokenReceived() {
            @Override
            public void onSuccess(String accessToken) {
                // Now fetch user profile
                gitHubService.fetchUser(accessToken, new GitHubService.OnUserLoaded() {
                    @Override
                    public void onSuccess(User user) {
                        // Also fetch repos to get languages
                        gitHubService.fetchRepos(accessToken, new GitHubService.OnReposLoaded() {
                            @Override
                            public void onSuccess(List<Repository> repos) {
                                Log.d(TAG, "Fetched " + repos.size() + " repos for " + user.getUsername());
                                runOnUiThread(() -> {
                                    showLoading(false);
                                    Toast.makeText(LoginActivity.this,
                                            "Welcome, " + user.getUsername() + "!",
                                            Toast.LENGTH_SHORT).show();
                                    navigateToMain();
                                });
                            }

                            @Override
                            public void onError(String error) {
                                // Repos failed but user is still logged in - proceed anyway
                                Log.w(TAG, "Repo fetch failed: " + error);
                                runOnUiThread(() -> {
                                    showLoading(false);
                                    Toast.makeText(LoginActivity.this,
                                            "Welcome, " + user.getUsername() + "!",
                                            Toast.LENGTH_SHORT).show();
                                    navigateToMain();
                                });
                            }
                        });
                    }

                    @Override
                    public void onError(String error) {
                        runOnUiThread(() -> {
                            showLoading(false);
                            Toast.makeText(LoginActivity.this,
                                    "Error: " + error, Toast.LENGTH_LONG).show();
                        });
                    }
                });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    showLoading(false);
                    Toast.makeText(LoginActivity.this,
                            "Auth failed: " + error, Toast.LENGTH_LONG).show();
                });
            }
        });
    }

    /**
     * Skip OAuth and enter demo mode with mock profile.
     */
    private void enterDemoMode() {
        SharedPreferences prefs = getSharedPreferences(Constants.PREF_NAME, MODE_PRIVATE);
        prefs.edit()
                .putBoolean(Constants.PREF_IS_LOGGED_IN, true)
                .putString(Constants.PREF_USER_ID, "demo_user")
                .putString(Constants.PREF_USERNAME, "demo_dev")
                .putString(Constants.PREF_AVATAR_URL, "https://avatars.githubusercontent.com/u/9919?v=4")
                .putString(Constants.PREF_USER_BIO, "Android Developer | Java & Kotlin | Open Source Contributor")
                .putString(Constants.PREF_LANGUAGES, "Java,Python,JavaScript")
                .apply();

        Toast.makeText(this, "Entering Demo Mode", Toast.LENGTH_SHORT).show();
        navigateToMain();
    }

    private void navigateToMain() {
        startActivity(new Intent(this, MainActivity.class));
        finish();
    }

    private void showLoading(boolean show) {
        progressLogin.setVisibility(show ? View.VISIBLE : View.GONE);
        btnGitHubLogin.setEnabled(!show);
        btnSkipLogin.setEnabled(!show);
    }
}
