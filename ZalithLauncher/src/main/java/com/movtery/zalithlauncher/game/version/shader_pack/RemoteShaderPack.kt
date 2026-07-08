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

package com.movtery.zalithlauncher.game.version.shader_pack

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.movtery.zalithlauncher.game.download.assets.platform.Platform
import com.movtery.zalithlauncher.game.download.assets.platform.curseforge.models.CurseForgeFile
import com.movtery.zalithlauncher.game.download.assets.platform.getProjectByVersion
import com.movtery.zalithlauncher.game.download.assets.platform.getVersionByLocalFile
import com.movtery.zalithlauncher.game.download.assets.platform.modrinth.models.ModrinthVersion
import com.movtery.zalithlauncher.ui.screens.content.versions.elements.ShaderPackInfo
import com.movtery.zalithlauncher.utils.file.calculateFileSha1
import com.movtery.zalithlauncher.utils.logging.Logger
import com.tencent.mmkv.MMKV
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext

private const val TAG = "RemoteShaderPack"

/**
 * 光影包，包含本地信息以及从平台上获取到的远端项目信息（用于展示光影包图标）
 */
class RemoteShaderPack(
    val info: ShaderPackInfo
) {
    /**
     * 是否正在加载项目信息
     */
    var isLoading by mutableStateOf(false)
        private set

    /**
     * 项目信息（若在远端平台匹配到该光影包，则不为空）
     */
    var projectInfo: ShaderProjectInfo? by mutableStateOf(null)
        private set

    /**
     * 是否已经加载过
     */
    var isLoaded: Boolean = false
        private set

    /**
     * @param loadFromCache 是否从缓存中加载
     */
    suspend fun load(loadFromCache: Boolean) {
        if (loadFromCache && isLoaded) return

        if (!loadFromCache) {
            projectInfo = null
        }

        isLoaded = false
        isLoading = true

        try {
            withContext(Dispatchers.IO) {
                val file = info.file
                val projectCache = shaderProjectCache()

                runCatching {
                    //获取文件 sha1，作为缓存的键
                    val sha1 = calculateFileSha1(file)

                    //从缓存加载项目信息
                    val cachedProject = if (loadFromCache) {
                        projectCache.decodeParcelable(sha1, ShaderProjectInfo::class.java)
                    } else null

                    if (loadFromCache && cachedProject != null) {
                        projectInfo = cachedProject
                    } else {
                        ensureActive()
                        val version = getVersionByLocalFile(file, sha1)
                        val projectRef = version?.let { toProjectRef(it) }

                        if (projectRef != null) {
                            val (projectId, platform) = projectRef
                            val project = getProjectByVersion(
                                projectId = projectId,
                                platform = platform,
                                printLog = false
                            )
                            val newProjectInfo = ShaderProjectInfo(
                                id = project.platformId(),
                                platform = project.platform(),
                                iconUrl = project.platformIconUrl(),
                                title = project.platformTitle(),
                                slug = project.platformSlug()
                            )

                            projectInfo = newProjectInfo
                            projectCache.encode(sha1, newProjectInfo, MMKV.ExpireInDay)
                        }
                    }

                    isLoaded = true
                }.onFailure { e ->
                    if (e is CancellationException) return@onFailure
                    Logger.warning(TAG, "Failed to load project info for shader pack: ${file.name}", e)
                }
            }
        } finally {
            isLoading = false
        }
    }

    /** 从平台版本信息中提取项目 ID 与所属平台 */
    private fun toProjectRef(version: com.movtery.zalithlauncher.game.download.assets.platform.PlatformVersion): Pair<String, Platform>? {
        return when (version) {
            is ModrinthVersion -> version.projectId to Platform.MODRINTH
            is CurseForgeFile -> version.modId.toString() to Platform.CURSEFORGE
            else -> null
        }
    }
}
