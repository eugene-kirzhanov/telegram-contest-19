package by.anegin.tgcontest.core.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import by.anegin.tgcontest.R;

public class ThemeHelper {

    private static final String PREF_DARK_THEME = "dark_theme";

    private final SharedPreferences prefs;

    public ThemeHelper(Context context) {
        prefs = PreferenceManager.getDefaultSharedPreferences(context);
    }

    public int getTheme() {
        boolean isDarkTheme = prefs.getBoolean(PREF_DARK_THEME, false);
        if (isDarkTheme) {
            return R.style.AppThemeDark;
        } else {
            return R.style.AppThemeLight;
        }
    }

    public void toggleTheme() {
        boolean isDarkTheme = prefs.getBoolean(PREF_DARK_THEME, false);
        prefs.edit()
                .putBoolean(PREF_DARK_THEME, !isDarkTheme)
                .apply();
    }

}