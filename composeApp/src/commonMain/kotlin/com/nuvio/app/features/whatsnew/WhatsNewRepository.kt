package com.nuvio.app.features.whatsnew

import com.nuvio.app.core.build.AppVersionConfig
import com.nuvio.app.features.updater.AppUpdaterPlatform
import com.nuvio.app.features.updater.AppUpdaterRepository
import com.nuvio.app.features.updater.ChannelReleaseNote
import com.nuvio.app.features.updater.VersionUtils
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.time.Clock

private const val CACHE_TTL_MILLIS = 24L * 60L * 60L * 1000L
private const val FAILED_REFRESH_RETRY_MILLIS = 6L * 60L * 60L * 1000L
// Bumped when the cached shape changes, so an old payload is refetched instead of misread.
private const val CACHE_FORMAT = 1

@Serializable
private data class CachedRelease(
    val tag: String,
    val title: String,
    val notes: String,
    val releaseUrl: String? = null,
    val publishedAt: String? = null,
)

@Serializable
private data class WhatsNewCacheEnvelope(
    val format: Int = CACHE_FORMAT,
    val fetchedAtMillis: Long,
    val lastFailedAttemptAtMillis: Long = 0L,
    val etag: String? = null,
    val releases: List<CachedRelease>,
)

/**
 * The Pro channel's release history (see [AppUpdaterRepository.getChannelReleaseHistory]), kept
 * on disk for a day so opening What's New doesn't hit GitHub every time — and so it still shows
 * something offline. Refreshes are conditional on the cached ETag.
 */
internal object WhatsNewRepository {
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    suspend fun load(
        currentVersion: String = AppVersionConfig.VERSION_NAME,
        forceRefresh: Boolean = false,
    ): Result<WhatsNewContent> = try {
        Result.success(loadInternal(currentVersion, forceRefresh))
    } catch (error: CancellationException) {
        throw error
    } catch (error: Throwable) {
        Result.failure(error)
    }

    private suspend fun loadInternal(currentVersion: String, forceRefresh: Boolean): WhatsNewContent {
        val now = Clock.System.now().toEpochMilliseconds()
        val cached = readCache()
        val cachedContent = cached?.let {
            content(it.releases, currentVersion, fromCache = true, fetchedAtMillis = it.fetchedAtMillis)
        }
        if (!forceRefresh && cached != null && cachedContent != null) {
            if (now - cached.fetchedAtMillis < CACHE_TTL_MILLIS) return cachedContent
            if (
                cached.lastFailedAttemptAtMillis > 0L &&
                now - cached.lastFailedAttemptAtMillis < FAILED_REFRESH_RETRY_MILLIS
            ) {
                return cachedContent.copy(isStale = true)
            }
        }

        val history = try {
            AppUpdaterRepository.getChannelReleaseHistory(etag = cached?.etag.takeIf { cachedContent != null })
        } catch (error: CancellationException) {
            throw error
        } catch (error: Throwable) {
            cached?.let { persist(it.copy(lastFailedAttemptAtMillis = now)) }
            if (cachedContent != null) return cachedContent.copy(isStale = true)
            throw error
        }

        if (history.notModified && cached != null && cachedContent != null) {
            persist(cached.copy(fetchedAtMillis = now, lastFailedAttemptAtMillis = 0L, etag = history.etag))
            return cachedContent.copy(fetchedAtMillis = now)
        }

        val releases = history.releases.map { it.toCached() }
        persist(
            WhatsNewCacheEnvelope(
                fetchedAtMillis = now,
                etag = history.etag,
                releases = releases,
            ),
        )
        return content(releases, currentVersion, fromCache = false, fetchedAtMillis = now)
            ?: error("GitHub returned release history without a valid release")
    }

    private fun content(
        releases: List<CachedRelease>,
        currentVersion: String,
        fromCache: Boolean,
        fetchedAtMillis: Long,
    ): WhatsNewContent? {
        val mapped = releases.mapNotNull { release ->
            val rawVersion = release.tag.takeIf { VersionUtils.parse(it) != null }
                ?: return@mapNotNull null
            val version = VersionUtils.normalize(rawVersion)
            WhatsNewRelease(
                version = version,
                title = release.title.takeIf { it.isNotBlank() } ?: version,
                notes = WhatsNewSnapshotBuilder.cleanReleaseNotes(release.notes),
                publishedAt = release.publishedAt,
                releaseUrl = release.releaseUrl?.takeIf(::isTrustedReleaseUrl),
            )
        }
        if (mapped.isEmpty()) return null
        return WhatsNewContent(
            snapshot = WhatsNewSnapshotBuilder.build(mapped, currentVersion),
            fromCache = fromCache,
            isStale = false,
            fetchedAtMillis = fetchedAtMillis,
        )
    }

    private fun readCache(): WhatsNewCacheEnvelope? {
        val raw = AppUpdaterPlatform.getWhatsNewCache() ?: return null
        return runCatching { json.decodeFromString<WhatsNewCacheEnvelope>(raw) }
            .getOrNull()
            ?.takeIf { it.format == CACHE_FORMAT && it.releases.isNotEmpty() }
    }

    private fun persist(cache: WhatsNewCacheEnvelope) {
        runCatching { AppUpdaterPlatform.setWhatsNewCache(json.encodeToString(cache)) }
    }

    private fun ChannelReleaseNote.toCached() = CachedRelease(
        tag = tag,
        title = title,
        notes = notes,
        releaseUrl = releaseUrl,
        publishedAt = publishedAt,
    )

    private fun isTrustedReleaseUrl(url: String): Boolean {
        val normalized = url.trim()
        return normalized.startsWith("https://github.com/albertovinaroz/NuvioPro/", ignoreCase = true) &&
            !normalized.any(Char::isWhitespace) &&
            !normalized.any(Char::isISOControl)
    }
}
