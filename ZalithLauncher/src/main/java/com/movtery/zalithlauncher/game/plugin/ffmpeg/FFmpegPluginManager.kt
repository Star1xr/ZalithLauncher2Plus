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

package com.movtery.zalithlauncher.game.plugin.ffmpeg

import com.movtery.zalithlauncher.path.PathManager
import com.movtery.zalithlauncher.utils.logging.Logger
import java.io.File

private const val TAG = "FFmpegPlugin"

/**
 * 内置的 FFmpeg 支持
 * FFmpeg 原生库随应用一同打包（jniLibs），无需再额外安装 FFmpegPlugin 插件应用
 * 参考: https://github.com/PojavLauncherTeam/FFmpegPlugin
 */
object FFmpegPluginManager {
    var libraryPath: String? = null
        private set

    var executablePath: String? = null
        private set

    /**
     * FFmpeg 是否可用
     */
    var isAvailable: Boolean = false
        private set

    /**
     * 加载内置的 FFmpeg 原生库
     */
    fun loadPlugin() {
        runCatching {
            val nativeLibDir = PathManager.DIR_NATIVE_LIB
            libraryPath = nativeLibDir
            val ffmpegExecutable = File(nativeLibDir, "libffmpeg.so")
            executablePath = ffmpegExecutable.absolutePath
            isAvailable = ffmpegExecutable.exists()
        }.onFailure { e ->
            Logger.warning(TAG, "Failed to load built-in FFmpeg library", e)
        }
    }
}
