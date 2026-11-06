package com.minimal.launcherkit.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/** Keeps the launcher app list warm instead of rescanning PackageManager per screen open. */
object AppsRepository {
    private val _apps = MutableStateFlow<List<AppInfo>>(emptyList())
    val apps: StateFlow<List<AppInfo>> = _apps.asStateFlow()

    private val mutex = Mutex()
    private var loadedForIconPack: String? = null

    suspend fun ensureLoaded(context: Context, iconPackPackage: String) {
        mutex.withLock {
            if (loadedForIconPack == iconPackPackage) return
            val loaded = withContext(Dispatchers.IO) {
                Apps.load(context.applicationContext, iconPackPackage)
            }
            _apps.value = loaded
            loadedForIconPack = iconPackPackage
        }
    }

    suspend fun refresh(context: Context, iconPackPackage: String) {
        mutex.withLock {
            loadedForIconPack = null
            val loaded = withContext(Dispatchers.IO) {
                Apps.load(context.applicationContext, iconPackPackage)
            }
            _apps.value = loaded
            loadedForIconPack = iconPackPackage
        }
    }
}
