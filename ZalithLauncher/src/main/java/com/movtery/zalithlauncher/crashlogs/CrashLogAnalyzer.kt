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

package com.movtery.zalithlauncher.crashlogs

import java.io.File

/**
 * 已知的、由"缺失模组依赖"引发的类前缀 -> 对应模组的展示名称
 * 用于在崩溃日志中检测到 NoClassDefFoundError / ClassNotFoundException 时，
 * 给出更友好的原因提示（例如：某个模组依赖 Sodium，但 Sodium 未安装）
 */
private val KNOWN_DEPENDENCY_PREFIXES = listOf(
    "net.caffeinemc.mods.sodium" to "Sodium",
    "me.jellysquid.mods.sodium" to "Sodium",
    "net.irisshaders" to "Iris",
    "net.optifine" to "OptiFine",
    "net.fabricmc.fabric" to "Fabric API",
    "net.neoforged" to "NeoForge",
    "net.minecraftforge" to "Forge",
    "net.minecraftforge.fml" to "Forge",
    "com.terraformersmc.modmenu" to "Mod Menu",
)

private val MISSING_CLASS_REGEX = Regex(
    "(?:NoClassDefFoundError|ClassNotFoundException):\\s*([\\w./$]+)"
)

/**
 * 一次崩溃原因分析的结果
 *
 * @param missingClass 缺失的类的完全限定名（以日志中记录的形式，可能是 `/` 或 `.` 分隔）
 * @param dependencyName 根据已知前缀推测出的、提供该类的模组名称；未知时为 null
 */
data class CrashCauseHint(
    val missingClass: String,
    val dependencyName: String?
)

/**
 * 分析游戏崩溃日志，尝试识别"因缺失模组/依赖导致的类加载失败"这一类问题。
 *
 * 游戏进程与启动器运行在不同的 JVM 中，启动器无法拦截游戏内部的异常，
 * 因此只能在崩溃发生后，通过分析崩溃日志的方式，为用户提供更有针对性的诊断信息，
 * 而不是仅仅展示一份原始的崩溃报告。
 */
object CrashLogAnalyzer {

    /**
     * 读取日志文件末尾的一部分内容并分析崩溃原因。
     * 只读取文件末尾，避免在日志文件很大时占用过多内存。
     */
    fun analyze(logFile: File, maxBytes: Long = 256 * 1024L): CrashCauseHint? {
        if (!logFile.exists() || !logFile.isFile) return null
        val content = runCatching { readTail(logFile, maxBytes) }.getOrNull() ?: return null
        return analyze(content)
    }

    /**
     * 直接分析一段日志文本，识别其中是否包含"缺失类"相关的异常。
     */
    fun analyze(logContent: String): CrashCauseHint? {
        val match = MISSING_CLASS_REGEX.find(logContent) ?: return null
        val missingClass = match.groupValues[1]
        val normalized = missingClass.replace('/', '.')
        val dependencyName = KNOWN_DEPENDENCY_PREFIXES.firstOrNull { (prefix, _) ->
            normalized.startsWith(prefix)
        }?.second
        return CrashCauseHint(
            missingClass = normalized,
            dependencyName = dependencyName
        )
    }

    private fun readTail(file: File, maxBytes: Long): String {
        val length = file.length()
        return if (length <= maxBytes) {
            file.readText()
        } else {
            file.inputStream().use { input ->
                input.skip(length - maxBytes)
                input.readBytes().toString(Charsets.UTF_8)
            }
        }
    }
}
