package com.brendigo.vojs

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.graphics.Color
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import kotlinx.coroutines.delay

internal val Violet = Color(0xFF7868FF)
internal val VioletDark = Color(0xFF5145CB)
internal val Ink = Color(0xFF20213B)
internal val Muted = Color(0xFF777783)
internal val Paper = Color(0xFFFFFFFF)
internal val Night = Color(0xFF20213B)

class MainActivity : ComponentActivity() {
    private val voice by lazy { VoiceRecorder(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            var appearance by rememberSaveable {
                mutableStateOf(VojsAppearance.load(this@MainActivity))
            }
            val dark = VojsAppearance.isDark(appearance, isSystemInDarkTheme())
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
                    onRecord = requestStart,
                    onCreateLink = {
                        val audio = voice.currentAudioForTransfer()
                        if (audio == null) {
                            voice.showError("Snimka više nije dostupna.")
                        } else {
                            startActivity(
                                android.content.Intent(this@MainActivity, PeerSenderActivity::class.java)
                                    .putExtra(PeerSenderActivity.EXTRA_AUDIO_PATH, audio.absolutePath)
                            )
                        }
                    },
                    onDiscard = { voice.discardCurrent() }
                )
                if (showSettings) {
                    VojsSettingsSheet(
                        appearance = appearance,
                        dark = dark,
                        secondary = secondary,
                        onDismiss = { showSettings = false },
                        onAppearanceChange = { value ->
                            appearance = value
                            VojsAppearance.save(this@MainActivity, value)
                        },
                        onPrivacy = {
                            startActivity(
                                android.content.Intent(
                                    android.content.Intent.ACTION_VIEW,
                                    android.net.Uri.parse("https://vojs.com.hr/privatnost/")
                                )
                            )
                        }
                    )
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

