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

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.movtery.zalithlauncher.R
import com.movtery.zalithlauncher.ui.theme.cardColor
import com.movtery.zalithlauncher.ui.theme.onCardColor
import com.movtery.zalithlauncher.utils.settings.MobileGluesConfig

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MobileGluesSettingsDialog(onDismissRequest: () -> Unit) {
    val config = remember {
        MobileGluesConfig.load() ?: MobileGluesConfig()
    }

    var enableANGLE by remember { mutableIntStateOf(config.enableANGLE) }
    var enableNoError by remember { mutableIntStateOf(config.enableNoError) }
    var enableExtTimerQuery by remember { mutableStateOf(config.enableExtTimerQuery == 0) }
    var enableExtComputeShader by remember { mutableStateOf(config.enableExtComputeShader == 1) }
    var enableExtDirectStateAccess by remember { mutableStateOf(config.enableExtDirectStateAccess == 1) }
    var maxGlslCacheSize by remember { mutableStateOf(config.maxGlslCacheSize.toString()) }
    var multidrawMode by remember { mutableIntStateOf(config.multidrawMode) }
    var angleDepthClearFixMode by remember { mutableIntStateOf(config.angleDepthClearFixMode) }
    var customGLVersion by remember { mutableIntStateOf(config.customGLVersion) }
    var fsr1Setting by remember { mutableStateOf(config.fsr1Setting == 1) }

    val glVersions = listOf(
        0 to "Disabled", 32 to "OpenGL 3.2", 33 to "OpenGL 3.3",
        40 to "OpenGL 4.0", 41 to "OpenGL 4.1", 42 to "OpenGL 4.2",
        43 to "OpenGL 4.3", 44 to "OpenGL 4.4", 45 to "OpenGL 4.5",
        46 to "OpenGL 4.6"
    )

    Dialog(onDismissRequest = onDismissRequest) {
        Surface(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            shape = MaterialTheme.shapes.extraLarge,
            color = cardColor(false),
            contentColor = onCardColor(),
            shadowElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "MobileGlues Settings",
                    style = MaterialTheme.typography.titleLarge
                )

                HorizontalDivider()

                SectionHeader("Rendering")

                ANGLEPicker(
                    value = enableANGLE,
                    onValueChange = { enableANGLE = it }
                )

                NoErrorPicker(
                    value = enableNoError,
                    onValueChange = { enableNoError = it }
                )

                MultidrawModeSegmented(
                    value = multidrawMode,
                    onValueChange = { multidrawMode = it }
                )

                AngleClearPicker(
                    value = angleDepthClearFixMode,
                    onValueChange = { angleDepthClearFixMode = it }
                )

                GLVersionDropdown(
                    value = customGLVersion,
                    options = glVersions,
                    onValueChange = { customGLVersion = it }
                )

                HorizontalDivider()

                SectionHeader("Extensions")

                SettingsSwitchRow(
                    title = "Compute Shader",
                    summary = "Enable EXT_compute_shader",
                    checked = enableExtComputeShader,
                    onCheckedChange = { enableExtComputeShader = it }
                )

                SettingsSwitchRow(
                    title = "Timer Query",
                    summary = "Enable EXT_timer_query (inverted: off = enabled)",
                    checked = enableExtTimerQuery,
                    onCheckedChange = { enableExtTimerQuery = it }
                )

                SettingsSwitchRow(
                    title = "Direct State Access",
                    summary = "Enable EXT_direct_state_access",
                    checked = enableExtDirectStateAccess,
                    onCheckedChange = { enableExtDirectStateAccess = it }
                )

                SettingsSwitchRow(
                    title = "FSR1 Upscaling",
                    summary = "Enable FSR1 upscaling",
                    checked = fsr1Setting,
                    onCheckedChange = { fsr1Setting = it }
                )

                HorizontalDivider()

                SectionHeader("Cache")

                OutlinedTextField(
                    modifier = Modifier.fillMaxWidth(),
                    value = maxGlslCacheSize,
                    onValueChange = { maxGlslCacheSize = it },
                    label = { Text("GLSL Cache Size (MB)") },
                    supportingText = { Text("Set -1 to clear cache; 0 uses default (32)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(8.dp))

                Button(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        config.enableANGLE = enableANGLE
                        config.enableNoError = enableNoError
                        config.enableExtTimerQuery = if (enableExtTimerQuery) 0 else 1
                        config.enableExtComputeShader = if (enableExtComputeShader) 1 else 0
                        config.enableExtDirectStateAccess = if (enableExtDirectStateAccess) 1 else 0
                        config.maxGlslCacheSize = maxGlslCacheSize.toIntOrNull() ?: 32
                        config.multidrawMode = multidrawMode
                        config.angleDepthClearFixMode = angleDepthClearFixMode
                        config.customGLVersion = customGLVersion
                        config.fsr1Setting = if (fsr1Setting) 1 else 0
                        config.save()
                        onDismissRequest()
                    }
                ) {
                    Text(stringResource(R.string.generic_confirm))
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary
    )
}

@Composable
private fun SettingsSwitchRow(
    title: String,
    summary: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.bodyMedium)
            Text(
                text = summary,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.outline
            )
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun ANGLEPicker(value: Int, onValueChange: (Int) -> Unit) {
    val options = listOf("Disable" to 0, "Auto" to 1, "Force" to 2, "Compatible" to 3)
    DropdownSettingRow(
        label = "ANGLE Mode",
        options = options,
        selectedValue = value,
        onValueChange = onValueChange
    )
}

@Composable
private fun NoErrorPicker(value: Int, onValueChange: (Int) -> Unit) {
    val options = listOf("Strict" to 0, "Moderate" to 1, "Relaxed" to 2, "Ignore All" to 3)
    DropdownSettingRow(
        label = "No Error Mode",
        options = options,
        selectedValue = value,
        onValueChange = onValueChange
    )
}

@Composable
private fun MultidrawModeSegmented(value: Int, onValueChange: (Int) -> Unit) {
    Text("Multidraw Mode", style = MaterialTheme.typography.bodyMedium)
    Spacer(modifier = Modifier.height(4.dp))
    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
        listOf("Auto" to 0, "BaseVertex" to 1, "Compute" to 2).forEachIndexed { index, (label, v) ->
            SegmentedButton(
                selected = value == v,
                onClick = { onValueChange(v) },
                shape = SegmentedButtonDefaults.itemShape(index = index, count = 3),
                label = { Text(label) }
            )
        }
    }
}

@Composable
private fun AngleClearPicker(value: Int, onValueChange: (Int) -> Unit) {
    val options = listOf("Off" to 0, "Mode 1" to 1, "Mode 2" to 2)
    DropdownSettingRow(
        label = "ANGLE Depth Clear Fix",
        options = options,
        selectedValue = value,
        onValueChange = onValueChange
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GLVersionDropdown(
    value: Int,
    options: List<Pair<Int, String>>,
    onValueChange: (Int) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedLabel = options.find { it.first == value }?.second ?: "Disabled"

    Text("Custom GL Version", style = MaterialTheme.typography.bodyMedium)
    Spacer(modifier = Modifier.height(4.dp))

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it }
    ) {
        OutlinedTextField(
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(),
            value = selectedLabel,
            onValueChange = {},
            readOnly = true,
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            singleLine = true
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            options.forEach { (v, label) ->
                DropdownMenuItem(
                    text = { Text(label) },
                    onClick = {
                        onValueChange(v)
                        expanded = false
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DropdownSettingRow(
    label: String,
    options: List<Pair<String, Int>>,
    selectedValue: Int,
    onValueChange: (Int) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedLabel = options.find { it.second == selectedValue }?.first ?: options.first().first

    Text(label, style = MaterialTheme.typography.bodyMedium)
    Spacer(modifier = Modifier.height(4.dp))

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it }
    ) {
        OutlinedTextField(
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(),
            value = selectedLabel,
            onValueChange = {},
            readOnly = true,
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            singleLine = true
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            options.forEach { (label, v) ->
                DropdownMenuItem(
                    text = { Text(label) },
                    onClick = {
                        onValueChange(v)
                        expanded = false
                    }
                )
            }
        }
    }
}
