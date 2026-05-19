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

package com.star1xr.treelauncher.game.version.installed

import com.star1xr.treelauncher.R
import com.star1xr.treelauncher.game.addons.modloader.ModLoader
import com.star1xr.treelauncher.setting.enums.VersionIconStyle

object VersionIconManager {
    fun getIconRes(version: Version, style: VersionIconStyle): Int {
        val category = VersionCategorizer.getCategory(version)
        
        // Snapshots and April Fools always keep their special icons
        if (category == VersionCategory.SNAPSHOT) return R.drawable.img_version_snapshot
        if (category == VersionCategory.APRIL_FOOLS) return R.drawable.img_version_cake
        
        val info = version.getVersionInfo() ?: return R.drawable.img_version_vanilla
        val loader = info.loaderInfo?.loader
        
        if (style == VersionIconStyle.OFFICIAL) {
            return when (loader) {
                ModLoader.FABRIC -> R.drawable.img_loader_fabric
                ModLoader.NEOFORGE -> R.drawable.img_loader_neoforge
                ModLoader.FORGE -> R.drawable.img_version_forge // Fallback as we don't have img_loader_forge
                ModLoader.QUILT -> R.drawable.img_loader_quilt
                ModLoader.CLEANROOM -> R.drawable.img_loader_cleanroom
                ModLoader.OPTIFINE -> R.drawable.img_loader_optifine
                ModLoader.LEGACY_FABRIC -> R.drawable.img_loader_legacy_fabric
                ModLoader.PACK -> R.drawable.img_chest
                else -> R.drawable.img_version_vanilla // Official Vanilla logo/icon
            }
        } else {
            // Current style (Stylized icons)
            return when (loader) {
                ModLoader.FABRIC -> R.drawable.img_version_fabric
                ModLoader.NEOFORGE -> R.drawable.img_version_neoforge
                ModLoader.FORGE -> R.drawable.img_version_forge
                ModLoader.QUILT -> R.drawable.img_version_quilt
                ModLoader.PACK -> R.drawable.img_chest
                else -> R.drawable.img_version_vanilla
            }
        }
    }
}
