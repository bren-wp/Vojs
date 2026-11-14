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

private val Violet = Color(0xFF7868FF)
private val VioletDark = Color(0xFF5145CB)
private val Ink = Color(0xFF20213B)
private val Muted = Color(0xFF777783)
private val Paper = Color(0xFFFFFFFF)
private val Night = Color(0xFF20213B)

@OptIn(ExperimentalMaterial3Api::class)
class MainActivity : ComponentActivity() {
    private val voice by lazy { VoiceRecorder(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            var appearance by rememberSaveable {
                mutableStateOf(
                    getPreferences(0).getString("vojs_appearance", "system")
                        ?.takeIf { it in setOf("system", "light", "dark") } ?: "system"
                )
            }
            val dark = when (appearance) {
                "dark" -> true
                "light" -> false
                else -> isSystemInDarkTheme()
            }
            var showSettings by remember { mutableStateOf(false) }
            var brandedSplash by remember { mutableStateOf(true) }
            LaunchedEffect(Unit) {
                // 2.5 seconds: the complete, genuine VOJS splash is readable.
                delay(2500L)
                brandedSplash = false
            }
            SideEffect {
                val insets = WindowCompat.getInsetsController(window, window.decorView)
                insets.isAppearanceLightStatusBars = !dark
                insets.isAppearanceLightNavigationBars = !dark
                // Preserve transparent edge-to-edge system bars; never enter
                // immersive mode, which triggers Android's blocking fullscreen
                // onboarding prompt on a fresh device.
                insets.show(WindowInsetsCompat.Type.systemBars())
            }
            val permissionRequest = rememberLauncherForActivityResult(
                ActivityResultContracts.RequestPermission()
            ) { allowed ->
                if (allowed) voice.start()
                else voice.errorMessage("Dopusti korištenje mikrofona u postavkama uređaja.")
            }
            LaunchedEffect(voice.stage, voice.paused) {
                while (voice.stage == RecordingStage.RECORDING && !voice.paused) {
                    voice.tick()
                    delay(120)
                }
            }
            val foreground = if (dark) Paper else Ink
            val background = if (dark) Night else Paper
            val secondary = if (dark) Color(0xFFB3B0C8) else Muted
            MaterialTheme(
                colorScheme = if (dark) darkColorScheme(
                    primary = Violet, background = Night, surface = Night,
                    onBackground = Paper, onSurface = Paper
                ) else lightColorScheme(
                    primary = Violet, background = Paper, surface = Paper,
                    onBackground = Ink, onSurface = Ink
                )
            ) {
                val requestStart = {
                    if (ContextCompat.checkSelfPermission(
                            this@MainActivity, Manifest.permission.RECORD_AUDIO
                        ) == PackageManager.PERMISSION_GRANTED
                    ) voice.start()
                    else permissionRequest.launch(Manifest.permission.RECORD_AUDIO)
                    Unit
                }
                if (brandedSplash) {
                    VojsBrandedSplash(dark = dark)
                } else {
                VoiceScreen(
                    recorder = voice,
                    dark = dark,
                    foreground = foreground,
                    background = background,
                    secondary = secondary,
                    onSettings = { showSettings = true },
                    onRecord = requestStart
                )
                if (showSettings) {
                    ModalBottomSheet(onDismissRequest = { showSettings = false }) {
                        Column(
                            Modifier.fillMaxWidth().padding(horizontal = 25.dp)
                                .padding(bottom = 36.dp)
                        ) {
                            Text("Postavke", style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold)
                            Spacer(Modifier.height(14.dp))
                            Text("Izgled aplikacije", color = secondary)
                            listOf("system" to "Prema postavkama uređaja",
                                   "light" to "Svijetla tema",
                                   "dark" to "Tamna tema").forEach { (key, title) ->
                                Row(
                                    Modifier.fillMaxWidth().clickable {
                                        appearance = key
                                        getPreferences(0).edit()
                                            .putString("vojs_appearance", key).apply()
                                    }
                                        .padding(vertical = 13.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    RadioButton(selected = appearance == key,
                                        onClick = {
                                            appearance = key
                                            getPreferences(0).edit()
                                                .putString("vojs_appearance", key).apply()
                                        })
                                    Spacer(Modifier.width(10.dp))
                                    Text(title)
                                }
                            }
                            HorizontalDivider()
                            Spacer(Modifier.height(18.dp))
                            Text("Privatnost", fontWeight = FontWeight.SemiBold)
                            Spacer(Modifier.height(8.dp))
                            Text(
                                "Snimke se privremeno čuvaju na uređaju radi dijeljenja. " +
                                "Odabrana aplikacija može spremiti svoju kopiju.",
                                color = secondary, fontSize = 13.sp, lineHeight = 19.sp
                            )
                        }
                    }
                }
                }
            }
        }
    }

    override fun onStop() {
        super.onStop()
        voice.onBackground()
    }

    override fun onDestroy() {
        voice.close()
        super.onDestroy()
    }
}

private fun VoiceRecorder.errorMessage(text: String) = showError(text)

@Composable
private fun VoiceScreen(
    recorder: VoiceRecorder,
    dark: Boolean,
    foreground: Color,
    background: Color,
    secondary: Color,
    onSettings: () -> Unit,
    onRecord: () -> Unit
) {
    val compact = androidx.compose.ui.platform.LocalConfiguration.current.screenHeightDp < 710
    val stage = recorder.stage
    Column(
        Modifier.fillMaxSize().background(background).safeDrawingPadding()
    ) {
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState())
                .fillMaxWidth().padding(horizontal = 24.dp)
                .padding(top = if (compact) 13.dp else 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    if (stage == RecordingStage.READY) "Poruka je spremna" else "Nova poruka",
                    color = foreground, fontSize = if (compact) 25.sp else 28.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                Image(
                    painter = painterResource(
                        if (dark) R.drawable.vojs_logo_dark else R.drawable.vojs_logo_light
                    ),
                    contentDescription = "VOJS",
                    modifier = Modifier.width(102.dp).height(40.dp),
                    contentScale = ContentScale.Fit
                )
                TextButton(onClick = onSettings,
                    modifier = Modifier.semantics {
                        contentDescription = "Postavke aplikacije"
                    }) {
                    Text("⚙", fontSize = 21.sp, color = Violet)
                }
            }
            Spacer(Modifier.height(if (compact) 20.dp else 40.dp))
            Text(
                when (stage) {
                    RecordingStage.IDLE -> "Dodirni gumb za snimanje"
                    RecordingStage.RECORDING ->
                        if (recorder.paused) "Snimanje je pauzirano" else ""
                    RecordingStage.READY -> "Preslušaj pa podijeli"
                },
                color = secondary, fontSize = 15.sp, textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(if (compact) 12.dp else 26.dp))
            VoiceWaveform(
                values = recorder.waveform.toList(),
                active = stage == RecordingStage.RECORDING && !recorder.paused,
                color = if (dark) Color(0xFF978BFF) else Violet,
                modifier = Modifier.fillMaxWidth()
                    .height(if (compact) 135.dp else 180.dp)
            )
            Spacer(Modifier.height(if (compact) 16.dp else 29.dp))
            Text(
                formatTime(recorder.elapsedMs),
                color = foreground, fontSize = if (compact) 49.sp else 62.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-2).sp
            )
            Text(
                if (stage == RecordingStage.READY) "glasovna poruka"
                else "najviše 05:00",
                color = secondary, fontSize = 15.sp
            )
            if (stage == RecordingStage.READY) {
                Spacer(Modifier.height(29.dp))
                OutlinedButton(
                    onClick = recorder::togglePlayback,
                    shape = RoundedCornerShape(17.dp),
                    modifier = Modifier.fillMaxWidth().height(53.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Violet)
                ) {
                    Text(if (recorder.playing) "■  Zaustavi" else "▶  Preslušaj",
                        fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
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
                    Spacer(Modifier.height(7.dp))
                    TextButton(onClick = recorder::togglePause) {
                        Text(if (recorder.paused) "▶ Nastavi snimanje" else "Ⅱ Pauziraj",
                            color = Violet, fontSize = 14.sp)
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
                    Spacer(Modifier.height(4.dp))
                    TextButton(onClick = recorder::discardCurrent) {
                        Text("Snimi novu poruku", color = Violet)
                    }
                }
            }
            Spacer(Modifier.height(if (compact) 8.dp else 14.dp))
        }
    }
}

@Composable
private fun VoiceWaveform(
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
        val reference = floatArrayOf(
            .28f, .46f, .63f, .42f, .76f, 1.00f, .56f, .78f,
            1.00f, .60f, .48f, .91f, .66f, .98f, .40f, .62f,
            .87f, .56f, .95f, .64f, .30f, .74f
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
            val height = (.18f + reference[i] * .82f) *
                size.height * .83f * dynamicScale
            val x = (i + 1) * step
            drawLine(
                color, Offset(x, mid - height / 2f),
                Offset(x, mid + height / 2f),
                strokeWidth = cap, cap = StrokeCap.Round
            )
        }
    }
}

private fun formatTime(ms: Long): String {
    val seconds = (ms / 1000).coerceIn(0L, 300L)
    return "%02d:%02d".format(seconds / 60, seconds % 60)
}
