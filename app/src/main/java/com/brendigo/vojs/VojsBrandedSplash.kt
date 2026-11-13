package com.brendigo.vojs

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource

/**
 * The source artwork is the untouched VOJS 1080x2400 image, tracked under
 * branding/pocetni-zaslon and copied into drawable-nodpi.
 * Fit never crops the brand image on other screen ratios.
 */
@Composable
internal fun VojsBrandedSplash(dark: Boolean) {
    Box(
        modifier = Modifier.fillMaxSize().background(
            if (dark) Color(0xFF20213B) else Color(0xFFF4F2FF)
        ),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(
                if (dark) R.drawable.vojs_splash_dark else R.drawable.vojs_splash_light
            ),
            contentDescription = if (dark) "VOJS splash tamni" else "VOJS splash svijetli",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Fit
        )
    }
}
