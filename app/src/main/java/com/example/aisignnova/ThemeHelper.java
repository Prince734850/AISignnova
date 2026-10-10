package com.example.aisignnova;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.appcompat.app.AppCompatDelegate;

public class ThemeHelper {

    private static final String PREF_NAME = "aisignnova_prefs";
    private static final String KEY_THEME = "app_theme";

    public static final String THEME_DARK = "dark";
    public static final String THEME_LIGHT = "light";

    public static void applyTheme(Context context) {
        String theme = getSavedTheme(context);
        if (THEME_LIGHT.equals(theme)) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        }
    }

    public static void setTheme(Context context, String theme) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        prefs.edit().putString(KEY_THEME, theme).apply();
        applyTheme(context);
    }

    public static String getSavedTheme(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        return prefs.getString(KEY_THEME, THEME_DARK);
    }

    public static boolean isDarkMode(Context context) {
        return THEME_DARK.equals(getSavedTheme(context));
    }
}