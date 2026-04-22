package com.example.gitlinked.api;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.example.gitlinked.models.Repository;
import com.example.gitlinked.models.User;
import com.example.gitlinked.database.UserDao;
import com.example.gitlinked.utils.Constants;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * High-level service wrapping GitHub API calls.
 */
public class GitHubService {

    private static final String TAG = "GitHubService";
    private final ApiInterface apiClient;
    private final ApiInterface authClient;
    private final Context context;

    public interface OnTokenReceived {
        void onSuccess(String accessToken);
        void onError(String error);
    }

    public interface OnUserLoaded {
        void onSuccess(User user);
        void onError(String error);
    }

    public interface OnReposLoaded {
        void onSuccess(List<Repository> repos);
        void onError(String error);
    }

    public GitHubService(Context context) {
        this.context = context;
        this.apiClient = ApiClient.getGitHubApiClient().create(ApiInterface.class);
        this.authClient = ApiClient.getGitHubAuthClient().create(ApiInterface.class);
    }

    /**
     * Exchange OAuth code for access token.
     */
    public void exchangeCodeForToken(String code, OnTokenReceived callback) {
        authClient.getAccessToken(
                Constants.GITHUB_CLIENT_ID,
                Constants.GITHUB_CLIENT_SECRET,
                code,
                Constants.GITHUB_REDIRECT_URI
        ).enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                if (response.isSuccessful() && response.body() != null) {
                    JsonObject body = response.body();
                    if (body.has("access_token")) {
                        String token = body.get("access_token").getAsString();
                        saveToken(token);
                        callback.onSuccess(token);
                    } else {
                        String error = body.has("error_description")
                                ? body.get("error_description").getAsString()
                                : "Failed to get access token";
                        callback.onError(error);
                    }
                } else {
                    callback.onError("Token exchange failed: " + response.code());
                }
            }

            @Override
            public void onFailure(Call<JsonObject> call, Throwable t) {
                callback.onError("Network error: " + t.getMessage());
            }
        });
    }

    /**
     * Fetch authenticated user profile.
     */
    public void fetchUser(String token, OnUserLoaded callback) {
        apiClient.getAuthenticatedUser("Bearer " + token)
                .enqueue(new Callback<JsonObject>() {
                    @Override
                    public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            // IMPORTANT: Use login (username) as the user ID.
                            // This is what gets broadcast via BLE, and other devices
                            // use it to fetch our public GitHub profile via API.
                            JsonObject json = response.body();
                            User user = new User();
                            String login = json.get("login").getAsString();
                            user.setId(login);
                            user.setUsername(login);
                            user.setAvatarUrl(json.has("avatar_url") && !json.get("avatar_url").isJsonNull()
                                    ? json.get("avatar_url").getAsString() : "");
                            user.setBio(json.has("bio") && !json.get("bio").isJsonNull()
                                    ? json.get("bio").getAsString() : "Developer on GitLinked");

                            // Save to preferences
                            saveUserInfo(user);
                            callback.onSuccess(user);
                        } else {
                            callback.onError("Failed to fetch user: " + response.code());
                        }
                    }

                    @Override
                    public void onFailure(Call<JsonObject> call, Throwable t) {
                        callback.onError("Network error: " + t.getMessage());
                    }
                });
    }

    /**
     * Fetch authenticated user's repositories, extract languages, and persist both.
     */
    public void fetchRepos(String token, OnReposLoaded callback) {
        apiClient.getUserRepos("Bearer " + token, "pushed", 10)
                .enqueue(new Callback<List<Repository>>() {
                    @Override
                    public void onResponse(Call<List<Repository>> call, Response<List<Repository>> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            List<Repository> repos = response.body();

                            // Filter out forks and keep only non-fork repos
                            List<Repository> ownRepos = new ArrayList<>();
                            Set<String> languages = new HashSet<>();
                            for (Repository repo : repos) {
                                if (!repo.isFork()) {
                                    ownRepos.add(repo);
                                }
                                if (repo.getLanguage() != null && !repo.getLanguage().isEmpty()) {
                                    languages.add(repo.getLanguage());
                                }
                            }

                            // Save languages to preferences
                            SharedPreferences prefs = context.getSharedPreferences(
                                    Constants.PREF_NAME, Context.MODE_PRIVATE);
                            prefs.edit()
                                    .putString(Constants.PREF_LANGUAGES,
                                            String.join(",", languages))
                                    .apply();

                            // Also update the user record in database with languages
                            String userId = prefs.getString(Constants.PREF_USER_ID, "");
                            if (!userId.isEmpty()) {
                                UserDao userDao = new UserDao(context);
                                User dbUser = userDao.getUserById(userId);
                                if (dbUser != null) {
                                    dbUser.setLanguages(new ArrayList<>(languages));
                                    dbUser.setTopRepos(ownRepos);
                                    userDao.insertOrUpdate(dbUser);
                                }
                            }

                            callback.onSuccess(ownRepos);
                        } else {
                            callback.onError("Failed to fetch repos: " + response.code());
                        }
                    }

                    @Override
                    public void onFailure(Call<List<Repository>> call, Throwable t) {
                        callback.onError("Network error: " + t.getMessage());
                    }
                });
    }

    /**
     * Fetch a public user's profile by username (no auth required).
     * Used when discovering a user via BLE — their GitHub username is in the BLE
     * service data, and we fetch their full profile to get avatar, bio, etc.
     */
    public void fetchPublicUser(String username, OnUserLoaded callback) {
        apiClient.getUser(username).enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                if (response.isSuccessful() && response.body() != null) {
                    JsonObject json = response.body();
                    User user = new User();

                    // For public API, use the login as the user ID
                    // (this matches what was advertised via BLE)
                    user.setId(json.get("login").getAsString());
                    user.setUsername(json.get("login").getAsString());
                    user.setAvatarUrl(json.has("avatar_url") && !json.get("avatar_url").isJsonNull()
                            ? json.get("avatar_url").getAsString() : "");
                    user.setBio(json.has("bio") && !json.get("bio").isJsonNull()
                            ? json.get("bio").getAsString() : "Developer on GitHub");

                    if (json.has("email") && !json.get("email").isJsonNull()) {
                        user.setEmail(json.get("email").getAsString());
                    }

                    callback.onSuccess(user);
                } else {
                    callback.onError("Failed to fetch public user: " + response.code());
                }
            }

            @Override
            public void onFailure(Call<JsonObject> call, Throwable t) {
                callback.onError("Network error: " + t.getMessage());
            }
        });
    }

    /**
     * Fetch a public user's repositories by username.
     */
    public void fetchPublicRepos(String username, OnReposLoaded callback) {
        apiClient.getPublicRepos(username, "pushed", 10)
                .enqueue(new Callback<List<Repository>>() {
                    @Override
                    public void onResponse(Call<List<Repository>> call, Response<List<Repository>> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            List<Repository> repos = response.body();
                            List<Repository> ownRepos = new ArrayList<>();
                            for (Repository repo : repos) {
                                if (!repo.isFork()) {
                                    ownRepos.add(repo);
                                }
                            }
                            callback.onSuccess(ownRepos);
                        } else {
                            callback.onError("Failed to fetch repos: " + response.code());
                        }
                    }

                    @Override
                    public void onFailure(Call<List<Repository>> call, Throwable t) {
                        callback.onError("Network error: " + t.getMessage());
                    }
                });
    }

    private void saveToken(String token) {
        SharedPreferences prefs = context.getSharedPreferences(Constants.PREF_NAME, Context.MODE_PRIVATE);
        prefs.edit().putString(Constants.PREF_ACCESS_TOKEN, token).apply();
    }

    private void saveUserInfo(User user) {
        SharedPreferences prefs = context.getSharedPreferences(Constants.PREF_NAME, Context.MODE_PRIVATE);
        prefs.edit()
                .putString(Constants.PREF_USER_ID, user.getId())
                .putString(Constants.PREF_USERNAME, user.getUsername())
                .putString(Constants.PREF_AVATAR_URL, user.getAvatarUrl())
                .putString(Constants.PREF_USER_BIO, user.getBio())
                .putBoolean(Constants.PREF_IS_LOGGED_IN, true)
                .apply();

        // Also save to database
        UserDao userDao = new UserDao(context);
        userDao.insertOrUpdate(user);
    }

    public String getSavedToken() {
        SharedPreferences prefs = context.getSharedPreferences(Constants.PREF_NAME, Context.MODE_PRIVATE);
        return prefs.getString(Constants.PREF_ACCESS_TOKEN, null);
    }

    public boolean isLoggedIn() {
        SharedPreferences prefs = context.getSharedPreferences(Constants.PREF_NAME, Context.MODE_PRIVATE);
        return prefs.getBoolean(Constants.PREF_IS_LOGGED_IN, false);
    }

    public void logout() {
        SharedPreferences prefs = context.getSharedPreferences(Constants.PREF_NAME, Context.MODE_PRIVATE);
        prefs.edit().clear().apply();
    }
}
