package com.example.petlink.util

import android.content.Context
import android.content.SharedPreferences

object UserSession {
    private const val PREFS_NAME = "user_session"
    private const val KEY_AUTH_TOKEN = "auth_token"
    private const val KEY_IS_LOGGED_IN = "is_logged_in"
    private const val KEY_USER_ID = "id"
    private const val KEY_IS_GUEST_MODE = "is_guest_mode"

    private fun prefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun isGuestMode(context: Context): Boolean {
        return prefs(context).getBoolean(KEY_IS_GUEST_MODE, false)
    }

    fun setGuestMode(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean(KEY_IS_GUEST_MODE, enabled).apply()
    }

    fun isLoggedIn(context: Context): Boolean {
        val sp = prefs(context)
        val token = sp.getString(KEY_AUTH_TOKEN, null)
        val flag = sp.getBoolean(KEY_IS_LOGGED_IN, false)
        return !token.isNullOrEmpty() && flag
    }

    fun getAuthToken(context: Context): String? {
        return prefs(context).getString(KEY_AUTH_TOKEN, null)
    }

    fun enterGuestMode(context: Context) {
        prefs(context).edit()
            .remove(KEY_AUTH_TOKEN)
            .remove(KEY_USER_ID)
            .putBoolean(KEY_IS_LOGGED_IN, false)
            .putBoolean(KEY_IS_GUEST_MODE, true)
            .apply()
    }
}
