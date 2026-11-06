package com.minimal.launcherkit

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.view.WindowManager
import androidx.annotation.RequiresApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import java.util.function.Consumer

internal fun Context.findActivity(): Activity? {
    var current: Context? = this
    while (current is ContextWrapper) {
        if (current is Activity) return current
        current = current.baseContext
    }
    return null
}

/** Reports whether the platform currently allows cross-window blur. */
@RequiresApi(31)
@Composable
internal fun rememberCrossWindowBlur(): Boolean {
    val context = LocalContext.current
    val windowManager = remember(context) { context.getSystemService(WindowManager::class.java) }
    var enabled by remember(windowManager) {
        mutableStateOf(windowManager?.isCrossWindowBlurEnabled == true)
    }

    DisposableEffect(windowManager, context) {
        val manager = windowManager
        if (manager == null) {
            onDispose { }
        } else {
            val listener = Consumer<Boolean> { enabled = it }
            manager.addCrossWindowBlurEnabledListener(context.mainExecutor, listener)
            onDispose { manager.removeCrossWindowBlurEnabledListener(listener) }
        }
    }

    return enabled
}
