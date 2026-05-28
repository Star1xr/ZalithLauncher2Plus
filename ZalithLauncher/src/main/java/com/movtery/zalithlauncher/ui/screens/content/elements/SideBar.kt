/*
 * Zalith Launcher 2
 * Copyright (C) 2025 MovTery <movtery228@qq.com> and contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/gpl-3.0.txt>.
 */

package com.movtery.zalithlauncher.ui.screens.content.elements

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.movtery.zalithlauncher.R
import kotlinx.coroutines.delay
import org.lwjgl.glfw.CallbackBridge

private val CollapsedWidth = 56.dp
private val ExpandedWidth = 84.dp

@Composable
fun SideBar(
    modifier: Modifier = Modifier,
    isVisible: Boolean,
    onFpsClick: () -> Unit,
    onRamClick: () -> Unit,
    onVersionsClick: () -> Unit,
    onInfoClick: () -> Unit
) {
    var fps by remember { mutableIntStateOf(0) }
    var memoryInfo by remember { mutableStateOf(getMemoryInfo()) }
    var expanded by rememberSaveable { mutableStateOf(false) }

    val targetWidth by animateDpAsState(
        targetValue = if (expanded) ExpandedWidth else CollapsedWidth,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "sidebarWidth"
    )

    LaunchedEffect(isVisible) {
        if (isVisible) {
            while (true) {
                fps = CallbackBridge.getCurrentFps()
                memoryInfo = getMemoryInfo()
                delay(1000)
            }
        }
    }

    Box(
        modifier = modifier
            .width(targetWidth)
            .fillMaxHeight()
            .padding(vertical = 8.dp)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .blur(0.dp),
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.35f),
            tonalElevation = 0.dp,
            shadowElevation = 0.dp
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.12f),
                                MaterialTheme.colorScheme.surface.copy(alpha = 0.08f)
                            )
                        )
                    )
            )
        }

        Surface(
            modifier = Modifier
                .fillMaxSize(),
            shape = RoundedCornerShape(20.dp),
            color = Color.Transparent,
            tonalElevation = 0.dp,
            shadowElevation = 0.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(vertical = 10.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                AnimatedVisibility(
                    visible = expanded,
                    enter = fadeIn(animationSpec = tween(250)) +
                        slideInVertically(
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                stiffness = Spring.StiffnessMedium
                            )
                        ) { it / 3 },
                    exit = fadeOut(animationSpec = tween(150)) +
                        slideOutVertically(
                            animationSpec = tween(150)
                        ) { it / 3 }
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        HorizontalDivider(
                            modifier = Modifier
                                .padding(horizontal = 18.dp)
                                .alpha(0.2f)
                        )

                        Spacer(modifier = Modifier.height(2.dp))

                        StaggeredItem(delay = 0) {
                            SideBarIndicator(
                                label = "FPS",
                                value = fps.toString(),
                                icon = painterResource(R.drawable.ic_video_settings),
                                onClick = onFpsClick
                            )
                        }

                        StaggeredItem(delay = 60) {
                            SideBarIndicator(
                                label = "RAM",
                                value = "${memoryInfo.first}M",
                                icon = painterResource(R.drawable.ic_dashboard_outlined),
                                onClick = onRamClick
                            )
                        }

                        HorizontalDivider(
                            modifier = Modifier
                                .padding(horizontal = 18.dp)
                                .alpha(0.2f)
                        )

                        Spacer(modifier = Modifier.height(2.dp))

                        StaggeredItem(delay = 120) {
                            SideBarShortcut(
                                icon = painterResource(R.drawable.ic_assignment_filled),
                                contentDescription = "Versions",
                                onClick = onVersionsClick
                            )
                        }

                        StaggeredItem(delay = 180) {
                            SideBarShortcut(
                                icon = painterResource(R.drawable.ic_info_outlined),
                                contentDescription = "About",
                                onClick = onInfoClick
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.weight(1f))
                SideBarToggle(
                    expanded = expanded,
                    onClick = { expanded = !expanded }
                )
            }
        }
    }
}

@Composable
private fun StaggeredItem(
    delay: Int,
    visible: Boolean = true,
    content: @Composable () -> Unit
) {
    var show by remember { mutableStateOf(!visible) }

    LaunchedEffect(visible) {
        if (visible) {
            kotlinx.coroutines.delay(delay.toLong())
            show = true
        } else {
            show = false
        }
    }

    AnimatedVisibility(
        visible = show,
        enter = fadeIn(animationSpec = tween(200)) +
            slideInVertically(
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMedium
                )
            ) { it },
        exit = fadeOut(animationSpec = tween(100))
    ) {
        content()
    }
}

@Composable
private fun SideBarToggle(
    expanded: Boolean,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.85f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessHigh
        ),
        label = "toggleScale"
    )

    val rotation by animateFloatAsState(
        targetValue = if (expanded) 180f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "toggleRotation"
    )

    Surface(
        modifier = Modifier
            .size(58.dp)
            .scale(scale)
            .shadow(
                elevation = if (isPressed) 1.dp else 6.dp,
                shape = RoundedCornerShape(14.dp),
                ambientColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                spotColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
            )
            .clip(RoundedCornerShape(14.dp))
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            ),
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f),
        tonalElevation = if (isPressed) 0.dp else 3.dp,
        shadowElevation = 0.dp
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                painter = if (expanded) painterResource(R.drawable.ic_arrow_left_rounded)
                    else painterResource(R.drawable.ic_menu),
                contentDescription = if (expanded) "Collapse" else "Expand",
                modifier = Modifier.size(28.dp),
                tint = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Text(
                text = stringResource(R.string.sidebar_shortcut_menu),
                style = MaterialTheme.typography.labelSmall,
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.9f)
            )
        }
    }
}

@Composable
private fun SideBarIndicator(
    label: String,
    value: String,
    icon: Painter,
    onClick: () -> Unit = {}
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.92f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessHigh
        ),
        label = "indicatorScale"
    )

    val elevation by animateDpAsState(
        targetValue = if (isPressed) 2.dp else 8.dp,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "indicatorElevation"
    )

    Surface(
        modifier = Modifier
            .padding(horizontal = 6.dp, vertical = 2.dp)
            .scale(scale)
            .shadow(
                elevation = elevation,
                shape = RoundedCornerShape(18.dp),
                ambientColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                spotColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
            )
            .clip(RoundedCornerShape(18.dp))
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            ),
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
        tonalElevation = if (isPressed) 1.dp else 4.dp,
        shadowElevation = 0.dp
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .padding(horizontal = 10.dp, vertical = 10.dp)
        ) {
            Icon(
                painter = icon,
                contentDescription = label,
                modifier = Modifier.size(22.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.labelSmall,
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f)
            )
        }
    }
}

@Composable
private fun SideBarShortcut(
    icon: Painter,
    contentDescription: String,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.85f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessHigh
        ),
        label = "shortcutScale"
    )

    val elevation by animateDpAsState(
        targetValue = if (isPressed) 1.dp else 8.dp,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "shortcutElevation"
    )

    Surface(
        modifier = Modifier
            .size(46.dp)
            .scale(scale)
            .shadow(
                elevation = elevation,
                shape = RoundedCornerShape(16.dp),
                ambientColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                spotColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
            )
            .clip(RoundedCornerShape(16.dp))
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            ),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
        tonalElevation = if (isPressed) 1.dp else 3.dp,
        shadowElevation = 0.dp
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = icon,
                contentDescription = contentDescription,
                modifier = Modifier.size(24.dp),
                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
            )
        }
    }
}

private fun getMemoryInfo(): Pair<Long, Long> {
    val runtime = Runtime.getRuntime()
    val used = (runtime.totalMemory() - runtime.freeMemory()) / 1024 / 1024
    val total = runtime.maxMemory() / 1024 / 1024
    return Pair(used, total)
}
