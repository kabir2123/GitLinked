package com.example.gitlinked.api;

import com.example.gitlinked.models.Repository;
import com.google.gson.JsonObject;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.Field;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.Headers;
import retrofit2.http.POST;
import retrofit2.http.Path;
import retrofit2.http.Query;

/**
 * Retrofit interface for GitHub API endpoints.
 */
public interface ApiInterface {

    // ==================== OAUTH ====================

    /**
     * Exchange authorization code for access token.
     */
    @Headers("Accept: application/json")
    @FormUrlEncoded
    @POST("login/oauth/access_token")
    Call<JsonObject> getAccessToken(
            @Field("client_id") String clientId,
            @Field("client_secret") String clientSecret,
            @Field("code") String code,
            @Field("redirect_uri") String redirectUri
    );

    // ==================== USER ====================

    /**
     * Get authenticated user profile.
     */
    @GET("user")
    Call<JsonObject> getAuthenticatedUser(
            @Header("Authorization") String authHeader
    );

    /**
     * Get a specific user's profile.
     */
    @GET("users/{username}")
    Call<JsonObject> getUser(
            @Path("username") String username
    );

    // ==================== REPOSITORIES ====================

    /**
     * Get authenticated user's repositories (sorted by recently pushed).
     */
    @GET("user/repos")
    Call<List<Repository>> getUserRepos(
            @Header("Authorization") String authHeader,
            @Query("sort") String sort,
            @Query("per_page") int perPage
    );

    /**
     * Get a specific user's public repositories.
     */
    @GET("users/{username}/repos")
    Call<List<Repository>> getPublicRepos(
            @Path("username") String username,
            @Query("sort") String sort,
            @Query("per_page") int perPage
    );
}
