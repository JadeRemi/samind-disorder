package com.samind.app.data

import android.content.Context

object Prefs {
    private const val FILE = "samind_prefs"
    private const val KEY_MONITORING = "monitoring_enabled"
    private const val KEY_NAME = "display_name"
    private const val KEY_TECHNIQUE = "practice_technique"
    private const val KEY_MINUTES = "practice_minutes"

    fun displayName(context: Context): String =
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE).getString(KEY_NAME, "") ?: ""

    fun setDisplayName(context: Context, name: String) {
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
            .edit().putString(KEY_NAME, name).apply()
    }

    fun technique(context: Context): String =
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
            .getString(KEY_TECHNIQUE, "4-4-4-4") ?: "4-4-4-4"

    fun minutes(context: Context): Int =
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE).getInt(KEY_MINUTES, 10)

    fun setPractice(context: Context, technique: String, minutes: Int) {
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE).edit()
            .putString(KEY_TECHNIQUE, technique).putInt(KEY_MINUTES, minutes).apply()
    }

    fun monitoringEnabled(context: Context): Boolean =
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
            .getBoolean(KEY_MONITORING, false)

    fun setMonitoringEnabled(context: Context, enabled: Boolean) {
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_MONITORING, enabled)
            .apply()
    }
}
