package com.minimal.launcherkit.media

import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.PlaybackState
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.app.NotificationManagerCompat
import kotlinx.coroutines.flow.MutableStateFlow

data class MediaUi(val title: String, val artist: String, val art: ImageBitmap?, val playing: Boolean, val pkg: String)

/** Ponte entre o serviço de notificações (sessões de mídia reais do sistema) e a UI. */
object MediaBridge {
    val state = MutableStateFlow<MediaUi?>(null)
    @Volatile var controller: MediaController? = null

    fun isEnabled(ctx: Context): Boolean =
        NotificationManagerCompat.getEnabledListenerPackages(ctx).contains(ctx.packageName)

    fun playPause() {
        val c = controller ?: return
        runCatching {
            if (c.playbackState?.state == PlaybackState.STATE_PLAYING) c.transportControls.pause()
            else c.transportControls.play()
        }
    }
    fun next() { runCatching { controller?.transportControls?.skipToNext() } }
    fun prev() { runCatching { controller?.transportControls?.skipToPrevious() } }

    fun publish() {
        try {
            val c = controller
            if (c == null) { state.value = null; return }
            val m = c.metadata
            val sourceArt = m?.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART)
                ?: m?.getBitmap(MediaMetadata.METADATA_KEY_ART)
            val art = sourceArt?.let { bitmap ->
                val maxDimension = maxOf(bitmap.width, bitmap.height)
                if (maxDimension <= MAX_ART_DIMENSION) bitmap else {
                    val scale = MAX_ART_DIMENSION.toFloat() / maxDimension
                    Bitmap.createScaledBitmap(
                        bitmap,
                        (bitmap.width * scale).toInt().coerceAtLeast(1),
                        (bitmap.height * scale).toInt().coerceAtLeast(1),
                        true,
                    )
                }
            }
            state.value = MediaUi(
                title = m?.getString(MediaMetadata.METADATA_KEY_TITLE) ?: "Sem título",
                artist = m?.getString(MediaMetadata.METADATA_KEY_ARTIST)
                    ?: m?.getString(MediaMetadata.METADATA_KEY_ALBUM_ARTIST) ?: "",
                art = art?.asImageBitmap(),
                playing = c.playbackState?.state == PlaybackState.STATE_PLAYING,
                pkg = c.packageName,
            )
        } catch (_: RuntimeException) {
            state.value = null
        }
    }

    private const val MAX_ART_DIMENSION = 512
}
