package com.brendigo.vojs

import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.os.Build
import android.os.SystemClock
import android.os.Handler
import android.os.Looper
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.FileProvider
import java.io.File
import kotlin.math.sqrt

internal enum class RecordingStage { IDLE, RECORDING, READY }

/**
 * One local recording, never a user library and never an upload.
 *
 * Android's Sharesheet requires a temporary file-backed content URI.
 * Shared audio is kept in private cache for at most ~24 h after the next launch,
 * because receivers may read it after the chooser closes.
 */
internal class VoiceRecorder(private val context: Context) {
    companion object {
        const val MAX_DURATION_MS = 300_000L
        private const val CACHE_RETENTION_MS = 86_400_000L
        private const val MAX_WAVEFORM_SAMPLES = 43
    }

    var stage by mutableStateOf(RecordingStage.IDLE)
        private set
    var paused by mutableStateOf(false)
        private set
    var playing by mutableStateOf(false)
        private set
    var elapsedMs by mutableLongStateOf(0L)
        private set
    var level by mutableFloatStateOf(0.1f)
        private set
    var error by mutableStateOf<String?>(null)
        private set
    val waveform = mutableStateListOf<Float>()

    private var recorder: MediaRecorder? = null
    private var player: MediaPlayer? = null
    private var workingFile: File? = null
    private var startedAt = 0L
    private var accumulatedMs = 0L
    private var shared = false
    private val mainHandler = Handler(Looper.getMainLooper())

    init {
        repeat(MAX_WAVEFORM_SAMPLES) { waveform.add(0.12f) }
        runCatching { cleanupExpired() }
    }

    private fun directory(): File = File(context.cacheDir, "shared_audio").apply { mkdirs() }

    private fun cleanupExpired() {
        val now = System.currentTimeMillis()
        directory().listFiles()?.forEach { file ->
            if (file.isFile && file.extension == "m4a" &&
                now - file.lastModified() > CACHE_RETENTION_MS
            ) file.delete()
        }
    }

    @Suppress("DEPRECATION")
    fun start(): Boolean {
        if (stage == RecordingStage.RECORDING) return false
        discardCurrent()
        val audio = try {
            File(directory(), "vojs-${System.currentTimeMillis()}.m4a")
        } catch (_: Exception) {
            error = "Nije moguće pristupiti privremenoj pohrani uređaja."
            return false
        }
        try {
            val candidate = if (Build.VERSION.SDK_INT >= 31) MediaRecorder(context) else MediaRecorder()
            recorder = candidate
            candidate.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setAudioEncodingBitRate(96_000)
                setAudioSamplingRate(44_100)
                setMaxDuration(MAX_DURATION_MS.toInt())
                setOnInfoListener { _, what, _ ->
                    if (what == MediaRecorder.MEDIA_RECORDER_INFO_MAX_DURATION_REACHED) {
                        mainHandler.post { finish() }
                    }
                }
                setOnErrorListener { _, _, _ ->
                    mainHandler.post {
                        if (stage == RecordingStage.RECORDING) {
                            finish()
                            error = "Mikrofon je prekinuo snimanje. Pokušaj ponovno."
                        }
                    }
                }
                setOutputFile(audio.absolutePath)
                prepare()
                start()
            }
            workingFile = audio
            shared = false
            accumulatedMs = 0L
            startedAt = SystemClock.elapsedRealtime()
            elapsedMs = 0L
            level = 0.1f
            waveform.clear()
            repeat(MAX_WAVEFORM_SAMPLES) { waveform.add(0.12f) }
            paused = false
            stage = RecordingStage.RECORDING
            error = null
            return true
        } catch (_: Exception) {
            recorder?.runCatching { reset() }
            recorder?.runCatching { release() }
            recorder = null
            audio.delete()
            workingFile = null
            stage = RecordingStage.IDLE
            error = "Snimanje nije uspjelo. Provjeri mikrofon i pokušaj ponovno."
            return false
        }
    }

    fun tick() {
        if (stage != RecordingStage.RECORDING || paused) return
        elapsedMs = (accumulatedMs + SystemClock.elapsedRealtime() - startedAt)
            .coerceIn(0L, MAX_DURATION_MS)
        val amplitude = runCatching { recorder?.maxAmplitude ?: 0 }.getOrDefault(0)
        level = (sqrt(amplitude.toFloat() / 32767f)).coerceIn(0.09f, 1f)
        if (waveform.size >= MAX_WAVEFORM_SAMPLES) waveform.removeAt(0)
        waveform.add(level)
        if (elapsedMs >= MAX_DURATION_MS) finish()
    }

    fun togglePause() {
        if (stage != RecordingStage.RECORDING) return
        try {
            if (paused) {
                recorder?.resume()
                startedAt = SystemClock.elapsedRealtime()
                paused = false
            } else {
                accumulatedMs += SystemClock.elapsedRealtime() - startedAt
                recorder?.pause()
                elapsedMs = accumulatedMs.coerceAtMost(MAX_DURATION_MS)
                paused = true
            }
        } catch (_: RuntimeException) {
            finish()
            error = "Nije moguće pauzirati snimanje na ovom uređaju."
        }
    }

    fun finish() {
        if (stage != RecordingStage.RECORDING) return
        if (!paused) {
            accumulatedMs += SystemClock.elapsedRealtime() - startedAt
            elapsedMs = accumulatedMs.coerceAtMost(MAX_DURATION_MS)
        }
        // Transition before stop() so max-duration callbacks cannot re-enter.
        stage = RecordingStage.IDLE
        paused = false
        val active = recorder
        recorder = null
        val valid = runCatching { active?.stop(); active != null }.getOrDefault(false)
        active?.runCatching { release() }
        val audio = workingFile
        if (valid && elapsedMs >= 600L && audio?.exists() == true && audio.length() > 0L) {
            stage = RecordingStage.READY
            error = null
        } else {
            audio?.delete()
            workingFile = null
            elapsedMs = 0L
            error = "Snimka je prekratka ili je snimanje prekinuto. Pokušaj ponovno."
        }
    }

    fun togglePlayback() {
        if (playing) { stopPlayback(); return }
        val audio = workingFile?.takeIf { stage == RecordingStage.READY && it.exists() } ?: return
        try {
            val created = MediaPlayer()
            player = created
            created.apply {
                setDataSource(audio.absolutePath)
                setOnCompletionListener { stopPlayback() }
                setOnErrorListener { _, _, _ ->
                    stopPlayback()
                    error = "Reprodukcija nije uspjela."
                    true
                }
                prepare()
                start()
            }
            playing = true
            error = null
        } catch (_: Exception) {
            stopPlayback()
            error = "Nije moguće preslušati snimku."
        }
    }

    fun share() {
        val audio = workingFile?.takeIf {
            stage == RecordingStage.READY && it.exists() && it.length() > 0L
        } ?: return
        try {
            stopPlayback()
            val uri = FileProvider.getUriForFile(
                context, "${context.packageName}.fileprovider", audio
            )
            val send = Intent(Intent.ACTION_SEND).apply {
                type = "audio/mp4"
                putExtra(Intent.EXTRA_STREAM, uri)
                clipData = ClipData.newUri(context.contentResolver, "VOJS", uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            val chooser = Intent.createChooser(send, "Podijeli glasovnu poruku").apply {
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(chooser)
            shared = true
            error = null
        } catch (_: Exception) {
            error = "Dijeljenje nije uspjelo. Provjeri dostupne aplikacije."
        }
    }

    fun showError(message: String) { error = message }

    fun discardCurrent() {
        if (stage == RecordingStage.RECORDING) finish()
        stopPlayback()
        // A receiver may still be fetching a shared URI: keep that temp file.
        if (!shared) workingFile?.delete()
        workingFile = null
        shared = false
        stage = RecordingStage.IDLE
        paused = false
        elapsedMs = 0L
        accumulatedMs = 0L
        level = 0.1f
        error = null
    }

    fun stopPlayback() {
        val active = player
        player = null
        playing = false
        active?.runCatching { release() }
    }

    fun onBackground() {
        // Recording never continues clandestinely after the app leaves foreground.
        if (stage == RecordingStage.RECORDING) finish()
        stopPlayback()
    }

    fun close() {
        onBackground()
        recorder?.runCatching { release() }
        recorder = null
    }
}
