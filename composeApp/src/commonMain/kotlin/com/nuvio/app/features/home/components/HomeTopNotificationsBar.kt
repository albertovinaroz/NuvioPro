package com.nuvio.app.features.home.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CloudDownload
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nuvio.app.features.notifications.NotificationFeedRepository
import org.jetbrains.compose.resources.stringResource
import nuvio.composeapp.generated.resources.*

/** Height of the zone [HomeHeroSection] keeps its own mute button clear of, so the two never sit on
 * top of each other — kept independent of the bar's own (shorter) visible height below so tightening
 * the bar's look doesn't creep the mute button back up into it. */
internal val HOME_NOTIFICATIONS_BAR_HEIGHT = 56.dp

/** The bar's own dark scrim only needs to reach just past the icons it holds, not the full mute
 * clearance above. */
private val HOME_NOTIFICATIONS_BAR_VISIBLE_HEIGHT = 46.dp

private const val BAR_BACKGROUND_FADE_DISTANCE_DP = 120f

/**
 * A persistent bar over the top of Home — unlike everything else here, it lives outside the
 * scrolling list (see HomeScreen) so it stays put as the hero scrolls away underneath it, the same
 * way HBO Max's own top bar does. Its background starts fully transparent (the hero's own top
 * scrim/gradient already gives the icon contrast while unscrolled) and fades to a translucent dark
 * bar once scrolled past that, so the icon stays legible over plain row content too.
 */
@Composable
internal fun HomeTopNotificationsBar(
    listState: LazyListState,
    isTablet: Boolean,
    notificationsIconEnabled: Boolean,
    downloadsIconEnabled: Boolean,
    onNotificationsClick: (() -> Unit)?,
    onDownloadsClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    if (!notificationsIconEnabled && !downloadsIconEnabled) return

    val density = LocalDensity.current
    val fadeDistancePx = with(density) { BAR_BACKGROUND_FADE_DISTANCE_DP.dp.toPx() }
    val backgroundVisibility by remember(listState) {
        derivedStateOf {
            if (listState.firstVisibleItemIndex > 0) {
                1f
            } else {
                (listState.firstVisibleItemScrollOffset / fadeDistancePx).coerceIn(0f, 1f)
            }
        }
    }
    val animatedBackgroundVisibility by animateFloatAsState(
        targetValue = backgroundVisibility,
        label = "home_notifications_bar_background",
    )
    // A fraction of the safe-area inset, not the full amount: at full statusBarTopPadding this sat
    // noticeably lower than the equivalent native (SwiftUI) button on the same screen, which only
    // uses a small flat padding — Home's Compose canvas appears to already absorb part of that
    // inset itself in the native-navigation embedding, so adding the whole thing double-counted it.
    val topInset = WindowInsets.statusBars.asPaddingValues().calculateTopPadding() * 0.8f
    val notificationFeedUiState by remember {
        NotificationFeedRepository.ensureLoaded()
        NotificationFeedRepository.uiState
    }.collectAsStateWithLifecycle()

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(topInset + HOME_NOTIFICATIONS_BAR_VISIBLE_HEIGHT),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { alpha = animatedBackgroundVisibility }
                .background(Color.Black.copy(alpha = 0.88f)),
        )
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    top = topInset,
                    end = if (isTablet) 32.dp else 18.dp,
                ),
            horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.End),
            verticalAlignment = Alignment.Top,
        ) {
            if (downloadsIconEnabled) {
                TopBarIconButton(
                    icon = Icons.Rounded.CloudDownload,
                    contentDescription = stringResource(Res.string.compose_settings_root_downloads_title),
                    onClick = { onDownloadsClick?.invoke() },
                )
            }
            if (notificationsIconEnabled) {
                NotificationsBellButton(
                    unreadCount = notificationFeedUiState.unreadCount,
                    onClick = { onNotificationsClick?.invoke() },
                )
            }
        }
    }
}

@Composable
private fun TopBarIconButton(
    icon: ImageVector,
    contentDescription: String?,
    onClick: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val pressScale by animateFloatAsState(
        targetValue = if (isPressed) 0.88f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "home_top_bar_icon_press_scale",
    )

    Box(
        modifier = Modifier
            .graphicsLayer {
                scaleX = pressScale
                scaleY = pressScale
            }
            .size(40.dp)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = Color.White,
            modifier = Modifier.size(22.dp),
        )
    }
}

@Composable
private fun NotificationsBellButton(
    unreadCount: Int,
    onClick: () -> Unit,
) {
    Box {
        TopBarIconButton(
            icon = Icons.Rounded.Notifications,
            contentDescription = stringResource(Res.string.notifications_feed_title),
            onClick = onClick,
        )
        if (unreadCount > 0) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 6.dp, end = 6.dp)
                    .size(9.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.error),
            )
        }
    }
}
