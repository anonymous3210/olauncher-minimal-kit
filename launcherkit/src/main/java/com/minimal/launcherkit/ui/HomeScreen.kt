package com.minimal.launcherkit.ui

import android.content.Intent
import android.net.Uri
import android.provider.AlarmClock
import android.provider.MediaStore
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material.icons.outlined.Place
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.minimal.launcherkit.data.Apps
import com.minimal.launcherkit.data.AppInfo
import com.minimal.launcherkit.media.MediaBridge
import kotlinx.coroutines.delay
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

private class Shortcut(val key: String, val label: String, val icon: ImageVector, val intent: () -> Intent)

private val shortcuts = listOf(
    Shortcut("h:Camera", "Camera", Icons.Outlined.CameraAlt) { Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA) },
    Shortcut("h:Maps", "Maps", Icons.Outlined.Place) { Intent(Intent.ACTION_VIEW, Uri.parse("geo:0,0?q=")) },
    Shortcut("h:Photos", "Photos", Icons.Outlined.PhotoLibrary) { Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_APP_GALLERY) },
)

@Composable
internal fun HomeScreen(
    hidden: Set<String>,
    apps: List<AppInfo>,
    favoriteApps: Set<String>,
    showIcons: Boolean,
    onDrawer: () -> Unit,
    onDrawerProgress: (Float) -> Unit,
    onSettings: () -> Unit,
    onSwipeLeft: () -> Unit,
    onSwipeRight: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val ctx = LocalContext.current
    var drag by remember { mutableFloatStateOf(0f) }
    val hhmm = remember { DateTimeFormatter.ofPattern("HH:mm") }
    val dateFmt = remember { DateTimeFormatter.ofPattern("EEE, MMM d") }
    val now by produceState(LocalDateTime.now()) { while (true) { value = LocalDateTime.now(); delay(5_000) } }

    Box(
        modifier
            .background(Color.Transparent)
            .pointerInput(Unit) {
                detectVerticalDragGestures(
                    onDragStart = { drag = 0f; onDrawerProgress(0f) },
                    onDragEnd = {
                        if (drag < -120f) { onDrawerProgress(1f); onDrawer() }
                        else onDrawerProgress(0f)
                    },
                    onDragCancel = { onDrawerProgress(0f) },
                ) { _, dy ->
                    drag += dy
                    onDrawerProgress((-drag / 260f).coerceIn(0f, 1f))
                }
            }
            .pointerInput(onSwipeLeft, onSwipeRight) {
                var horizontal = 0f
                detectHorizontalDragGestures(
                    onDragStart = { horizontal = 0f },
                    onDragEnd = {
                        if (horizontal < -100f) onSwipeLeft()
                        else if (horizontal > 100f) onSwipeRight()
                    },
                    onDragCancel = {},
                ) { _, dx -> horizontal += dx }
            }
            .statusBarsPadding()
    ) {
        IconButton(onClick = onSettings, modifier = Modifier.align(Alignment.TopEnd).padding(top = 12.dp, end = 12.dp)) {
            Icon(Icons.Outlined.Settings, contentDescription = "Configurações", tint = LK.Ink)
        }
        Column(Modifier.padding(horizontal = 30.dp).padding(top = 56.dp)) {
            Text(
                now.format(hhmm), color = LK.Ink, fontSize = 112.sp, lineHeight = 112.sp,
                fontWeight = FontWeight.ExtraLight, letterSpacing = (-2).sp,
                modifier = Modifier.springPressClickable(onClick = { Apps.startSafe(ctx, Intent(AlarmClock.ACTION_SHOW_ALARMS)) }),
            )
            Text(now.format(dateFmt), color = LK.Ink, fontSize = 27.sp, fontWeight = FontWeight.Normal, modifier = Modifier.padding(top = 22.dp, start = 4.dp))
            if ("h:Music" !in hidden) MediaWidget(Modifier.padding(top = 34.dp))
            apps.filter { it.pkg in favoriteApps && it.pkg !in hidden }.take(8).forEach { app ->
                Row(
                    Modifier.fillMaxWidth().padding(top = 18.dp).clip(RoundedCornerShape(12.dp))
                        .springPressClickable(onClick = { Apps.launch(ctx, app.pkg) })
                        .padding(vertical = 12.dp, horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (showIcons) {
                        Image(app.icon, app.label, Modifier.size(38.dp), contentScale = ContentScale.Fit)
                        Spacer(Modifier.width(36.dp))
                    }
                    Text(app.label, color = LK.Ink, fontSize = 29.sp)
                }
            }
            shortcuts.filter { it.key !in hidden }.forEach { s ->
                Row(
                    Modifier.fillMaxWidth().padding(top = 18.dp).clip(RoundedCornerShape(12.dp))
                        .springPressClickable(onClick = { Apps.startSafe(ctx, s.intent()) }).padding(vertical = 12.dp, horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(s.icon, null, tint = LK.Ink, modifier = Modifier.size(38.dp))
                    Spacer(Modifier.width(36.dp))
                    Text(s.label, color = LK.Ink, fontSize = 29.sp)
                }
            }
        }
        Text(
            "deslize para cima para abrir os apps", color = LK.Ink, fontSize = 14.sp,
            modifier = Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(bottom = 16.dp).alpha(.55f),
        )
    }
}

/** Widget de música REAL: mostra e controla a sessão de mídia ativa (Spotify, YouTube Music, etc.). */
@Composable
private fun MediaWidget(modifier: Modifier = Modifier) {
    val ctx = LocalContext.current
    val media by MediaBridge.state.collectAsState()
    var enabled by remember { mutableStateOf(MediaBridge.isEnabled(ctx)) }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { enabled = MediaBridge.isEnabled(ctx) }

    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier.size(128.dp).clip(RoundedCornerShape(12.dp)).background(Color(0x33000000))
                .clickable(enabled = media != null) { media?.let { Apps.launch(ctx, it.pkg) } },
            contentAlignment = Alignment.Center,
        ) {
            val art = media?.art
            if (art != null) Image(art, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
            else Icon(Icons.Filled.MusicNote, null, tint = Color(0xFFF3EAD6), modifier = Modifier.size(52.dp))
        }
        Spacer(Modifier.width(24.dp))
        Column(Modifier.weight(1f)) {
            val m = media
            when {
                !enabled -> {
                    Text("Ative o acesso às notificações para ver a música que está tocando.", color = LK.Ink, fontSize = 15.sp)
                    TextButton(onClick = {
                        Apps.startSafe(ctx, Intent(android.provider.Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
                    }) { Text("Ativar acesso", color = LK.Ink, fontSize = 17.sp, fontWeight = FontWeight.Medium) }
                }
                m == null -> Text("Nenhuma música tocando", color = LK.Ink, fontSize = 22.sp)
                else -> {
                    Text(m.title, color = LK.Ink, fontSize = 24.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(m.artist, color = LK.Ink, fontSize = 20.sp, fontWeight = FontWeight.Light, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Spacer(Modifier.height(8.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        IconButton({ MediaBridge.prev() }) { Icon(Icons.Filled.SkipPrevious, "Anterior", tint = LK.Ink, modifier = Modifier.size(30.dp)) }
                        IconButton({ MediaBridge.playPause() }, Modifier.size(62.dp).border(3.dp, LK.Ink, CircleShape)) {
                            Icon(if (m.playing) Icons.Filled.Pause else Icons.Filled.PlayArrow, if (m.playing) "Pausar" else "Tocar", tint = LK.Ink, modifier = Modifier.size(32.dp))
                        }
                        IconButton({ MediaBridge.next() }) { Icon(Icons.Filled.SkipNext, "Próxima", tint = LK.Ink, modifier = Modifier.size(30.dp)) }
                    }
                }
            }
        }
    }
}
