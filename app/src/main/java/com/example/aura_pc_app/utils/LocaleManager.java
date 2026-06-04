package com.example.aura_pc_app.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import java.util.Locale;

public final class LocaleManager {
    public static final String VIETNAMESE = "vi";
    public static final String ENGLISH = "en";

    private static final String PREFS_NAME = "aurapc_locale";
    private static final String KEY_LANGUAGE = "app_language";

    private LocaleManager() {
    }

    public static Context wrap(Context context) {
        String language = getLanguage(context);
        Locale locale = new Locale(language);
        Locale.setDefault(locale);

        Configuration configuration = new Configuration(context.getResources().getConfiguration());
        configuration.setLocale(locale);
        configuration.setLayoutDirection(locale);
        return context.createConfigurationContext(configuration);
    }

    public static String getLanguage(Context context) {
        return getPrefs(context).getString(KEY_LANGUAGE, VIETNAMESE);
    }

    public static boolean isVietnamese(Context context) {
        return VIETNAMESE.equals(getLanguage(context));
    }

    public static String toggleLanguage(Context context) {
        String nextLanguage = isVietnamese(context) ? ENGLISH : VIETNAMESE;
        setLanguage(context, nextLanguage);
        return nextLanguage;
    }

    public static void setLanguage(Context context, String language) {
        String normalizedLanguage = ENGLISH.equals(language) ? ENGLISH : VIETNAMESE;
        getPrefs(context).edit().putString(KEY_LANGUAGE, normalizedLanguage).apply();
    }

    private static SharedPreferences getPrefs(Context context) {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }
}
