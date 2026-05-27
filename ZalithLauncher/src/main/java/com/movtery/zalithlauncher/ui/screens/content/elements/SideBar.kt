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
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.movtery.zalithlauncher.R
import com.movtery.zalithlauncher.ui.components.BackgroundCard
import com.movtery.zalithlauncher.ui.theme.cardColor
import kotlinx.coroutines.delay
import org.lwjgl.glfw.CallbackBridge

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
    var expanded by remember { mutableStateOf(false) }

    LaunchedEffect(isVisible) {
        if (isVisible) {
            while (true) {
                fps = CallbackBridge.getCurrentFps()
                memoryInfo = getMemoryInfo()
                delay(1000)
            }
        }
    }

    BackgroundCard(
        modifier = modifier
            .width(if (expanded) 80.dp else 56.dp)
            .fillMaxHeight(),
        shape = MaterialTheme.shapes.extraLarge
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            SideBarToggle(
                expanded = expanded,
                onClick = { expanded = !expanded }
            )

            AnimatedVisibility(
                visible = expanded,
                enter = fadeIn(animationSpec = tween(200)) +
                    slideInVertically(animationSpec = tween(200)) { it / 4 },
                exit = fadeOut(animationSpec = tween(150)) +
                    slideOutVertically(animationSpec = tween(150)) { it / 4 }
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    HorizontalDivider(
                        modifier = Modifier
                            .padding(horizontal = 16.dp)
                            .alpha(0.3f)
                    )

                    SideBarIndicator(
                        label = "FPS",
                        value = fps.toString(),
                        icon = painterResource(R.drawable.ic_video_settings),
                        onClick = onFpsClick
                    )

                    SideBarIndicator(
                        label = "RAM",
                        value = "${memoryInfo.first}M",
                        icon = painterResource(R.drawable.ic_dashboard_outlined),
                        onClick = onRamClick
                    )

                    HorizontalDivider(
                        modifier = Modifier
                            .padding(horizontal = 16.dp)
                            .alpha(0.3f)
                    )

                    SideBarShortcut(
                        icon = painterResource(R.drawable.ic_assignment_filled),
                        contentDescription = "Versions",
                        onClick = onVersionsClick
                    )

                    SideBarShortcut(
                        icon = painterResource(R.drawable.ic_info_outlined),
                        contentDescription = "About",
                        onClick = onInfoClick
                    )
                }
            }
        }
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
        targetValue = if (isPressed) 0.88f else 1f,
        animationSpec = tween(100),
        label = "toggleScale"
    )

    Surface(
        modifier = Modifier
            .size(40.dp)
            .scale(scale)
            .clip(RoundedCornerShape(12.dp))
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            ),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f),
        tonalElevation = if (isPressed) 0.dp else 2.dp,
        shadowElevation = if (isPressed) 0.dp else 3.dp
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = if (expanded) painterResource(R.drawable.ic_arrow_left_rounded)
                    else painterResource(R.drawable.ic_menu),
                contentDescription = if (expanded) "Collapse" else "Expand",
                modifier = Modifier.size(22.dp),
                tint = MaterialTheme.colorScheme.onPrimaryContainer
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
        animationSpec = tween(120),
        label = "scale"
    )

    Surface(
        modifier = Modifier
            .padding(horizontal = 8.dp, vertical = 2.dp)
            .scale(scale)
            .clip(RoundedCornerShape(16.dp))
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            ),
        shape = RoundedCornerShape(16.dp),
        color = cardColor(false).copy(alpha = 0.7f),
        tonalElevation = if (isPressed) 1.dp else 3.dp,
        shadowElevation = if (isPressed) 1.dp else 4.dp
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .padding(horizontal = 12.dp, vertical = 10.dp)
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
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
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
        targetValue = if (isPressed) 0.88f else 1f,
        animationSpec = tween(100),
        label = "shortcutScale"
    )

    Surface(
        modifier = Modifier
            .size(48.dp)
            .scale(scale)
            .clip(RoundedCornerShape(14.dp))
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            ),
        shape = RoundedCornerShape(14.dp),
        color = cardColor(false).copy(alpha = 0.5f),
        tonalElevation = if (isPressed) 0.dp else 2.dp,
        shadowElevation = if (isPressed) 0.dp else 3.dp
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = icon,
                contentDescription = contentDescription,
                modifier = Modifier.size(24.dp),
                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f)
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
