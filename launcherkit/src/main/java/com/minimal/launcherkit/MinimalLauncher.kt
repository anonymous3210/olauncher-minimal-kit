package com.minimal.launcherkit

import android.graphics.Color as AndroidColor
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.view.WindowManager
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.minimal.launcherkit.data.AppInfo
import com.minimal.launcherkit.data.Apps
import com.minimal.launcherkit.data.LauncherPrefs
import com.minimal.launcherkit.data.PrefsRepo
import com.minimal.launcherkit.ui.DrawerScreen
import com.minimal.launcherkit.ui.HomeScreen
import com.minimal.launcherkit.ui.SettingsScreen
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalTime

private enum class Page { Home, Drawer, Settings }

/** Launcher Compose do kit; resetSignal faz o botão Home regressar à página inicial. */
@Composable
fun MinimalLauncher(modifier: Modifier = Modifier, resetSignal: Int = 0) {
    val ctx = LocalContext.current
    val repo = remember { PrefsRepo(ctx.applicationContext) }
    val prefs by repo.flow.collectAsState(initial = LauncherPrefs())
    val scope = rememberCoroutineScope()
    var page by remember { mutableStateOf(Page.Home) }
    var from by remember { mutableStateOf(Page.Home) }
    var apps by remember { mutableStateOf<List<AppInfo>>(emptyList()) }
    var gestureProgress by remember { mutableFloatStateOf(0f) }
    var hour by remember { mutableIntStateOf(LocalTime.now().hour) }

    LaunchedEffect(resetSignal) {
        page = Page.Home
        gestureProgress = 0f
    }
    LaunchedEffect(page, prefs.iconPackPackage) {
        if (page == Page.Drawer || apps.isEmpty()) {
            apps = withContext(Dispatchers.IO) { Apps.load(ctx.applicationContext, prefs.iconPackPackage) }
        }
    }

    val drawerIsBehind = page == Page.Drawer || (page == Page.Settings && from == Page.Drawer)
    LaunchedEffect(drawerIsBehind, prefs.dynamicBlur) {
        if (drawerIsBehind && prefs.dynamicBlur) {
            while (isActive) {
                hour = LocalTime.now().hour
                delay(60_000L)
            }
        }
    }

    val targetDrawerProgress = if (drawerIsBehind) 1f else gestureProgress.coerceIn(0f, 1f)
    val drawerProgress by animateFloatAsState(
        targetValue = targetDrawerProgress,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = 300f),
        label = "drawerProgress",
    )
    val targetGlassProgress = if (page == Page.Home) gestureProgress.coerceIn(0f, 1f) else 1f
    val glassProgress by animateFloatAsState(
        targetValue = targetGlassProgress,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = 300f),
        label = "glassProgress",
    )

    val dayFactor = if (prefs.dynamicBlur && hour in 7..18) 0.62f else 1f
    val blurDp = (prefs.blur.coerceIn(0, 60) * dayFactor * glassProgress).dp
    val backgroundColorFilter = remember(prefs.wallpaperFilter) {
        when (prefs.wallpaperFilter) {
            1 -> ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(0f) })
            2 -> ColorFilter.colorMatrix(ColorMatrix(floatArrayOf(
                .393f, .769f, .189f, 0f, 0f,
                .349f, .686f, .168f, 0f, 0f,
                .272f, .534f, .131f, 0f, 0f,
                0f, 0f, 0f, 1f, 0f,
            )))
            else -> null
        }
    }

    BackHandler(enabled = page != Page.Home) {
        when (page) {
            Page.Settings -> page = from
            Page.Drawer -> { gestureProgress = 0f; page = Page.Home }
            Page.Home -> Unit
        }
    }

    val window = remember(ctx) { ctx.findActivity()?.window }
    val nativeBlurEnabled = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        rememberCrossWindowBlur()
    } else {
        false
    }
    val glassOpacity = if (nativeBlurEnabled) prefs.opacity else maxOf(prefs.opacity, 60)
    val blurRadiusPx = with(LocalDensity.current) { blurDp.roundToPx().coerceAtMost(150) }

    @Suppress("DEPRECATION")
    LaunchedEffect(window) {
        window?.setBackgroundDrawable(ColorDrawable(AndroidColor.TRANSPARENT))
        window?.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WALLPAPER)
    }
    SideEffect {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            window?.setBackgroundBlurRadius(if (nativeBlurEnabled) blurRadiusPx else 0)
        }
    }
    DisposableEffect(window) {
        onDispose {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) window?.setBackgroundBlurRadius(0)
        }
    }

    val nightTint = prefs.dynamicBlur && (hour >= 19 || hour < 7)
    val overlayColor = if (nightTint) Color(0xFF7A4B30) else Color.Black

    Box(modifier.fillMaxSize().background(Color.Transparent)) {
        val homeModifier = Modifier.fillMaxSize()
            .graphicsLayer { alpha = 1f - glassProgress }
            .drawWithCache {
                val filterPaint = Paint().apply { colorFilter = backgroundColorFilter }
                onDrawWithContent {
                    if (backgroundColorFilter != null) {
                        drawIntoCanvas { canvas ->
                            canvas.saveLayer(Rect(Offset.Zero, size), filterPaint)
                            drawContent()
                            canvas.restore()
                        }
                    } else {
                        drawContent()
                    }
                }
            }

        HomeScreen(
            hidden = prefs.hidden,
            apps = apps,
            favoriteApps = prefs.favoriteApps,
            showIcons = prefs.showIcons,
            onDrawer = { page = Page.Drawer },
            onDrawerProgress = { gestureProgress = it.coerceIn(0f, 1f) },
            onSettings = { from = Page.Home; page = Page.Settings },
            onSwipeLeft = { prefs.swipeLeftPackage.takeIf { it.isNotBlank() }?.let { Apps.launch(ctx, it) } },
            onSwipeRight = { prefs.swipeRightPackage.takeIf { it.isNotBlank() }?.let { Apps.launch(ctx, it) } },
            modifier = homeModifier,
        )

        if (glassProgress > 0.001f && prefs.overlay > 0) {
            Box(
                Modifier.fillMaxSize()
                    .graphicsLayer { alpha = glassProgress }
                    .background(overlayColor.copy(alpha = prefs.overlay.coerceIn(0, 100) / 100f))
            )
        }

        if (drawerProgress > 0.001f || page == Page.Drawer || (page == Page.Settings && from == Page.Drawer)) {
            DrawerScreen(
                apps = apps,
                hidden = prefs.hidden,
                opacity = glassOpacity,
                showIcons = prefs.showIcons,
                progress = drawerProgress,
                onBack = { gestureProgress = 0f; page = Page.Home },
                onSettings = { from = Page.Drawer; page = Page.Settings },
                modifier = Modifier.fillMaxSize().graphicsLayer {
                    translationY = size.height * (1f - drawerProgress)
                    alpha = drawerProgress
                },
            )
        }

        AnimatedVisibility(
            visible = page == Page.Settings,
            enter = slideInHorizontally { it },
            exit = slideOutHorizontally { it },
        ) {
            SettingsScreen(
                apps = apps,
                prefs = prefs,
                glassOpacity = glassOpacity,
                onBack = { page = from },
                onToggle = { key, hide -> scope.launch { repo.setHidden(key, hide) } },
                onSetHidden = { scope.launch { repo.setHiddenAll(it) } },
                onBlur = { scope.launch { repo.setBlur(it) } },
                onOpacity = { scope.launch { repo.setOpacity(it) } },
                onShowIcons = { scope.launch { repo.setShowIcons(it) } },
                onIconPack = { scope.launch { repo.setIconPack(it) } },
                onDynamicBlur = { scope.launch { repo.setDynamicBlur(it) } },
                onWallpaperFilter = { scope.launch { repo.setWallpaperFilter(it) } },
                onOverlay = { scope.launch { repo.setOverlay(it) } },
                onFavorite = { pkg, favorite -> scope.launch { repo.setFavorite(pkg, favorite) } },
                onSwipeLeft = { scope.launch { repo.setSwipeLeft(it) } },
                onSwipeRight = { scope.launch { repo.setSwipeRight(it) } },
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}
