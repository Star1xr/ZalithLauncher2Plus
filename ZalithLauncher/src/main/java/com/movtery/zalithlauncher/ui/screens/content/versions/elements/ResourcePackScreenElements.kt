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

package com.movtery.zalithlauncher.ui.screens.content.versions.elements

import com.movtery.zalithlauncher.R
import com.movtery.zalithlauncher.game.version.resource_pack.ResourcePackInfo

/** 资源包操作状态 */
sealed interface ResourcePackOperation {
    data object None : ResourcePackOperation
    /** 执行任务中 */
    data object Progress : ResourcePackOperation
    /** 删除资源包确认对话框 */
    data class DeletePack(val packInfo: ResourcePackInfo) : ResourcePackOperation
}

/** 资源包/光影包状态过滤器（全部 / 已启用 / 已禁用） */
enum class PackStateFilter(val textRes: Int) {
    All(R.string.generic_all),
    Enabled(R.string.generic_enabled),
    Disabled(R.string.generic_disabled)
}

/**
 * 资源包过滤器
 */
data class ResourcePackFilter(
    val stateFilter: PackStateFilter = PackStateFilter.All,
    val filterName: String = ""
)

/**
 * 过滤资源包列表
 */
fun List<ResourcePackInfo>.filterPacks(filter: ResourcePackFilter) = this.filter {
    val matchesState = when (filter.stateFilter) {
        PackStateFilter.All -> true
        PackStateFilter.Enabled -> it.isEnabled
        PackStateFilter.Disabled -> !it.isEnabled
    }
    val nameMatched = filter.filterName.isEmpty() ||
            it.rawName.contains(filter.filterName, true)
    matchesState && nameMatched
}
