package com.nuvio.app.features.details.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.nuvio.app.features.library.LibraryRatingMax
import nuvio.composeapp.generated.resources.Res
import nuvio.composeapp.generated.resources.detail_rating_star
import org.jetbrains.compose.resources.stringResource

/**
 * A row of 5 tappable stars for the viewer's own, local-only rating of this title. Tapping the
 * already-selected star clears the rating back to unrated, matching the usual star-picker
 * convention of a second tap meaning "undo this".
 */
@Composable
fun DetailRatingStars(
    rating: Int,
    onRatingSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        for (star in 1..LibraryRatingMax) {
            val label = stringResource(Res.string.detail_rating_star, star)
            Icon(
                imageVector = if (star <= rating) Icons.Filled.Star else Icons.Outlined.StarBorder,
                contentDescription = label,
                tint = if (star <= rating) {
                    MaterialTheme.colorScheme.onBackground
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
                modifier = Modifier
                    .size(26.dp)
                    .clickable(role = Role.Button) {
                        onRatingSelected(if (star == rating) 0 else star)
                    },
            )
        }
    }
}
