package com.minimal.launcherkit.data

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.drawable.toBitmap
import java.text.Normalizer

/** Ícone é lido do ActivityInfo instalado, não de uma lista de ícones ilustrativos. */
data class AppInfo(
    val label: String,
    val pkg: String,
    val icon: ImageBitmap,
    val activityName: String = "",
    val normalizedLabel: String = normalizeLabel(label),
)

private val combiningMarks = "\\p{Mn}+".toRegex()

internal fun normalizeLabel(value: String): String =
    Normalizer.normalize(value, Normalizer.Form.NFD)
        .replace(combiningMarks, "")
        .lowercase()
        .trim()

object Apps {
    /** Deve ser executado fora da main thread. Fallback sempre usa o ícone Android real do app. */
    fun load(ctx: Context, iconPackPackage: String = ""): List<AppInfo> {
        val pm = ctx.packageManager
        val pack = IconPackManager.load(ctx, iconPackPackage)
        val main = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        return pm.queryIntentActivities(main, 0)
            // Deduplicate before loading labels and allocating icon bitmaps.
            .distinctBy { it.activityInfo?.packageName }
            .mapNotNull { resolveInfo ->
                val activity = resolveInfo.activityInfo ?: return@mapNotNull null
                val packageName = activity.packageName
                val className = activity.name
                val drawable = pack?.iconFor(packageName, className) ?: resolveInfo.loadIcon(pm)
                AppInfo(
                    label = resolveInfo.loadLabel(pm).toString(),
                    pkg = packageName,
                    icon = drawable.toBitmap(64, 64).asImageBitmap(),
                    activityName = className,
                )
            }
            .sortedBy { it.label.lowercase() }
    }

    fun launch(ctx: Context, pkg: String) {
        ctx.packageManager.getLaunchIntentForPackage(pkg)?.let { startSafe(ctx, it) }
    }

    fun details(ctx: Context, pkg: String) {
        startSafe(ctx, Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:$pkg")))
    }

    fun startSafe(ctx: Context, intent: Intent) {
        runCatching { ctx.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
    }
}
