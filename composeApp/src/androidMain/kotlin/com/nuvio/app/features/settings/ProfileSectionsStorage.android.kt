package com.nuvio.app.features.settings

import android.content.Context
import android.content.SharedPreferences

actual object ProfileSectionsStorage {
    private const val preferencesName = "nuvio_profile_sections"
    private const val expandedKey = "expanded"

    private var preferences: SharedPreferences? = null

    fun initialize(context: Context) {
        preferences = context.getSharedPreferences(preferencesName, Context.MODE_PRIVATE)
    }

    actual fun loadExpanded(): String? = preferences?.getString(expandedKey, null)

    actual fun saveExpanded(value: String) {
        preferences?.edit()?.putString(expandedKey, value)?.apply()
    }
}
