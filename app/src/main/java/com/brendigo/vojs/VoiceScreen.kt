package com.brendigo.vojs

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
internal fun VoiceScreen(
    recorder: VoiceRecorder,
    dark: Boolean,
    foreground: Color,
    background: Color,
    secondary: Color,
    onSettings: () -> Unit,
    onRecord: () -> Unit,
    onCreateLink: () -> Unit,
    onDiscard: () -> Unit
) {
    val config = androidx.compose.ui.platform.LocalConfiguration.current
    // A 1.25x font on Pixel 2 needs the compact layout even above 710dp.
    // Keep the original roomy geometry at normal font scaling.
    val compact = config.screenHeightDp < 710 ||
        (config.fontScale > 1.15f && config.screenHeightDp < 850)
    val stage = recorder.stage
    Box(
        modifier = Modifier.fillMaxSize().background(background),
        contentAlignment = Alignment.TopCenter
    ) {
    Column(
        Modifier.widthIn(max = 620.dp).fillMaxWidth().fillMaxHeight()
            .safeDrawingPadding()
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
            VojsHeader(dark = dark, onSettings = onSettings)
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
                    if (compact)
                        "Offline poruka ostaje lokalno. P2P zahtijeva oba uređaja online; VOJS ne pohranjuje zvuk."
                    else
                        "Datoteka se može podijeliti offline. P2P poveznica služi za izravni " +
                        "prijenos u preglednik bez spremanja audija na VOJS server. " +
                        "Oba uređaja moraju biti online dok prijenos traje.",
                    color = secondary, fontSize = 13.sp, lineHeight = 19.sp
                )
                Spacer(Modifier.height(12.dp))
                if (!compact) {
                    VojsPlaybackButton(recorder)
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
                    modifier = Modifier.fillMaxWidth(if (compact) 0.92f else 0.84f)
                        .padding(horizontal = 8.dp)
                        .height(if (compact) 164.dp else 210.dp)
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
                    // Keep the play action visible on short / large-text screens.
                    // Long description and metadata remain scrollable above it.
                    if (compact) {
                        VojsPlaybackButton(recorder)
                        Spacer(Modifier.height(8.dp))
                    }
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
                        onClick = onCreateLink,
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        shape = RoundedCornerShape(17.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Violet)
                    ) {
                        Text("Podijeli izravno (P2P)", fontWeight = FontWeight.SemiBold)
                    }
                    Spacer(Modifier.height(4.dp))
                    TextButton(onClick = onDiscard) {
                        Text("Snimi novu poruku", color = Violet)
                    }
                }
            }
            Spacer(Modifier.height(if (compact) 8.dp else 14.dp))
        }
    }
    }
}
