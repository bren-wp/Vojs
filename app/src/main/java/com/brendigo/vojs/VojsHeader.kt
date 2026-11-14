package com.brendigo.vojs

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/** Exact supplied logo asset, stable vector-like settings gear and 48dp touch target. */
@Composable
internal fun VojsHeader(dark: Boolean, onSettings: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().height(48.dp),
        horizontalArrangement = Arrangement.End,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Image(
            painter = painterResource(
                if (dark) R.drawable.vojs_logo_dark else R.drawable.vojs_logo_light
            ),
            contentDescription = "VOJS",
            modifier = Modifier.size(width = 102.dp, height = 38.dp),
            contentScale = ContentScale.Fit
        )
        IconButton(
            onClick = onSettings,
            modifier = Modifier.size(48.dp).semantics {
                contentDescription = "Postavke aplikacije"
            }
        ) {
            Canvas(Modifier.size(22.dp)) {
                val inner = 8.dp.toPx()
                val outer = 10.5.dp.toPx()
                val c = center
                for (i in 0 until 8) {
                    val angle = 2.0 * PI * i / 8.0
                    val dx = cos(angle).toFloat()
                    val dy = sin(angle).toFloat()
                    drawLine(
                        color = Violet,
                        start = Offset(c.x + dx * inner, c.y + dy * inner),
                        end = Offset(c.x + dx * outer, c.y + dy * outer),
                        strokeWidth = 1.9.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                }
                drawCircle(
                    color = Violet,
                    radius = 7.8.dp.toPx(),
                    style = Stroke(width = 1.9.dp.toPx())
                )
                drawCircle(color = Violet, radius = 2.3.dp.toPx())
            }
        }
    }
}
