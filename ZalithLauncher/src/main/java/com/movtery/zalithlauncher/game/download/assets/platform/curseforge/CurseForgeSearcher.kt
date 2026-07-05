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

package com.movtery.zalithlauncher.game.download.assets.platform.curseforge

import com.movtery.zalithlauncher.game.download.assets.platform.AbstractPlatformSearcher
import com.movtery.zalithlauncher.game.download.assets.platform.Platform
import com.movtery.zalithlauncher.game.download.assets.platform.PlatformClasses
import com.movtery.zalithlauncher.game.download.assets.platform.PlatformSearchFilter
import com.movtery.zalithlauncher.game.download.assets.platform.curseforge.models.CurseForgeFile
import com.movtery.zalithlauncher.game.download.assets.platform.curseforge.models.CurseForgeFingerprintsMatches
import com.movtery.zalithlauncher.game.download.assets.platform.curseforge.models.CurseForgeProject
import com.movtery.zalithlauncher.game.download.assets.platform.curseforge.models.CurseForgeVersion
import com.movtery.zalithlauncher.game.download.assets.platform.curseforge.models.CurseForgeVersions
import com.movtery.zalithlauncher.game.download.assets.platform.curseforge.models.isApproved
import com.movtery.zalithlauncher.utils.file.MurmurHash2Incremental
import com.movtery.zalithlauncher.utils.network.httpGetJson
import com.movtery.zalithlauncher.utils.network.httpPostJson
import io.ktor.http.Parameters
import io.ktor.server.plugins.NotFoundException
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import java.io.File

class CurseForgeSearcher(
    val api: String = CURSEFORGE_API,
    source: String = "Official CurseForge"
): AbstractPlatformSearcher(
    platform = Platform.CURSEFORGE,
    source = source
) {
    override suspend fun searchAssets(
        query: String,
        searchFilter: PlatformSearchFilter,
        platformClasses: PlatformClasses
    ): CurseForgeSearchResult {
        return httpGetJson(
            url = "$api/mods/search",
            parameters = searchFilter.toCurseForgeRequest(
                query = query,
                platformClasses = platformClasses
            ).toParameters()
        )
    }

    override suspend fun getProject(projectID: String): CurseForgeProject {
        val project = httpGetJson<CurseForgeProject>(
            url = "$api/mods/$projectID"
        )
        if (!project.isApproved()) throw NotFoundException("The project {$projectID} is not in a publicly available state.")
        return project
    }

    /**
     * 在 CurseForge 平台获取某项目的某个文件
     */
    suspend fun getVersion(
        projectID: String,
        fileID: String,
    ): CurseForgeVersion {
        return httpGetJson(
            url = "$api/mods/$projectID/files/$fileID"
        )
    }

    /**
     * 在 CurseForge 平台根据分页获取项目的版本列表
     * @param index 开始处
     * @param pageSize 每页请求数量
     */
    suspend fun getVersions(
        projectID: String,
        index: Int = 0,
        pageSize: Int = 100
    ): CurseForgeVersions = httpGetJson(
        url = "$api/mods/$projectID/files",
        parameters = Parameters.build {
            append("index", index.toString())
            append("pageSize", pageSize.toString())
        }
    )

    override suspend fun getVersions(
        projectID: String,
        pageCallback: (chunk: Int, page: Int) -> Unit
    ): List<CurseForgeFile> = coroutineScope {
        val pageSize = 50

        // Probe page 0 — pagination.totalCount tells us the exact total upfront,
        // so we never fire speculative requests that get cancelled.
        val firstPage = getVersions(projectID = projectID, index = 0, pageSize = pageSize)
        pageCallback(1, 1)
        val firstFiles = firstPage.data.toList()
        // Keep as Long to avoid Int overflow on pathological API responses
        val totalCount = firstPage.pagination.totalCount

        // The vast majority of mods fit on one page — return immediately with 1 request.
        if (totalCount <= pageSize) return@coroutineScope firstFiles

        // Fetch all remaining pages concurrently (exact count known, no waste).
        val semaphore = Semaphore(10)
        val remainingFiles = (pageSize.toLong() until totalCount step pageSize.toLong())
            .mapIndexed { idx, offset ->
                async {
                    semaphore.withPermit {
                        pageCallback(1, idx + 2)
                        // offset safely fits in Int for any realistic file count
                        getVersions(projectID = projectID, index = offset.toInt(), pageSize = pageSize).data.toList()
                    }
                }
            }.awaitAll().flatten()

        firstFiles + remainingFiles
    }

    override suspend fun getVersionByLocalFile(
        file: File,
        sha1: String
    ): CurseForgeFile? {
        val hash = MurmurHash2Incremental.computeHash(file, byteToSkip = listOf(0x9, 0xa, 0xd, 0x20))
        return httpPostJson<CurseForgeFingerprintsMatches>(
            url = "$api/fingerprints",
            body = mapOf("fingerprints" to listOf(hash))
        ).data.exactMatches
            ?.takeIf { it.isNotEmpty() }
            ?.firstOrNull()
            ?.file
    }
}

