package com.example.gitlinked.utils;

import java.util.UUID;

/**
 * Application-wide constants.
 *
 * =====================================================================
 * SETUP INSTRUCTIONS:
 * =====================================================================
 *
 * 1. GITHUB OAUTH APP:
 *    - Go to: https://github.com/settings/developers
 *    - Click "New OAuth App"
 *    - Application name: GitLinked
 *    - Homepage URL: https://github.com (or any URL)
 *    - Authorization callback URL: gitlinked://callback
 *    - Click "Register application"
 *    - Copy the "Client ID" and replace GITHUB_CLIENT_ID below
 *    - Generate a "Client Secret" and replace GITHUB_CLIENT_SECRET below
 *
 * 2. GOOGLE MAPS API KEY:
 *    - Go to: https://console.cloud.google.com/
 *    - Create a project or select an existing one
 *    - Enable "Maps SDK for Android"
 *    - Create an API Key under "Credentials"
 *    - Replace the placeholder in AndroidManifest.xml
 *
 * =====================================================================
 */
public class Constants {

    // ==================== GITHUB OAUTH ====================
    // ⚠️ REPLACE THESE WITH YOUR OWN VALUES (see instructions above)
    public static final String GITHUB_CLIENT_ID = "Ov23liwLfNhhnHL5qGWv";
    public static final String GITHUB_CLIENT_SECRET = "2fe629fc7a766e203029eb3a490a9d687ab9db3d";
    public static final String GITHUB_REDIRECT_URI = "gitlinked://callback";
    public static final String GITHUB_SCOPE = "user,repo";

    // GitHub URLs
    public static final String GITHUB_AUTH_URL = "https://github.com/login/oauth/authorize";
    public static final String GITHUB_TOKEN_URL = "https://github.com/login/oauth/access_token";
    public static final String GITHUB_API_BASE_URL = "https://api.github.com/";
    public static final String GITHUB_BASE_URL = "https://github.com/";

    // ==================== BLE ====================
    public static final UUID GITLINKED_SERVICE_UUID =
            UUID.fromString("a1b2c3d4-e5f6-7890-abcd-ef1234567890");
    public static final UUID GITLINKED_CHAR_UUID =
            UUID.fromString("b2c3d4e5-f6a7-8901-bcde-f12345678901");
    public static final long BLE_SCAN_DURATION_MS = 10000; // 10 seconds

    // ==================== SHARED PREFERENCES ====================
    public static final String PREF_NAME = "gitlinked_prefs";
    public static final String PREF_ACCESS_TOKEN = "access_token";
    public static final String PREF_USERNAME = "username";
    public static final String PREF_AVATAR_URL = "avatar_url";
    public static final String PREF_USER_ID = "user_id";
    public static final String PREF_USER_BIO = "user_bio";
    public static final String PREF_IS_LOGGED_IN = "is_logged_in";
    public static final String PREF_LANGUAGES = "languages";

    // ==================== ENCRYPTION ====================
    // AES-128 key (must be exactly 16 bytes)
    public static final String AES_KEY = "GitLinked2024Key";

    // ==================== DATABASE ====================
    public static final String DB_NAME = "gitlinked.db";
    public static final int DB_VERSION = 3;

    // ==================== NOTIFICATIONS ====================
    public static final String BLE_CHANNEL_ID = "ble_service_channel";
    public static final String BLE_CHANNEL_NAME = "BLE Discovery Service";
    public static final int BLE_NOTIFICATION_ID = 1001;

    // ==================== INTENT EXTRAS ====================
    public static final String EXTRA_USER_ID = "extra_user_id";
    public static final String EXTRA_USERNAME = "extra_username";
    public static final String EXTRA_AVATAR_URL = "extra_avatar_url";
}
