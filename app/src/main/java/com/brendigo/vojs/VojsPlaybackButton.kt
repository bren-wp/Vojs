package com.brendigo.vojs

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Same reference playback button, placed where it remains reachable. */
@Composable
internal fun VojsPlaybackButton(recorder: VoiceRecorder) {
    OutlinedButton(
        onClick = recorder::togglePlayback,
        shape = RoundedCornerShape(17.dp),
        modifier = Modifier.fillMaxWidth().height(53.dp),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = Violet)
    ) {
        Text(
            if (recorder.playing) "■  Zaustavi" else "▶  Preslušaj",
            fontSize = 16.sp, fontWeight = FontWeight.SemiBold
        )
    }
}
