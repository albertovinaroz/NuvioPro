package com.nuvio.app.features.social

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.PersonAdd
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nuvio.app.core.ui.NuvioAsyncImage
import com.nuvio.app.core.ui.NuvioInputField
import com.nuvio.app.core.ui.NuvioModalBottomSheet
import com.nuvio.app.core.ui.NuvioPrimaryButton
import com.nuvio.app.core.ui.NuvioSectionLabel
import com.nuvio.app.core.ui.NuvioToastController
import com.nuvio.app.core.ui.nuvio
import com.nuvio.app.features.home.MetaPreview
import com.nuvio.app.features.notifications.NotificationFeedRepository
import com.nuvio.app.features.profiles.ProfileRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import nuvio.composeapp.generated.resources.*
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import kotlin.time.Clock

/** How often the Friends section re-fetches while it's on screen, for "watching now". */
private const val SocialLiveRefreshMs = 60_000L

/**
 * Keeps the social state fresh while the Profile page is showing: once per profile and then
 * every minute, so the collapsed summary and "watching now" stay current.
 */
@Composable
internal fun SocialAutoRefresh() {
    val profileState by ProfileRepository.state.collectAsStateWithLifecycle()
    val profileId = profileState.activeProfile?.profileIndex
    LaunchedEffect(profileId) {
        while (true) {
            SocialRepository.refresh()
            delay(SocialLiveRefreshMs)
        }
    }
}

/** "3 friends · 1 watching now", the Friends section's collapsed summary. */
@Composable
internal fun socialSummary(): String {
    val state by SocialRepository.state.collectAsStateWithLifecycle()
    if (!SocialRepository.isAvailable) return ""
    val friends = state.friends.friends
    if (friends.isEmpty() && state.friends.incoming.isEmpty()) {
        return if (state.me == null) "" else stringResource(Res.string.social_summary_empty)
    }
    return buildList {
        add(pluralStringResource(Res.plurals.social_summary_friends, friends.size, friends.size))
        val watching = friends.count { it.watchingNow != null }
        if (watching > 0) add(stringResource(Res.string.social_summary_watching, watching))
        val requests = state.friends.incoming.size
        if (requests > 0) add(pluralStringResource(Res.plurals.social_summary_requests, requests, requests))
        if (state.unseenRecommendations > 0) {
            add(stringResource(Res.string.social_recommendations_new, state.unseenRecommendations))
        }
    }.joinToString(" · ")
}

@Composable
internal fun ProfileFriendsContent(
    isTablet: Boolean,
    onPosterClick: ((MetaPreview) -> Unit)?,
) {
    val tokens = MaterialTheme.nuvio
    val state by SocialRepository.state.collectAsStateWithLifecycle()
    var showAddFriend by remember { mutableStateOf(false) }
    var showRecommendations by remember { mutableStateOf(false) }
    var openFriend by remember { mutableStateOf<SocialFriend?>(null) }
    val feedState by NotificationFeedRepository.uiState.collectAsStateWithLifecycle()
    // Seeing this section is seeing its accepted-request and reply notices.
    LaunchedEffect(feedState.unreadCount) { markSocialInfoNoticesRead() }

    if (!SocialRepository.isAvailable) {
        Text(
            text = stringResource(Res.string.social_unavailable),
            style = MaterialTheme.typography.bodyMedium,
            color = tokens.colors.textMuted,
        )
        return
    }
    val me = state.me
    if (me == null) {
        if (state.error != null) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = stringResource(Res.string.social_load_failed),
                    style = MaterialTheme.typography.bodyMedium,
                    color = tokens.colors.textMuted,
                )
                SocialPillButton(text = stringResource(Res.string.social_retry), onClick = SocialRepository::refresh)
            }
        } else {
            Text(
                text = "…",
                style = MaterialTheme.typography.bodyMedium,
                color = tokens.colors.textMuted,
            )
        }
        return
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        FriendCodeCard(code = me.friendCode, onAddFriend = { showAddFriend = true })

        RecommendationsEntryRow(
            unseen = state.unseenRecommendations,
            onClick = { showRecommendations = true },
        )

        if (state.friends.incoming.isNotEmpty()) {
            SocialGroup(title = stringResource(Res.string.social_requests_header)) {
                state.friends.incoming.forEach { request ->
                    PersonRow(person = request.person, status = null) {
                        SocialPillButton(
                            text = stringResource(Res.string.social_accept),
                            filled = true,
                            onClick = { SocialRepository.acceptFriend(request.id) },
                        )
                        SocialPillButton(
                            text = stringResource(Res.string.social_decline),
                            onClick = { SocialRepository.declineFriend(request.id) },
                        )
                    }
                }
            }
        }

        SocialGroup(title = stringResource(Res.string.social_friends_header)) {
            if (state.friends.friends.isEmpty()) {
                Text(
                    text = stringResource(Res.string.social_no_friends),
                    style = MaterialTheme.typography.bodyMedium,
                    color = tokens.colors.textMuted,
                )
            }
            state.friends.friends.forEach { friend ->
                val lastFinished = state.feed.firstOrNull { it.person.id == friend.id && it.kind == "finished" }
                PersonRow(
                    person = friend.person,
                    status = friendStatus(friend, lastFinished),
                    live = friend.watchingNow != null,
                    onClick = { openFriend = friend },
                )
            }
        }

        if (state.friends.outgoing.isNotEmpty()) {
            SocialGroup(title = stringResource(Res.string.social_pending_header)) {
                state.friends.outgoing.forEach { request ->
                    PersonRow(person = request.person, status = null) {
                        SocialPillButton(
                            text = stringResource(Res.string.social_cancel),
                            onClick = { SocialRepository.cancelRequest(request.id) },
                        )
                    }
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 12.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    text = stringResource(Res.string.social_sharing_title),
                    style = MaterialTheme.typography.bodyLarge,
                    color = tokens.colors.textPrimary,
                    fontWeight = FontWeight.Medium,
                )
                Text(
                    text = stringResource(Res.string.social_sharing_description),
                    style = MaterialTheme.typography.bodySmall,
                    color = tokens.colors.textMuted,
                )
            }
            Switch(
                checked = me.sharing == 1,
                onCheckedChange = SocialRepository::setSharing,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = tokens.colors.onAccent,
                    checkedTrackColor = tokens.colors.accent,
                    uncheckedThumbColor = tokens.colors.textMuted,
                    uncheckedTrackColor = tokens.colors.borderDefault,
                ),
            )
        }
    }

    if (showAddFriend) {
        AddFriendSheet(isTablet = isTablet, onDismiss = { showAddFriend = false })
    }
    if (showRecommendations) {
        RecommendationsSheet(
            isTablet = isTablet,
            onDismiss = { showRecommendations = false },
            onPosterClick = onPosterClick,
        )
    }
    openFriend?.let { friend ->
        FriendSheet(
            friend = state.friends.friends.firstOrNull { it.id == friend.id } ?: friend,
            feed = state.feed.filter { it.person.id == friend.id },
            isTablet = isTablet,
            onDismiss = { openFriend = null },
            onPosterClick = onPosterClick,
        )
    }
}

@Composable
private fun FriendCodeCard(code: String, onAddFriend: () -> Unit) {
    val tokens = MaterialTheme.nuvio
    val clipboard = LocalClipboardManager.current
    val scope = rememberCoroutineScope()
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(tokens.colors.accent.copy(alpha = 0.08f))
            .border(1.dp, tokens.colors.accent.copy(alpha = 0.18f), RoundedCornerShape(16.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                NuvioSectionLabel(text = stringResource(Res.string.social_your_code))
                Text(
                    text = code,
                    style = MaterialTheme.typography.headlineSmall,
                    color = tokens.colors.textPrimary,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                )
            }
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .clickable {
                        clipboard.setText(AnnotatedString(code))
                        scope.launch { NuvioToastController.show(getString(Res.string.social_code_copied)) }
                    }
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Icon(
                    imageVector = Icons.Rounded.ContentCopy,
                    contentDescription = null,
                    tint = tokens.colors.accent,
                    modifier = Modifier.size(18.dp),
                )
                Text(
                    text = stringResource(Res.string.social_copy),
                    style = MaterialTheme.typography.labelLarge,
                    color = tokens.colors.accent,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(tokens.colors.accent)
                .clickable(onClick = onAddFriend)
                .padding(vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Rounded.PersonAdd,
                contentDescription = null,
                tint = tokens.colors.onAccent,
                modifier = Modifier.size(20.dp),
            )
            Text(
                text = stringResource(Res.string.social_add_friend),
                style = MaterialTheme.typography.labelLarge,
                color = tokens.colors.onAccent,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

@Composable
private fun RecommendationsEntryRow(unseen: Int, onClick: () -> Unit) {
    val tokens = MaterialTheme.nuvio
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .border(1.dp, tokens.colors.borderSubtle, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(
            text = stringResource(Res.string.social_recommendations),
            style = MaterialTheme.typography.bodyLarge,
            color = tokens.colors.textPrimary,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.weight(1f),
        )
        if (unseen > 0) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .background(tokens.colors.accent)
                    .padding(horizontal = 8.dp, vertical = 2.dp),
            ) {
                Text(
                    text = stringResource(Res.string.social_recommendations_new, unseen),
                    style = MaterialTheme.typography.labelSmall,
                    color = tokens.colors.onAccent,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
        Icon(
            imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
            contentDescription = null,
            tint = tokens.colors.textMuted,
        )
    }
}

@Composable
private fun SocialGroup(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        NuvioSectionLabel(text = title)
        content()
    }
}

@Composable
private fun PersonRow(
    person: SocialPerson,
    status: String?,
    live: Boolean = false,
    onClick: (() -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
) {
    val tokens = MaterialTheme.nuvio
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        SocialAvatar(person = person, size = 42.dp, live = live)
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = person.displayName(),
                style = MaterialTheme.typography.bodyLarge,
                color = tokens.colors.textPrimary,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (status != null) {
                Text(
                    text = status,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (live) tokens.colors.accent else tokens.colors.textMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        if (trailing != null) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) { trailing() }
        }
    }
}

@Composable
internal fun SocialAvatar(person: SocialPerson, size: Dp, live: Boolean = false) {
    val tokens = MaterialTheme.nuvio
    Box(modifier = Modifier.size(size)) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(CircleShape)
                .background(tokens.colors.accent.copy(alpha = 0.16f))
                .then(if (live) Modifier.border(2.dp, tokens.colors.accent, CircleShape) else Modifier),
            contentAlignment = Alignment.Center,
        ) {
            val avatar = person.avatar
            if (avatar != null) {
                NuvioAsyncImage(
                    imageUrl = avatar,
                    contentDescription = person.displayName(),
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                    // Profiles can use animated (GIF) avatars; play them like Settings does.
                    animateIfPossible = true,
                )
            } else {
                Text(
                    text = person.displayName().take(1).uppercase(),
                    style = if (size > 56.dp) MaterialTheme.typography.headlineMedium else MaterialTheme.typography.titleMedium,
                    color = tokens.colors.accent,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
        if (live) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .size(size * 0.28f)
                    .clip(CircleShape)
                    .background(tokens.colors.surface)
                    .padding(2.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF30D158)),
            )
        }
    }
}

@Composable
internal fun SocialPillButton(text: String, filled: Boolean = false, onClick: () -> Unit) {
    val tokens = MaterialTheme.nuvio
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        color = if (filled) tokens.colors.onAccent else tokens.colors.textPrimary,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(if (filled) tokens.colors.accent else tokens.colors.textPrimary.copy(alpha = 0.08f))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
    )
}

@Composable
private fun friendStatus(friend: SocialFriend, lastFinished: SocialFeedItem?): String? {
    friend.watchingNow?.let { watching ->
        return stringResource(
            Res.string.social_status_watching,
            titleWithEpisode(watching.title, watching.season, watching.episode),
        )
    }
    lastFinished ?: return null
    return stringResource(
        Res.string.social_status_finished,
        titleWithEpisode(lastFinished.title, lastFinished.season, lastFinished.episode),
        relativeTime(lastFinished.updatedAt),
    )
}

@Composable
internal fun titleWithEpisode(title: String, season: Int?, episode: Int?): String =
    if (season != null && episode != null) {
        stringResource(Res.string.social_episode_suffix, title, season, episode)
    } else {
        title
    }

@Composable
internal fun relativeTime(epochMs: Long): String {
    val minutes = ((Clock.System.now().toEpochMilliseconds() - epochMs) / 60_000L).coerceAtLeast(0L).toInt()
    return when {
        minutes < 1 -> stringResource(Res.string.social_time_now)
        minutes < 60 -> stringResource(Res.string.social_time_minutes, minutes)
        minutes < 48 * 60 -> stringResource(Res.string.social_time_hours, minutes / 60)
        else -> stringResource(Res.string.social_time_days, minutes / (24 * 60))
    }
}

@Composable
internal fun SocialPerson.displayName(): String =
    name.ifBlank { stringResource(Res.string.social_someone) }

// ---------------------------------------------------------------------------------------------
// Sheets
// ---------------------------------------------------------------------------------------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddFriendSheet(isTablet: Boolean, onDismiss: () -> Unit) {
    val tokens = MaterialTheme.nuvio
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    var code by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var sending by remember { mutableStateOf(false) }
    NuvioModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = if (isTablet) 24.dp else 18.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(
                text = stringResource(Res.string.social_add_friend_title),
                style = MaterialTheme.typography.titleLarge,
                color = tokens.colors.textPrimary,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = stringResource(Res.string.social_add_friend_message),
                style = MaterialTheme.typography.bodyMedium,
                color = tokens.colors.textMuted,
            )
            NuvioInputField(
                value = code,
                onValueChange = { value ->
                    code = value.uppercase().filter { it.isLetterOrDigit() || it == '-' || it == ' ' }.take(12)
                    error = null
                },
                placeholder = stringResource(Res.string.social_add_friend_placeholder),
            )
            error?.let { message ->
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodySmall,
                    color = tokens.colors.danger,
                )
            }
            NuvioPrimaryButton(
                text = stringResource(Res.string.social_add_friend_send),
                enabled = !sending && code.count { it.isLetterOrDigit() } == 8,
                onClick = {
                    sending = true
                    scope.launch {
                        SocialRepository.addFriend(code)
                            .onSuccess { result ->
                                val name = result.person?.name?.ifBlank { null } ?: getString(Res.string.social_someone)
                                NuvioToastController.show(
                                    if (result.status == "friends") {
                                        getString(Res.string.social_add_friend_added, name)
                                    } else {
                                        getString(Res.string.social_add_friend_requested, name)
                                    },
                                )
                                onDismiss()
                            }
                            .onFailure { failure ->
                                error = getString(
                                    when (failure.socialCode()) {
                                        "code_not_found" -> Res.string.social_error_code_not_found
                                        "own_code" -> Res.string.social_error_own_code
                                        "bad_code" -> Res.string.social_error_bad_code
                                        else -> Res.string.social_error_generic
                                    },
                                )
                            }
                        sending = false
                    }
                },
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FriendSheet(
    friend: SocialFriend,
    feed: List<SocialFeedItem>,
    isTablet: Boolean,
    onDismiss: () -> Unit,
    onPosterClick: ((MetaPreview) -> Unit)?,
) {
    val tokens = MaterialTheme.nuvio
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var confirmRemove by remember { mutableStateOf(false) }
    NuvioModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = if (isTablet) 24.dp else 18.dp)
                // Clear of the grabber up top, with each block (who, activity, remove) set apart.
                .padding(top = 16.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(28.dp),
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                SocialAvatar(person = friend.person, size = 84.dp, live = friend.watchingNow != null)
                Text(
                    text = friend.person.displayName(),
                    style = MaterialTheme.typography.titleLarge,
                    color = tokens.colors.textPrimary,
                    fontWeight = FontWeight.Bold,
                )
            }
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                NuvioSectionLabel(text = stringResource(Res.string.social_friend_activity_header))
                if (feed.isEmpty()) {
                    Text(
                        text = stringResource(Res.string.social_friend_activity_empty),
                        style = MaterialTheme.typography.bodyMedium,
                        color = tokens.colors.textMuted,
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = if (isTablet) 480.dp else 380.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        items(items = feed, key = { it.id }) { item ->
                            ActivityRow(
                                item = item,
                                onClick = onPosterClick?.let { open ->
                                    {
                                        onDismiss()
                                        open(MetaPreview(id = item.contentId, type = item.contentType, name = item.title, poster = item.poster))
                                    }
                                },
                            )
                        }
                    }
                }
            }
            Text(
                text = stringResource(
                    if (confirmRemove) Res.string.social_remove_friend_confirm else Res.string.social_remove_friend,
                ),
                style = MaterialTheme.typography.labelLarge,
                color = tokens.colors.danger,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(tokens.colors.danger.copy(alpha = if (confirmRemove) 0.16f else 0.08f))
                    .clickable {
                        if (confirmRemove) {
                            SocialRepository.removeFriend(friend.id)
                            onDismiss()
                        } else {
                            confirmRemove = true
                        }
                    }
                    .padding(vertical = 12.dp),
            )
        }
    }
}

@Composable
private fun ActivityRow(item: SocialFeedItem, onClick: (() -> Unit)?) {
    val tokens = MaterialTheme.nuvio
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        SocialPoster(title = item.title, poster = item.poster, contentType = item.contentType, contentId = item.contentId, width = 44.dp)
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = titleWithEpisode(item.title, item.season, item.episode),
                style = MaterialTheme.typography.bodyLarge,
                color = tokens.colors.textPrimary,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = if (item.live) stringResource(Res.string.social_live) else relativeTime(item.updatedAt),
                style = MaterialTheme.typography.bodySmall,
                color = if (item.live) tokens.colors.accent else tokens.colors.textMuted,
            )
        }
    }
}

/** A small poster: the URL the server has, else whatever the metadata cache can find. */
@Composable
internal fun SocialPoster(title: String, poster: String?, contentType: String, contentId: String, width: Dp) {
    val tokens = MaterialTheme.nuvio
    val initial = remember(contentType, contentId, poster) {
        poster ?: com.nuvio.app.features.settings.profileCachedArtwork(contentType, contentId)
    }
    var artwork by remember(contentType, contentId) { mutableStateOf(initial) }
    LaunchedEffect(contentType, contentId, initial) {
        if (initial == null) {
            artwork = com.nuvio.app.features.settings.profileFetchPosterMetadata(contentType, contentId).first
        }
    }
    val shape = RoundedCornerShape(8.dp)
    Box(
        modifier = Modifier
            .width(width)
            .aspectRatio(2f / 3f)
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .border(1.dp, tokens.colors.borderSubtle, shape),
        contentAlignment = Alignment.Center,
    ) {
        val url = artwork
        if (url != null) {
            NuvioAsyncImage(imageUrl = url, contentDescription = title, modifier = Modifier.fillMaxSize())
        } else {
            Text(
                text = title.take(1).uppercase(),
                style = MaterialTheme.typography.titleMedium,
                color = tokens.colors.textMuted,
                fontWeight = FontWeight.Black,
            )
        }
    }
}

/** Up to three friends' avatars, overlapping; anyone watching right now gets the live ring. */
@Composable
internal fun SocialFriendsPreview() {
    val state by SocialRepository.state.collectAsStateWithLifecycle()
    val friends = state.friends.friends
        .sortedByDescending { it.watchingNow != null }
        .take(3)
    if (friends.isEmpty()) return
    val tokens = MaterialTheme.nuvio
    Box {
        friends.forEachIndexed { index, friend ->
            Box(
                modifier = Modifier
                    .padding(start = (index * 18).dp)
                    .clip(CircleShape)
                    .background(tokens.colors.surface)
                    .padding(2.dp),
            ) {
                SocialAvatar(person = friend.person, size = 26.dp, live = friend.watchingNow != null)
            }
        }
    }
}
