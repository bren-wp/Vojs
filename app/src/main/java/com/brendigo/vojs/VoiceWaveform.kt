package com.brendigo.vojs

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import kotlinx.coroutines.delay
import kotlin.math.abs
import kotlin.math.sin

@Composable
internal fun VoiceWaveform(
    values: List<Float>, active: Boolean, color: Color,
    modifier: Modifier = Modifier
) {
    Canvas(
        modifier.semantics {
            contentDescription =
                if (active) "Prikaz jakosti zvuka tijekom snimanja"
                else "Valni prikaz glasovne poruke"
        }
    ) {
        // Original VOJS concept has 22 separated rounded waveform bars.
        // Heights measured from all 22 purple bars in the original VOJS
        // 1080×1920 concept (the phone's white display area), normalized
        // to its tallest bar. Preserve contour instead of adding a base
        // floor that made all shorter bars substantially too tall.
        val reference = floatArrayOf(
            .187f, .351f, .545f, .313f, .709f, .918f, .478f, .754f,
            1.000f, .604f, .418f, .836f, .649f, .963f, .373f, .560f,
            .791f, .455f, .896f, .627f, .291f, .731f
        )
        val count = reference.size
        val step = size.width / (count + 1)
        val mid = size.height / 2f
        val cap = 5.dp.toPx()
        for (i in 0 until count) {
            val amplitudeIndex = i * (values.size - 1).coerceAtLeast(0) / (count - 1)
            val measured = (values.getOrNull(amplitudeIndex) ?: 0.12f)
                .coerceIn(0.09f, 1f)
            // Preserve the reference contour at low volumes while reacting
            // to actual microphone amplitude instead of drawing flat bars.
            val dynamicScale = if (active) 0.72f + 0.28f * measured else 1f
            val height = reference[i] * size.height * .98f * dynamicScale
            val x = (i + 1) * step
            drawLine(
                color, Offset(x, mid - height / 2f),
                Offset(x, mid + height / 2f),
                strokeWidth = cap, cap = StrokeCap.Round
            )
        }
    }
}

internal fun formatTime(ms: Long): String {
    val seconds = (ms / 1000).coerceIn(0L, 300L)
    return "%02d:%02d".format(seconds / 60, seconds % 60)
}
