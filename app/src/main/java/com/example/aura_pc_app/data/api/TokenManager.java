package com.example.aura_pc_app.data.api;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKey;

import com.example.aura_pc_app.utils.Constants;

import java.io.IOException;
import java.security.GeneralSecurityException;

/**
 * Manages secure storage of authentication tokens using EncryptedSharedPreferences.
 * Keys and values are encrypted with AES-256, backed by Android Keystore.
 */
public class TokenManager implements TokenProvider {

    private static final String TAG = "TokenManager";
    private static TokenManager instance;
    private final SharedPreferences encryptedPrefs;

    private TokenManager(Context context) {
        SharedPreferences prefs;
        try {
            MasterKey masterKey = new MasterKey.Builder(context.getApplicationContext())
                    .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                    .build();

            prefs = EncryptedSharedPreferences.create(
                    context.getApplicationContext(),
                    Constants.SECURE_PREFS_FILE,
                    masterKey,
                    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            );

            Log.d(TAG, "EncryptedSharedPreferences initialized");

        } catch (GeneralSecurityException | IOException e) {
            Log.e(TAG, "Encryption init failed, using plain SharedPreferences", e);
            prefs = context.getApplicationContext()
                    .getSharedPreferences(Constants.SECURE_PREFS_FILE, Context.MODE_PRIVATE);
        }
        encryptedPrefs = prefs;
    }

    public static synchronized TokenManager getInstance(Context context) {
        if (instance == null) {
            instance = new TokenManager(context);
        }
        return instance;
    }

    // ── Save ──────────────────────────────────────────────

    public void saveAccessToken(String token) {
        encryptedPrefs.edit().putString(Constants.KEY_ACCESS_TOKEN, token).apply();
        Log.d(TAG, "Access token saved (encrypted)");
    }

    public void saveRefreshToken(String token) {
        encryptedPrefs.edit().putString(Constants.KEY_REFRESH_TOKEN, token).apply();
        Log.d(TAG, "Refresh token saved (encrypted)");
    }

    public void saveCurrentUserJson(String userJson) {
        encryptedPrefs.edit().putString(Constants.KEY_CURRENT_USER, userJson).apply();
        Log.d(TAG, "Current user saved (encrypted)");
    }

    // ── Read ──────────────────────────────────────────────

    @Override
    public String getAccessToken() {
        return encryptedPrefs.getString(Constants.KEY_ACCESS_TOKEN, null);
    }

    public String getRefreshToken() {
        return encryptedPrefs.getString(Constants.KEY_REFRESH_TOKEN, null);
    }

    public String getCurrentUserJson() {
        return encryptedPrefs.getString(Constants.KEY_CURRENT_USER, null);
    }

    // ── Clear / Check ─────────────────────────────────────

    public void clearTokens() {
        encryptedPrefs.edit().clear().apply();
        Log.d(TAG, "All tokens cleared");
    }

    public boolean isLoggedIn() {
        String token = getAccessToken();
        return token != null && !token.isEmpty();
    }
}
