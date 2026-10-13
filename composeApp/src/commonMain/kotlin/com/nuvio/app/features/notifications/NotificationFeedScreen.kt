package com.nuvio.app.features.notifications

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.People
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.nuvio.app.core.format.formatReleaseDateForDisplay
import com.nuvio.app.core.ui.NuvioScreen
import com.nuvio.app.core.ui.NuvioPullRefreshIndicator
import com.nuvio.app.core.ui.NuvioScreenHeader
import com.nuvio.app.core.ui.nuvioPullToRefresh
import com.nuvio.app.core.ui.rememberNuvioPullRefreshController
import com.nuvio.app.features.social.SocialRepository
import com.nuvio.app.core.ui.StaggeredEntrance
import com.nuvio.app.core.ui.nuvio
import com.nuvio.app.features.home.components.HomeEmptyStateCard
import com.nuvio.app.features.settings.NotificationsSettingsScreen
import com.nuvio.app.features.social.SocialNoticeContentType
import com.nuvio.app.features.social.mergedSocialReplyBody
import com.nuvio.app.features.social.socialReplyGroupKey
import nuvio.composeapp.generated.resources.Res
import nuvio.composeapp.generated.resources.action_remove
import nuvio.composeapp.generated.resources.app_icon_original
import nuvio.composeapp.generated.resources.compose_settings_page_notifications
import nuvio.composeapp.generated.resources.notifications_feed_clear_all
import nuvio.composeapp.generated.resources.notifications_feed_empty_description
import nuvio.composeapp.generated.resources.notifications_feed_empty_title
import nuvio.composeapp.generated.resources.notifications_feed_mark_all_read
import nuvio.composeapp.generated.resources.notifications_feed_title
import org.jetbrains.compose.resources.painterResource
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.stringResource
import kotlin.time.TimeSource

@Composable
fun NotificationFeedScreen(
    onBack: () -> Unit,
    onItemClick: (NotificationFeedItem) -> Unit,
    onOpenSettings: (() -> Unit)? = null,
) {
    val uiState by remember {
        NotificationFeedRepository.ensureLoaded()
        NotificationFeedRepository.uiState
    }.collectAsStateWithLifecycle()
    var showSettings by rememberSaveable { mutableStateOf(false) }

    if (showSettings) {
        NotificationsSettingsScreen(
            onBack = { showSettings = false },
        )
        return
    }

    val tokens = MaterialTheme.nuvio
    val entries = remember(uiState.items) { uiState.items.toFeedEntries() }
    val entrance = remember { TimeSource.Monotonic.markNow() }
    val pullRefresh = rememberNuvioPullRefreshController {
        EpisodeReleaseNotificationsRepository.refreshAsync()
        SocialRepository.pollNotifications()
    }

    Box(modifier = Modifier.fillMaxSize()) {
    NuvioScreen(modifier = Modifier.fillMaxSize().nuvioPullToRefresh(pullRefresh)) {
        stickyHeader {
            NuvioScreenHeader(
                title = stringResource(Res.string.notifications_feed_title),
                onBack = onBack,
                actions = {
                    if (uiState.unreadCount > 0) {
                        TextButton(onClick = NotificationFeedRepository::markAllRead) {
                            Text(stringResource(Res.string.notifications_feed_mark_all_read))
                        }
                    }
                    if (uiState.items.isNotEmpty()) {
                        TextButton(onClick = NotificationFeedRepository::clearAll) {
                            Text(stringResource(Res.string.notifications_feed_clear_all))
                        }
                    }
                    IconButton(onClick = { onOpenSettings?.invoke() ?: run { showSettings = true } }) {
                        Icon(
                            imageVector = Icons.Rounded.Settings,
                            contentDescription = stringResource(Res.string.compose_settings_page_notifications),
                            tint = tokens.colors.textPrimary,
                        )
                    }
                },
            )
        }

        if (uiState.items.isEmpty()) {
            item {
                HomeEmptyStateCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 24.dp),
                    title = stringResource(Res.string.notifications_feed_empty_title),
                    message = stringResource(Res.string.notifications_feed_empty_description),
                )
            }
        } else {
            itemsIndexed(entries, key = { _, entry -> entry.ids.first() }) { index, entry ->
                // A removed row fades out and the ones below slide up into its place.
                Box(modifier = Modifier.animateItem()) {
                StaggeredEntrance(batch = entrance, index = index) {
                NotificationFeedRow(
                    item = entry.item,
                    count = entry.ids.size,
                    onClick = {
                        entry.ids.forEach(NotificationFeedRepository::markRead)
                        onItemClick(entry.item)
                    },
                    onRemoveClick = { entry.ids.forEach(NotificationFeedRepository::remove) },
                )
                }
                }
            }
        }
    }
    NuvioPullRefreshIndicator(
        controller = pullRefresh,
        modifier = Modifier
            .align(Alignment.TopCenter)
            .padding(top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + 8.dp),
    )
    }
}

/** One feed row: a single item, or several repeated replies collapsed into the newest one. */
private data class NotificationFeedEntry(
    val item: NotificationFeedItem,
    val ids: List<String>,
)

private fun List<NotificationFeedItem>.toFeedEntries(): List<NotificationFeedEntry> {
    val groups = LinkedHashMap<String, MutableList<NotificationFeedItem>>()
    forEach { item ->
        groups.getOrPut(item.socialReplyGroupKey() ?: "item:${item.id}") { mutableListOf() } += item
    }
    return groups.values.map { group ->
        val newest = group.first()
        val item = if (group.size == 1) {
            newest
        } else {
            // Replies recorded before posters were attached only carry the avatar.
            val artwork = group.firstOrNull { it.avatarUrl != null } ?: newest
            newest.copy(
                body = mergedSocialReplyBody(group),
                backdropUrl = artwork.backdropUrl,
                avatarUrl = artwork.avatarUrl,
                isRead = group.all(NotificationFeedItem::isRead),
            )
        }
        NotificationFeedEntry(item = item, ids = group.map(NotificationFeedItem::id))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NotificationFeedRow(
    item: NotificationFeedItem,
    count: Int,
    onClick: () -> Unit,
    onRemoveClick: () -> Unit,
) {
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            if (value == SwipeToDismissBoxValue.EndToStart) {
                onRemoveClick()
            }
            true
        },
    )

    SwipeToDismissBox(
        state = dismissState,
        enableDismissFromStartToEnd = false,
        enableDismissFromEndToStart = true,
        backgroundContent = {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.error)
                    .padding(end = 24.dp),
                contentAlignment = Alignment.CenterEnd,
            ) {
                Icon(
                    imageVector = Icons.Rounded.Close,
                    contentDescription = stringResource(Res.string.action_remove),
                    tint = MaterialTheme.colorScheme.onError,
                )
            }
        },
    ) {
        NotificationFeedRowContent(item = item, count = count, onClick = onClick)
    }
}

@Composable
private fun NotificationFeedRowContent(
    item: NotificationFeedItem,
    count: Int,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background)
            .clickable(onClick = onClick)
            .padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(width = 96.dp, height = 54.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(
                    if (item.contentType == "app_update") {
                        Color.Black
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant
                    },
                ),
            contentAlignment = Alignment.Center,
        ) {
            if (item.contentType == "app_update") {
                Image(
                    painter = painterResource(Res.drawable.app_icon_original),
                    contentDescription = null,
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(9.dp)),
                    contentScale = ContentScale.Fit,
                )
            } else if (item.avatarUrl != null && item.backdropUrl != null) {
                PosterWithAvatar(posterUrl = item.backdropUrl, avatarUrl = item.avatarUrl)
            } else if (item.contentType == SocialNoticeContentType) {
                // Friend notices show the friend's avatar, round, instead of a cropped backdrop.
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)),
                    contentAlignment = Alignment.Center,
                ) {
                    val avatar = item.backdropUrl
                    if (avatar != null) {
                        AsyncImage(
                            model = avatar,
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop,
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Rounded.People,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
            } else {
                item.backdropUrl?.let { url ->
                    AsyncImage(
                        model = url,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                    )
                }
            }
        }

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = item.title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = if (item.isRead) FontWeight.Normal else FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = item.body,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = formatReleaseDateForDisplay(item.releaseDateIso).let { date ->
                    if (count > 1) "$date · ×$count" else date
                },
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        if (!item.isRead) {
            UnreadDot()
        }
    }
}

/** A title's poster over a blurred fill of itself, with the friend's avatar on its corner. */
@Composable
private fun PosterWithAvatar(posterUrl: String, avatarUrl: String) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        AsyncImage(
            model = posterUrl,
            contentDescription = null,
            modifier = Modifier
                .fillMaxSize()
                .blur(16.dp),
            contentScale = ContentScale.Crop,
            alpha = 0.5f,
        )
        AsyncImage(
            model = posterUrl,
            contentDescription = null,
            modifier = Modifier
                .size(width = 36.dp, height = 54.dp)
                .clip(RoundedCornerShape(4.dp)),
            contentScale = ContentScale.Crop,
        )
        AsyncImage(
            model = avatarUrl,
            contentDescription = null,
            modifier = Modifier
                .offset(x = 22.dp, y = 12.dp)
                .size(24.dp)
                .clip(CircleShape)
                .border(1.5.dp, MaterialTheme.colorScheme.background, CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentScale = ContentScale.Crop,
        )
    }
}

/** The unread dot, sending out one soft ring shortly after it appears to draw the eye. */
@Composable
private fun UnreadDot() {
    val color = MaterialTheme.colorScheme.primary
    val ring = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        delay(350)
        ring.animateTo(1f, tween(durationMillis = 800, easing = FastOutSlowInEasing))
    }
    Box(
        modifier = Modifier
            .size(8.dp)
            .drawBehind {
                val p = ring.value
                if (p in 0.001f..0.999f) {
                    drawCircle(
                        color = color.copy(alpha = 0.55f * (1f - p)),
                        radius = size.minDimension / 2f * (1f + 1.6f * p),
                    )
                }
            }
            .clip(CircleShape)
            .background(color),
    )
}

