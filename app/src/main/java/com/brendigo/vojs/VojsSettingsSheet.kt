package com.brendigo.vojs

import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Native VOJS appearance and privacy sheet.
 * Scrolls when a short phone, tablet landscape or larger text would clip rows.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun VojsSettingsSheet(
    appearance: String,
    dark: Boolean,
    secondary: Color,
    onDismiss: () -> Unit,
    onAppearanceChange: (String) -> Unit,
    onPrivacy: () -> Unit
) {
    val maxHeight = (LocalConfiguration.current.screenHeightDp * 0.82f).dp
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = if (dark) Night else Paper,
        contentColor = if (dark) Paper else Ink
    ) {
        Column(
            Modifier.fillMaxWidth()
                .heightIn(max = maxHeight)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 25.dp)
                .padding(bottom = 36.dp)
        ) {
            Text("Postavke", style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(14.dp))
            Text("Izgled aplikacije", color = secondary)
            Text("Zadano: tamna tema. Tvoj izbor ostaje spremljen.",
                color = secondary, fontSize = 12.sp)
            listOf(
                VojsAppearance.DARK to "Tamna tema (zadana)",
                VojsAppearance.LIGHT to "Svijetla tema",
                VojsAppearance.SYSTEM to "Prema postavkama uređaja"
            ).forEach { (value, title) ->
                Row(
                    Modifier.fillMaxWidth()
                        .clickable { onAppearanceChange(value) }
                        .padding(vertical = 13.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = appearance == value,
                        onClick = { onAppearanceChange(value) }
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(title)
                }
            }
            HorizontalDivider()
            Spacer(Modifier.height(18.dp))
            Text("Privatnost", fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(8.dp))
            Text(
                "Snimanje i dijeljenje datoteke radi offline. Izravna web poveznica " +
                "koristi WebRTC i kratkotrajne JSON podatke za uspostavu veze. " +
                "VOJS poslužitelj ne prima audio. Za prijenos primatelj i " +
                "pošiljatelj moraju biti online u isto vrijeme.",
                color = secondary, fontSize = 13.sp, lineHeight = 19.sp
            )
            Spacer(Modifier.height(12.dp))
            TextButton(onClick = onPrivacy) {
                Text("Pravila privatnosti", color = Violet)
            }
        }
    }
}
