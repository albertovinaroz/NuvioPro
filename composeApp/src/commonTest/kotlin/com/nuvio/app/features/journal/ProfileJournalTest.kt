package com.nuvio.app.features.journal

import com.nuvio.app.features.watched.WatchedItem
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

private val Utc = TimeZone.UTC

private fun at(year: Int, month: Int, day: Int, hour: Int = 20, minute: Int = 0): Long =
    LocalDateTime(year, month, day, hour, minute).toInstant(Utc).toEpochMilliseconds()

private fun movie(id: String, markedAt: Long) =
    WatchedItem(id = id, type = "movie", name = "Movie $id", poster = "p-$id", markedAtEpochMs = markedAt)

private fun episode(id: String, season: Int, episode: Int, markedAt: Long) =
    WatchedItem(
        id = id,
        type = "series",
        name = "Show $id",
        season = season,
        episode = episode,
        markedAtEpochMs = markedAt,
    )

class BuildDiaryTest {

    @Test
    fun merges_episodes_of_one_series_on_one_day() {
        val diary = buildDiary(
            listOf(
                episode("tt1", 1, 2, at(2026, 3, 4, 21)),
                episode("tt1", 1, 1, at(2026, 3, 4, 20)),
                movie("tt2", at(2026, 3, 4, 23)),
            ),
            Utc,
        )
        val day = diary.single().days.single()
        assertEquals(LocalDate(2026, 3, 4), day.date)
        assertEquals(listOf("tt2", "tt1"), day.entries.map { it.id })
        assertEquals(listOf(1 to 1, 1 to 2), day.entries[1].episodes)
        assertFalse(day.entries[0].isSeries)
    }

    @Test
    fun orders_months_and_days_newest_first() {
        val diary = buildDiary(
            listOf(
                movie("a", at(2025, 12, 31)),
                movie("b", at(2026, 1, 2)),
                movie("c", at(2026, 1, 5)),
            ),
            Utc,
        )
        assertEquals(listOf(2026 to 1, 2025 to 12), diary.map { it.year to it.month })
        assertEquals(listOf(5, 2), diary[0].days.map { it.date.day })
        assertEquals(2, diary[0].movieCount)
    }

    @Test
    fun skips_items_without_a_date() {
        assertTrue(buildDiary(listOf(movie("a", 0)), Utc).isEmpty())
    }
}

class BuildAchievementsTest {

    private fun List<Achievement>.of(kind: AchievementKind) = first { it.kind == kind }

    @Test
    fun counts_binge_streak_and_night_owl() {
        val items = listOf(
            episode("s", 1, 1, at(2026, 2, 1, 1)),
            episode("s", 1, 2, at(2026, 2, 1, 2)),
            episode("s", 1, 3, at(2026, 2, 1, 22)),
            movie("m1", at(2026, 2, 2)),
            movie("m2", at(2026, 2, 3)),
            movie("m3", at(2026, 2, 10)),
        )
        val achievements = buildAchievements(items, completedSeriesCount = 1, nowEpochMs = at(2026, 3, 3), timeZone = Utc)

        assertEquals(3, achievements.of(AchievementKind.Movies).current)
        assertEquals(3, achievements.of(AchievementKind.Episodes).current)
        assertEquals(3, achievements.of(AchievementKind.Binge).current)
        assertEquals(3, achievements.of(AchievementKind.Streak).current)
        assertEquals(2, achievements.of(AchievementKind.NightOwl).current)
        assertEquals(30, achievements.of(AchievementKind.Anniversary).current)
        assertEquals(1, achievements.of(AchievementKind.CompletedSeries).level)
    }

    @Test
    fun level_target_and_progress_follow_tiers() {
        val partway = Achievement(AchievementKind.Movies, listOf(1, 10, 50), current = 30)
        assertEquals(2, partway.level)
        assertEquals(50, partway.target)
        assertEquals(0.5f, partway.progress)

        val locked = Achievement(AchievementKind.Movies, listOf(1, 10, 50), current = 0)
        assertFalse(locked.isUnlocked)
        assertEquals(1, locked.target)

        val maxed = Achievement(AchievementKind.Movies, listOf(1, 10, 50), current = 80)
        assertTrue(maxed.isMaxed)
        assertEquals(1f, maxed.progress)
    }

    @Test
    fun longest_streak_ignores_gaps() {
        val days = setOf(
            LocalDate(2026, 1, 30),
            LocalDate(2026, 1, 31),
            LocalDate(2026, 2, 1),
            LocalDate(2026, 2, 5),
        )
        assertEquals(3, longestDailyStreak(days))
        assertEquals(0, longestDailyStreak(emptySet()))
    }
}
