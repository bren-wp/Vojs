package com.brendigo.vojs

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.io.IOException
import java.net.URL
import java.nio.charset.StandardCharsets
import java.util.UUID
import javax.net.ssl.HttpsURLConnection

internal data class VojsShareLink(val id: String, val url: String, val deleteToken: String)

/** Opt-in only; local recording/playback and file sharing never invoke this client. */
internal object VoiceLinkService {
    private const val HOST = "vojs.com.hr"
    private const val MAX_BYTES = 8L * 1024L * 1024L
    private const val MAX_RESPONSE_BYTES = 8192

    suspend fun create(file: File): VojsShareLink = withContext(Dispatchers.IO) {
        if (!file.isFile || file.length() !in 64L..MAX_BYTES) {
            throw IOException("Invalid audio size")
        }
        val boundary = "Vojs" + UUID.randomUUID().toString().replace("-", "")
        val connection = open("/api/upload.php").apply {
            requestMethod = "POST"
            doOutput = true
            setRequestProperty("Content-Type", "multipart/form-data; boundary=$boundary")
            chunkedStreamingMode = 8192
        }
        try {
            connection.outputStream.use { output ->
                output.write(("--$boundary\r\n" +
                    "Content-Disposition: form-data; name=\"audio\"; filename=\"vojs.m4a\"\r\n" +
                    "Content-Type: audio/mp4\r\n\r\n").toByteArray(StandardCharsets.UTF_8))
                file.inputStream().use { input -> input.copyTo(output, 8192) }
                output.write("\r\n--$boundary--\r\n".toByteArray(StandardCharsets.UTF_8))
            }
            if (connection.responseCode != 201) throw IOException("Upload rejected")
            val payload = connection.inputStream.use { stream ->
                val bytes = stream.readNBytes(MAX_RESPONSE_BYTES + 1)
                if (bytes.size > MAX_RESPONSE_BYTES) throw IOException("Oversized response")
                String(bytes, StandardCharsets.UTF_8)
            }
            val json = JSONObject(payload)
            val id = json.getString("id")
            val url = json.getString("url")
            val delete = json.getString("delete_token")
            val uri = Uri.parse(url)
            if (!id.matches(Regex("[a-f0-9]{48}")) ||
                !delete.matches(Regex("[a-f0-9]{64}")) ||
                uri.scheme != "https" || uri.host != HOST ||
                uri.path != "/poruka/" || uri.getQueryParameter("id") != id) {
                throw IOException("Invalid share response")
            }
            VojsShareLink(id, url, delete)
        } finally {
            connection.disconnect()
        }
    }

    suspend fun revoke(link: VojsShareLink) = withContext(Dispatchers.IO) {
        val connection = open("/api/delete.php").apply {
            requestMethod = "POST"
            doOutput = true
            setRequestProperty("Content-Type", "application/json; charset=utf-8")
        }
        try {
            connection.outputStream.use {
                it.write(JSONObject().put("id", link.id)
                    .put("delete_token", link.deleteToken)
                    .toString().toByteArray(StandardCharsets.UTF_8))
            }
            if (connection.responseCode != 204) throw IOException("Deletion failed")
        } finally {
            connection.disconnect()
        }
    }

    private fun open(path: String): HttpsURLConnection =
        (URL("https://$HOST$path").openConnection() as HttpsURLConnection).apply {
            connectTimeout = 15_000
            readTimeout = 45_000
            instanceFollowRedirects = false
            useCaches = false
            setRequestProperty("Accept", "application/json")
        }

    fun copy(context: Context, url: String) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText("VOJS poveznica", url))
    }

    fun share(context: Context, url: String) {
        val send = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, url)
        }
        context.startActivity(Intent.createChooser(send, "Podijeli poveznicu"))
    }
}
