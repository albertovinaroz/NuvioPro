package com.nuvio.app.features.journal

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.Cake
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.EventRepeat
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Movie
import androidx.compose.material.icons.rounded.TaskAlt
import androidx.compose.material.icons.rounded.Tv
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nuvio.app.core.i18n.localizedMonthName
import com.nuvio.app.core.i18n.localizedShortMonthName
import com.nuvio.app.core.ui.NuvioAsyncImage
import com.nuvio.app.core.ui.NuvioModalBottomSheet
import com.nuvio.app.core.ui.NuvioSectionLabel
import com.nuvio.app.core.ui.nuvio
import com.nuvio.app.features.home.MetaPreview
import com.nuvio.app.features.settings.profileCachedArtwork
import com.nuvio.app.features.settings.profileFetchPosterMetadata
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.toLocalDateTime
import nuvio.composeapp.generated.resources.Res
import nuvio.composeapp.generated.resources.journal_achievement_anniversary
import nuvio.composeapp.generated.resources.journal_achievement_anniversary_goal
import nuvio.composeapp.generated.resources.journal_achievement_binge
import nuvio.composeapp.generated.resources.journal_achievement_binge_goal
import nuvio.composeapp.generated.resources.journal_achievement_completed_series
import nuvio.composeapp.generated.resources.journal_achievement_completed_series_goal
import nuvio.composeapp.generated.resources.journal_achievement_episodes
import nuvio.composeapp.generated.resources.journal_achievement_episodes_goal
import nuvio.composeapp.generated.resources.journal_achievement_level
import nuvio.composeapp.generated.resources.journal_achievement_level_of
import nuvio.composeapp.generated.resources.journal_achievement_levels_header
import nuvio.composeapp.generated.resources.journal_achievement_locked
import nuvio.composeapp.generated.resources.journal_achievement_maxed
import nuvio.composeapp.generated.resources.journal_achievement_movies
import nuvio.composeapp.generated.resources.journal_achievement_movies_goal
import nuvio.composeapp.generated.resources.journal_achievement_next
import nuvio.composeapp.generated.resources.journal_achievement_night_owl
import nuvio.composeapp.generated.resources.journal_achievement_night_owl_goal
import nuvio.composeapp.generated.resources.journal_achievement_progress
import nuvio.composeapp.generated.resources.journal_achievement_progress_maxed
import nuvio.composeapp.generated.resources.journal_achievement_streak
import nuvio.composeapp.generated.resources.journal_achievement_streak_goal
import nuvio.composeapp.generated.resources.journal_achievements_unlocked
import nuvio.composeapp.generated.resources.journal_diary_day
import nuvio.composeapp.generated.resources.journal_diary_day_year
import nuvio.composeapp.generated.resources.journal_diary_episode
import nuvio.composeapp.generated.resources.journal_diary_episode_range
import nuvio.composeapp.generated.resources.journal_diary_episodes
import nuvio.composeapp.generated.resources.journal_diary_month
import nuvio.composeapp.generated.resources.journal_diary_month_movies
import nuvio.composeapp.generated.resources.journal_diary_movie
import nuvio.composeapp.generated.resources.journal_diary_see_all
import nuvio.composeapp.generated.resources.journal_diary_summary
import nuvio.composeapp.generated.resources.journal_diary_titles
import nuvio.composeapp.generated.resources.journal_diary_series
import nuvio.composeapp.generated.resources.journal_diary_title
import nuvio.composeapp.generated.resources.journal_diary_today
import nuvio.composeapp.generated.resources.journal_diary_yesterday
import org.jetbrains.compose.resources.PluralStringResource
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import kotlin.time.Clock
import kotlin.time.Instant

/** The narrowest an achievement tile gets before the grid drops a column. */
private val AchievementTileMinWidth = 76.dp
private val AchievementTileGap = 8.dp

/** How many of the latest diary days the profile page shows inline; the rest live in the sheet. */
private const val DiaryRecentDayCount = 3

// ---------------------------------------------------------------------------------------------
// Achievements
// ---------------------------------------------------------------------------------------------

/** The best few unlocked badges in miniature, overlapping: the collapsed Achievements preview. */
@Composable
internal fun AchievementsPreview(achievements: List<Achievement>) {
    val tokens = MaterialTheme.nuvio
    val shown = achievements
        .filter { it.isUnlocked }
        .sortedByDescending { it.level.toFloat() / it.tiers.size }
        .take(4)
    if (shown.isEmpty()) return
    // Side by side rather than stacked: overlapping hid most of each icon.
    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        shown.forEach { achievement ->
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(if (achievement.isMaxed) tokens.colors.accent else tokens.colors.accent.copy(alpha = 0.22f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = achievement.kind.icon,
                    contentDescription = null,
                    tint = if (achievement.isMaxed) tokens.colors.onAccent else tokens.colors.accent,
                    modifier = Modifier.size(14.dp),
                )
            }
        }
    }
}

/** "5 of 7 unlocked": the Achievements section's collapsed summary. */
@Composable
internal fun achievementsSummary(achievements: List<Achievement>): String =
    stringResource(
        Res.string.journal_achievements_unlocked,
        achievements.count { it.isUnlocked },
        achievements.size,
    )

@Composable
internal fun ProfileAchievementsContent(
    achievements: List<Achievement>,
    isTablet: Boolean,
) {
    var selected by remember { mutableStateOf<AchievementKind?>(null) }
    // Columns follow the width actually available, not the device class: landscape phones use
    // the tablet layout but show this inside a narrow detail pane.
    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val columns = ((maxWidth + AchievementTileGap) / (AchievementTileMinWidth + AchievementTileGap)).toInt()
            .coerceIn(3, achievements.size.coerceAtLeast(3))
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            achievements.chunked(columns).forEach { row ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(AchievementTileGap),
                ) {
                    row.forEach { achievement ->
                        AchievementTile(
                            achievement = achievement,
                            onClick = { selected = achievement.kind },
                            modifier = Modifier.weight(1f),
                        )
                    }
                    repeat(columns - row.size) { Spacer(modifier = Modifier.weight(1f)) }
                }
            }
        }
    }

    // Looked up by kind so the sheet follows live progress instead of a stale snapshot.
    selected
        ?.let { kind -> achievements.firstOrNull { it.kind == kind } }
        ?.let { achievement ->
            AchievementDetailSheet(
                achievement = achievement,
                isTablet = isTablet,
                onDismiss = { selected = null },
            )
        }
}

@Composable
private fun AchievementTile(
    achievement: Achievement,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val tokens = MaterialTheme.nuvio
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(7.dp),
    ) {
        AchievementBadge(achievement = achievement, size = 58.dp)
        Text(
            text = stringResource(achievement.kind.titleRes),
            // Shrinks a long name ("Episode Hunter") to fit its column instead of cutting it off.
            autoSize = TextAutoSize.StepBased(
                minFontSize = 9.sp,
                maxFontSize = MaterialTheme.typography.labelMedium.fontSize,
            ),
            style = MaterialTheme.typography.labelMedium,
            color = if (achievement.isUnlocked) tokens.colors.textPrimary else tokens.colors.textMuted,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
            maxLines = 1,
        )
        Text(
            text = if (achievement.isMaxed) {
                stringResource(Res.string.journal_achievement_progress_maxed, achievement.current)
            } else {
                stringResource(Res.string.journal_achievement_progress, achievement.current, achievement.target)
            },
            style = MaterialTheme.typography.labelSmall,
            color = tokens.colors.textMuted,
            maxLines = 1,
        )
    }
}

/**
 * A round badge whose ring fills towards the next level. Locked badges stay muted; each level
 * deepens the accent wash, and a maxed badge turns solid.
 */
@Composable
private fun AchievementBadge(
    achievement: Achievement,
    size: Dp,
) {
    val tokens = MaterialTheme.nuvio
    val accent = tokens.colors.accent
    val track = tokens.colors.borderSubtle
    val levelShare = achievement.level.toFloat() / achievement.tiers.size
    val fill = when {
        achievement.isMaxed -> accent
        achievement.isUnlocked -> accent.copy(alpha = 0.10f + 0.22f * levelShare)
        else -> tokens.colors.textMuted.copy(alpha = 0.08f)
    }
    val iconTint = when {
        achievement.isMaxed -> tokens.colors.onAccent
        achievement.isUnlocked -> accent
        else -> tokens.colors.textMuted.copy(alpha = 0.6f)
    }
    val ringWidth = size * 0.06f
    Box(
        modifier = Modifier
            .size(size)
            .drawBehind {
                val stroke = ringWidth.toPx()
                val inset = stroke / 2f
                val arcSize = Size(this.size.width - stroke, this.size.height - stroke)
                drawArc(
                    color = track,
                    startAngle = 0f,
                    sweepAngle = 360f,
                    useCenter = false,
                    topLeft = Offset(inset, inset),
                    size = arcSize,
                    style = Stroke(width = stroke),
                )
                if (achievement.progress > 0f) {
                    drawArc(
                        color = accent,
                        startAngle = -90f,
                        sweepAngle = 360f * achievement.progress,
                        useCenter = false,
                        topLeft = Offset(inset, inset),
                        size = arcSize,
                        style = Stroke(width = stroke, cap = StrokeCap.Round),
                    )
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size(size * 0.78f)
                .clip(CircleShape)
                .background(fill),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = achievement.kind.icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(size * 0.38f),
            )
        }
        if (achievement.isUnlocked) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .offset(x = size * 0.04f, y = size * 0.04f)
                    .size(size * 0.36f)
                    .clip(CircleShape)
                    .background(tokens.colors.surface)
                    .padding(2.dp)
                    .clip(CircleShape)
                    .background(accent),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = achievement.level.toString(),
                    style = if (size > 64.dp) MaterialTheme.typography.labelLarge else MaterialTheme.typography.labelSmall,
                    color = tokens.colors.onAccent,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AchievementDetailSheet(
    achievement: Achievement,
    isTablet: Boolean,
    onDismiss: () -> Unit,
) {
    val tokens = MaterialTheme.nuvio
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    NuvioModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = if (isTablet) 24.dp else 18.dp)
                .padding(top = 8.dp, bottom = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            AchievementBadge(achievement = achievement, size = 104.dp)
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = stringResource(achievement.kind.titleRes),
                    style = MaterialTheme.typography.titleLarge,
                    color = tokens.colors.textPrimary,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = when {
                        achievement.isMaxed -> stringResource(Res.string.journal_achievement_maxed)
                        achievement.isUnlocked -> stringResource(
                            Res.string.journal_achievement_level_of,
                            achievement.level,
                            achievement.tiers.size,
                        )
                        else -> stringResource(Res.string.journal_achievement_locked)
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = tokens.colors.textMuted,
                )
            }
            if (!achievement.isMaxed) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            text = stringResource(
                                Res.string.journal_achievement_next,
                                pluralStringResource(achievement.kind.goalRes, achievement.target, achievement.target),
                            ),
                            style = MaterialTheme.typography.labelLarge,
                            color = tokens.colors.textPrimary,
                            modifier = Modifier.weight(1f, fill = false),
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = stringResource(
                                Res.string.journal_achievement_progress,
                                achievement.current,
                                achievement.target,
                            ),
                            style = MaterialTheme.typography.labelLarge,
                            color = tokens.colors.textMuted,
                        )
                    }
                    AchievementProgressBar(progress = achievement.progress)
                }
            }
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                NuvioSectionLabel(
                    text = stringResource(Res.string.journal_achievement_levels_header),
                    modifier = Modifier.padding(bottom = 6.dp),
                )
                achievement.tiers.forEachIndexed { index, tier ->
                    val reached = achievement.current >= tier
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Icon(
                            imageVector = if (reached) Icons.Rounded.CheckCircle else Icons.Rounded.Lock,
                            contentDescription = null,
                            tint = if (reached) tokens.colors.accent else tokens.colors.textMuted.copy(alpha = 0.6f),
                            modifier = Modifier.size(20.dp),
                        )
                        Text(
                            text = stringResource(Res.string.journal_achievement_level, index + 1),
                            style = MaterialTheme.typography.labelLarge,
                            color = if (reached) tokens.colors.textPrimary else tokens.colors.textMuted,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.width(64.dp),
                        )
                        Text(
                            text = pluralStringResource(achievement.kind.goalRes, tier, tier),
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (reached) tokens.colors.textPrimary else tokens.colors.textMuted,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AchievementProgressBar(progress: Float) {
    val tokens = MaterialTheme.nuvio
    val accent = tokens.colors.accent
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(6.dp)
            .clip(RoundedCornerShape(999.dp))
            .background(tokens.colors.borderSubtle)
            .drawBehind {
                drawRect(color = accent, size = Size(size.width * progress, size.height))
            },
    )
}

private val AchievementKind.titleRes: StringResource
    get() = when (this) {
        AchievementKind.Movies -> Res.string.journal_achievement_movies
        AchievementKind.Episodes -> Res.string.journal_achievement_episodes
        AchievementKind.CompletedSeries -> Res.string.journal_achievement_completed_series
        AchievementKind.Binge -> Res.string.journal_achievement_binge
        AchievementKind.Streak -> Res.string.journal_achievement_streak
        AchievementKind.NightOwl -> Res.string.journal_achievement_night_owl
        AchievementKind.Anniversary -> Res.string.journal_achievement_anniversary
    }

private val AchievementKind.goalRes: PluralStringResource
    get() = when (this) {
        AchievementKind.Movies -> Res.plurals.journal_achievement_movies_goal
        AchievementKind.Episodes -> Res.plurals.journal_achievement_episodes_goal
        AchievementKind.CompletedSeries -> Res.plurals.journal_achievement_completed_series_goal
        AchievementKind.Binge -> Res.plurals.journal_achievement_binge_goal
        AchievementKind.Streak -> Res.plurals.journal_achievement_streak_goal
        AchievementKind.NightOwl -> Res.plurals.journal_achievement_night_owl_goal
        AchievementKind.Anniversary -> Res.plurals.journal_achievement_anniversary_goal
    }

private val AchievementKind.icon: ImageVector
    get() = when (this) {
        AchievementKind.Movies -> Icons.Rounded.Movie
        AchievementKind.Episodes -> Icons.Rounded.Tv
        AchievementKind.CompletedSeries -> Icons.Rounded.TaskAlt
        AchievementKind.Binge -> Icons.Rounded.LocalFireDepartment
        AchievementKind.Streak -> Icons.Rounded.EventRepeat
        AchievementKind.NightOwl -> Icons.Rounded.Bedtime
        AchievementKind.Anniversary -> Icons.Rounded.Cake
    }

// ---------------------------------------------------------------------------------------------
// Diary
// ---------------------------------------------------------------------------------------------

/** The last few titles watched as small overlapping posters: the collapsed Diary preview. */
@Composable
internal fun DiaryPreview(diary: List<DiaryMonth>) {
    val tokens = MaterialTheme.nuvio
    val entries = remember(diary) {
        diary.asSequence().flatMap { it.days }.flatMap { it.entries }.take(3).toList()
    }
    if (entries.isEmpty()) return
    Box {
        entries.forEachIndexed { index, entry ->
            val artwork = rememberDiaryArtwork(entry)
            Box(
                modifier = Modifier
                    .padding(start = (index * 14).dp)
                    .clip(RoundedCornerShape(5.dp))
                    .background(tokens.colors.surface)
                    .padding(1.5.dp),
            ) {
                DiaryPoster(
                    entry = entry,
                    artwork = artwork,
                    modifier = Modifier
                        .width(22.dp)
                        .aspectRatio(2f / 3f),
                    cornerRadius = 4.dp,
                )
            }
        }
    }
}

/** "Today · 3 titles": the latest diary day, as the Diary section's collapsed summary. */
@Composable
internal fun diarySummary(diary: List<DiaryMonth>): String? {
    val latest = diary.firstOrNull()?.days?.firstOrNull() ?: return null
    val today = remember { Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date }
    return stringResource(
        Res.string.journal_diary_summary,
        diaryDayLabel(latest.date, today),
        pluralStringResource(Res.plurals.journal_diary_titles, latest.entries.size, latest.entries.size),
    )
}

@Composable
internal fun ProfileDiaryContent(
    diary: List<DiaryMonth>,
    isTablet: Boolean,
    onPosterClick: ((MetaPreview) -> Unit)?,
) {
    val tokens = MaterialTheme.nuvio
    var showFullDiary by remember { mutableStateOf(false) }
    val today = remember { Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date }
    val recentDays = remember(diary) { diary.asSequence().flatMap { it.days }.take(DiaryRecentDayCount).toList() }
    val totalDays = remember(diary) { diary.sumOf { it.days.size } }
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        recentDays.forEach { day ->
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = diaryDayLabel(day.date, today),
                    style = MaterialTheme.typography.labelLarge,
                    color = tokens.colors.textPrimary,
                    fontWeight = FontWeight.SemiBold,
                )
                LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(items = day.entries, key = { entry -> "${entry.type}:${entry.id}" }) { entry ->
                        DiaryPosterTile(
                            entry = entry,
                            width = if (isTablet) 112.dp else 92.dp,
                            onClick = onPosterClick,
                        )
                    }
                }
            }
        }
        if (totalDays > recentDays.size) {
            Text(
                text = stringResource(Res.string.journal_diary_see_all),
                style = MaterialTheme.typography.labelLarge,
                color = tokens.colors.accent,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(tokens.colors.accent.copy(alpha = 0.10f))
                    .clickable { showFullDiary = true }
                    .padding(vertical = 11.dp),
            )
        }
    }

    if (showFullDiary) {
        DiarySheet(
            diary = diary,
            today = today,
            isTablet = isTablet,
            onDismiss = { showFullDiary = false },
            onPosterClick = onPosterClick,
        )
    }
}

@Composable
private fun DiaryPosterTile(
    entry: DiaryEntry,
    width: Dp,
    onClick: ((MetaPreview) -> Unit)?,
) {
    val tokens = MaterialTheme.nuvio
    val artwork = rememberDiaryArtwork(entry)
    Column(
        modifier = Modifier
            .width(width)
            .diaryClickable(entry, artwork, onClick),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        DiaryPoster(
            entry = entry,
            artwork = artwork,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(2f / 3f),
            cornerRadius = 12.dp,
        )
        Text(
            text = entry.title,
            style = MaterialTheme.typography.labelMedium,
            color = tokens.colors.textPrimary,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = diaryEntryDetail(entry),
            style = MaterialTheme.typography.labelSmall,
            color = tokens.colors.textMuted,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DiarySheet(
    diary: List<DiaryMonth>,
    today: LocalDate,
    isTablet: Boolean,
    onDismiss: () -> Unit,
    onPosterClick: ((MetaPreview) -> Unit)?,
) {
    val tokens = MaterialTheme.nuvio
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val timeZone = remember { TimeZone.currentSystemDefault() }
    val onEntryClick = onPosterClick?.let { callback ->
        { preview: MetaPreview ->
            onDismiss()
            callback(preview)
        }
    }
    NuvioModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = if (isTablet) 24.dp else 18.dp)
                .padding(bottom = 22.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(
                text = stringResource(Res.string.journal_diary_title),
                style = MaterialTheme.typography.titleLarge,
                color = tokens.colors.textPrimary,
                fontWeight = FontWeight.Bold,
            )
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = if (isTablet) 680.dp else 560.dp),
            ) {
                diary.forEach { month ->
                    item(key = "month:${month.year}-${month.month}") {
                        DiaryMonthHeader(month = month)
                    }
                    month.days.forEach { day ->
                        item(key = "day:${day.date}") {
                            Text(
                                text = diaryDayLabel(day.date, today),
                                style = MaterialTheme.typography.labelMedium,
                                color = tokens.colors.textMuted,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(top = 10.dp, bottom = 4.dp),
                            )
                        }
                        items(items = day.entries, key = { entry -> "entry:${day.date}:${entry.type}:${entry.id}" }) { entry ->
                            DiaryRow(entry = entry, timeZone = timeZone, onClick = onEntryClick)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DiaryMonthHeader(month: DiaryMonth) {
    val tokens = MaterialTheme.nuvio
    val counts = buildList {
        if (month.movieCount > 0) {
            add(pluralStringResource(Res.plurals.journal_diary_month_movies, month.movieCount, month.movieCount))
        }
        if (month.episodeCount > 0) {
            add(pluralStringResource(Res.plurals.journal_diary_episodes, month.episodeCount, month.episodeCount))
        }
    }.joinToString(" · ")
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 14.dp, bottom = 2.dp),
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = stringResource(Res.string.journal_diary_month, localizedMonthName(month.month), month.year),
            style = MaterialTheme.typography.titleMedium,
            color = tokens.colors.textPrimary,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = counts,
            style = MaterialTheme.typography.labelMedium,
            color = tokens.colors.textMuted,
        )
    }
}

@Composable
private fun DiaryRow(
    entry: DiaryEntry,
    timeZone: TimeZone,
    onClick: ((MetaPreview) -> Unit)?,
) {
    val tokens = MaterialTheme.nuvio
    val artwork = rememberDiaryArtwork(entry)
    val time = remember(entry.lastMarkedAtEpochMs, timeZone) {
        val local = Instant.fromEpochMilliseconds(entry.lastMarkedAtEpochMs).toLocalDateTime(timeZone)
        "${local.hour.toString().padStart(2, '0')}:${local.minute.toString().padStart(2, '0')}"
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .diaryClickable(entry, artwork, onClick)
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        DiaryPoster(
            entry = entry,
            artwork = artwork,
            modifier = Modifier
                .width(44.dp)
                .aspectRatio(2f / 3f),
            cornerRadius = 8.dp,
        )
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = entry.title,
                style = MaterialTheme.typography.bodyLarge,
                color = tokens.colors.textPrimary,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = diaryEntryDetail(entry),
                style = MaterialTheme.typography.bodySmall,
                color = tokens.colors.textMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Text(
            text = time,
            style = MaterialTheme.typography.labelMedium,
            color = tokens.colors.textMuted,
        )
    }
}

@Composable
private fun DiaryPoster(
    entry: DiaryEntry,
    artwork: String?,
    modifier: Modifier,
    cornerRadius: Dp,
) {
    val tokens = MaterialTheme.nuvio
    val shape = RoundedCornerShape(cornerRadius)
    Box(
        modifier = modifier
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .border(1.dp, tokens.colors.borderSubtle, shape),
        contentAlignment = Alignment.Center,
    ) {
        if (artwork != null) {
            NuvioAsyncImage(
                imageUrl = artwork,
                contentDescription = entry.title,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
        } else {
            Text(
                text = entry.title.take(1).uppercase(),
                style = MaterialTheme.typography.titleLarge,
                color = tokens.colors.textMuted,
                fontWeight = FontWeight.Black,
            )
        }
    }
}

/** The stored poster, else cached metadata, else a one-off metadata fetch. */
@Composable
private fun rememberDiaryArtwork(entry: DiaryEntry): String? {
    val initial = remember(entry.type, entry.id, entry.poster) {
        entry.poster ?: profileCachedArtwork(entry.type, entry.id)
    }
    var artwork by remember(entry.type, entry.id) { mutableStateOf(initial) }
    LaunchedEffect(entry.type, entry.id, initial) {
        if (initial != null) return@LaunchedEffect
        artwork = profileFetchPosterMetadata(entry.type, entry.id).first
    }
    return artwork
}

private fun Modifier.diaryClickable(
    entry: DiaryEntry,
    artwork: String?,
    onClick: ((MetaPreview) -> Unit)?,
): Modifier =
    if (onClick == null) {
        this
    } else {
        clickable {
            onClick(MetaPreview(id = entry.id, type = entry.type, name = entry.title, poster = artwork))
        }
    }

@Composable
private fun diaryDayLabel(date: LocalDate, today: LocalDate): String = when {
    date == today -> stringResource(Res.string.journal_diary_today)
    date == today.minus(1, DateTimeUnit.DAY) -> stringResource(Res.string.journal_diary_yesterday)
    date.year == today.year ->
        stringResource(Res.string.journal_diary_day, date.day, localizedShortMonthName(date.month.ordinal + 1))
    else -> stringResource(
        Res.string.journal_diary_day_year,
        date.day,
        localizedShortMonthName(date.month.ordinal + 1),
        date.year,
    )
}

@Composable
private fun diaryEntryDetail(entry: DiaryEntry): String {
    val episodes = entry.episodes
    return when {
        episodes.isEmpty() -> stringResource(
            if (entry.isSeries) Res.string.journal_diary_series else Res.string.journal_diary_movie,
        )
        episodes.size == 1 -> stringResource(Res.string.journal_diary_episode, episodes[0].first, episodes[0].second)
        episodes.isContiguousRun() -> stringResource(
            Res.string.journal_diary_episode_range,
            episodes.first().first,
            episodes.first().second,
            episodes.last().second,
        )
        else -> pluralStringResource(Res.plurals.journal_diary_episodes, episodes.size, episodes.size)
    }
}

/** True when every episode is in one season and they follow on with no gaps (E1, E2, E3). */
private fun List<Pair<Int, Int>>.isContiguousRun(): Boolean =
    all { it.first == first().first } && zipWithNext().all { (a, b) -> b.second == a.second + 1 }
