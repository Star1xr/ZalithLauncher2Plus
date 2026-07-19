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

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.movtery.zalithlauncher.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.movtery.zalithlauncher.game.plugin.driver.Driver
import com.movtery.zalithlauncher.game.plugin.driver.DriverPluginManager
import com.movtery.zalithlauncher.game.plugin.renderer_v2.RendererV2Data
import com.movtery.zalithlauncher.game.renderer.RendererInterface
import com.movtery.zalithlauncher.game.renderer.Renderers
import com.movtery.zalithlauncher.game.renderer.renderers.KopperZinkRenderer
import com.movtery.zalithlauncher.game.version.installed.GraphicsApi
import com.movtery.zalithlauncher.path.URL_CLOUD_DRIVE_DRIVER_PLUGINS
import com.movtery.zalithlauncher.path.URL_CLOUD_RENDERER_PLUGINS
import com.movtery.zalithlauncher.path.URL_GITHUB_DRIVER_PLUGINS
import com.movtery.zalithlauncher.utils.driver.TurnipDownloader
import com.movtery.zalithlauncher.path.URL_GITHUB_RENDERER_PLUGINS
import com.movtery.zalithlauncher.bridge.ZLBridge
import com.movtery.zalithlauncher.setting.AllSettings
import com.movtery.zalithlauncher.setting.unit.floatRange
import com.movtery.zalithlauncher.ui.base.BaseScreen
import com.movtery.zalithlauncher.utils.settings.MobileGluesConfig
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
import com.movtery.zalithlauncher.utils.device.checkVulkanSupport
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
        var showBenchmark by remember { mutableStateOf(false) }
        var driverToDelete by remember { mutableStateOf<Driver?>(null) }

        if (showMobileGluesSettings) {
            MobileGluesSettingsDialog(onDismissRequest = { showMobileGluesSettings = false })
        }

        if (showBenchmark) {
            Dialog(
                onDismissRequest = { showBenchmark = false },
                properties = DialogProperties(usePlatformDefaultWidth = false)
            ) {
                RendererBenchmarkOverlay(
                    availableRenderers = Renderers.getRenderers(),
                    onDismiss = { showBenchmark = false }
                )
            }
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
                    RunBenchmarkPill(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp),
                        onClick = { showBenchmark = true }
                    )
                    val currentRendererId = AllSettings.renderer.state
                    val v2PluginEnvUnits = remember(currentRendererId) {
                        Renderers.getRenderers()
                            .filterIsInstance<RendererV2Data>()
                            .find { it.getUniqueIdentifier() == currentRendererId }
                            ?.env?.getConfigurableUnits()?.takeIf { it.isNotEmpty() }
                    }
                    var showV2ConfigDialog by remember { mutableStateOf(false) }

                    ListSettingsCard(
                        modifier = Modifier.fillMaxWidth(),
                        position = CardPosition.Top,
                        unit = AllSettings.renderer,
                        items = Renderers.getRenderers(),
                        title = stringResource(R.string.settings_renderer_global_renderer_title),
                        summary = stringResource(R.string.settings_renderer_global_renderer_summary),
                        getItemText = { it.getRendererName() },
                        getItemId = { it.getUniqueIdentifier() },
                        getItemSummary = { renderer ->
                            Column {
                                RendererSummaryLayout(renderer)
                                if (renderer.getRendererName() == "MobileGlues") {
                                    val hasConfig = remember { MobileGluesConfig.load() != null }
                                    if (hasConfig) {
                                        Text(
                                            text = "✓ Configured",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            }
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
                        trailingIcon = {
                            //选中新一代渲染器插件且存在可配置项时，提供配置入口
                            if (v2PluginEnvUnits != null) {
                                IconButton(
                                    onClick = { showV2ConfigDialog = true }
                                ) {
                                    Icon(
                                        painter = painterResource(R.drawable.ic_settings_filled),
                                        contentDescription = stringResource(R.string.settings_renderer_config_title)
                                    )
                                }
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
                    )

                    //新一代渲染器插件的环境变量配置对话框
                    if (showV2ConfigDialog && v2PluginEnvUnits != null) {
                        RendererV2ConfigDialog(
                            units = v2PluginEnvUnits,
                            onDismissRequest = { showV2ConfigDialog = false }
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
                                        eventViewModel.sendEvent(EventViewModel.Event.OpenWeb(TurnipDownloader.getRepoReleasesUrl()))
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

                    IntSliderSettingsCard(
                        modifier = Modifier.fillMaxWidth(),
                        position = CardPosition.Middle,
                        value = AllSettings.fpsLimit.state,
                        onValueChange = { AllSettings.fpsLimit.updateState(it) },
                        onValueChangeFinished = {
                            val fps = AllSettings.fpsLimit.state
                            AllSettings.fpsLimit.save(fps)
                            ZLBridge.fpsLimitSet(fps)
                        },
                        title = stringResource(R.string.settings_renderer_fps_limit_title),
                        summary = stringResource(R.string.settings_renderer_fps_limit_summary),
                        valueRange = AllSettings.fpsLimit.floatRange,
                        suffix = " FPS",
                        fineTuningControl = false
                    )

                    val isKopperZinkSelected = AllSettings.renderer.state == KopperZinkRenderer.getUniqueIdentifier()
                    var surfaceViewAutoDisabledAlert by remember { mutableStateOf(false) }

                    LaunchedEffect(isKopperZinkSelected) {
                        if (isKopperZinkSelected && AllSettings.useSurfaceView.state) {
                            AllSettings.useSurfaceView.save(false)
                            if (!AllSettings.surfaceViewKopperWarningDontShow.state) {
                                surfaceViewAutoDisabledAlert = true
                            }
                        }
                    }

                    SwitchSettingsCard(
                        modifier = Modifier.fillMaxWidth(),
                        position = CardPosition.Middle,
                        //Kopper Zink 选中时，无论保存的偏好值是什么，都在界面上显示为关闭+禁用状态
                        checked = AllSettings.useSurfaceView.state && !isKopperZinkSelected,
                        enabled = !isKopperZinkSelected,
                        onCheckedChange = { checked ->
                            AllSettings.useSurfaceView.save(checked)
                        },
                        title = stringResource(R.string.settings_renderer_surface_title),
                        summary = if (isKopperZinkSelected) {
                            stringResource(R.string.settings_renderer_surface_summary_kopper_disabled)
                        } else {
                            stringResource(R.string.settings_renderer_surface_summary)
                        }
                    )

                    if (surfaceViewAutoDisabledAlert) {
                        AlertDialog(
                            onDismissRequest = { surfaceViewAutoDisabledAlert = false },
                            title = { Text(stringResource(R.string.generic_warning)) },
                            text = { Text(stringResource(R.string.settings_renderer_surface_kopper_warning)) },
                            confirmButton = {
                                Button(onClick = {
                                    AllSettings.surfaceViewKopperWarningDontShow.save(true)
                                    surfaceViewAutoDisabledAlert = false
                                }) {
                                    Text(stringResource(R.string.settings_renderer_surface_kopper_warning_dont_show))
                                }
                            },
                            dismissButton = {
                                OutlinedButton(onClick = { surfaceViewAutoDisabledAlert = false }) {
                                    Text(stringResource(R.string.generic_confirm))
                                }
                            }
                        )
                    }

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

@Composable
private fun RunBenchmarkPill(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colorScheme = MaterialTheme.colorScheme
    val gradient = remember(colorScheme) {
        Brush.horizontalGradient(
            listOf(colorScheme.primary, colorScheme.tertiary)
        )
    }

    Surface(
        modifier = modifier,
        shape = CircleShape,
        color = Color.Transparent,
        shadowElevation = 3.dp,
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .background(brush = gradient, shape = CircleShape)
                .padding(horizontal = 20.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_rocket_launch_filled),
                contentDescription = null,
                tint = colorScheme.onPrimary,
                modifier = Modifier.size(20.dp)
            )
            Text(
                modifier = Modifier.padding(start = 10.dp),
                text = stringResource(R.string.benchmark_run),
                style = MaterialTheme.typography.titleSmall,
                color = colorScheme.onPrimary
            )
        }
    }
}

