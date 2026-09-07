package com.nuvio.app.features.downloads

import android.content.Context
import android.content.SharedPreferences
import com.nuvio.app.core.storage.ProfileScopedKey

internal actual object DownloadsStorage {
    private const val preferencesName = "nuvio_downloads"
    private const val payloadKey = "downloads_payload"
    private const val downloadLocationUriKey = "download_location_uri"

    private var preferences: SharedPreferences? = null

    fun initialize(context: Context) {
        preferences = context.getSharedPreferences(preferencesName, Context.MODE_PRIVATE)
    }

    actual fun loadPayload(): String? =
        preferences?.getString(ProfileScopedKey.of(payloadKey), null)

    actual fun savePayload(payload: String) {
        preferences
            ?.edit()
            ?.putString(ProfileScopedKey.of(payloadKey), payload)
            ?.apply()
    }

    // Device-level, deliberately *not* profile-scoped (see DownloadsSettingsRepository) — unlike
    // payloadKey above, which is per-profile on purpose since each profile's downloaded content
    // list is its own.
    actual fun getDownloadLocationUri(): String? =
        preferences?.getString(downloadLocationUriKey, null)

    actual fun setDownloadLocationUri(uri: String?) {
        preferences
            ?.edit()
            ?.run {
                if (uri == null) {
                    remove(downloadLocationUriKey)
                } else {
                    putString(downloadLocationUriKey, uri)
                }
            }
            ?.apply()
    }
}
