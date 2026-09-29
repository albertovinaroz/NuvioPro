package com.nuvio.app.features.settings

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nuvio.app.core.ui.NuvioScreen
import com.nuvio.app.core.ui.NuvioScreenHeader
import com.nuvio.app.features.notifications.EpisodeReleaseNotificationsRepository
import nuvio.composeapp.generated.resources.Res
import nuvio.composeapp.generated.resources.compose_settings_page_notifications
import org.jetbrains.compose.resources.stringResource

@Composable
fun NotificationsSettingsScreen(
    onBack: () -> Unit,
) {
    val uiState by remember {
        EpisodeReleaseNotificationsRepository.ensureLoaded()
        EpisodeReleaseNotificationsRepository.uiState
    }.collectAsStateWithLifecycle()

    NuvioScreen(
        modifier = Modifier.fillMaxSize(),
    ) {
        stickyHeader {
            NuvioScreenHeader(
                title = stringResource(Res.string.compose_settings_page_notifications),
                onBack = onBack,
            )
        }
        notificationsSettingsContent(isTablet = false, uiState = uiState)
    }
}
