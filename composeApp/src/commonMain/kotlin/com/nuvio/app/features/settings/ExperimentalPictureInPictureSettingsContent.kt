package com.nuvio.app.features.settings

import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.nuvio.app.features.player.IosExperimentalPictureInPictureSettingsStorage

internal fun LazyListScope.experimentalPictureInPictureSettingsContent(isTablet: Boolean) {
    if (!IosExperimentalPictureInPictureSettingsStorage.isAvailable) return

    item {
        var enabled by remember {
            mutableStateOf(IosExperimentalPictureInPictureSettingsStorage.loadSinglePrimaryRendererEnabled())
        }
        SettingsGroup(isTablet = isTablet) {
            SettingsSwitchRow(
                title = "Picture in Picture",
                description = "Uses an experimental Metal-based render pipeline to seamlessly transition into PiP without reopening the stream. This changes the core video output and may cause unexpected behavior.",
                checked = enabled,
                isTablet = isTablet,
                onCheckedChange = { newValue ->
                    enabled = newValue
                    IosExperimentalPictureInPictureSettingsStorage.saveSinglePrimaryRendererEnabled(newValue)
                },
            )
        }
    }
}
