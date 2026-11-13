package com.brendigo.vojs

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import kotlinx.coroutines.delay
import java.io.File

private val Purple = Color(0xFF7868FF)
private val DarkPurple = Color(0xFF5145CB)
private val Ink = Color(0xFF20213B)
private val Light = Color(0xFFF7F6FF)
private const val MAX_SECONDS = 300

class MainActivity : ComponentActivity() {
    private var recorder: MediaRecorder? = null
    private var player: MediaPlayer? = null
    private var file: File? = null
    private var recording by mutableStateOf(false)
    private var paused by mutableStateOf(false)
    private var playing by mutableStateOf(false)
    private var elapsed by mutableIntStateOf(0)
    private var message by mutableStateOf("")
    private var finished by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val directory = File(cacheDir, "shared_audio").apply { mkdirs() }
        // Keep active shares readable; remove older temporary exports on a later launch.
        directory.listFiles()?.filter { System.currentTimeMillis() - it.lastModified() > 24L * 3600 * 1000 }?.forEach { it.delete() }
        setContent {
            val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
                if (granted) startRecord() else message = "Za snimanje je potrebno dopuštenje za mikrofon."
            }
            LaunchedEffect(recording, paused) {
                while (recording && !paused) {
                    delay(1000)
                    if (recording && !paused) {
                        elapsed += 1
                        if (elapsed >= MAX_SECONDS) stopRecord()
                    }
                }
            }
            MaterialTheme(colorScheme = lightColorScheme(primary = Purple, background = Light, surface = Color.White)) {
                Column(
                    Modifier.fillMaxSize().background(Light).verticalScroll(rememberScrollState())
                        .padding(horizontal = 24.dp, vertical = 28.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("VOJS", fontSize = 32.sp, fontWeight = FontWeight.ExtraBold, color = Ink)
                    Spacer(Modifier.height(6.dp))
                    Text("Neke stvari bolje zvuče.", fontSize = 15.sp, color = DarkPurple)
                    Spacer(Modifier.height(54.dp))
                    Box(Modifier.size(178.dp).background(Purple, CircleShape), contentAlignment = Alignment.Center) {
                        Text(if (recording) "●" else if (finished) "▶" else "♫", fontSize = 80.sp, color = Color.White)
                    }
                    Spacer(Modifier.height(34.dp))
                    Text(if (recording) "Snimanje poruke" else if (finished) "Poruka je spremna" else "Tvoj glas. Tvoja poruka.",
                        fontSize = 24.sp, color = Ink, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                    Spacer(Modifier.height(12.dp))
                    Text("%02d:%02d / 05:00".format(elapsed / 60, elapsed % 60), fontSize = 24.sp, color = DarkPurple)
                    Spacer(Modifier.height(24.dp))
                    if (!recording && !finished) {
                        Button(
                            onClick = {
                                if (ContextCompat.checkSelfPermission(this@MainActivity, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) startRecord()
                                else permission.launch(Manifest.permission.RECORD_AUDIO)
                            },
                            modifier = Modifier.fillMaxWidth().height(56.dp),
                            shape = RoundedCornerShape(18.dp)
                        ) { Text("●  Započni snimanje") }
                    }
                    if (recording) {
                        Button(onClick = { stopRecord() }, modifier = Modifier.fillMaxWidth().height(56.dp)) { Text("■  Završi snimanje") }
                        Spacer(Modifier.height(12.dp))
                        OutlinedButton(onClick = { togglePause() }, modifier = Modifier.fillMaxWidth()) {
                            Text(if (paused) "▶  Nastavi" else "Ⅱ  Pauziraj")
                        }
                    }
                    if (finished) {
                        Button(onClick = { share() }, modifier = Modifier.fillMaxWidth().height(56.dp)) { Text("↗  Podijeli glasovnu poruku") }
                        Spacer(Modifier.height(12.dp))
                        OutlinedButton(onClick = { playOrStop() }, modifier = Modifier.fillMaxWidth()) { Text(if (playing) "■  Zaustavi" else "▶  Preslušaj") }
                        TextButton(onClick = { clear() }) { Text("Snimi novu poruku") }
                    }
                    Spacer(Modifier.height(22.dp))
                    if (message.isNotEmpty()) Text(message, color = MaterialTheme.colorScheme.error, textAlign = TextAlign.Center)
                    Spacer(Modifier.height(22.dp))
                    Text("Bez računa • Bez VOJS servera • Bez oglasa", fontSize = 12.sp, color = Ink, textAlign = TextAlign.Center)
                    Spacer(Modifier.height(6.dp))
                    Text("Snimke se privremeno čuvaju samo na uređaju radi dijeljenja. Primateljska aplikacija može pohraniti vlastitu kopiju.",
                        fontSize = 12.sp, color = Ink.copy(alpha = 0.7f), textAlign = TextAlign.Center)
                    Spacer(Modifier.height(30.dp))
                    Text("BRENDIGO · brendigo.com", fontSize = 11.sp, color = DarkPurple)
                }
            }
        }
    }

    @Suppress("DEPRECATION")
    private fun startRecord() {
        clear()
        try {
            file = File(File(cacheDir, "shared_audio").apply { mkdirs() }, "vojs-${System.currentTimeMillis()}.m4a")
            recorder = if (Build.VERSION.SDK_INT >= 31) MediaRecorder(this) else MediaRecorder()
            recorder!!.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setAudioEncodingBitRate(128000)
                setAudioSamplingRate(44100)
                setMaxDuration(MAX_SECONDS * 1000)
                setOutputFile(file!!.absolutePath)
                prepare()
                start()
            }
            recording = true
            elapsed = 0
        } catch (e: Exception) {
            message = "Snimanje nije uspjelo: ${e.localizedMessage ?: "nepoznata pogreška"}"
            recorder?.runCatching { release() }
            recorder = null
            recording = false
        }
    }
    private fun togglePause() {
        try {
            if (paused) recorder?.resume() else recorder?.pause()
            paused = !paused
        } catch (e: Exception) { message = "Pauziranje nije uspjelo."; stopRecord() }
    }
    private fun stopRecord() {
        if (!recording) return
        recording = false
        paused = false
        val success = runCatching { recorder?.stop() }.isSuccess
        recorder?.runCatching { release() }
        recorder = null
        finished = success && file?.let { it.exists() && it.length() > 0 } == true
        if (!finished) {
            file?.delete()
            message = "Snimka nije uspjela. Pokušaj ponovno."
        }
    }
    private fun playOrStop() {
        if (playing) {
            player?.release(); player = null; playing = false
        } else {
            val audio = file ?: return
            try {
                player = MediaPlayer().apply {
                    setDataSource(audio.absolutePath)
                    setOnCompletionListener { it.release(); player = null; playing = false }
                    prepare(); start()
                }
                playing = true
            } catch (e: Exception) { player?.release(); player = null; message = "Reprodukcija nije uspjela." }
        }
    }
    private fun share() {
        val audio = file?.takeIf { it.exists() && it.length() > 0 } ?: return
        try {
            val uri = FileProvider.getUriForFile(this, "${packageName}.fileprovider", audio)
            val send = Intent(Intent.ACTION_SEND).apply {
                type = "audio/mp4"
                putExtra(Intent.EXTRA_STREAM, uri)
                clipData = android.content.ClipData.newUri(contentResolver, "VOJS", uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            startActivity(Intent.createChooser(send, "Podijeli putem"))
        } catch (e: Exception) { message = "Dijeljenje nije moguće na ovom uređaju." }
    }
    private fun clear() {
        if (recording) stopRecord()
        player?.runCatching { release() }; player = null; playing = false
        // Delete prior local working recording only when starting a new one;
        // Android receiver may already have copied the shared URI.
        file?.delete(); file = null
        finished = false; elapsed = 0; paused = false; message = ""
    }
    override fun onDestroy() {
        if (recording) stopRecord()
        player?.runCatching { release() }; player = null
        super.onDestroy()
    }
}
