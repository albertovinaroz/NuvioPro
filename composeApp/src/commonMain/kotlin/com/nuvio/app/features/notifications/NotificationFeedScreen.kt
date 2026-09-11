package com.nuvio.app.features.notifications

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.nuvio.app.core.format.formatReleaseDateForDisplay
import com.nuvio.app.core.ui.NuvioScreen
import com.nuvio.app.core.ui.NuvioScreenHeader
import com.nuvio.app.features.home.components.HomeEmptyStateCard
import nuvio.composeapp.generated.resources.Res
import nuvio.composeapp.generated.resources.action_remove
import nuvio.composeapp.generated.resources.app_icon_original
import nuvio.composeapp.generated.resources.notifications_feed_clear_all
import nuvio.composeapp.generated.resources.notifications_feed_empty_description
import nuvio.composeapp.generated.resources.notifications_feed_empty_title
import nuvio.composeapp.generated.resources.notifications_feed_mark_all_read
import nuvio.composeapp.generated.resources.notifications_feed_title
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

@Composable
fun NotificationFeedScreen(
    onBack: () -> Unit,
    onItemClick: (NotificationFeedItem) -> Unit,
) {
    val uiState by remember {
        NotificationFeedRepository.ensureLoaded()
        NotificationFeedRepository.uiState
    }.collectAsStateWithLifecycle()

    NuvioScreen(modifier = Modifier.fillMaxSize()) {
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
            items(uiState.items, key = NotificationFeedItem::id) { feedItem ->
                NotificationFeedRow(
                    item = feedItem,
                    onClick = {
                        NotificationFeedRepository.markRead(feedItem.id)
                        onItemClick(feedItem)
                    },
                    onRemoveClick = { NotificationFeedRepository.remove(feedItem.id) },
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NotificationFeedRow(
    item: NotificationFeedItem,
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
        NotificationFeedRowContent(item = item, onClick = onClick)
    }
}

@Composable
private fun NotificationFeedRowContent(
    item: NotificationFeedItem,
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
                text = formatReleaseDateForDisplay(item.releaseDateIso),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        if (!item.isRead) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary),
            )
        }
    }
}
