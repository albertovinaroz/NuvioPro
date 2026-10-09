package com.nuvio.app.features.journal

import com.nuvio.app.features.watched.WatchedItem
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.daysUntil
import kotlinx.datetime.minus
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Instant

/**
 * A profile's watch diary and achievements, worked out locally from its watched marks
 * ([WatchedItem.markedAtEpochMs]) — no server involved.
 */

/** One title on one day: a movie, or a series with the episodes watched that day. */
data class DiaryEntry(
    val date: LocalDate,
    val id: String,
    val type: String,
    val title: String,
    val poster: String?,
    /** (season, episode) pairs watched that day, oldest first; empty for a movie. */
    val episodes: List<Pair<Int, Int>>,
    val lastMarkedAtEpochMs: Long,
) {
    val isSeries: Boolean get() = episodes.isNotEmpty() || type.isSeriesType()
}

data class DiaryDay(
    val date: LocalDate,
    val entries: List<DiaryEntry>,
)

data class DiaryMonth(
    val year: Int,
    /** 1..12 */
    val month: Int,
    val days: List<DiaryDay>,
) {
    val movieCount: Int get() = days.sumOf { day -> day.entries.count { !it.isSeries } }
    val episodeCount: Int get() = days.sumOf { day -> day.entries.sumOf { it.episodes.size } }
}

/** Newest month first, newest day first, latest entry of the day first. */
fun buildDiary(items: List<WatchedItem>, timeZone: TimeZone): List<DiaryMonth> {
    val dated = items.filter { it.markedAtEpochMs > 0 }
    if (dated.isEmpty()) return emptyList()

    val days = dated
        .groupBy { it.localDate(timeZone) }
        .map { (date, dayItems) ->
            val entries = dayItems
                .groupBy { item -> item.type.lowercase() to item.id }
                .map { (key, titleItems) ->
                    val latest = titleItems.maxBy { it.markedAtEpochMs }
                    DiaryEntry(
                        date = date,
                        id = key.second,
                        type = latest.type,
                        title = latest.name,
                        poster = titleItems.firstNotNullOfOrNull { it.poster?.takeIf(String::isNotBlank) },
                        episodes = titleItems
                            .mapNotNull { item ->
                                val season = item.season ?: return@mapNotNull null
                                val episode = item.episode ?: return@mapNotNull null
                                season to episode
                            }
                            .distinct()
                            .sortedWith(compareBy({ it.first }, { it.second })),
                        lastMarkedAtEpochMs = latest.markedAtEpochMs,
                    )
                }
                .sortedByDescending { it.lastMarkedAtEpochMs }
            DiaryDay(date = date, entries = entries)
        }
        .sortedByDescending { it.date }

    return days
        .groupBy { day -> day.date.year to day.date.month.ordinal + 1 }
        .map { (yearMonth, monthDays) -> DiaryMonth(yearMonth.first, yearMonth.second, monthDays) }
        .sortedWith(compareByDescending<DiaryMonth> { it.year }.thenByDescending { it.month })
}

enum class AchievementKind {
    Movies,
    Episodes,
    CompletedSeries,
    Binge,
    Streak,
    NightOwl,
    Anniversary,
}

/**
 * A tiered achievement: [current] measured against ascending [tiers]. Reaching tier N unlocks
 * level N (1-based); [level] is 0 while the first tier is still ahead.
 */
data class Achievement(
    val kind: AchievementKind,
    val tiers: List<Int>,
    val current: Int,
) {
    val level: Int get() = tiers.count { current >= it }
    val isUnlocked: Boolean get() = level > 0
    val isMaxed: Boolean get() = level == tiers.size

    /** The next tier to reach, or the top one once every tier is done. */
    val target: Int get() = tiers.getOrNull(level) ?: tiers.last()

    /** Progress from the previous tier to [target], 0..1. */
    val progress: Float
        get() {
            if (isMaxed) return 1f
            val from = tiers.getOrNull(level - 1) ?: 0
            val span = (target - from).coerceAtLeast(1)
            return ((current - from).toFloat() / span).coerceIn(0f, 1f)
        }
}

/** The thresholds per achievement, lowest first. */
val AchievementTiers: Map<AchievementKind, List<Int>> = mapOf(
    AchievementKind.Movies to listOf(1, 10, 50, 100, 250, 500),
    AchievementKind.Episodes to listOf(10, 100, 500, 1000, 2500),
    AchievementKind.CompletedSeries to listOf(1, 5, 10, 25, 50),
    AchievementKind.Binge to listOf(3, 6, 10, 15),
    AchievementKind.Streak to listOf(3, 7, 14, 30),
    AchievementKind.NightOwl to listOf(1, 10, 50),
    AchievementKind.Anniversary to listOf(30, 180, 365, 730),
)

fun buildAchievements(
    items: List<WatchedItem>,
    completedSeriesCount: Int,
    nowEpochMs: Long,
    timeZone: TimeZone,
): List<Achievement> {
    val movies = items.count { it.isMovie() }
    val episodes = items.count { it.season != null && it.episode != null }
    val dated = items.filter { it.markedAtEpochMs > 0 }

    // Most episodes of a single series marked on the same day.
    val binge = dated
        .filter { it.season != null && it.episode != null }
        .groupBy { it.id to it.localDate(timeZone) }
        .maxOfOrNull { (_, sameDay) -> sameDay.distinctBy { it.season to it.episode }.size } ?: 0

    val streak = longestDailyStreak(dated.map { it.localDate(timeZone) }.toSet())

    // Movies and episodes finished between midnight and 5 am.
    val nightOwl = dated.count { item ->
        Instant.fromEpochMilliseconds(item.markedAtEpochMs).toLocalDateTime(timeZone).hour < 5
    }

    val firstWatch = dated.minOfOrNull { it.markedAtEpochMs }
    val daysSinceFirstWatch = firstWatch
        ?.let { first ->
            Instant.fromEpochMilliseconds(first).toLocalDateTime(timeZone).date
                .daysUntil(Instant.fromEpochMilliseconds(nowEpochMs).toLocalDateTime(timeZone).date)
        }
        ?.coerceAtLeast(0)
        ?: 0

    val values = mapOf(
        AchievementKind.Movies to movies,
        AchievementKind.Episodes to episodes,
        AchievementKind.CompletedSeries to completedSeriesCount,
        AchievementKind.Binge to binge,
        AchievementKind.Streak to streak,
        AchievementKind.NightOwl to nightOwl,
        AchievementKind.Anniversary to daysSinceFirstWatch,
    )
    return AchievementKind.entries.map { kind ->
        Achievement(kind = kind, tiers = AchievementTiers.getValue(kind), current = values.getValue(kind))
    }
}

/** The longest run of consecutive calendar days in [days]. */
internal fun longestDailyStreak(days: Set<LocalDate>): Int {
    var best = 0
    for (day in days) {
        // Only count from the first day of a run.
        if (day.minus(1, DateTimeUnit.DAY) in days) continue
        var length = 1
        var next = day
        while (true) {
            next = next.plusDays(1)
            if (next !in days) break
            length++
        }
        best = maxOf(best, length)
    }
    return best
}

private fun LocalDate.plusDays(days: Int): LocalDate = minus(-days, DateTimeUnit.DAY)

private fun WatchedItem.localDate(timeZone: TimeZone): LocalDate =
    Instant.fromEpochMilliseconds(markedAtEpochMs).toLocalDateTime(timeZone).date

private val SeriesTypes = setOf("series", "tv", "show", "tvshow")

private fun String.isSeriesType(): Boolean = lowercase() in SeriesTypes

private fun WatchedItem.isMovie(): Boolean =
    !type.isSeriesType() && season == null && episode == null
