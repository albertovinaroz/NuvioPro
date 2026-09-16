package com.nuvio.app.features.library

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil3.compose.AsyncImage
import com.nuvio.app.core.ui.NuvioDropdownChip
import com.nuvio.app.core.ui.NuvioDropdownOption
import com.nuvio.app.features.home.PosterShape
import com.nuvio.app.features.home.components.posterGridColumnCountForWidth
import nuvio.composeapp.generated.resources.Res
import nuvio.composeapp.generated.resources.library_filter_rating
import nuvio.composeapp.generated.resources.library_rated_empty_message
import nuvio.composeapp.generated.resources.library_rated_empty_title
import nuvio.composeapp.generated.resources.library_rated_title
import nuvio.composeapp.generated.resources.library_rating_any
import nuvio.composeapp.generated.resources.library_rating_min_stars
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun LibraryRatedPanel(
    entries: List<RatedLibraryEntry>,
    onDismiss: () -> Unit,
    onPosterClick: (RatedLibraryEntry) -> Unit,
) {
    var minRating by rememberSaveable { mutableStateOf(0) }
    val anyRatingLabel = stringResource(Res.string.library_rating_any)
    val sortedEntries = remember(entries, minRating) {
        entries
            .filter { minRating <= 0 || it.rating >= minRating }
            .sortedWith(
                compareByDescending<RatedLibraryEntry> { it.rating }.thenByDescending { it.ratedAtEpochMs },
            )
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background,
        ) {
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxSize()
                    .windowInsetsPadding(WindowInsets.safeDrawing)
                    .padding(horizontal = 16.dp),
            ) {
                val columns = posterGridColumnCountForWidth(maxWidth)
                Column(modifier = Modifier.fillMaxSize()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = stringResource(Res.string.library_rated_title),
                            style = MaterialTheme.typography.headlineLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.ExtraBold,
                            modifier = Modifier.weight(1f),
                        )
                        IconButton(onClick = onDismiss) {
                            Icon(
                                imageVector = Icons.Rounded.Close,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurface,
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    val ratingOptions = remember {
                        buildList {
                            add(NuvioDropdownOption(key = "0", label = anyRatingLabel))
                            for (stars in LibraryRatingMax downTo 1) {
                                add(NuvioDropdownOption(key = stars.toString(), label = "$stars+ ★"))
                            }
                        }
                    }
                    NuvioDropdownChip(
                        title = stringResource(Res.string.library_filter_rating),
                        label = if (minRating <= 0) anyRatingLabel else stringResource(Res.string.library_rating_min_stars, minRating),
                        selectedKey = minRating.toString(),
                        options = ratingOptions,
                        onSelected = { option -> minRating = option.key.toIntOrNull() ?: 0 },
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    if (sortedEntries.isEmpty()) {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Text(
                                text = stringResource(Res.string.library_rated_empty_title),
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.SemiBold,
                            )
                            Text(
                                text = stringResource(Res.string.library_rated_empty_message),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    } else {
                        LazyColumn(verticalArrangement = Arrangement.spacedBy(20.dp)) {
                            items(
                                items = sortedEntries.chunked(columns),
                                key = { rowEntries -> "rated:${rowEntries.first().type}:${rowEntries.first().id}" },
                            ) { rowEntries ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                ) {
                                    rowEntries.forEach { entry ->
                                        RatedPosterTile(
                                            entry = entry,
                                            modifier = Modifier.weight(1f),
                                            onClick = { onPosterClick(entry) },
                                        )
                                    }
                                    repeat(columns - rowEntries.size) {
                                        Spacer(modifier = Modifier.weight(1f))
                                    }
                                }
                            }
                            item(key = "rated-bottom-spacer") {
                                Spacer(modifier = Modifier.height(24.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RatedPosterTile(
    entry: RatedLibraryEntry,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(if (entry.posterShape == PosterShape.Landscape) 1.78f else 0.68f)
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surface)
                .clickable(onClick = onClick),
        ) {
            if (entry.poster != null) {
                AsyncImage(
                    model = entry.poster,
                    contentDescription = entry.name,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                )
            }
        }
        Text(
            text = entry.name,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.onBackground,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(
                text = entry.rating.toString(),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Icon(
                imageVector = Icons.Rounded.Star,
                contentDescription = null,
                modifier = Modifier.size(14.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
