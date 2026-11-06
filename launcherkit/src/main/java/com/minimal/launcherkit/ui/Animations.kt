package com.minimal.launcherkit.ui

import android.view.animation.PathInterpolator
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer

private val pathInterpolator = PathInterpolator(0.25f, 0.1f, 0.25f, 1.0f)
internal val LauncherEasing = Easing { fraction -> pathInterpolator.getInterpolation(fraction) }

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun Modifier.springPressClickable(
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
): Modifier {
    val interactions = remember { MutableInteractionSource() }
    val pressed by interactions.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.95f else 1f,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = 300f),
        label = "springPressScale",
    )
    val scaled = graphicsLayer { scaleX = scale; scaleY = scale }
    return if (onLongClick == null) {
        scaled.combinedClickable(interactionSource = interactions, indication = null, onClick = onClick)
    } else {
        scaled.combinedClickable(interactionSource = interactions, indication = null, onClick = onClick, onLongClick = onLongClick)
    }
}
