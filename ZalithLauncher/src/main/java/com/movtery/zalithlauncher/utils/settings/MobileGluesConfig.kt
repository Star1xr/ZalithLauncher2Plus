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

package com.movtery.zalithlauncher.utils.settings

import android.os.Environment
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import java.io.File

class MobileGluesConfig private constructor(private var isInitializing: Boolean) {

    constructor() : this(false)

    var enableANGLE: Int = 1
        set(value) {
            if (field != value) { field = value; saveIfReady() }
        }

    var enableNoError: Int = 0
        set(value) {
            if (field != value) { field = value; saveIfReady() }
        }

    var enableExtTimerQuery: Int = 1
        set(value) {
            if (field != value) { field = value; saveIfReady() }
        }

    var enableExtComputeShader: Int = 0
        set(value) {
            if (field != value) { field = value; saveIfReady() }
        }

    var enableExtDirectStateAccess: Int = 0
        set(value) {
            if (field != value) { field = value; saveIfReady() }
        }

    var maxGlslCacheSize: Int = 32
        set(value) {
            if (field != value) { field = value; saveIfReady() }
        }

    var multidrawMode: Int = 0
        set(value) {
            if (field != value) { field = value; saveIfReady() }
        }

    var angleDepthClearFixMode: Int = 0
        set(value) {
            if (field != value) { field = value; saveIfReady() }
        }

    var customGLVersion: Int = 0
        set(value) {
            if (field != value) { field = value; saveIfReady() }
        }

    var fsr1Setting: Int = 0
        set(value) {
            if (field != value) { field = value; saveIfReady() }
        }

    private fun saveIfReady() {
        if (!isInitializing) save()
    }

    fun save() {
        runCatching {
            val configFile = File(CONFIG_FILE_PATH)
            configFile.parentFile?.mkdirs()
            configFile.writeText(Gson().toJson(buildConfigMap()))
        }
    }

    private fun buildConfigMap(): Map<String, Int> = mapOf(
        "enableANGLE" to enableANGLE,
        "enableNoError" to enableNoError,
        "enableExtTimerQuery" to enableExtTimerQuery,
        "enableExtComputeShader" to enableExtComputeShader,
        "enableExtDirectStateAccess" to enableExtDirectStateAccess,
        "maxGlslCacheSize" to maxGlslCacheSize,
        "multidrawMode" to multidrawMode,
        "angleDepthClearFixMode" to angleDepthClearFixMode,
        "customGLVersion" to customGLVersion,
        "fsr1Setting" to fsr1Setting
    )

    companion object {
        private const val CONFIG_DIR = "/MG"
        private const val CONFIG_FILE = "config.json"

        val CONFIG_FILE_PATH: String
            get() = "${Environment.getExternalStorageDirectory().absolutePath}$CONFIG_DIR/$CONFIG_FILE"

        fun load(): MobileGluesConfig? {
            val configFile = File(CONFIG_FILE_PATH)
            if (!configFile.exists()) return null

            val configStr = runCatching { configFile.readText() }.getOrNull() ?: return null

            return runCatching {
                val obj: JsonObject = JsonParser.parseString(configStr).asJsonObject
                val config = MobileGluesConfig(isInitializing = true)
                config.applyFromJson(obj)
                config.isInitializing = false
                config
            }.getOrNull()
        }

        private fun MobileGluesConfig.applyFromJson(obj: JsonObject) {
            fun JsonObject.int(key: String, default: Int) = get(key)?.asInt ?: default

            enableANGLE = obj.int("enableANGLE", 1)
            enableNoError = obj.int("enableNoError", 0)
            enableExtTimerQuery = obj.int("enableExtTimerQuery", 1)
            enableExtComputeShader = obj.int("enableExtComputeShader", 0)
            enableExtDirectStateAccess = obj.int("enableExtDirectStateAccess", 0)
            maxGlslCacheSize = obj.int("maxGlslCacheSize", 32)
            multidrawMode = obj.int("multidrawMode", 0)
            angleDepthClearFixMode = obj.int("angleDepthClearFixMode", 0)
            customGLVersion = obj.int("customGLVersion", 0)
            fsr1Setting = obj.int("fsr1Setting", 0)
        }
    }
}
