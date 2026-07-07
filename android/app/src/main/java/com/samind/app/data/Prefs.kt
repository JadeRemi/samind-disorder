package com.samind.app.data

import android.content.Context

object Prefs {
    private const val FILE = "samind_prefs"
    private const val KEY_MONITORING = "monitoring_enabled"

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
