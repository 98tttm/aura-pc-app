package com.example.aura_pc_app.utils;

import android.app.Activity;
import android.content.Intent;
import com.example.aura_pc_app.data.api.TokenManager;
import com.example.aura_pc_app.ui.auth.AuthActivity;

public final class AuthGate {
    public static final String EXTRA_REDIRECT_CLASS_NAME =
            "com.example.aura_pc_app.extra.REDIRECT_CLASS_NAME";

    private AuthGate() {
    }

    public static boolean isLoggedIn(Activity activity) {
        return TokenManager.getInstance(activity).isLoggedIn();
    }

    public static boolean requireLogin(Activity activity, Class<?> redirectClass) {
        if (isLoggedIn(activity)) {
            return true;
        }

        Intent intent = new Intent(activity, AuthActivity.class);
        if (redirectClass != null) {
            intent.putExtra(EXTRA_REDIRECT_CLASS_NAME, redirectClass.getName());
        }
        activity.startActivity(intent);
        activity.overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        return false;
    }
}
