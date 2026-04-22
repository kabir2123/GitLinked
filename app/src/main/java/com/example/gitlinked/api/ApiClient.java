package com.example.gitlinked.api;

import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

import com.example.gitlinked.utils.Constants;

/**
 * Retrofit singleton client for GitHub API calls.
 */
public class ApiClient {

    private static Retrofit githubApiRetrofit = null;
    private static Retrofit githubAuthRetrofit = null;

    /**
     * Get Retrofit instance for GitHub API (https://api.github.com/).
     */
    public static Retrofit getGitHubApiClient() {
        if (githubApiRetrofit == null) {
            githubApiRetrofit = new Retrofit.Builder()
                    .baseUrl(Constants.GITHUB_API_BASE_URL)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
        }
        return githubApiRetrofit;
    }

    /**
     * Get Retrofit instance for GitHub OAuth (https://github.com/).
     */
    public static Retrofit getGitHubAuthClient() {
        if (githubAuthRetrofit == null) {
            githubAuthRetrofit = new Retrofit.Builder()
                    .baseUrl(Constants.GITHUB_BASE_URL)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
        }
        return githubAuthRetrofit;
    }
}
