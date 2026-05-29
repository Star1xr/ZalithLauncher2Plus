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

package com.movtery.zalithlauncher.game.version.installed

import com.movtery.zalithlauncher.setting.launcherMMKV

object PlayTimeRepository {
    private fun lastPlayedKey(versionName: String) = "pt_last_$versionName"
    private fun totalPlayTimeKey(versionName: String) = "pt_total_$versionName"

    fun getLastPlayed(versionName: String): Long =
        launcherMMKV().getLong(lastPlayedKey(versionName), 0L)

    fun getTotalPlayTime(versionName: String): Long =
        launcherMMKV().getLong(totalPlayTimeKey(versionName), 0L)

    fun recordSession(versionName: String, sessionStartMs: Long, sessionEndMs: Long) {
        val duration = sessionEndMs - sessionStartMs
        if (duration <= 0) return
        val mmkv = launcherMMKV()
        mmkv.putLong(lastPlayedKey(versionName), sessionEndMs).apply()
        val previous = mmkv.getLong(totalPlayTimeKey(versionName), 0L)
        mmkv.putLong(totalPlayTimeKey(versionName), previous + duration).apply()
    }
}
