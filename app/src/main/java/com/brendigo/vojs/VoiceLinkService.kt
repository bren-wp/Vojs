package com.brendigo.vojs

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri

/**
 * Native share helpers. No network upload implementation or stored audio links.
 */
internal object VoiceLinkService {
    fun isValid(url: String): Boolean {
        val uri = Uri.parse(url)
        return uri.scheme == "https" && uri.host == "vojs.com.hr" &&
            uri.path == "/poruka/" &&
            uri.getQueryParameter("id")?.matches(Regex("[a-f0-9]{48}")) == true &&
            uri.queryParameterNames == setOf("id") &&
            uri.fragment == null
    }

    fun copy(context: Context, url: String) {
        require(isValid(url))
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText("VOJS P2P poveznica", url))
    }

    fun share(context: Context, url: String) {
        require(isValid(url))
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, url)
        }
        context.startActivity(Intent.createChooser(intent, "Podijeli VOJS P2P poveznicu"))
    }
}
