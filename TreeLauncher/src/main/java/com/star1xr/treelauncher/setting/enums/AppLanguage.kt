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

package com.star1xr.treelauncher.setting.enums

import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import com.star1xr.treelauncher.R

enum class AppLanguage(
    val tag: String,
    @param:StringRes val textRes: Int
) {
    FOLLOW_SYSTEM("", R.string.generic_follow_system),
    ENGLISH("en", R.string.language_english),
    TURKISH("tr", R.string.language_turkish),
    SIMPLIFIED_CHINESE("zh-CN", R.string.language_simplified_chinese),
    TRADITIONAL_CHINESE("zh-TW", R.string.language_traditional_chinese),
}

fun applyLanguage(language: AppLanguage) {
    val appLocale = if (language != AppLanguage.FOLLOW_SYSTEM) {
        LocaleListCompat.forLanguageTags(language.tag)
    } else {
        LocaleListCompat.getEmptyLocaleList()
    }
    AppCompatDelegate.setApplicationLocales(appLocale)
}
