package com.minimal.launcherkit.media

import android.content.ComponentName
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.service.notification.NotificationListenerService

class MediaListenerService : NotificationListenerService() {
    private var msm: MediaSessionManager? = null
    private val component by lazy { ComponentName(this, MediaListenerService::class.java) }

    private val callback = object : MediaController.Callback() {
        override fun onMetadataChanged(metadata: MediaMetadata?) = MediaBridge.publish()
        override fun onPlaybackStateChanged(state: PlaybackState?) = MediaBridge.publish()
        override fun onSessionDestroyed() = pick(null)
    }
    private val sessionsListener = MediaSessionManager.OnActiveSessionsChangedListener { pick(it) }

    override fun onListenerConnected() {
        msm = getSystemService(MediaSessionManager::class.java)
        runCatching { msm?.addOnActiveSessionsChangedListener(sessionsListener, component) }
        pick(null)
    }

    override fun onListenerDisconnected() {
        runCatching { msm?.removeOnActiveSessionsChangedListener(sessionsListener) }
        MediaBridge.controller?.unregisterCallback(callback)
        MediaBridge.controller = null
        MediaBridge.publish()
    }

    private fun pick(list: List<MediaController>?) {
        val all = list ?: runCatching { msm?.getActiveSessions(component) }.getOrNull() ?: emptyList()
        MediaBridge.controller?.unregisterCallback(callback)
        val c = all.firstOrNull { it.playbackState?.state == PlaybackState.STATE_PLAYING } ?: all.firstOrNull()
        MediaBridge.controller = c
        c?.registerCallback(callback)
        MediaBridge.publish()
    }
}
