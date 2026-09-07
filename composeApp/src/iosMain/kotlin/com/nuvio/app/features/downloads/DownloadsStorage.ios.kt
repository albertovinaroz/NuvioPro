package com.nuvio.app.features.downloads

import com.nuvio.app.core.storage.ProfileScopedKey
import platform.Foundation.NSUserDefaults

internal actual object DownloadsStorage {
    private const val payloadKey = "downloads_payload"
    private const val downloadLocationUriKey = "download_location_uri"

    actual fun loadPayload(): String? =
        NSUserDefaults.standardUserDefaults.stringForKey(ProfileScopedKey.of(payloadKey))

    actual fun savePayload(payload: String) {
        NSUserDefaults.standardUserDefaults.setObject(payload, forKey = ProfileScopedKey.of(payloadKey))
    }

    // Device-level, deliberately *not* profile-scoped (see DownloadsSettingsRepository) — unlike
    // payloadKey above, which is per-profile on purpose since each profile's downloaded content
    // list is its own.
    actual fun getDownloadLocationUri(): String? =
        NSUserDefaults.standardUserDefaults.stringForKey(downloadLocationUriKey)

    actual fun setDownloadLocationUri(uri: String?) {
        if (uri == null) {
            NSUserDefaults.standardUserDefaults.removeObjectForKey(downloadLocationUriKey)
        } else {
            NSUserDefaults.standardUserDefaults.setObject(uri, forKey = downloadLocationUriKey)
        }
    }
}
