package com.aura.pc.ui.profile;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.example.aura_pc_app.R;
import com.example.aura_pc_app.data.api.TokenManager;
import com.example.aura_pc_app.ui.home.HomeActivity;
import com.example.aura_pc_app.utils.AuthGate;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.util.Map;

public class CheckingAccountActivity extends AppCompatActivity {

    static final String PROFILE_PREFS = "aura_profile_state";
    static final String KEY_PROFILE_COMPLETED = "profile_completed";
    private static final Gson GSON = new GsonBuilder().create();

    private final Handler handler = new Handler(Looper.getMainLooper());
    private TextView statusText;
    private TextView headlineText;
    private ProgressBar progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_checking_account);
        configureBars();

        statusText = findViewById(R.id.tvCheckingStatus);
        headlineText = findViewById(R.id.tvCheckingHeadline);
        progressBar = findViewById(R.id.progressChecking);
        checkProfileState();
    }

    private void configureBars() {
        Window window = getWindow();
        window.setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        window.setStatusBarColor(ContextCompat.getColor(this, R.color.premium_nav_active));
        window.setNavigationBarColor(ContextCompat.getColor(this, R.color.white));
    }

    private void checkProfileState() {
        SharedPreferences prefs = getSharedPreferences(PROFILE_PREFS, MODE_PRIVATE);
        Map<String, Object> currentUser = getStoredCurrentUser();
        if (hasProfile(currentUser)) {
            prefs.edit().putBoolean(KEY_PROFILE_COMPLETED, true).apply();
            goHome(600);
        } else {
            prefs.edit().putBoolean(KEY_PROFILE_COMPLETED, false).apply();
            goCompleteProfile(700);
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> getStoredCurrentUser() {
        String userJson = TokenManager.getInstance(this).getCurrentUserJson();
        if (userJson == null || userJson.trim().isEmpty()) return null;
        try {
            return GSON.fromJson(userJson, Map.class);
        } catch (Exception ignored) {
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    private boolean hasProfile(Map<String, Object> responseBody) {
        if (responseBody == null) return false;

        Object data = responseBody.get("data");
        if (data instanceof Map) {
            responseBody = (Map<String, Object>) data;
        }

        Object user = responseBody.get("user");
        if (user instanceof Map) {
            responseBody = (Map<String, Object>) user;
        }

        if (Boolean.TRUE.equals(responseBody.get("isProfileCompleted")) ||
                Boolean.TRUE.equals(responseBody.get("profileCompleted")) ||
                Boolean.TRUE.equals(responseBody.get("hasProfile"))) {
            return true;
        }

        Object profile = responseBody.get("profile");
        if (profile instanceof Map) {
            Object profileFullName = ((Map<String, Object>) profile).get("fullName");
            if (profileFullName instanceof String && !((String) profileFullName).trim().isEmpty()) {
                return true;
            }
        }

        Object fullName = responseBody.get("fullName");
        if (fullName == null) fullName = responseBody.get("name");
        return fullName instanceof String && !((String) fullName).trim().isEmpty();
    }

    private void goHome(long delayMillis) {
        if (statusText != null) {
            statusText.setText(getString(R.string.label_redirecting_home));
        }
        handler.postDelayed(() -> {
            startActivity(createPostProfileIntent());
        }, delayMillis);
    }

    private void goCompleteProfile(long delayMillis) {
        handler.postDelayed(() -> {
            Intent intent = new Intent(this, CompleteProfileActivity.class);
            copyRedirectExtra(intent);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
        }, delayMillis);
    }

    private Intent createPostProfileIntent() {
        String redirectClassName = getIntent().getStringExtra(AuthGate.EXTRA_REDIRECT_CLASS_NAME);
        if (redirectClassName != null && !redirectClassName.isEmpty()) {
            try {
                Class<?> redirectClass = Class.forName(redirectClassName);
                Intent redirectIntent = new Intent(this, redirectClass);
                redirectIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                return redirectIntent;
            } catch (ClassNotFoundException ignored) {
                // Fall back to Home if an old redirect target is no longer available.
            }
        }

        Intent intent = new Intent(this, HomeActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        return intent;
    }

    private void copyRedirectExtra(Intent targetIntent) {
        String redirectClassName = getIntent().getStringExtra(AuthGate.EXTRA_REDIRECT_CLASS_NAME);
        if (redirectClassName != null && !redirectClassName.isEmpty()) {
            targetIntent.putExtra(AuthGate.EXTRA_REDIRECT_CLASS_NAME, redirectClassName);
        }
    }
}
