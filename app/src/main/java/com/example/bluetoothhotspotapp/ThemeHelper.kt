package com.example.bluetoothhotspotapp


import android.app.Activity
import android.content.Context
import android.content.SharedPreferences

class ThemeHelper(private val context: Context) {
    companion object {
        private const val PREF_NAME = "theme_preferences"
        private const val KEY_THEME = "selected_theme"
        private const val THEME_IPN = "ipn"
        private const val THEME_ESCOM = "escom"
    }

    private val preferences: SharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

    fun getCurrentTheme(): String {
        return preferences.getString(KEY_THEME, THEME_IPN) ?: THEME_IPN
    }

    fun setTheme(theme: String) {
        preferences.edit().putString(KEY_THEME, theme).apply()
    }

    fun applyTheme(activity: Activity) {
        when (getCurrentTheme()) {
            THEME_IPN -> {
                activity.setTheme(R.style.Theme_IPN)
            }
            THEME_ESCOM -> {
                activity.setTheme(R.style.Theme_ESCOM)
            }
        }
    }

    fun toggleTheme(activity: Activity): String {
        val currentTheme = getCurrentTheme()
        val newTheme = if (currentTheme == THEME_IPN) THEME_ESCOM else THEME_IPN

        setTheme(newTheme)
        return newTheme
    }

}
