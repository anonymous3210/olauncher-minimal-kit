package com.minimal.launcherkit.ui

import android.graphics.Bitmap
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageShader
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.dp

/** A fine, deterministic grain texture used only on translucent glass surfaces. */
private val glassGrain by lazy {
    val size = 128
    val random = java.util.Random(7)
    val pixels = IntArray(size * size) {
        val shade = random.nextInt(256)
        android.graphics.Color.argb(16, shade, shade, shade)
    }
    Bitmap.createBitmap(pixels, size, size, Bitmap.Config.ARGB_8888).asImageBitmap()
}

/** Neutral graphite tint, soft top reflection, subtle grain and a light edge. */
internal fun Modifier.glassSurface(
    opacityPercent: Int,
    shape: Shape = RectangleShape,
): Modifier {
    val alpha = (opacityPercent / 100f).coerceIn(0f, 1f)
    return background(
        Brush.verticalGradient(
            listOf(
                Color(0xFF24282D).copy(alpha = alpha * .90f),
                Color(0xFF171A1E).copy(alpha = alpha),
            )
        ),
        shape,
    ).drawBehind {
        drawRect(
            Brush.verticalGradient(
                listOf(Color.White.copy(alpha = .10f), Color.Transparent),
                endY = size.height * .4f,
            )
        )
        drawRect(ShaderBrush(ImageShader(glassGrain, TileMode.Repeated, TileMode.Repeated)))
    }.border(
        1.dp,
        Brush.verticalGradient(
            listOf(Color.White.copy(alpha = .30f), Color.White.copy(alpha = .05f))
        ),
        shape,
    )
}
