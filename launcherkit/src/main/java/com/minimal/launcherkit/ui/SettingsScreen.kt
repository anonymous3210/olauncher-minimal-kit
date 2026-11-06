package com.minimal.launcherkit.ui

import android.content.Intent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.minimal.launcherkit.data.AppInfo
import com.minimal.launcherkit.data.Apps
import com.minimal.launcherkit.data.IconPackManager
import com.minimal.launcherkit.data.InstalledIconPack
import com.minimal.launcherkit.data.LauncherPrefs
import com.minimal.launcherkit.media.MediaBridge
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt

private val homeKeys = listOf("h:Music" to "Widget de música", "h:Camera" to "Camera", "h:Maps" to "Maps", "h:Photos" to "Photos")

@Composable
internal fun SettingsScreen(
    apps: List<AppInfo>,
    prefs: LauncherPrefs,
    glassOpacity: Int,
    onBack: () -> Unit,
    onToggle: (key: String, hide: Boolean) -> Unit,
    onSetHidden: (Set<String>) -> Unit,
    onBlur: (Int) -> Unit,
    onOpacity: (Int) -> Unit,
    onShowIcons: (Boolean) -> Unit,
    onIconPack: (String) -> Unit,
    onDynamicBlur: (Boolean) -> Unit,
    onWallpaperFilter: (Int) -> Unit,
    onOverlay: (Int) -> Unit,
    onFavorite: (String, Boolean) -> Unit,
    onSwipeLeft: (String) -> Unit,
    onSwipeRight: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val ctx = LocalContext.current
    var blurV by remember(prefs.blur) { mutableFloatStateOf(prefs.blur.toFloat()) }
    var opacityV by remember(prefs.opacity) { mutableFloatStateOf(prefs.opacity.toFloat()) }
    var overlayV by remember(prefs.overlay) { mutableFloatStateOf(prefs.overlay.toFloat()) }
    var notifOn by remember { mutableStateOf(MediaBridge.isEnabled(ctx)) }
    var packs by remember { mutableStateOf<List<InstalledIconPack>>(emptyList()) }
    var packDialog by remember { mutableStateOf(false) }
    var gestureTarget by remember { mutableStateOf<String?>(null) }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { notifOn = MediaBridge.isEnabled(ctx) }
    LaunchedEffect(Unit) { packs = withContext(Dispatchers.IO) { IconPackManager.installedPacks(ctx) } }
    LaunchedEffect(blurV, prefs.blur) {
        if (blurV.roundToInt() != prefs.blur) {
            kotlinx.coroutines.delay(250L)
            onBlur(blurV.roundToInt())
        }
    }
    LaunchedEffect(opacityV, prefs.opacity) {
        if (opacityV.roundToInt() != prefs.opacity) {
            kotlinx.coroutines.delay(250L)
            onOpacity(opacityV.roundToInt())
        }
    }
    LaunchedEffect(overlayV, prefs.overlay) {
        if (overlayV.roundToInt() != prefs.overlay) {
            kotlinx.coroutines.delay(250L)
            onOverlay(overlayV.roundToInt())
        }
    }

    val sliderColors = SliderDefaults.colors(
        thumbColor = LK.Cream, activeTrackColor = LK.Cream, inactiveTrackColor = LK.Cream.copy(alpha = .25f),
    )
    val currentPackName = packs.firstOrNull { it.packageName == prefs.iconPackPackage }?.label ?: "Sistema"

    Box(modifier.glassSurface(glassOpacity)) {
        LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 28.dp, end = 28.dp, top = 120.dp, bottom = 60.dp),
        ) {
            item { Text("Configurações", color = LK.Cream, fontSize = 38.sp) }

            item { SectionTitle("Aparência da gaveta") }
            item {
                Box(Modifier.fillMaxWidth().height(120.dp).clip(RoundedCornerShape(16.dp)).background(LK.SandTop)) {
                    Canvas(Modifier.fillMaxSize().blur(blurV.dp)) {
                        drawCircle(Color(0xFF4F8A2C), 45.dp.toPx(), androidx.compose.ui.geometry.Offset(70.dp.toPx(), 60.dp.toPx()))
                        drawCircle(Color(0xFFD7AA2C), 35.dp.toPx(), androidx.compose.ui.geometry.Offset(size.width - 70.dp.toPx(), 70.dp.toPx()))
                    }
                    Box(Modifier.fillMaxSize().glassSurface(opacityV.roundToInt()), contentAlignment = Alignment.Center) {
                        Text("Prévia", color = LK.Cream, fontSize = 20.sp)
                    }
                }
            }
            item {
                LabelValue("Desfoque", "${blurV.roundToInt()} dp")
                Slider(blurV, { blurV = it }, valueRange = 0f..60f, colors = sliderColors)
                LabelValue("Opacidade da gaveta", "${opacityV.roundToInt()}%")
                Slider(opacityV, { opacityV = it }, valueRange = 0f..100f, colors = sliderColors)
                SwitchRow("Blur dinâmico dia/noite", prefs.dynamicBlur, onDynamicBlur)
                LabelValue("Escurecimento do fundo", "${overlayV.roundToInt()}%")
                Slider(overlayV, { overlayV = it }, valueRange = 0f..100f, colors = sliderColors)
                Text("O sistema desfoca o papel de parede atrás da janela no Android 12+ com efeitos de janela ativos. Sem esse suporte, a gaveta fica translúcida e levemente escurecida.", color = LK.Cream.copy(alpha = .6f), fontSize = 14.sp)
            }

            item { SectionTitle("Ícones dos aplicativos") }
            item { SwitchRow("Mostrar ícones na gaveta", prefs.showIcons, onShowIcons) }
            item {
                SettingAction("Pacote de ícones", currentPackName) { packDialog = true }
            }

            item { SectionTitle("Filtro do fundo") }
            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterPill("Normal", prefs.wallpaperFilter == 0, Modifier.weight(1f)) { onWallpaperFilter(0) }
                    FilterPill("P&B", prefs.wallpaperFilter == 1, Modifier.weight(1f)) { onWallpaperFilter(1) }
                    FilterPill("Sépia", prefs.wallpaperFilter == 2, Modifier.weight(1f)) { onWallpaperFilter(2) }
                }
            }
            item { SettingAction("Papel de parede", "Abrir seletor do Android") { Apps.startSafe(ctx, Intent(Intent.ACTION_SET_WALLPAPER)) } }

            item { SectionTitle("Música") }
            item {
                SwitchRow(
                    if (notifOn) "Acesso às notificações ativo" else "Ativar acesso às notificações",
                    notifOn,
                ) { Apps.startSafe(ctx, Intent(android.provider.Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)) }
                Text("Permissão opcional para mostrar e controlar a mídia em reprodução.", color = LK.Cream.copy(alpha = .6f), fontSize = 14.sp)
            }

            item { SectionTitle("Atalhos da tela inicial") }
            items(homeKeys, key = { it.first }) { (key, label) ->
                SwitchRow(label, key !in prefs.hidden) { onToggle(key, !it) }
            }
            item { SectionTitle("Apps favoritos na tela inicial") }
            items(apps, key = { "favorite-" + it.pkg }) { app ->
                SwitchRow(app.label, app.pkg in prefs.favoriteApps) { onFavorite(app.pkg, it) }
            }

            item { SectionTitle("Gestos do Olauncher") }
            item {
                SettingAction("Deslizar para a esquerda", apps.firstOrNull { it.pkg == prefs.swipeLeftPackage }?.label ?: "Escolher app") {
                    gestureTarget = "left"
                }
                SettingAction("Deslizar para a direita", apps.firstOrNull { it.pkg == prefs.swipeRightPackage }?.label ?: "Escolher app") {
                    gestureTarget = "right"
                }
            }

            item { SectionTitle("Gaveta de apps") }
            items(apps, key = { "hidden-" + it.pkg }) { app ->
                SwitchRow(app.label, app.pkg !in prefs.hidden) { onToggle(app.pkg, !it) }
            }
            item {
                Row(Modifier.fillMaxWidth().padding(top = 28.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedPill("Mostrar todos", Modifier.weight(1f)) { onSetHidden(emptySet()) }
                    OutlinedPill("Ocultar todos", Modifier.weight(1f)) {
                        onSetHidden(apps.map { it.pkg }.toSet() + homeKeys.map { it.first })
                    }
                }
            }
        }
        IconButton(onBack, Modifier.align(Alignment.TopStart).statusBarsPadding().padding(12.dp).background(Color(0x66000000), androidx.compose.foundation.shape.CircleShape)) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, "Voltar", tint = LK.Cream)
        }
    }

    if (packDialog) {
        AlertDialog(
            onDismissRequest = { packDialog = false },
            containerColor = LK.Wine,
            title = { Text("Pacote de ícones", color = LK.Cream) },
            text = {
                Column {
                    PackOption("Ícones reais do Android", prefs.iconPackPackage.isBlank(), LK.Cream) {
                        onIconPack(""); packDialog = false
                    }
                    packs.forEach { pack ->
                        PackOption(pack.label, prefs.iconPackPackage == pack.packageName, LK.Cream) {
                            onIconPack(pack.packageName); packDialog = false
                        }
                    }
                    if (packs.isEmpty()) Text("Nenhum icon pack com appfilter.xml foi encontrado.", color = LK.Cream.copy(alpha = .7f), modifier = Modifier.padding(top = 8.dp))
                }
            },
            confirmButton = { TextButton(onClick = { packDialog = false }) { Text("Fechar", color = LK.Cream) } },
        )
    }

    gestureTarget?.let { target ->
        AlertDialog(
            onDismissRequest = { gestureTarget = null },
            containerColor = LK.Wine,
            title = { Text(if (target == "left") "App ao deslizar para a esquerda" else "App ao deslizar para a direita", color = LK.Cream) },
            text = {
                Column(Modifier.heightIn(max = 380.dp)) {
                    PackOption("Desativado", false, LK.Cream) {
                        if (target == "left") onSwipeLeft("") else onSwipeRight("")
                        gestureTarget = null
                    }
                    apps.forEach { app ->
                        Row(
                            Modifier.fillMaxWidth().clickable {
                                if (target == "left") onSwipeLeft(app.pkg) else onSwipeRight(app.pkg)
                                gestureTarget = null
                            }.padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Image(app.icon, app.label, Modifier.padding(end = 12.dp).size(32.dp))
                            Text(app.label, color = LK.Cream, fontSize = 17.sp)
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { gestureTarget = null }) { Text("Fechar", color = LK.Cream) } },
        )
    }
}

@Composable private fun PackOption(label: String, selected: Boolean, color: Color, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(if (selected) "●" else "○", color = color, modifier = Modifier.padding(end = 12.dp))
        Text(label, color = color, fontSize = 17.sp)
    }
}

@Composable private fun FilterPill(text: String, selected: Boolean, modifier: Modifier, onClick: () -> Unit) =
    TextButton(onClick, modifier.border(1.dp, LK.Cream.copy(alpha = if (selected) 1f else .4f), RoundedCornerShape(14.dp))) {
        Text(text, color = LK.Cream, fontSize = 17.sp)
    }

@Composable private fun SettingAction(label: String, value: String, onClick: () -> Unit) =
    Row(Modifier.fillMaxWidth().springPressClickable(onClick).padding(vertical = 12.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text(label, color = LK.Cream, fontSize = 21.sp)
        Text(value, color = LK.Cream.copy(alpha = .65f), fontSize = 16.sp)
    }

@Composable private fun SectionTitle(text: String) =
    Text(text, color = LK.Cream, fontSize = 22.sp, modifier = Modifier.padding(top = 30.dp, bottom = 6.dp).alpha(.7f))

@Composable private fun LabelValue(label: String, value: String) =
    Row(Modifier.fillMaxWidth().padding(top = 12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = LK.Cream, fontSize = 22.sp)
        Text(value, color = LK.Cream.copy(alpha = .7f), fontSize = 20.sp)
    }

@Composable private fun SwitchRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit) =
    Row(Modifier.fillMaxWidth().padding(vertical = 10.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text(label, color = LK.Cream, fontSize = 23.sp, modifier = Modifier.weight(1f))
        Switch(
            checked, onChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = LK.Cream, checkedTrackColor = LK.Green,
                uncheckedThumbColor = LK.Cream, uncheckedTrackColor = Color(0x33FFFFFF), uncheckedBorderColor = Color.Transparent,
            ),
        )
    }

@Composable private fun OutlinedPill(text: String, modifier: Modifier, onClick: () -> Unit) =
    TextButton(onClick, modifier.border(2.dp, LK.Cream, RoundedCornerShape(14.dp))) { Text(text, color = LK.Cream, fontSize = 18.sp) }
