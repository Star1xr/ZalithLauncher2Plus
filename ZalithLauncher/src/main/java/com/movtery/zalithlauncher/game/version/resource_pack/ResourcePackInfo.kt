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

import com.movtery.zalithlauncher.utils.string.stripColorCodes
import java.io.File

/** Strip .disabled suffix (case-insensitive, 9 chars including dot) */
internal fun String.stripDisabledSuffix(): String =
    if (endsWith(".disabled", ignoreCase = true)) dropLast(9) else this

/** Strip .zip suffix (case-insensitive, 4 chars including dot) */
internal fun String.stripZipSuffix(): String =
    if (endsWith(".zip", ignoreCase = true)) dropLast(4) else this

/**
 * 资源包信息类
 */
data class ResourcePackInfo(
    /** 资源包文件 */
    val file: File,
    /** 提前计算好的文件大小（文件夹形式的资源包不计算文件大小） */
    val fileSize: Long? = null,
    /** 清除颜色替换符后的显示名称（已剥除 .disabled 与 .zip 后缀） */
    val rawName: String = file.name.stripDisabledSuffix().stripZipSuffix().stripColorCodes(),
    /** 显示名称（去掉 .disabled 与 .zip 后缀） */
    val displayName: String = file.name.stripDisabledSuffix().stripZipSuffix(),
    /** 资源包是否有效 */
    val isValid: Boolean,
    /** 资源包是否已启用（false 表示已用 .disabled 后缀禁用） */
    val isEnabled: Boolean = !file.name.endsWith(".disabled", ignoreCase = true),
    /** 资源包的描述信息 */
    val description: String?,
    /** 资源包的格式版本 */
    val packFormat: Int?,
    /** 资源包的图标 */
    val icon: ByteArray?
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ResourcePackInfo

        if (isValid != other.isValid) return false
        if (isEnabled != other.isEnabled) return false
        if (packFormat != other.packFormat) return false
        if (file != other.file) return false
        if (description != other.description) return false
        if (icon != null) {
            if (other.icon == null) return false
            if (!icon.contentEquals(other.icon)) return false
        } else if (other.icon != null) return false

        return true
    }

    override fun hashCode(): Int {
        var result = isValid.hashCode()
        result = 31 * result + isEnabled.hashCode()
        result = 31 * result + (packFormat ?: 0)
        result = 31 * result + file.hashCode()
        result = 31 * result + (description?.hashCode() ?: 0)
        result = 31 * result + (icon?.contentHashCode() ?: 0)
        return result
    }
}
