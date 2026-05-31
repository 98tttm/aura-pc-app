package com.example.aura_pc_app.utils;

public final class Constants {

    public static final String BASE_URL = "https://aurapc-backend.onrender.com/api/";
    public static final String DB_NAME = "aura_pc_database";

    // Secure storage
    public static final String SECURE_PREFS_FILE = "aurapc_secure_prefs";
    public static final String KEY_ACCESS_TOKEN = "access_token";
    public static final String KEY_REFRESH_TOKEN = "refresh_token";
    public static final String KEY_CURRENT_USER = "current_user";

    // Token refresh
    public static final int MAX_TOKEN_RETRY = 2;

    private Constants() {}
}
