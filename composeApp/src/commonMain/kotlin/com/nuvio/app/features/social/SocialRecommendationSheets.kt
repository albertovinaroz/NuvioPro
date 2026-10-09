package com.nuvio.app.features.social

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nuvio.app.core.ui.NuvioInputField
import com.nuvio.app.core.ui.NuvioModalBottomSheet
import com.nuvio.app.core.ui.NuvioPrimaryButton
import com.nuvio.app.core.ui.NuvioToastController
import com.nuvio.app.core.ui.nuvio
import com.nuvio.app.features.home.MetaPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import nuvio.composeapp.generated.resources.*
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.stringResource

// ---------------------------------------------------------------------------------------------
// Inbox
// ---------------------------------------------------------------------------------------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun RecommendationsSheet(
    isTablet: Boolean,
    onDismiss: () -> Unit,
    onPosterClick: ((MetaPreview) -> Unit)?,
) {
    val tokens = MaterialTheme.nuvio
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val state by SocialRepository.state.collectAsStateWithLifecycle()
    var showSent by remember { mutableStateOf(false) }
    // Opening the inbox counts as seeing everything in it.
    LaunchedEffect(Unit) {
        SocialRepository.markSeen(state.recommendations.received.filterNot { it.seen }.map { it.id })
    }
    val openTitle: ((SocialRecommendation) -> Unit)? = onPosterClick?.let { open ->
        { rec ->
            onDismiss()
            open(MetaPreview(id = rec.contentId, type = rec.contentType, name = rec.title, poster = rec.poster))
        }
    }
    NuvioModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = if (isTablet) 24.dp else 18.dp)
                .padding(bottom = 22.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(
                text = stringResource(Res.string.social_recommendations),
                style = MaterialTheme.typography.titleLarge,
                color = tokens.colors.textPrimary,
                fontWeight = FontWeight.Bold,
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(999.dp))
                    .background(tokens.colors.textPrimary.copy(alpha = 0.06f))
                    .padding(3.dp),
            ) {
                SegmentTab(
                    text = stringResource(Res.string.social_recommendations_received),
                    selected = !showSent,
                    onClick = { showSent = false },
                    modifier = Modifier.weight(1f),
                )
                SegmentTab(
                    text = stringResource(Res.string.social_recommendations_sent),
                    selected = showSent,
                    onClick = { showSent = true },
                    modifier = Modifier.weight(1f),
                )
            }
            val items = if (showSent) state.recommendations.sent else state.recommendations.received
            if (items.isEmpty()) {
                Text(
                    text = stringResource(
                        if (showSent) Res.string.social_recommendations_empty_sent else Res.string.social_recommendations_empty_received,
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = tokens.colors.textMuted,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 28.dp),
                )
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = if (isTablet) 640.dp else 540.dp),
                    verticalArrangement = Arrangement.spacedBy(18.dp),
                ) {
                    items(items = items, key = { (if (showSent) "s:" else "r:") + it.id }) { rec ->
                        RecommendationItem(rec = rec, isSent = showSent, onOpen = openTitle)
                    }
                }
            }
        }
    }
}

@Composable
private fun SegmentTab(text: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier) {
    val tokens = MaterialTheme.nuvio
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        color = if (selected) tokens.colors.textPrimary else tokens.colors.textMuted,
        fontWeight = FontWeight.SemiBold,
        textAlign = TextAlign.Center,
        modifier = modifier
            .clip(RoundedCornerShape(999.dp))
            .background(if (selected) tokens.colors.surfaceElevated else tokens.colors.textPrimary.copy(alpha = 0f))
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
    )
}

@Composable
private fun RecommendationItem(
    rec: SocialRecommendation,
    isSent: Boolean,
    onOpen: ((SocialRecommendation) -> Unit)?,
) {
    val tokens = MaterialTheme.nuvio
    val scope = rememberCoroutineScope()
    var reply by remember(rec.id) { mutableStateOf("") }
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Box(modifier = if (onOpen != null) Modifier.clickable { onOpen(rec) } else Modifier) {
            SocialPoster(title = rec.title, poster = rec.poster, contentType = rec.contentType, contentId = rec.contentId, width = 64.dp)
        }
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                text = rec.title,
                style = MaterialTheme.typography.bodyLarge,
                color = tokens.colors.textPrimary,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = if (onOpen != null) Modifier.clickable { onOpen(rec) } else Modifier,
            )
            Text(
                text = stringResource(
                    if (isSent) Res.string.social_recommended_to else Res.string.social_recommended_by,
                    rec.person.displayName(),
                    relativeTime(rec.createdAt),
                ),
                style = MaterialTheme.typography.bodySmall,
                color = tokens.colors.textMuted,
            )
            rec.note?.let { note ->
                Text(
                    text = "“$note”",
                    style = MaterialTheme.typography.bodyMedium,
                    color = tokens.colors.textPrimary,
                    fontStyle = FontStyle.Italic,
                )
            }
            val reaction = SocialReaction.fromKey(rec.reaction)
            if (isSent) {
                val response = listOfNotNull(reaction?.emoji, rec.reply).joinToString("  ")
                Text(
                    text = if (response.isBlank()) {
                        stringResource(Res.string.social_no_reply_yet)
                    } else {
                        stringResource(Res.string.social_their_reply, rec.person.displayName(), response)
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = if (response.isBlank()) tokens.colors.textMuted else tokens.colors.textPrimary,
                )
            } else {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    SocialReaction.entries.forEach { option ->
                        val selected = option == reaction
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(
                                    if (selected) tokens.colors.accent.copy(alpha = 0.22f) else tokens.colors.textPrimary.copy(alpha = 0.06f),
                                )
                                .then(if (selected) Modifier.border(1.dp, tokens.colors.accent, CircleShape) else Modifier)
                                .clickable { scope.launch { SocialRepository.respond(rec.id, option, null) } },
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(text = option.emoji, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
                if (rec.reply != null) {
                    Text(
                        text = stringResource(Res.string.social_your_reply, rec.reply),
                        style = MaterialTheme.typography.bodySmall,
                        color = tokens.colors.textMuted,
                    )
                } else {
                    NuvioInputField(
                        value = reply,
                        onValueChange = { reply = it.take(280) },
                        placeholder = stringResource(Res.string.social_reply_placeholder),
                        trailingContent = if (reply.isNotBlank()) {
                            {
                                Text(
                                    text = stringResource(Res.string.social_reply_send),
                                    style = MaterialTheme.typography.labelLarge,
                                    color = tokens.colors.accent,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable {
                                            val text = reply
                                            scope.launch {
                                                SocialRepository.respond(rec.id, null, text)
                                                    .onSuccess { reply = "" }
                                                    .onFailure { NuvioToastController.show(getString(Res.string.social_error_generic)) }
                                            }
                                        }
                                        .padding(horizontal = 10.dp, vertical = 6.dp),
                                )
                            }
                        } else {
                            null
                        },
                    )
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------------------------
// Recommend a title
// ---------------------------------------------------------------------------------------------

/** Opens the "Recommend to friends" sheet from anywhere; [RecommendSheetHost] shows it. */
object RecommendSheetController {
    private val _target = MutableStateFlow<MetaPreview?>(null)
    val target = _target.asStateFlow()

    fun open(title: MetaPreview) {
        SocialRepository.refresh()
        _target.value = title
    }

    fun dismiss() {
        _target.value = null
    }
}

@Composable
fun RecommendSheetHost(isTablet: Boolean = false) {
    val target by RecommendSheetController.target.collectAsStateWithLifecycle()
    target?.let { title ->
        RecommendSheet(title = title, isTablet = isTablet, onDismiss = RecommendSheetController::dismiss)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RecommendSheet(title: MetaPreview, isTablet: Boolean, onDismiss: () -> Unit) {
    val tokens = MaterialTheme.nuvio
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val state by SocialRepository.state.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    val selected = remember(title.id) { mutableStateListOf<String>() }
    var note by remember(title.id) { mutableStateOf("") }
    var sending by remember { mutableStateOf(false) }
    NuvioModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = if (isTablet) 24.dp else 18.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                SocialPoster(title = title.name, poster = title.poster, contentType = title.type, contentId = title.id, width = 48.dp)
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = stringResource(Res.string.social_recommend_title),
                        style = MaterialTheme.typography.titleLarge,
                        color = tokens.colors.textPrimary,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = title.name,
                        style = MaterialTheme.typography.bodyMedium,
                        color = tokens.colors.textMuted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            val friends = state.friends.friends
            if (friends.isEmpty()) {
                Text(
                    text = stringResource(Res.string.social_recommend_no_friends),
                    style = MaterialTheme.typography.bodyMedium,
                    color = tokens.colors.textMuted,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp),
                )
                return@Column
            }
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = if (isTablet) 420.dp else 320.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                items(items = friends, key = { it.id }) { friend ->
                    val checked = friend.id in selected
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { if (checked) selected.remove(friend.id) else selected.add(friend.id) }
                            .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        SocialAvatar(person = friend.person, size = 40.dp)
                        Text(
                            text = friend.person.displayName(),
                            style = MaterialTheme.typography.bodyLarge,
                            color = tokens.colors.textPrimary,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.weight(1f),
                        )
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(if (checked) tokens.colors.accent else tokens.colors.textPrimary.copy(alpha = 0f))
                                .border(1.5.dp, if (checked) tokens.colors.accent else tokens.colors.borderDefault, CircleShape),
                            contentAlignment = Alignment.Center,
                        ) {
                            if (checked) {
                                Icon(
                                    imageVector = Icons.Rounded.Check,
                                    contentDescription = null,
                                    tint = tokens.colors.onAccent,
                                    modifier = Modifier.size(16.dp),
                                )
                            }
                        }
                    }
                }
            }
            NuvioInputField(
                value = note,
                onValueChange = { note = it.take(280) },
                placeholder = stringResource(Res.string.social_recommend_note_placeholder),
            )
            NuvioPrimaryButton(
                text = if (selected.isEmpty()) {
                    stringResource(Res.string.social_recommend_send)
                } else {
                    stringResource(Res.string.social_recommend_send_count, selected.size)
                },
                enabled = selected.isNotEmpty() && !sending,
                onClick = {
                    sending = true
                    scope.launch {
                        SocialRepository.recommend(
                            friendIds = selected.toList(),
                            contentType = title.type,
                            contentId = title.id,
                            title = title.name,
                            poster = title.poster,
                            note = note,
                        )
                            .onSuccess {
                                NuvioToastController.show(getString(Res.string.social_recommend_sent))
                                onDismiss()
                            }
                            .onFailure { NuvioToastController.show(getString(Res.string.social_error_generic)) }
                        sending = false
                    }
                },
            )
        }
    }
}
