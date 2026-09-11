package com.nuvio.app.features.updater

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.foundation.clickable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.nuvio.app.core.format.formatReleaseDateForDisplay
import com.nuvio.app.core.ui.NuvioScreen
import com.nuvio.app.core.ui.NuvioScreenHeader
import com.nuvio.app.features.home.components.HomeEmptyStateCard
import nuvio.composeapp.generated.resources.Res
import nuvio.composeapp.generated.resources.whats_new_empty_description
import nuvio.composeapp.generated.resources.whats_new_empty_title
import nuvio.composeapp.generated.resources.whats_new_error
import nuvio.composeapp.generated.resources.whats_new_title
import nuvio.composeapp.generated.resources.whats_new_view_on_github
import org.jetbrains.compose.resources.stringResource

/**
 * In-app changelog — reuses the same GitHub Releases channel [AppUpdateFeedNotifier] already
 * checks for the passive update alert, so tapping that alert's card can land here instead of
 * bouncing out to a browser.
 */
@Composable
fun WhatsNewScreen(onBack: () -> Unit) {
    var releases by remember { mutableStateOf<List<ChannelReleaseNote>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var loadFailed by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        AppUpdaterRepository.getRecentChannelReleases(limit = 10)
            .onSuccess { releases = it }
            .onFailure { loadFailed = true }
        isLoading = false
    }

    NuvioScreen(modifier = Modifier) {
        stickyHeader {
            NuvioScreenHeader(
                title = stringResource(Res.string.whats_new_title),
                modifier = Modifier.padding(horizontal = 16.dp),
                onBack = onBack,
            )
        }
        when {
            isLoading -> {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 48.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator()
                    }
                }
            }
            loadFailed || releases.isEmpty() -> {
                item {
                    HomeEmptyStateCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 24.dp),
                        title = stringResource(Res.string.whats_new_empty_title),
                        message = if (loadFailed) {
                            stringResource(Res.string.whats_new_error)
                        } else {
                            stringResource(Res.string.whats_new_empty_description)
                        },
                    )
                }
            }
            else -> {
                items(releases, key = ChannelReleaseNote::tag) { release ->
                    WhatsNewReleaseCard(
                        release = release,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 6.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun WhatsNewReleaseCard(release: ChannelReleaseNote, modifier: Modifier = Modifier) {
    val uriHandler = LocalUriHandler.current
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = release.tag,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            release.publishedAt?.let { publishedAt ->
                Text(
                    text = formatReleaseDateForDisplay(publishedAt),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Text(
            text = cleanChangelogMarkdown(release.notes),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        release.releaseUrl?.let { url ->
            Text(
                text = stringResource(Res.string.whats_new_view_on_github),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .padding(top = 8.dp)
                    .clickable { uriHandler.openUri(url) },
            )
        }
    }
}

/**
 * Cheap markdown-to-plain-text cleanup for a full changelog rendering — not a real renderer, just
 * enough to make the same text that reads fine on GitHub read reasonably in a plain [Text]. Only
 * strips the ``` fence markers themselves (e.g. around the AltStore source URL), keeping their
 * contents visible instead of dropping the whole block.
 */
private fun cleanChangelogMarkdown(raw: String): String {
    val withoutFences = raw.replace("```", "").trim()
    return withoutFences.lines()
        .map { line ->
            line
                .replace(Regex("^#{1,6}\\s*"), "")
                .replace(Regex("^>\\s*"), "")
                .replace(Regex("^[-*]\\s+"), "• ")
                .replace(Regex("\\*\\*(.*?)\\*\\*"), "$1")
                .replace(Regex("`(.*?)`"), "$1")
        }
        .joinToString("\n")
        .replace(Regex("\n{3,}"), "\n\n")
        .trim()
}
