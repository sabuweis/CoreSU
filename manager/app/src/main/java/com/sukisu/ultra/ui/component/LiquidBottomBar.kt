package com.sukisu.ultra.ui.component

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.ramcosta.composedestinations.generated.destinations.HomeScreenDestination
import com.ramcosta.composedestinations.generated.destinations.ModuleScreenDestination
import com.ramcosta.composedestinations.generated.destinations.SettingsScreenDestination
import com.ramcosta.composedestinations.generated.destinations.SuperUserScreenDestination
import com.sukisu.ultra.ui.component.liquid.InnerShadow
import com.sukisu.ultra.ui.component.liquid.innerShadow
import com.sukisu.ultra.ui.component.liquid.lens
import com.sukisu.ultra.ui.component.liquid.vibrancy
import top.yukonga.miuix.kmp.blur.Backdrop
import top.yukonga.miuix.kmp.blur.blur
import top.yukonga.miuix.kmp.blur.drawBackdrop

private val TAB_ICONS: List<ImageVector> = listOf(
    Icons.Filled.Home, Icons.Filled.Shield, Icons.Filled.Extension, Icons.Filled.Settings,
)

private val TAB_ROUTES: List<String> = listOf(
    HomeScreenDestination.route,
    SuperUserScreenDestination.route,
    ModuleScreenDestination.route,
    SettingsScreenDestination.route,
)

private fun routeToIndex(route: String?): Int {
    if (route.isNullOrBlank()) return 0
    val idx = TAB_ROUTES.indexOfFirst { route == it || route.startsWith(it) }
    return if (idx >= 0) idx else 0
}

@Composable
fun LiquidBottomBar(
    navController: NavController,
    backdrop: Backdrop,
    isBlurEnabled: Boolean,
    modifier: Modifier = Modifier,
) {
    val entry by navController.currentBackStackEntryAsState()
    val currentIndex = routeToIndex(entry?.destination?.route)
    val density = LocalDensity.current

    val containerColor = if (isBlurEnabled)
        MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.4f)
    else MaterialTheme.colorScheme.surfaceContainerHigh

    var tabWidthPx by remember { mutableFloatStateOf(0f) }
    val indicatorOffset by animateFloatAsState(
        targetValue = currentIndex * tabWidthPx,
        animationSpec = spring(dampingRatio = 0.8f, stiffness = 300f),
        label = "indicator",
    )
    val pillShape = remember { CircleShape }

    Box(
        modifier = modifier.fillMaxWidth()
            .windowInsetsPadding(WindowInsets.navigationBars)
            .padding(horizontal = 24.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Row(
            modifier = Modifier.widthIn(max = 480.dp)
                .onGloballyPositioned { c ->
                    tabWidthPx = ((c.size.width - with(density) { 8.dp.roundToPx() }) / 4).toFloat()
                }
                .selectableGroup()
                .shadow(elevation = 8.dp, shape = pillShape, clip = false)
                .then(
                    if (isBlurEnabled) {
                        Modifier.drawBackdrop(
                            backdrop = backdrop,
                            shape = { pillShape },
                            effects = {
                                padding = maxOf(padding, 40.dp.toPx())
                                vibrancy()
                                blur(4.dp.toPx(), 4.dp.toPx())
                                lens(refractionHeight = 24.dp.toPx(), refractionAmount = 24.dp.toPx())
                            },
                            onDrawSurface = { drawRect(containerColor) },
                        )
                    } else {
                        Modifier.background(containerColor, pillShape)
                    }
                )
                .height(64.dp).padding(4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (tabWidthPx > 0f) {
                Box(
                    Modifier.padding(horizontal = 4.dp)
                        .offset { IntOffset(indicatorOffset.toInt(), 0) }
                        .width(with(density) { tabWidthPx.toDp() })
                        .height(56.dp)
                        .clip(pillShape)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f), pillShape)
                        .innerShadow(shape = pillShape) {
                            InnerShadow(radius = 6.dp, color = Color.Black.copy(0.12f), alpha = 0.8f)
                        }
                )
            }
            TAB_ICONS.forEachIndexed { index, icon ->
                val selected = index == currentIndex
                Box(
                    Modifier.weight(1f).fillMaxHeight().clip(CircleShape)
                        .selectable(
                            selected = selected, role = Role.Tab,
                            onClick = {
                                if (!selected) {
                                    try {
                                        navController.navigate(TAB_ROUTES[index]) {
                                            popUpTo(navController.graph.startDestinationId) { saveState = true }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    } catch (_: Throwable) {}
                                }
                            },
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = icon, contentDescription = null,
                        tint = if (selected) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}
