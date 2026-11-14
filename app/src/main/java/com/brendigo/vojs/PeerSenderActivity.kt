package com.brendigo.vojs

import android.annotation.SuppressLint
import android.os.Bundle
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import android.util.Base64
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import java.io.File
import java.io.FileInputStream
import org.json.JSONTokener

/**
 * A local, packaged sender page at a whitelisted HTTPS origin.
 * Audio bytes are copied ONLY into this on-device WebView and transmitted
 * directly to a WebRTC recipient, never to PHP.
 *
 * This activity is not exported and uses no JavaScriptInterface.
 */
class PeerSenderActivity : ComponentActivity() {
    companion object {
        const val EXTRA_AUDIO_PATH = "audio_path"
        private const val HOST = "vojs.com.hr"
        private const val SENDER_PATH = "/p2p/android-sender/"
        private const val CHUNK = 12 * 1024
        private const val MAX_FILE = 8L * 1024L * 1024L
    }
    private var web: WebView? = null
    private var currentInput: FileInputStream? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val root = File(cacheDir, "shared_audio").canonicalFile
        val path = intent.getStringExtra(EXTRA_AUDIO_PATH)
        val audio = path?.let { File(it).canonicalFile }?.takeIf {
            it.parentFile == root && it.extension.equals("m4a", true) &&
                it.isFile && it.length() in 64L..MAX_FILE
        }
        if (audio == null) {
            Toast.makeText(this, "Snimka više nije dostupna.", Toast.LENGTH_LONG).show()
            finish()
            return
        }
        setContent {
            var browserReady by remember { mutableStateOf(false) }
            MaterialTheme(colorScheme = darkColorScheme(primary = Violet)) {
                Column(Modifier.fillMaxSize()) {
                    Row(
                        Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(onClick = { finish() }) { Text("Natrag") }
                        Button(onClick = { withLink { VoiceLinkService.copy(this@PeerSenderActivity, it) } },
                            enabled = browserReady) { Text("Kopiraj link") }
                        Button(onClick = { withLink { VoiceLinkService.share(this@PeerSenderActivity, it) } },
                            enabled = browserReady) { Text("Podijeli") }
                    }
                    AndroidView(
                        modifier = Modifier.fillMaxSize(),
                        factory = { context ->
                            configureWebView(context, audio).also {
                                web = it
                                browserReady = true
                                it.loadUrl("https://$HOST$SENDER_PATH")
                            }
                        }
                    )
                }
            }
        }
    }

    private fun withLink(action: (String) -> Unit) {
        web?.evaluateJavascript(
            "(document.getElementById('link')?.value || '')"
        ) { result ->
            val url = try { JSONTokener(result).nextValue() as? String } catch (_: Exception) { null }
            if (url != null && VoiceLinkService.isValid(url)) {
                action(url)
            } else {
                Toast.makeText(this, "Najprije stvori poveznicu.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun configureWebView(context: android.content.Context, audio: File): WebView {
        val browser = WebView(context)
        browser.settings.apply {
            javaScriptEnabled = true
            allowFileAccess = false
            allowContentAccess = false
            javaScriptCanOpenWindowsAutomatically = false
            domStorageEnabled = false
            mixedContentMode = android.webkit.WebSettings.MIXED_CONTENT_NEVER_ALLOW
            cacheMode = android.webkit.WebSettings.LOAD_NO_CACHE
        }
        browser.webViewClient = object : WebViewClient() {
            override fun shouldInterceptRequest(view: WebView?, request: WebResourceRequest?): WebResourceResponse? {
                val uri = request?.url ?: return null
                if (uri.scheme == "https" && uri.host == HOST && uri.path == SENDER_PATH) {
                    return WebResourceResponse(
                        "text/html", "UTF-8",
                        assets.open("p2p-android-sender.html")
                    ).apply {
                        responseHeaders = mapOf(
                            "Cache-Control" to "no-store",
                            "X-Content-Type-Options" to "nosniff",
                            "Content-Security-Policy" to
                                "default-src 'none'; script-src 'unsafe-inline' https://vojs.com.hr; " +
                                "style-src 'unsafe-inline'; connect-src https://vojs.com.hr"
                        )
                    }
                }
                return null
            }
            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                val uri = request?.url ?: return true
                return !(uri.scheme == "https" && uri.host == HOST && uri.path == SENDER_PATH)
            }
            override fun onPageFinished(view: WebView?, url: String?) {
                if (view != null && url == "https://$HOST$SENDER_PATH" && !isFinishing) {
                    streamIntoWebView(view, audio)
                }
            }
        }
        return browser
    }

    private fun streamIntoWebView(browser: WebView, audio: File) {
        currentInput?.close()
        val input = try { FileInputStream(audio) } catch (_: Exception) { return }
        currentInput = input
        fun next() {
            if (isFinishing || isDestroyed) { input.close(); currentInput = null; return }
            val buffer = ByteArray(CHUNK)
            val read = try { input.read(buffer) } catch (_: Exception) { -1 }
            if (read <= 0) {
                input.close(); currentInput = null
                browser.evaluateJavascript("window.vojsAndroidReceiveChunk('', true)", null)
                return
            }
            val value = Base64.encodeToString(buffer, 0, read, Base64.NO_WRAP)
            val js = "window.vojsAndroidReceiveChunk(" + org.json.JSONObject.quote(value) + ", false)"
            browser.evaluateJavascript(js) { browser.post { next() } }
        }
        next()
    }

    override fun onDestroy() {
        currentInput?.close()
        currentInput = null
        web?.loadUrl("about:blank")
        web?.destroy()
        web = null
        super.onDestroy()
    }
}
