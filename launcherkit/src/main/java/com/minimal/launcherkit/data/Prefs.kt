package com.minimal.launcherkit.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.launcherStore by preferencesDataStore("launcherkit")

data class LauncherPrefs(
    val hidden: Set<String> = emptySet(),
    /** Android window blur radius, in dp. */
    val blur: Int = 28,
    val opacity: Int = 45,
    val showIcons: Boolean = true,
    val iconPackPackage: String = "",
    val dynamicBlur: Boolean = true,
    /** 0 = none, 1 = monochrome, 2 = sepia. */
    val wallpaperFilter: Int = 0,
    val overlay: Int = 0,
    val favoriteApps: Set<String> = emptySet(),
    val swipeLeftPackage: String = "",
    val swipeRightPackage: String = "",
)

class PrefsRepo(private val ctx: Context) {
    private val hiddenKey = stringSetPreferencesKey("hidden")
    private val blurKey = intPreferencesKey("blur")
    private val opacityKey = intPreferencesKey("opacity")
    private val showIconsKey = booleanPreferencesKey("show_icons")
    private val iconPackKey = stringPreferencesKey("icon_pack_package")
    private val dynamicBlurKey = booleanPreferencesKey("dynamic_blur")
    private val wallpaperFilterKey = intPreferencesKey("wallpaper_filter")
    private val overlayKey = intPreferencesKey("wallpaper_overlay")
    private val favoriteAppsKey = stringSetPreferencesKey("favorite_apps")
    private val swipeLeftKey = stringPreferencesKey("swipe_left_package")
    private val swipeRightKey = stringPreferencesKey("swipe_right_package")
    private val legacyImportedKey = booleanPreferencesKey("legacy_imported")

    val flow: Flow<LauncherPrefs> = ctx.launcherStore.data.map { data ->
        LauncherPrefs(
            hidden = data[hiddenKey] ?: emptySet(),
            blur = data[blurKey] ?: 28,
            opacity = data[opacityKey] ?: 45,
            showIcons = data[showIconsKey] ?: true,
            iconPackPackage = data[iconPackKey] ?: "",
            dynamicBlur = data[dynamicBlurKey] ?: true,
            wallpaperFilter = data[wallpaperFilterKey] ?: 0,
            overlay = data[overlayKey] ?: 0,
            favoriteApps = data[favoriteAppsKey] ?: emptySet(),
            swipeLeftPackage = data[swipeLeftKey] ?: "",
            swipeRightPackage = data[swipeRightKey] ?: "",
        )
    }

    suspend fun migrateLegacy(hiddenApps: Set<String>, favorites: Set<String>, swipeLeft: String = "", swipeRight: String = "") {
        ctx.launcherStore.edit { data ->
            if (data[legacyImportedKey] != true) {
                if (data[hiddenKey] == null) data[hiddenKey] = hiddenApps
                if (data[favoriteAppsKey] == null) data[favoriteAppsKey] = favorites
                if (data[swipeLeftKey] == null) data[swipeLeftKey] = swipeLeft
                if (data[swipeRightKey] == null) data[swipeRightKey] = swipeRight
                data[legacyImportedKey] = true
            }
        }
    }

    suspend fun setHidden(key: String, hide: Boolean) {
        ctx.launcherStore.edit { data ->
            val current = data[hiddenKey] ?: emptySet()
            data[hiddenKey] = if (hide) current + key else current - key
        }
    }

    suspend fun setHiddenAll(keys: Set<String>) { ctx.launcherStore.edit { it[hiddenKey] = keys } }
    suspend fun setBlur(radiusDp: Int) { ctx.launcherStore.edit { it[blurKey] = radiusDp.coerceIn(0, 60) } }
    suspend fun setOpacity(percent: Int) { ctx.launcherStore.edit { it[opacityKey] = percent.coerceIn(0, 100) } }
    suspend fun setShowIcons(enabled: Boolean) { ctx.launcherStore.edit { it[showIconsKey] = enabled } }
    suspend fun setIconPack(packageName: String) { ctx.launcherStore.edit { it[iconPackKey] = packageName } }
    suspend fun setDynamicBlur(enabled: Boolean) { ctx.launcherStore.edit { it[dynamicBlurKey] = enabled } }
    suspend fun setWallpaperFilter(filter: Int) { ctx.launcherStore.edit { it[wallpaperFilterKey] = filter.coerceIn(0, 2) } }
    suspend fun setOverlay(percent: Int) { ctx.launcherStore.edit { it[overlayKey] = percent.coerceIn(0, 100) } }

    suspend fun setFavorite(packageName: String, favorite: Boolean) {
        ctx.launcherStore.edit { data ->
            val current = data[favoriteAppsKey] ?: emptySet()
            data[favoriteAppsKey] = if (favorite) current + packageName else current - packageName
        }
    }

    suspend fun setSwipeLeft(packageName: String) { ctx.launcherStore.edit { it[swipeLeftKey] = packageName } }
    suspend fun setSwipeRight(packageName: String) { ctx.launcherStore.edit { it[swipeRightKey] = packageName } }
}
