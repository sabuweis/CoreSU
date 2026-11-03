package me.li.coresu.data.repository

import android.content.Context

object PredictiveBackPrefs {
    private const val PREFS = "settings_direct_boot"
    private const val KEY = "enable_predictive_back"

    private fun prefs(context: Context) =
        context.createDeviceProtectedStorageContext()
            .getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun read(context: Context): Boolean = prefs(context).getBoolean(KEY, false)

    fun write(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean(KEY, enabled).commit()
    }
}
