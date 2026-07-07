package com.aurapc.admin.utils;

import android.view.View;

import com.google.android.material.snackbar.Snackbar;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * Lightweight UI helpers.
 */
public final class UiUtils {

    private UiUtils() {}

    public static void showSnackbar(@NonNull View anchor, @Nullable String message) {
        if (message == null || message.isEmpty() || anchor.getContext() == null) return;
        Snackbar.make(anchor, message, Snackbar.LENGTH_LONG).show();
    }

    public static String joinNonEmpty(@NonNull String separator, @Nullable String... parts) {
        if (parts == null) return "";
        StringBuilder sb = new StringBuilder();
        boolean first = true;
        for (String p : parts) {
            if (p == null || p.trim().isEmpty()) continue;
            if (!first) sb.append(separator);
            sb.append(p.trim());
            first = false;
        }
        return sb.toString();
    }

    public static String mask(@Nullable String value, int visibleChars) {
        if (value == null || value.isEmpty()) return "";
        if (value.length() <= visibleChars) return value;
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < value.length() - visibleChars; i++) sb.append('•');
        sb.append(value.substring(value.length() - visibleChars));
        return sb.toString();
    }
}