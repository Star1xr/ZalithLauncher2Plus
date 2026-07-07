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

package com.movtery.zalithlauncher.game.version.resource_pack

import com.movtery.zalithlauncher.game.version.mod.meta.PackMcMeta
import com.movtery.zalithlauncher.utils.GSON
import com.movtery.zalithlauncher.utils.logging.Logger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.apache.commons.io.FileUtils
import java.io.File
import java.util.zip.ZipFile

private const val TAG = "ResourcePackUtils"

/**
 * 解析资源包文件，游戏内仅支持加载文件夹、文件后缀为zip的资源包。
 * 同时支持以 .disabled 结尾的已禁用资源包（pack.zip.disabled / packfolder.disabled）。
 * @param file 资源包文件
 */
suspend fun parseResourcePack(file: File): ResourcePackInfo? = withContext(Dispatchers.IO) {
    val isEnabled = !file.name.endsWith(".disabled", ignoreCase = true)
    val nameWithoutDisabled = if (!isEnabled) file.name.dropLast(9) else file.name
    val isZipPack = nameWithoutDisabled.endsWith(".zip", ignoreCase = true)

    // 只处理文件夹（含 folder.disabled）和 .zip / .zip.disabled
    if (!file.isDirectory && !isZipPack) return@withContext null

    runCatching {
        var isValid = false
        var metaContent: String? = null
        var iconBytes: ByteArray? = null
        var fileSize: Long? = null

        if (file.isDirectory) { // 文件夹形式的资源包（包括 folder.disabled）
            File(file, "pack.mcmeta").takeIf { it.exists() }?.let { metaFile ->
                metaContent = metaFile.readText()
            }
            File(file, "pack.png").takeIf { it.exists() }?.let { iconFile ->
                iconBytes = iconFile.readBytes()
            }
        } else if (isZipPack) { // 压缩包形式的资源包（包括 pack.zip.disabled）
            fileSize = FileUtils.sizeOf(file)
            ZipFile(file).use { zip ->
                zip.getEntry("pack.mcmeta")?.let { metaEntry ->
                    metaContent = zip.getInputStream(metaEntry).bufferedReader().readText()
                }
                zip.getEntry("pack.png")?.let { iconEntry ->
                    iconBytes = zip.getInputStream(iconEntry).readBytes()
                }
            }
        }

        val meta = metaContent?.let { content ->
            runCatching {
                GSON.fromJson(content, PackMcMeta::class.java)
            }.onFailure {
                Logger.warning(TAG, "Failed to parse the resource package metadata: ${file.absolutePath}", it)
            }.getOrNull()
        }?.also {
            isValid = true
        }

        ResourcePackInfo(
            file = file,
            fileSize = fileSize,
            isValid = isValid,
            isEnabled = isEnabled,
            description = meta?.pack?.description?.toPlainText(),
            packFormat = meta?.pack?.packFormat,
            icon = iconBytes
        )
    }.onFailure {
        Logger.warning(TAG, "Failed to parse the resource package: ${file.absolutePath}", it)
    }.getOrNull()
}
