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
internal fun VoiceScreen(
    recorder: VoiceRecorder,
    dark: Boolean,
    foreground: Color,
    background: Color,
    secondary: Color,
    onSettings: () -> Unit,
    onRecord: () -> Unit,
    shareLink: VojsShareLink?,
    linkBusy: Boolean,
    linkError: String?,
    onCreateLink: () -> Unit,
    onCopyLink: () -> Unit,
    onShareLink: () -> Unit,
    onRevokeLink: () -> Unit,
    onDiscard: () -> Unit
) {
    val compact = androidx.compose.ui.platform.LocalConfiguration.current.screenHeightDp < 710
    val stage = recorder.stage
    Column(
        Modifier.fillMaxSize().background(background).safeDrawingPadding()
    ) {
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState())
                .fillMaxWidth().padding(horizontal = 24.dp)
                .padding(top = if (compact) 8.dp else 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Original VOJS wordmark is unmodified. Keep it in a dedicated
            // compact toolbar so "Poruka je spremna" never wraps or collides
            // with the logo/gear on 360dp+ screens or with large text scaling.
            Row(
                Modifier.fillMaxWidth().height(38.dp),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Image(
                    painter = painterResource(
                        if (dark) R.drawable.vojs_logo_dark else R.drawable.vojs_logo_light
                    ),
                    contentDescription = "VOJS",
                    modifier = Modifier.width(102.dp).height(38.dp),
                    contentScale = ContentScale.Fit
                )
                TextButton(onClick = onSettings,
                    modifier = Modifier.semantics {
                        contentDescription = "Postavke aplikacije"
                    }) {
                    Text("⚙", fontSize = 21.sp, color = Violet)
                }
            }
            Spacer(Modifier.height(5.dp))
            Text(
                if (stage == RecordingStage.READY) "Poruka je spremna" else "Nova poruka",
                color = foreground, fontSize = if (compact) 25.sp else 28.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.fillMaxWidth(),
                maxLines = 2
            )
            Spacer(Modifier.height(if (compact) 9.dp else 17.dp))
            if (stage == RecordingStage.READY) {
                // Original share reference uses concise cards instead of the
                // large recording-only waveform. There is no fake public URL
                // or expiry selector: the native app shares an actual M4A.
                Text(
                    "Preslušaj pa podijeli",
                    color = secondary, fontSize = 15.sp,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(if (compact) 20.dp else 34.dp))
                Row(
                    Modifier.fillMaxWidth()
                        .background(
                            if (dark) Color(0xFF302D52) else Color(0xFFF4F2FF),
                            RoundedCornerShape(16.dp)
                        )
                        .border(
                            1.dp, if (dark) Color(0xFF6256B5) else Color(0xFFE0DDF3),
                            RoundedCornerShape(16.dp)
                        )
                        .padding(horizontal = 20.dp, vertical = 24.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Glasovna poruka", color = foreground,
                        fontWeight = FontWeight.Bold, fontSize = 17.sp,
                        modifier = Modifier.weight(1f))
                    Text("M4A", color = Violet, fontWeight = FontWeight.SemiBold)
                }
                Spacer(Modifier.height(14.dp))
                Row(
                    Modifier.fillMaxWidth()
                        .border(
                            1.dp, if (dark) Color(0xFF625F79) else Color(0xFFDAD9E3),
                            RoundedCornerShape(16.dp)
                        )
                        .padding(horizontal = 20.dp, vertical = 21.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Trajanje snimke", color = secondary,
                        modifier = Modifier.weight(1f), fontSize = 15.sp)
                    Text(formatTime(recorder.elapsedMs), color = foreground,
                        fontWeight = FontWeight.SemiBold, fontSize = 17.sp)
                }
                Spacer(Modifier.height(if (compact) 20.dp else 30.dp))
                Text(
                    "Dijeljenje datoteke radi i bez interneta. Za web poveznicu " +
                    "snimka se uz tvoj odabir prenosi na poslužitelj i dostupna je do 24 sata.",
                    color = secondary, fontSize = 13.sp, lineHeight = 19.sp
                )
                if (shareLink != null) {
                    Spacer(Modifier.height(12.dp))
                    Text("Privatna poveznica:", color = foreground, fontWeight = FontWeight.SemiBold)
                    Text(shareLink.url, color = Violet, fontSize = 12.sp)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        TextButton(onClick = onCopyLink) { Text("Kopiraj poveznicu") }
                        TextButton(onClick = onRevokeLink, enabled = !linkBusy) { Text("Opozovi") }
                    }
                }
                if (linkError != null) {
                    Text(linkError, color = if (dark) Color(0xFFFFA3B3) else Color(0xFFB92342),
                        fontSize = 13.sp)
                }
                Spacer(Modifier.height(12.dp))
                OutlinedButton(
                    onClick = recorder::togglePlayback,
                    shape = RoundedCornerShape(17.dp),
                    modifier = Modifier.fillMaxWidth().height(53.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Violet)
                ) {
                    Text(if (recorder.playing) "■  Zaustavi" else "▶  Preslušaj",
                        fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                }
            } else {
                Text(
                    if (stage == RecordingStage.IDLE) "Dodirni gumb za snimanje"
                    else if (recorder.paused) "Snimanje je pauzirano" else "",
                    color = secondary, fontSize = 15.sp,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(if (compact) 12.dp else 26.dp))
                VoiceWaveform(
                    values = recorder.waveform.toList(),
                    active = stage == RecordingStage.RECORDING && !recorder.paused,
                    color = if (dark) Color(0xFF978BFF) else Violet,
                    modifier = Modifier.fillMaxWidth()
                        .padding(horizontal = 8.dp)
                        .height(if (compact) 170.dp else 228.dp)
                )
                Spacer(Modifier.height(if (compact) 12.dp else 20.dp))
                Text(
                    formatTime(recorder.elapsedMs),
                    color = foreground, fontSize = if (compact) 49.sp else 62.sp,
                    fontWeight = FontWeight.Bold, letterSpacing = (-2).sp
                )
                Text("najviše 05:00", color = secondary, fontSize = 15.sp)
                if (stage == RecordingStage.RECORDING) {
                    Spacer(Modifier.height(5.dp))
                    TextButton(onClick = recorder::togglePause) {
                        Text(
                            if (recorder.paused) "▶ Nastavi snimanje" else "Ⅱ Pauziraj",
                            color = Violet, fontSize = 14.sp
                        )
                    }
                }
            }
            if (recorder.error != null) {
                Spacer(Modifier.height(20.dp))
                Text(
                    recorder.error ?: "",
                    color = if (dark) Color(0xFFFFA3B3) else Color(0xFFB92342),
                    fontSize = 13.sp, textAlign = TextAlign.Center
                )
            }
        }
        Column(
            modifier = Modifier.fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = if (compact) 10.dp else 23.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            when (stage) {
                RecordingStage.IDLE -> {
                    Box(
                        modifier = Modifier
                            .size(if (compact) 94.dp else 112.dp)
                            .background(Brush.linearGradient(listOf(Violet, VioletDark)), CircleShape)
                            .clickable(
                                role = androidx.compose.ui.semantics.Role.Button,
                                onClick = onRecord
                            )
                            .semantics { contentDescription = "Započni snimanje" },
                        contentAlignment = Alignment.Center
                    ) {
                        Box(Modifier.size(32.dp).background(Paper, CircleShape))
                    }
                    Spacer(Modifier.height(13.dp))
                    TextButton(onClick = onRecord) {
                        Text("Započni snimanje", color = secondary,
                            fontSize = 14.sp, fontWeight = FontWeight.Medium)
                    }
                }
                RecordingStage.RECORDING -> {
                    Box(
                        modifier = Modifier
                            .size(if (compact) 94.dp else 112.dp)
                            .background(Brush.linearGradient(listOf(Violet, VioletDark)), CircleShape)
                            .clickable(
                                role = androidx.compose.ui.semantics.Role.Button,
                                onClick = recorder::finish
                            )
                            .semantics { contentDescription = "Završi snimanje" },
                        contentAlignment = Alignment.Center
                    ) {
                        Box(Modifier.width(29.dp).height(40.dp)
                            .background(Paper, RoundedCornerShape(22.dp)))
                    }
                    Spacer(Modifier.height(10.dp))
                    TextButton(onClick = recorder::finish) {
                        Text("Završi snimanje", color = secondary,
                            fontSize = 14.sp, fontWeight = FontWeight.Medium)
                    }

                }
                RecordingStage.READY -> {
                    // Use the original VOJS horizontal gradient CTA rather than
                    // a generic flat Material button; preserve native ShareSheet.
                    Box(
                        modifier = Modifier.fillMaxWidth().height(58.dp)
                            .background(
                                Brush.horizontalGradient(listOf(Violet, VioletDark)),
                                RoundedCornerShape(18.dp)
                            )
                            .clickable(
                                role = androidx.compose.ui.semantics.Role.Button,
                                onClick = recorder::share
                            )
                            .semantics {
                                contentDescription = "Podijeli glasovnu poruku"
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "Podijeli glasovnu poruku",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Paper
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = if (shareLink == null) onCreateLink else onShareLink,
                        enabled = !linkBusy,
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        shape = RoundedCornerShape(17.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Violet)
                    ) {
                        if (linkBusy) {
                            CircularProgressIndicator(modifier = Modifier.size(22.dp),
                                strokeWidth = 2.dp)
                            Spacer(Modifier.width(10.dp))
                        }
                        Text(
                            if (linkBusy) "Pričekaj…" else if (shareLink == null)
                                "Stvori web poveznicu" else "Podijeli poveznicu",
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    Spacer(Modifier.height(4.dp))
                    TextButton(onClick = onDiscard, enabled = !linkBusy) {
                        Text("Snimi novu poruku", color = Violet)
                    }
                }
            }
            Spacer(Modifier.height(if (compact) 8.dp else 14.dp))
        }
    }
}

