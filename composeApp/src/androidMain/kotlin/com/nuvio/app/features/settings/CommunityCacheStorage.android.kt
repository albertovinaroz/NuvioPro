package com.nuvio.app.features.settings

import android.content.Context
import android.content.SharedPreferences

actual object CommunityCacheStorage {
    private const val preferencesName = "nuvio_community_cache"

    private var preferences: SharedPreferences? = null

    fun initialize(context: Context) {
        preferences = context.getSharedPreferences(preferencesName, Context.MODE_PRIVATE)
    }

    actual fun load(key: String): String? = preferences?.getString(key, null)

    actual fun save(key: String, payload: String?) {
        preferences
            ?.edit()
            ?.apply { if (payload == null) remove(key) else putString(key, payload) }
            ?.apply()
    }
}
