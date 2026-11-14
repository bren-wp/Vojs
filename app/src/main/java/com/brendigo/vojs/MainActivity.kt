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
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.sin

internal val Violet = Color(0xFF7868FF)
internal val VioletDark = Color(0xFF5145CB)
internal val Ink = Color(0xFF20213B)
internal val Muted = Color(0xFF777783)
internal val Paper = Color(0xFFFFFFFF)
internal val Night = Color(0xFF20213B)

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
                    getPreferences(0).getString("vojs_appearance", "dark")
                        ?.takeIf { it in setOf("system", "light", "dark") } ?: "dark"
                )
            }
            val dark = when (appearance) {
                "dark" -> true
                "light" -> false
                else -> isSystemInDarkTheme()
            }
            var showSettings by remember { mutableStateOf(false) }
            val scope = rememberCoroutineScope()
            var shareLink by remember { mutableStateOf<VojsShareLink?>(null) }
            var linkBusy by remember { mutableStateOf(false) }
            var linkError by remember { mutableStateOf<String?>(null) }
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
                    onRecord = requestStart,
                    shareLink = shareLink,
                    linkBusy = linkBusy,
                    linkError = linkError,
                    onCreateLink = {
                        if (!linkBusy) {
                            val file = voice.currentAudioForUpload()
                            if (file == null) {
                                linkError = "Snimka više nije dostupna."
                            } else {
                                linkBusy = true
                                linkError = null
                                scope.launch {
                                    try {
                                        shareLink = VoiceLinkService.create(file)
                                    } catch (_: Exception) {
                                        linkError = "Poveznica nije stvorena. Provjeri internet i dostupnost poslužitelja."
                                    } finally {
                                        linkBusy = false
                                    }
                                }
                            }
                        }
                    },
                    onCopyLink = {
                        shareLink?.let { VoiceLinkService.copy(this@MainActivity, it.url) }
                    },
                    onShareLink = {
                        shareLink?.let { VoiceLinkService.share(this@MainActivity, it.url) }
                    },
                    onRevokeLink = {
                        val old = shareLink
                        if (old != null && !linkBusy) {
                            linkBusy = true
                            linkError = null
                            scope.launch {
                                try {
                                    VoiceLinkService.revoke(old)
                                    shareLink = null
                                } catch (_: Exception) {
                                    linkError = "Brisanje nije uspjelo. Pokušaj ponovno dok poveznica ne istekne."
                                } finally {
                                    linkBusy = false
                                }
                            }
                        }
                    },
                    onDiscard = {
                        voice.discardCurrent()
                        shareLink = null
                        linkError = null
                    }
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
                                "Snimke nastaju lokalno i mogu se dijeliti offline. " +
                                "Samo ako odabereš stvaranje poveznice, snimka se šalje na " +
                                "vojs.com.hr, dostupna je posjednicima poveznice do 24 sata " +
                                "i može se opozvati tijekom ove sesije.",
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

