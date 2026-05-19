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

enum class VersionCategory(val textRes: Int) {
    VANILLA(R.string.version_group_vanilla),
    SNAPSHOT(R.string.version_group_snapshot),
    APRIL_FOOLS(R.string.version_group_april_fools),
    MODLOADER(R.string.version_group_modloader),
    MODPACK(R.string.version_group_modpack)
}

object VersionCategorizer {
    private val aprilFoolsVersions = setOf(
        "15w14a", "1.RV-Pre1", "3D Shareware v1.34", "20w14infinite", 
        "22w13oneblockatatime", "23w13a_or_b", "24w14potato", "April Fools 2.0"
    )

    fun getCategory(version: Version): VersionCategory {
        val info = version.getVersionInfo() ?: return VersionCategory.VANILLA
        val mcVersion = info.minecraftVersion
        
        // Check for April Fools
        if (info.type == "april_fools" || aprilFoolsVersions.contains(mcVersion) || mcVersion.contains("infinite") || mcVersion.contains("potato")) {
            return VersionCategory.APRIL_FOOLS
        }

        // Check for Modpack
        if (info.loaderInfo?.loader == ModLoader.PACK) {
            return VersionCategory.MODPACK
        }

        // Check for ModLoaders
        val loader = info.loaderInfo?.loader
        if (loader != null && loader != ModLoader.OPTIFINE) {
            return VersionCategory.MODLOADER
        }

        // Check for Snapshot
        if (info.type == "snapshot") {
            return VersionCategory.SNAPSHOT
        }

        return VersionCategory.VANILLA
    }
}
