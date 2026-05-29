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

package com.movtery.zalithlauncher.ui.screens.game.elements

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.movtery.zalithlauncher.R
import com.movtery.zalithlauncher.setting.AllSettings
import com.movtery.zalithlauncher.ui.components.MarqueeText
import com.movtery.zalithlauncher.ui.screens.main.control_editor.InfoLayoutSliderItem
import com.movtery.zalithlauncher.ui.screens.main.control_editor.InfoLayoutSwitchItem
import com.movtery.zalithlauncher.ui.screens.main.control_editor.InfoLayoutSelectItem
import com.movtery.zalithlauncher.ui.theme.cardColor
import com.movtery.zalithlauncher.ui.theme.onCardColor

sealed interface PerformanceSettingsOperation {
    data object None : PerformanceSettingsOperation
    data object Fps : PerformanceSettingsOperation
    data object Ram : PerformanceSettingsOperation
}

@Composable
fun PerformanceSettingsDialog(
    operation: PerformanceSettingsOperation,
    onDismissRequest: () -> Unit
) {
    when (operation) {
        PerformanceSettingsOperation.None -> {}
        PerformanceSettingsOperation.Fps -> {
            PerformanceDialog(
                title = stringResource(R.string.game_menu_option_fps_settings),
                onDismissRequest = onDismissRequest
            ) {
                InfoLayoutSwitchItem(
                    modifier = Modifier.fillMaxWidth(),
                    title = stringResource(R.string.game_menu_option_switch_fps),
                    value = AllSettings.showFPS.state,
                    onValueChange = { AllSettings.showFPS.save(it) }
                )
                InfoLayoutSliderItem(
                    modifier = Modifier.fillMaxWidth(),
                    title = stringResource(R.string.settings_renderer_resolution_scale_title),
                    value = (AllSettings.resolutionRatio.state ?: 100).toFloat(),
                    onValueChange = { AllSettings.resolutionRatio.updateState(it.toInt()) },
                    onValueChangeFinished = { AllSettings.resolutionRatio.save() },
                    valueRange = 25f..300f,
                    decimalFormat = "#0",
                    suffix = "%"
                )

                InfoLayoutSwitchItem(
                    modifier = Modifier.fillMaxWidth(),
                    title = stringResource(R.string.settings_renderer_fsr_title),
                    value = AllSettings.fsrEnabled.state,
                    onValueChange = {
                        AllSettings.fsrEnabled.save(it)
                        if (it) AllSettings.resolutionRatio.save(100)
                    }
                )

                if (AllSettings.fsrEnabled.state) {
                    InfoLayoutSelectItem(
                        modifier = Modifier.fillMaxWidth(),
                        title = stringResource(R.string.settings_renderer_fsr_quality_title),
                        options = listOf(1, 2, 3, 4),
                        current = AllSettings.fsrQuality.state ?: 2,
                        onClick = { AllSettings.fsrQuality.save(it) },
                        label = { quality ->
                            Text(
                                text = when (quality) {
                                    1 -> stringResource(R.string.settings_renderer_fsr_quality_ultra)
                                    2 -> stringResource(R.string.settings_renderer_fsr_quality_quality)
                                    3 -> stringResource(R.string.settings_renderer_fsr_quality_balanced)
                                    4 -> stringResource(R.string.settings_renderer_fsr_quality_performance)
                                    else -> ""
                                },
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    )
                }
            }
        }
        PerformanceSettingsOperation.Ram -> {
            PerformanceDialog(
                title = stringResource(R.string.game_menu_option_ram_settings),
                onDismissRequest = onDismissRequest
            ) {
                InfoLayoutSwitchItem(
                    modifier = Modifier.fillMaxWidth(),
                    title = stringResource(R.string.game_menu_option_switch_memory),
                    value = AllSettings.showMemory.state,
                    onValueChange = { AllSettings.showMemory.save(it) }
                )

                InfoLayoutSliderItem(
                    modifier = Modifier.fillMaxWidth(),
                    title = stringResource(R.string.settings_game_java_memory_title),
                    value = AllSettings.ramAllocation.state?.toFloat() ?: 1024f,
                    onValueChange = { AllSettings.ramAllocation.updateState(it.toInt()) },
                    onValueChangeFinished = { AllSettings.ramAllocation.save() },
                    valueRange = 256f..8192f, // Adjust range as needed
                    decimalFormat = "#0",
                    suffix = "MB"
                )
                
                Text(
                    modifier = Modifier.padding(top = 8.dp),
                    text = stringResource(R.string.settings_game_java_memory_summary),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }
        }
    }
}

@Composable
private fun PerformanceDialog(
    title: String,
    onDismissRequest: () -> Unit,
    content: @Composable () -> Unit
) {
    Dialog(onDismissRequest = onDismissRequest) {
        Surface(
            modifier = Modifier
                .padding(all = 16.dp)
                .fillMaxWidth()
                .wrapContentHeight(),
            shape = MaterialTheme.shapes.extraLarge,
            color = cardColor(false),
            contentColor = onCardColor(),
            shadowElevation = 6.dp
        ) {
            Column(
                modifier = Modifier.padding(all = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge
                )

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    content()
                }

                Button(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = onDismissRequest
                ) {
                    MarqueeText(text = stringResource(R.string.generic_confirm))
                }
            }
        }
    }
}
