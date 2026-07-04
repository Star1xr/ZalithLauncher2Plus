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

package com.movtery.zalithlauncher.ui.screens.content.settings

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.movtery.zalithlauncher.R
import com.movtery.zalithlauncher.game.plugin.driver.Driver
import com.movtery.zalithlauncher.game.plugin.driver.DriverPluginManager
import com.movtery.zalithlauncher.game.plugin.renderer.RendererPluginManager
import com.movtery.zalithlauncher.game.renderer.RendererInterface
import com.movtery.zalithlauncher.game.renderer.Renderers
import com.movtery.zalithlauncher.game.version.installed.GraphicsApi
import com.movtery.zalithlauncher.path.URL_CLOUD_DRIVE_DRIVER_PLUGINS
import com.movtery.zalithlauncher.path.URL_CLOUD_RENDERER_PLUGINS
import com.movtery.zalithlauncher.path.URL_GITHUB_DRIVER_PLUGINS
import com.movtery.zalithlauncher.path.URL_GITHUB_RENDERER_PLUGINS
import com.movtery.zalithlauncher.setting.AllSettings
import com.movtery.zalithlauncher.setting.unit.floatRange
import com.movtery.zalithlauncher.ui.base.BaseScreen
import com.movtery.zalithlauncher.ui.components.AnimatedColumn
import com.movtery.zalithlauncher.ui.components.SimpleAlertDialog
import com.movtery.zalithlauncher.ui.components.verticalScrollWithBar
import com.movtery.zalithlauncher.ui.screens.NestedNavKey
import com.movtery.zalithlauncher.ui.screens.NormalNavKey
import com.movtery.zalithlauncher.ui.screens.TitledNavKey
import com.movtery.zalithlauncher.ui.screens.content.settings.layouts.CardPosition
import com.movtery.zalithlauncher.ui.screens.content.settings.layouts.IntSliderSettingsCard
import com.movtery.zalithlauncher.ui.screens.content.settings.layouts.ListSettingsCard
import com.movtery.zalithlauncher.ui.screens.content.settings.layouts.SettingsCard
import com.movtery.zalithlauncher.ui.screens.content.settings.layouts.SettingsCardColumn
import com.movtery.zalithlauncher.ui.screens.content.settings.layouts.SwitchSettingsCard
import com.movtery.zalithlauncher.ui.screens.navigateTo
import com.movtery.zalithlauncher.utils.fsr.FSRUtils
import com.movtery.zalithlauncher.utils.isAdrenoGPU
import com.movtery.zalithlauncher.viewmodel.EventViewModel
import com.movtery.zalithlauncher.viewmodel.sendDLPlugin

@Composable
fun RendererSettingsScreen(
    key: NestedNavKey.Settings,
    settingsScreenKey: TitledNavKey?,
    mainScreenKey: TitledNavKey?,
    eventViewModel: EventViewModel,
) {
    BaseScreen(
        Triple(key, mainScreenKey, false),
        Triple(NormalNavKey.Settings.Renderer, settingsScreenKey, false)
    ) { isVisible ->
        val context = LocalContext.current
        var showMobileGluesSettings by remember { mutableStateOf(false) }
        var driverToDelete by remember { mutableStateOf<Driver?>(null) }

        // Tab state: 0 = Built-in Renderers, 1 = App Renderers (external plugins)
        var selectedRendererTab by remember { mutableIntStateOf(0) }

        // Split all compatible renderers into built-in vs external plugin categories
        val allCompatibleRenderers = Renderers.getCompatibleRenderers(context).second
        val externalIdentifiers = RendererPluginManager.getRendererList()
            .map { it.uniqueIdentifier }
            .toSet()
        val builtInRenderers = remember(allCompatibleRenderers, externalIdentifiers) {
            allCompatibleRenderers.filter { it.getUniqueIdentifier() !in externalIdentifiers }
        }
        val externalRenderers = remember(allCompatibleRenderers, externalIdentifiers) {
            allCompatibleRenderers.filter { it.getUniqueIdentifier() in externalIdentifiers }
        }

        if (showMobileGluesSettings) {
            MobileGluesSettingsDialog(onDismissRequest = { showMobileGluesSettings = false })
        }

        driverToDelete?.let { driver ->
            SimpleAlertDialog(
                title = stringResource(R.string.generic_delete),
                text = stringResource(R.string.turnip_driver_delete_confirm, driver.name),
                confirmText = stringResource(R.string.generic_delete),
                onConfirm = {
                    java.io.File(driver.path).deleteRecursively()
                    DriverPluginManager.scanExternalDrivers(context)
                    if (AllSettings.vulkanDriver.getValue() == driver.id) {
                        AllSettings.vulkanDriver.save(AllSettings.vulkanDriver.defaultValue)
                    }
                    driverToDelete = null
                },
                onDismiss = { driverToDelete = null }
            )
        }

        AnimatedColumn(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScrollWithBar(state = rememberScrollState())
                .padding(all = 12.dp),
            isVisible = isVisible
        ) { scope ->

            AnimatedItem(scope) { yOffset ->
                SettingsCardColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .offset { IntOffset(x = 0, y = yOffset.roundToPx()) }
                ) {
                    // Renderer selection — tab row is embedded inside the dropdown card.
                    // The tab row content is declared once and passed as topContent to whichever
                    // card variant is shown so it always appears at the top of the same surface.
                    val rendererTabContent: @Composable () -> Unit = {
                        RendererTabRow(
                            selectedTab = selectedRendererTab,
                            onTabSelected = { selectedRendererTab = it },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 12.dp)
                        )
                    }

                    if (selectedRendererTab == 1 && externalRenderers.isEmpty()) {
                        // Empty state: no App Renderer plugins installed.
                        // Show the tab row at the top of a plain card, then an informational row.
                        SettingsCard(
                            modifier = Modifier.fillMaxWidth(),
                            position = CardPosition.Top
                        ) {
                            rendererTabContent()
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(start = 16.dp, end = 4.dp, top = 4.dp, bottom = 16.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = stringResource(R.string.settings_renderer_tab_external_empty_title),
                                        style = MaterialTheme.typography.titleSmall
                                    )
                                    Text(
                                        modifier = Modifier.alpha(0.7f),
                                        text = stringResource(R.string.settings_renderer_tab_external_empty),
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                }
                                IconButton(
                                    onClick = {
                                        eventViewModel.sendDLPlugin(
                                            githubLink = URL_GITHUB_RENDERER_PLUGINS,
                                            cloudDrives = listOf(
                                                EventViewModel.Event.DownloadPlugins.CloudDrive(
                                                    language = "zh",
                                                    link = URL_CLOUD_RENDERER_PLUGINS
                                                )
                                            )
                                        )
                                    }
                                ) {
                                    Icon(
                                        painter = painterResource(R.drawable.ic_download_2_filled),
                                        contentDescription = stringResource(R.string.generic_download)
                                    )
                                }
                            }
                        }
                    } else {
                        // Non-empty case: built-in tab (always) or external tab with plugins.
                        val rendererItems = if (selectedRendererTab == 0) builtInRenderers else externalRenderers
                        ListSettingsCard(
                            modifier = Modifier.fillMaxWidth(),
                            position = CardPosition.Top,
                            unit = AllSettings.renderer,
                            items = rendererItems,
                            title = stringResource(R.string.settings_renderer_global_renderer_title),
                            summary = stringResource(R.string.settings_renderer_global_renderer_summary),
                            getItemText = { it.getRendererName() },
                            getItemId = { it.getUniqueIdentifier() },
                            getItemSummary = {
                                RendererSummaryLayout(it)
                            },
                            getItemTrailing = { renderer ->
                                if (renderer.getRendererName() == "MobileGlues") {
                                    IconButton(onClick = { showMobileGluesSettings = true }) {
                                        Icon(
                                            painter = painterResource(R.drawable.ic_settings_filled),
                                            contentDescription = stringResource(R.string.generic_setting)
                                        )
                                    }
                                }
                            },
                            topContent = rendererTabContent,
                            trailingIcon = {
                                IconButton(
                                    onClick = {
                                        eventViewModel.sendDLPlugin(
                                            githubLink = URL_GITHUB_RENDERER_PLUGINS,
                                            cloudDrives = listOf(
                                                EventViewModel.Event.DownloadPlugins.CloudDrive(
                                                    language = "zh",
                                                    link = URL_CLOUD_RENDERER_PLUGINS
                                                )
                                            )
                                        )
                                    }
                                ) {
                                    Icon(
                                        painter = painterResource(R.drawable.ic_download_2_filled),
                                        contentDescription = stringResource(R.string.generic_download)
                                    )
                                }
                            }
                        )
                    }

                    ListSettingsCard(
                        modifier = Modifier.fillMaxWidth(),
                        position = CardPosition.Middle,
                        unit = AllSettings.vulkanDriver,
                        items = DriverPluginManager.getDriverList(),
                        title = stringResource(R.string.settings_renderer_global_vulkan_driver_title),
                        getItemText = { it.name },
                        getItemId = { it.id },
                        getItemSummary = {
                            DriverSummaryLayout(it)
                        },
                        getItemTrailing = { driver ->
                            if (driver.isExternal) {
                                IconButton(onClick = { driverToDelete = driver }) {
                                    Icon(
                                        modifier = Modifier.padding(4.dp),
                                        painter = painterResource(R.drawable.ic_delete_filled),
                                        contentDescription = stringResource(R.string.generic_delete),
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                        },
                        trailingIcon = {
                            IconButton(
                                onClick = {
                                    eventViewModel.sendDLPlugin(
                                        githubLink = URL_GITHUB_DRIVER_PLUGINS,
                                        cloudDrives = listOf(
                                            EventViewModel.Event.DownloadPlugins.CloudDrive(
                                                language = "zh",
                                                link = URL_CLOUD_DRIVE_DRIVER_PLUGINS
                                            )
                                        )
                                    )
                                }
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.ic_download_2_filled),
                                    contentDescription = stringResource(R.string.generic_download)
                                )
                            }
                        }
                    )

                    SettingsCard(
                        modifier = Modifier.fillMaxWidth(),
                        position = CardPosition.Middle,
                        title = stringResource(R.string.settings_renderer_download_turnip),
                        summary = stringResource(R.string.settings_renderer_download_turnip_summary),
                        onClick = {
                            key.backStack.navigateTo(NormalNavKey.Settings.TurnipDrivers)
                        },
                        trailingIcon = {
                            Row {
                                IconButton(
                                    onClick = {
                                        eventViewModel.sendEvent(EventViewModel.Event.OpenWeb("https://github.com/K11MCH1/AdrenoToolsDrivers/releases"))
                                    }
                                ) {
                                    Icon(
                                        painter = painterResource(R.drawable.ic_link),
                                        contentDescription = stringResource(R.string.generic_open_link)
                                    )
                                }
                                Icon(
                                    painter = painterResource(R.drawable.ic_download),
                                    contentDescription = null,
                                    modifier = Modifier.align(Alignment.CenterVertically).padding(end = 12.dp)
                                )
                            }
                        }
                    )

                    ListSettingsCard(
                        modifier = Modifier.fillMaxWidth(),
                        position = CardPosition.Middle,
                        unit = AllSettings.graphicsApi,
                        items = GraphicsApi.entries,
                        title = stringResource(R.string.settings_game_graphics_api_title),
                        summary = stringResource(R.string.settings_game_graphics_api_summary),
                        getItemText = {
                            when (it) {
                                GraphicsApi.DEFAULT -> stringResource(R.string.settings_game_graphics_api_default)
                                GraphicsApi.DEFAULT_OPENGL -> stringResource(R.string.settings_game_graphics_api_default_opengl)
                                else -> it.displayName
                            }
                        }
                    )

                    SwitchSettingsCard(
                        modifier = Modifier.fillMaxWidth(),
                        position = CardPosition.Middle,
                        unit = AllSettings.fsrEnabled,
                        title = stringResource(R.string.settings_renderer_fsr_title),
                        summary = stringResource(R.string.settings_renderer_fsr_summary),
                        onCheckedChange = { enabled ->
                            if (enabled) {
                                val ratio = FSRUtils.qualityToResolutionRatio(AllSettings.fsrQuality.getValue())
                                AllSettings.resolutionRatio.updateState(ratio)
                                AllSettings.resolutionRatio.save()
                            } else {
                                AllSettings.resolutionRatio.updateState(100)
                                AllSettings.resolutionRatio.save()
                            }
                        }
                    )

                    if (AllSettings.fsrEnabled.state) {
                        SettingsCardColumn(
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            val fsrQualityLabels = listOf(
                                1 to stringResource(R.string.settings_renderer_fsr_quality_ultra),
                                2 to stringResource(R.string.settings_renderer_fsr_quality_quality),
                                3 to stringResource(R.string.settings_renderer_fsr_quality_balanced),
                                4 to stringResource(R.string.settings_renderer_fsr_quality_performance)
                            )

                            ListSettingsCard(
                                modifier = Modifier.fillMaxWidth(),
                                position = CardPosition.Middle,
                                items = fsrQualityLabels,
                                currentId = AllSettings.fsrQuality.state.toString(),
                                defaultId = AllSettings.fsrQuality.defaultValue.toString(),
                                title = stringResource(R.string.settings_renderer_fsr_quality_title),
                                summary = stringResource(R.string.settings_renderer_fsr_quality_summary),
                                getItemText = { it.second },
                                getItemId = { it.first.toString() },
                                getItemSummary = {
                                    when (it.first) {
                                        1 -> "1.33x (1080p → 1440p)"
                                        2 -> "1.5x (720p → 1080p)"
                                        3 -> "1.7x (632p → 1080p)"
                                        4 -> "2.0x (540p → 1080p)"
                                        else -> ""
                                    }
                                },
                                onValueChange = { item ->
                                    AllSettings.fsrQuality.save(item.first)
                                    if (AllSettings.fsrEnabled.getValue()) {
                                        FSRUtils.updateQuality(item.first)
                                    }
                                }
                            )
                        }
                    }

                    IntSliderSettingsCard(
                        modifier = Modifier.fillMaxWidth(),
                        position = CardPosition.Middle,
                        unit = AllSettings.resolutionRatio,
                        title = stringResource(R.string.settings_renderer_resolution_scale_title),
                        summary = stringResource(R.string.settings_renderer_resolution_scale_summary),
                        valueRange = AllSettings.resolutionRatio.floatRange,
                        suffix = "%",
                        fineTuningControl = true
                    )

                    SwitchSettingsCard(
                        modifier = Modifier.fillMaxWidth(),
                        position = CardPosition.Bottom,
                        unit = AllSettings.gameFullScreen,
                        title = stringResource(R.string.settings_renderer_full_screen_title),
                        summary = stringResource(R.string.settings_renderer_full_screen_summary)
                    )
                }
            }

            AnimatedItem(scope) { yOffset ->
                SettingsCardColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .offset { IntOffset(x = 0, y = yOffset.roundToPx()) }
                ) {
                    SwitchSettingsCard(
                        modifier = Modifier.fillMaxWidth(),
                        position = CardPosition.Top,
                        unit = AllSettings.sustainedPerformance,
                        title = stringResource(R.string.settings_renderer_sustained_performance_title),
                        summary = stringResource(R.string.settings_renderer_sustained_performance_summary)
                    )

                    var adrenoGPUAlert by remember { mutableStateOf(false) }

                    SwitchSettingsCard(
                        modifier = Modifier.fillMaxWidth(),
                        position = CardPosition.Middle,
                        unit = AllSettings.zinkPreferSystemDriver,
                        title = stringResource(R.string.settings_renderer_vulkan_driver_system_title),
                        summary = stringResource(R.string.settings_renderer_vulkan_driver_system_summary),
                        onCheckedChange = { checked ->
                            if (checked && isAdrenoGPU()) adrenoGPUAlert = true
                        }
                    )

                    if (adrenoGPUAlert) {
                        SimpleAlertDialog(
                            title = stringResource(R.string.generic_warning),
                            text = stringResource(R.string.settings_renderer_zink_driver_adreno),
                            onConfirm = {
                                AllSettings.zinkPreferSystemDriver.save(true)
                                adrenoGPUAlert = false
                            },
                            onDismiss = {
                                AllSettings.zinkPreferSystemDriver.save(false)
                                adrenoGPUAlert = false
                            }
                        )
                    }

                    SwitchSettingsCard(
                        modifier = Modifier.fillMaxWidth(),
                        position = CardPosition.Middle,
                        unit = AllSettings.vsyncInZink,
                        title = stringResource(R.string.settings_renderer_vsync_in_zink_title),
                        summary = stringResource(R.string.settings_renderer_vsync_in_zink_summary)
                    )

                    SwitchSettingsCard(
                        modifier = Modifier.fillMaxWidth(),
                        position = CardPosition.Middle,
                        unit = AllSettings.bigCoreAffinity,
                        title = stringResource(R.string.settings_renderer_force_big_core_title),
                        summary = stringResource(R.string.settings_renderer_force_big_core_summary)
                    )

                    SwitchSettingsCard(
                        modifier = Modifier.fillMaxWidth(),
                        position = CardPosition.Middle,
                        unit = AllSettings.useSurfaceView,
                        title = stringResource(R.string.settings_renderer_surface_title),
                        summary = stringResource(R.string.settings_renderer_surface_summary)
                    )

                    SwitchSettingsCard(
                        modifier = Modifier.fillMaxWidth(),
                        position = CardPosition.Middle,
                        unit = AllSettings.dumpShaders,
                        title = stringResource(R.string.settings_renderer_shader_dump_title),
                        summary = stringResource(R.string.settings_renderer_shader_dump_summary)
                    )

                }
            }

        }
    }
}

/**
 * Pill-style tab row for switching between renderer categories.
 * Matches the design of the Zalith 2 / Legacy tab selector in Control Management.
 *
 * @param selectedTab 0 = Built-in Renderers, 1 = App Renderers
 * @param onTabSelected callback when the user picks a tab
 */
@Composable
private fun RendererTabRow(
    selectedTab: Int,
    onTabSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val tabs = listOf(
        Pair(stringResource(R.string.settings_renderer_tab_builtin), R.drawable.ic_build_outlined),
        Pair(stringResource(R.string.settings_renderer_tab_external), R.drawable.ic_extension_outlined)
    )
    BoxWithConstraints(
        modifier = modifier
            .height(52.dp)
            .clip(MaterialTheme.shapes.extraLarge)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(4.dp)
    ) {
        val tabWidth = maxWidth / tabs.size
        val indicatorOffset by animateDpAsState(
            targetValue = tabWidth * selectedTab,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessMediumLow
            ),
            label = "renderer_tab_indicator"
        )
        // Animated selection indicator
        Box(
            modifier = Modifier
                .offset(x = indicatorOffset)
                .width(tabWidth)
                .fillMaxHeight()
                .clip(MaterialTheme.shapes.extraLarge)
                .background(MaterialTheme.colorScheme.primaryContainer)
        )
        // Tab labels
        Row(modifier = Modifier.fillMaxSize()) {
            tabs.forEachIndexed { index, tabEntry ->
                val (label, iconRes) = tabEntry
                val isSelected = selectedTab == index
                val contentColor = if (isSelected)
                    MaterialTheme.colorScheme.onPrimaryContainer
                else
                    MaterialTheme.colorScheme.onSurfaceVariant
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clickable { onTabSelected(index) },
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            painter = painterResource(iconRes),
                            contentDescription = null,
                            tint = contentColor,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelLarge,
                            color = contentColor
                        )
                    }
                }
            }
        }
    }
}

@Composable
@OptIn(ExperimentalLayoutApi::class)
fun RendererSummaryLayout(renderer: RendererInterface) {
    FlowRow(
        modifier = Modifier.alpha(0.7f),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        with(renderer) {
            getRendererSummary()?.let { summary ->
                Text(text = summary, style = MaterialTheme.typography.labelSmall)
            }

            val minVer = getMinMCVersion()
            val maxVer = getMaxMCVersion()

            if (minVer != null || maxVer != null) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(text = stringResource(R.string.renderer_version_support), style = MaterialTheme.typography.labelSmall)

                    minVer?.let {
                        Text(text = ">= $it", style = MaterialTheme.typography.labelSmall)
                    }

                    maxVer?.let {
                        Text(text = "<= $it", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }
    }
}

@Composable
fun DriverSummaryLayout(driver: Driver) {
    with(driver) {
        summary?.let { text ->
            Text(
                modifier = Modifier.alpha(0.7f),
                text = text, style = MaterialTheme.typography.labelSmall
            )
        }
    }
}
