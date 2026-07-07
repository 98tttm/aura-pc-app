package com.aurapc.admin.data.local;

import android.content.Context;
import android.content.SharedPreferences;
import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKey;
import android.content.SharedPreferences.Editor;

public class TokenManager {
    private static final String PREFS_NAME = "aura_admin_secure_prefs";
    private static final String KEY_TOKEN = "jwt_token";
    private static final String KEY_ADMIN_ID = "admin_id";
    private static final String KEY_ADMIN_EMAIL = "admin_email";
    private static final String KEY_ADMIN_NAME = "admin_name";
    private static final String KEY_ADMIN_ROLE = "admin_role";
    private static final String KEY_ADMIN_AVATAR = "admin_avatar";
    private static final String KEY_BIOMETRIC_ENABLED = "biometric_enabled";

    private final SharedPreferences prefs;

    public TokenManager(Context ctx) {
        SharedPreferences p;
        try {
            MasterKey mk = new MasterKey.Builder(ctx)
                    .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                    .build();
            p = EncryptedSharedPreferences.create(ctx, PREFS_NAME, mk,
                    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM);
        } catch (Exception e) {
            p = ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        }
        this.prefs = p;
    }

    public void saveToken(String token) {
        prefs.edit().putString(KEY_TOKEN, token).apply();
    }

    public String getToken() {
        return prefs.getString(KEY_TOKEN, null);
    }

    public void saveAdminProfile(String id, String email, String name, String role, String avatar) {
        Editor e = prefs.edit();
        e.putString(KEY_ADMIN_ID, id);
        e.putString(KEY_ADMIN_EMAIL, email);
        e.putString(KEY_ADMIN_NAME, name);
        e.putString(KEY_ADMIN_ROLE, role);
        e.putString(KEY_ADMIN_AVATAR, avatar);
        e.apply();
    }

    public boolean isLoggedIn() {
        return getToken() != null && !getToken().isEmpty();
    }

    public boolean isBiometricEnabled() {
        return prefs.getBoolean(KEY_BIOMETRIC_ENABLED, false);
    }

    public void setBiometricEnabled(boolean enabled) {
        prefs.edit().putBoolean(KEY_BIOMETRIC_ENABLED, enabled).apply();
    }

    public String getAdminId() { return prefs.getString(KEY_ADMIN_ID, null); }
    public String getAdminEmail() { return prefs.getString(KEY_ADMIN_EMAIL, null); }
    public String getAdminName() { return prefs.getString(KEY_ADMIN_NAME, null); }
    public String getAdminRole() { return prefs.getString(KEY_ADMIN_ROLE, null); }
    public String getAdminAvatar() { return prefs.getString(KEY_ADMIN_AVATAR, null); }

    public void clearAll() {
        prefs.edit().clear().apply();
    }

    public void logout() {
        clearAll();
    }
}
