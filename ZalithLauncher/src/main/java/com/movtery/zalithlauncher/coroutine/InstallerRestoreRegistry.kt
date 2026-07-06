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

package com.movtery.zalithlauncher.coroutine

import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.flow.StateFlow

/**
 * Registry that maps background proxy task IDs to their corresponding installer entries.
 *
 * When a TitleTaskFlowDialog is minimized, the running installer is registered here so
 * that tapping the background task item in the task menu can restore the full dialog.
 *
 * Both the popup dialog and the background task item observe the SAME [tasksFlow] from
 * the installer — there is no duplicated progress state.
 */
object InstallerRestoreRegistry {

    /**
     * A restorable installer entry stored in the registry.
     *
     * @param title     The dialog title to display when restored.
     * @param tasksFlow The live task list from the installer (same source as the popup dialog).
     * @param onCancel  Callback to cancel the installation and dismiss the dialog.
     */
    data class RestorableInstaller(
        val title: String,
        val tasksFlow: StateFlow<List<TitledTask>>,
        val onCancel: () -> Unit
    )

    private val registry = ConcurrentHashMap<String, RestorableInstaller>()

    /** Register a restorable installer entry for the given background task ID. */
    fun register(taskId: String, entry: RestorableInstaller) {
        registry[taskId] = entry
    }

    /** Remove the registry entry for the given task ID (call when the task ends). */
    fun unregister(taskId: String) {
        registry.remove(taskId)
    }

    /** Returns true if a restore entry exists for the given task ID. */
    fun hasEntry(taskId: String): Boolean = registry.containsKey(taskId)

    /** Returns the restore entry for the given task ID, or null if not present. */
    fun getEntry(taskId: String): RestorableInstaller? = registry[taskId]
}
