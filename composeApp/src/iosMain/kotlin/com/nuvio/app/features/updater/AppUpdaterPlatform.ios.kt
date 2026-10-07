package com.nuvio.app.features.updater

import platform.Foundation.NSUserDefaults
import kotlinx.coroutines.runBlocking
import nuvio.composeapp.generated.resources.Res
import nuvio.composeapp.generated.resources.updates_not_available
import org.jetbrains.compose.resources.getString

private const val whatsNewCacheKey = "nuvio_whats_new_cache"

actual object AppUpdaterPlatform {
    actual val isSupported: Boolean = false
    actual val isDebugBuild: Boolean = false

    actual fun getSupportedAbis(): List<String> = emptyList()

    actual fun getIgnoredTag(): String? = null

    actual fun setIgnoredTag(tag: String?) = Unit

    actual fun getUpdateChannel(): String? = null

    actual fun setUpdateChannel(channel: String) = Unit

    actual fun getWhatsNewCache(): String? =
        NSUserDefaults.standardUserDefaults.stringForKey(whatsNewCacheKey)

    actual fun setWhatsNewCache(payload: String?) {
        val defaults = NSUserDefaults.standardUserDefaults
        if (payload == null) {
            defaults.removeObjectForKey(whatsNewCacheKey)
        } else {
            defaults.setObject(payload, forKey = whatsNewCacheKey)
        }
    }

    actual fun deleteDownloadedApk(path: String) = Unit

    actual suspend fun downloadApk(
        assetUrl: String,
        assetName: String,
        onProgress: (downloadedBytes: Long, totalBytes: Long?) -> Unit,
    ): Result<String> = Result.failure(IllegalStateException(getString(Res.string.updates_not_available)))

    actual fun canRequestPackageInstalls(): Boolean = false

    actual fun openUnknownSourcesSettings() = Unit

    actual fun installDownloadedApk(path: String): Result<Unit> =
        Result.failure(IllegalStateException(runBlocking { getString(Res.string.updates_not_available) }))
}
