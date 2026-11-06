package app.olauncher

import android.content.Intent
import android.os.Bundle
import android.os.Process
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.lifecycleScope
import androidx.work.WorkManager
import app.olauncher.data.Constants
import app.olauncher.data.Prefs
import com.minimal.launcherkit.MinimalLauncher
import com.minimal.launcherkit.data.PrefsRepo
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private var homeTick by mutableIntStateOf(0)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        migrateOlauncherPreferences()
        // Wallpaper changes remain user-controlled in the Android wallpaper picker.
        WorkManager.getInstance(applicationContext).cancelUniqueWork(Constants.WALLPAPER_WORKER_NAME)
        setContent {
            MinimalLauncher(resetSignal = homeTick)
        }
    }

    private fun migrateOlauncherPreferences() {
        val legacy = Prefs(this)
        val myProfile = Process.myUserHandle().toString()
        val storedHidden = legacy.hiddenApps.map { key ->
            if ('|' in key) key else "$key|$myProfile"
        }.toMutableSet()
        if (!legacy.hiddenAppsUpdated) {
            legacy.hiddenApps = storedHidden
            legacy.hiddenAppsUpdated = true
        }
        val hidden = storedHidden
            .filter { it.endsWith("|$myProfile") }
            .map { it.substringBefore('|') }
            .toSet()
        val favorites = (1..8).map { legacy.getAppPackage(it) }.filter { it.isNotBlank() }.toSet()
        lifecycleScope.launch {
            PrefsRepo(applicationContext).migrateLegacy(
                hidden,
                favorites,
                legacy.appPackageSwipeLeft,
                legacy.appPackageSwipeRight,
            )
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        homeTick++
    }
}
