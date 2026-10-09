package com.nuvio.app.features.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.LocalCafe
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.nuvio.app.core.ui.NuvioModalBottomSheet
import com.nuvio.app.core.ui.dismissNuvioBottomSheet
import com.nuvio.app.core.ui.nuvio
import com.nuvio.app.core.ui.nuvioSafeBottomPadding
import kotlinx.coroutines.launch
import nuvio.composeapp.generated.resources.Res
import nuvio.composeapp.generated.resources.support_pro_heading
import nuvio.composeapp.generated.resources.support_pro_kofi_button
import nuvio.composeapp.generated.resources.support_pro_message
import nuvio.composeapp.generated.resources.support_pro_upstream_note
import org.jetbrains.compose.resources.stringResource

/**
 * Where "Support Nuvio Pro" sends people: the Pro developer's own Ko-fi page (not NuvioMedia's).
 * Blank leaves the button disabled. Only shown in builds where donation links are allowed
 * ([com.nuvio.app.core.build.AppFeaturePolicy.donationActionsEnabled]) — never App Store or
 * Play Store, whose rules route tips to developers through their own billing.
 */
internal const val SUPPORT_PRO_KOFI_URL = "https://ko-fi.com/albertovinaroz"

/** Ko-fi's brand red, used for its button. */
internal val KofiBrandColor = Color(0xFFFF5E5B)

/** A short thank-you note and a Ko-fi button, opened from Settings → About. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SupportProSheet(onDismiss: () -> Unit) {
    val tokens = MaterialTheme.nuvio
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    val uriHandler = LocalUriHandler.current
    val kofiUrl = SUPPORT_PRO_KOFI_URL.trim()

    NuvioModalBottomSheet(
        onDismissRequest = { scope.launch { dismissNuvioBottomSheet(sheetState, onDismiss) } },
        sheetState = sheetState,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 28.dp)
                .padding(top = 8.dp, bottom = nuvioSafeBottomPadding(24.dp)),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(KofiBrandColor.copy(alpha = 0.16f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Rounded.LocalCafe,
                    contentDescription = null,
                    tint = KofiBrandColor,
                    modifier = Modifier.size(34.dp),
                )
            }
            Spacer(modifier = Modifier.height(18.dp))
            Text(
                text = stringResource(Res.string.support_pro_heading),
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold),
                color = tokens.colors.textPrimary,
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = stringResource(Res.string.support_pro_message),
                style = MaterialTheme.typography.bodyMedium,
                color = tokens.colors.textSecondary,
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(24.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(CircleShape)
                    .background(KofiBrandColor)
                    .alpha(if (kofiUrl.isNotEmpty()) 1f else 0.5f)
                    .clickable(enabled = kofiUrl.isNotEmpty()) { uriHandler.openUri(kofiUrl) }
                    .padding(vertical = 14.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = Icons.Rounded.LocalCafe,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(20.dp),
                )
                Spacer(modifier = Modifier.size(8.dp))
                Text(
                    text = stringResource(Res.string.support_pro_kofi_button),
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = Color.White,
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            // Donations here go to Pro, so say plainly it isn't NuvioMedia's own app.
            Text(
                text = stringResource(Res.string.support_pro_upstream_note),
                style = MaterialTheme.typography.bodySmall,
                color = tokens.colors.textMuted,
                textAlign = TextAlign.Center,
            )
        }
    }
}
