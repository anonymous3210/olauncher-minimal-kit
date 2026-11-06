package com.minimal.launcherkit.data

import android.content.Context
import android.content.pm.PackageManager
import android.content.res.Resources
import android.graphics.drawable.Drawable
import android.util.Xml
import org.xmlpull.v1.XmlPullParser

 data class InstalledIconPack(val label: String, val packageName: String)

object IconPackManager {
    private const val APP_FILTER = "appfilter"

    fun installedPacks(context: Context): List<InstalledIconPack> {
        val pm = context.packageManager
        return runCatching { pm.getInstalledApplications(PackageManager.GET_META_DATA) }
            .getOrDefault(emptyList())
            .mapNotNull { app ->
                runCatching {
                    val res = pm.getResourcesForApplication(app.packageName)
                    val id = res.getIdentifier(APP_FILTER, "xml", app.packageName)
                    if (id == 0) null else InstalledIconPack(pm.getApplicationLabel(app).toString(), app.packageName)
                }.getOrNull()
            }
            .distinctBy { it.packageName }
            .sortedBy { it.label.lowercase() }
    }

    fun load(context: Context, packageName: String): IconPack? {
        if (packageName.isBlank()) return null
        return runCatching {
            val resources = context.packageManager.getResourcesForApplication(packageName)
            val xmlId = resources.getIdentifier(APP_FILTER, "xml", packageName)
            if (xmlId == 0) return null
            val mappings = HashMap<String, String>()
            resources.getXml(xmlId).use { parser ->
                var event = parser.eventType
                while (event != XmlPullParser.END_DOCUMENT) {
                    if (event == XmlPullParser.START_TAG && parser.name == "item") {
                        val component = parser.getAttributeValue(null, "component").orEmpty()
                        val drawable = parser.getAttributeValue(null, "drawable").orEmpty()
                        parseComponent(component)?.let { key -> if (drawable.isNotBlank()) mappings[key] = drawable }
                    }
                    event = parser.next()
                }
            }
            IconPack(packageName, resources, mappings)
        }.getOrNull()
    }

    private fun parseComponent(value: String): String? {
        val body = value.substringAfter('{', "").substringBefore('}')
        val split = body.split('/', limit = 2)
        if (split.size != 2 || split[0].isBlank()) return null
        val pkg = split[0]
        val className = when {
            split[1].startsWith(".") -> pkg + split[1]
            '.' !in split[1] -> "$pkg.${split[1]}"
            else -> split[1]
        }
        return "$pkg/$className"
    }
}

class IconPack internal constructor(
    private val packageName: String,
    private val resources: Resources,
    private val mappings: Map<String, String>,
) {
    fun iconFor(packageName: String, activityName: String): Drawable? {
        val normalized = if (activityName.startsWith(".")) packageName + activityName else activityName
        val drawableName = mappings["$packageName/$normalized"]
            ?: mappings["$packageName/$packageName"]
            ?: return null
        val id = resources.getIdentifier(drawableName, "drawable", this.packageName)
            .takeIf { it != 0 }
            ?: resources.getIdentifier(drawableName, "mipmap", this.packageName)
                .takeIf { it != 0 }
            ?: return null
        return runCatching { resources.getDrawable(id, null) }.getOrNull()
    }
}
