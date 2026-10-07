package com.nuvio.app.core.ui

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity

private const val PinnedHeaderFillerKey = "pinned_header_filler"

/**
 * Bottom padding that lets a short list still scroll far enough for its pinned header to reach
 * the top: without it, a list barely taller than the screen moves a few points and stops, leaving
 * the large title half under the status bar and the header never pinned.
 */
@Stable
class PinnedHeaderFillerState internal constructor() {
    internal var heightPx by mutableIntStateOf(0)
}

/**
 * Keeps [PinnedHeaderFillerState] sized so the item keyed [slotKey] can scroll up to [pinnedTopPx].
 * [firstKey] is the list's first item; the list's last item must be [pinnedHeaderFiller].
 */
@Composable
internal fun rememberPinnedHeaderFiller(
    listState: LazyListState,
    firstKey: Any,
    slotKey: Any,
    pinnedTopPx: Float,
): PinnedHeaderFillerState {
    val state = remember { PinnedHeaderFillerState() }
    LaunchedEffect(listState, firstKey, slotKey, pinnedTopPx) {
        snapshotFlow {
            val layoutInfo = listState.layoutInfo
            val items = layoutInfo.visibleItemsInfo
            val first = items.firstOrNull { it.key == firstKey }
            val slot = items.firstOrNull { it.key == slotKey }
            // Positions can only be measured from the top of the list; once it's scrolled past,
            // the last answer still holds.
            if (first == null || slot == null) return@snapshotFlow null
            val filler = items.firstOrNull { it.key == PinnedHeaderFillerKey }
                // Content already runs past the bottom of the screen: no filler needed.
                ?: return@snapshotFlow 0
            // In content coordinates, independent of the filler's own height, so this settles.
            val slotTop = slot.offset - first.offset
            val fillerTop = filler.offset - first.offset
            val needed = (slotTop - pinnedTopPx) +
                layoutInfo.viewportSize.height -
                fillerTop -
                layoutInfo.afterContentPadding
            needed.toInt().coerceAtLeast(0)
        }.collect { height -> if (height != null) state.heightPx = height }
    }
    return state
}

/** The list's last item: blank space sized by [rememberPinnedHeaderFiller]. */
internal fun LazyListScope.pinnedHeaderFiller(state: PinnedHeaderFillerState) {
    item(key = PinnedHeaderFillerKey) {
        PinnedHeaderFillerSpacer(state)
    }
}

@Composable
private fun PinnedHeaderFillerSpacer(state: PinnedHeaderFillerState) {
    val height = with(LocalDensity.current) { state.heightPx.toDp() }
    Spacer(modifier = Modifier.fillMaxWidth().height(height))
}
