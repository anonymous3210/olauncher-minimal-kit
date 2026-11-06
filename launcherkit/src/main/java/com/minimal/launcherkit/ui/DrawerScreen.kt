package com.minimal.launcherkit.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.graphics.SolidColor
import kotlinx.coroutines.delay
import com.minimal.launcherkit.data.AppInfo
import com.minimal.launcherkit.data.Apps
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.roundToInt

private sealed interface DrawerItem {
    data class Header(val letter: String) : DrawerItem
    data class Entry(val app: AppInfo) : DrawerItem
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun DrawerScreen(
    apps: List<AppInfo>,
    hidden: Set<String>,
    opacity: Int,
    showIcons: Boolean,
    progress: Float,
    onBack: () -> Unit,
    onSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val ctx = LocalContext.current
    var query by remember { mutableStateOf("") }
    val visible = remember(apps, hidden, query) {
        apps.filter { it.pkg !in hidden && it.label.contains(query.trim(), ignoreCase = true) }
    }
    val groups = remember(visible) {
        visible.groupBy { a -> a.label.firstOrNull()?.uppercaseChar()?.takeIf { it.isLetter() } ?: '#' }
            .toSortedMap(compareBy<Char> { if (it == '#') Char.MAX_VALUE else it })
    }
    val rows = remember(groups) {
        buildList<DrawerItem> {
            groups.forEach { (k, v) -> add(DrawerItem.Header(k.toString())); v.forEach { add(DrawerItem.Entry(it)) } }
        }
    }
    val headerAt = remember(rows) {
        rows.mapIndexedNotNull { i, r -> (r as? DrawerItem.Header)?.let { it.letter to i } }.toMap()
    }
    val letters = remember(groups) { listOf("★") + groups.keys.map { it.toString() } }
    val listState = rememberLazyListState()
    var active by remember { mutableIntStateOf(-1) }

    LaunchedEffect(active) {
        if (active in letters.indices) listState.scrollToItem(if (active == 0) 0 else headerAt[letters[active]] ?: 0)
    }

    Box(modifier.glassSurface(opacity)) {
        LazyColumn(
            state = listState, modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 40.dp, end = 100.dp, top = 230.dp, bottom = 140.dp),
        ) {
            if (rows.isEmpty()) item { Text("Nenhum app visível. Abra as configurações para mostrar.", color = LK.Cream, fontSize = 20.sp) }
            itemsIndexed(rows, key = { _, r -> when (r) { is DrawerItem.Header -> "h" + r.letter; is DrawerItem.Entry -> r.app.pkg } }) { index, r ->
                var entered by remember(rows) { mutableStateOf(false) }
                LaunchedEffect(rows) {
                    delay((index.coerceAtMost(12) * 35L))
                    entered = true
                }
                val entryAlpha = androidx.compose.animation.core.animateFloatAsState(
                    if (entered) 1f else 0f,
                    animationSpec = androidx.compose.animation.core.tween(220, easing = LauncherEasing),
                    label = "drawerEntryAlpha",
                )
                val entryOffset = androidx.compose.animation.core.animateDpAsState(
                    if (entered) 0.dp else 30.dp,
                    animationSpec = androidx.compose.animation.core.tween(280, easing = LauncherEasing),
                    label = "drawerEntryOffset",
                )
                val entryOffsetPx = with(LocalDensity.current) { entryOffset.value.toPx() }
                Box(
                    Modifier.alpha(entryAlpha.value).graphicsLayer { translationY = entryOffsetPx }
                ) {
                    when (r) {
                        is DrawerItem.Header -> Text(r.letter, color = LK.Cream, fontSize = 44.sp, modifier = Modifier.alpha(.9f))
                        is DrawerItem.Entry -> Row(
                            Modifier.fillMaxWidth()
                                .springPressClickable(onClick = { Apps.launch(ctx, r.app.pkg) }, onLongClick = { Apps.details(ctx, r.app.pkg) })
                                .padding(vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            if (showIcons) {
                                Box(Modifier.size(66.dp).border(4.dp, LK.Cream, CircleShape).padding(7.dp).clip(CircleShape)) {
                                    Image(r.app.icon, r.app.label, Modifier.fillMaxSize(), contentScale = ContentScale.Fit)
                                }
                                Spacer(Modifier.width(40.dp))
                            }
                            Text(r.app.label, color = LK.Cream, fontSize = 30.sp, fontWeight = FontWeight.Normal, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
            }
        }
        Row(
            Modifier.align(Alignment.TopCenter).statusBarsPadding().padding(top = 76.dp, start = 72.dp, end = 72.dp)
                .fillMaxWidth().background(Color(0xFF202429).copy(alpha = (opacity / 100f).coerceIn(0f, 1f)), RoundedCornerShape(24.dp))
                .border(1.dp, LK.Cream.copy(alpha = .35f), RoundedCornerShape(24.dp)).padding(horizontal = 18.dp, vertical = 13.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BasicTextField(
                value = query,
                onValueChange = { query = it },
                singleLine = true,
                textStyle = TextStyle(color = LK.Cream, fontSize = 18.sp),
                cursorBrush = SolidColor(LK.Cream),
                decorationBox = { inner ->
                    Box {
                        if (query.isEmpty()) Text("Buscar aplicativos", color = LK.Cream.copy(alpha = .55f), fontSize = 18.sp)
                        inner()
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            )
        }
        IconButton(onBack, Modifier.align(Alignment.TopStart).statusBarsPadding().padding(12.dp).background(Color(0x66000000), CircleShape)) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, "Voltar", tint = LK.Cream)
        }
        IconButton(onSettings, Modifier.align(Alignment.TopEnd).statusBarsPadding().padding(12.dp).background(Color(0x66000000), CircleShape)) {
            Icon(Icons.Outlined.Settings, "Configurações", tint = LK.Cream)
        }
        if (query.isBlank()) AlphabetBar(letters, active, { active = it }, { active = -1 }, Modifier.align(Alignment.CenterEnd))
    }
}

/** Alfabeto curvo: arraste o dedo; a letra ativa vira uma bolha e as vizinhas se curvam. */
@Composable
private fun AlphabetBar(letters: List<String>, active: Int, onActive: (Int) -> Unit, onEnd: () -> Unit, modifier: Modifier = Modifier) {
    val density = LocalDensity.current
    val n = letters.size
    BoxWithConstraints(
        modifier.width(120.dp).fillMaxHeight().pointerInput(n) {
            val top = size.height * .30f
            val step = (size.height * .88f - top) / (n - 1).coerceAtLeast(1)
            fun idx(y: Float) = ((y - top) / step).roundToInt().coerceIn(0, n - 1)
            awaitEachGesture {
                val down = awaitFirstDown()
                onActive(idx(down.position.y))
                do {
                    val ev = awaitPointerEvent()
                    val p = ev.changes.first()
                    onActive(idx(p.position.y)); p.consume()
                } while (ev.changes.any { it.pressed })
                onEnd()
            }
        }
    ) {
        val h = constraints.maxHeight.toFloat()
        val top = h * .30f
        val step = (h * .88f - top) / (n - 1).coerceAtLeast(1)
        val baseX = with(density) { 70.dp.toPx() }
        val maxBulge = with(density) { 70.dp.toPx() }
        letters.forEachIndexed { i, l ->
            val d = if (active >= 0) i - active else 99
            val bulge = if (active >= 0) cos(min(abs(d) * .42f, (PI / 2).toFloat())) * maxBulge else 0f
            val y = top + i * step
            Box(
                Modifier.offset { IntOffset((baseX - bulge).roundToInt(), (y - 18.dp.toPx()).roundToInt()) }
                    .alpha(if (i == active) 0f else 1f)
            ) {
                if (l == "★") Icon(Icons.Outlined.StarBorder, null, tint = LK.Cream, modifier = Modifier.size(28.dp))
                else Text(l, color = LK.Cream, fontSize = 28.sp)
            }
        }
        if (active in letters.indices) {
            Box(
                Modifier.offset { IntOffset((-90.dp.toPx()).roundToInt(), (top + active * step - 46.dp.toPx()).roundToInt()) }
                    .size(92.dp).background(Color(0xFFCBBDAE), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                if (letters[active] == "★") Icon(Icons.Outlined.StarBorder, null, tint = LK.Wine, modifier = Modifier.size(44.dp))
                else Text(letters[active], color = LK.Wine, fontSize = 58.sp, fontWeight = FontWeight.Medium)
            }
        }
    }
}
