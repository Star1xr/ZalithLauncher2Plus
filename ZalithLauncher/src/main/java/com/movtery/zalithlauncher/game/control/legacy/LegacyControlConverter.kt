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

package com.movtery.zalithlauncher.game.control.legacy

import com.movtery.layer_controller.layout.ControlLayout
import com.movtery.layer_controller.layout.loadLayoutFromString
import com.movtery.zalithlauncher.ui.control.event.LAUNCHER_EVENT_SCROLL_DOWN
import com.movtery.zalithlauncher.ui.control.event.LAUNCHER_EVENT_SCROLL_UP
import com.movtery.zalithlauncher.ui.control.event.LAUNCHER_EVENT_SWITCH_IME
import com.movtery.zalithlauncher.ui.control.event.LAUNCHER_EVENT_SWITCH_MENU
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.UUID

/**
 * Converts Zalith Launcher 1 legacy control layouts to the LayerController ControlLayout format.
 * Parses the Zalith 1 JSON format and builds a valid LayerController v11 JSON string,
 * then uses loadLayoutFromString() to produce a ControlLayout.
 */
object LegacyControlConverter {

    // Zalith 1 special button keycodes
    private const val SPECIALBTN_KEYBOARD    = -1
    private const val SPECIALBTN_TOGGLECTRL  = -2
    private const val SPECIALBTN_MOUSEPRI    = -3
    private const val SPECIALBTN_MOUSESEC    = -4
    private const val SPECIALBTN_VIRTUALMOUSE = -5
    private const val SPECIALBTN_MOUSEMID   = -6
    private const val SPECIALBTN_SCROLLUP   = -7
    private const val SPECIALBTN_SCROLLDOWN = -8
    private const val SPECIALBTN_MENU       = -9

    // GLFW mouse button event strings (launcherEvent routes "GLFW_MOUSE_*" to sendMouseButton)
    private const val GLFW_MOUSE_LEFT   = "GLFW_MOUSE_BUTTON_LEFT"
    private const val GLFW_MOUSE_RIGHT  = "GLFW_MOUSE_BUTTON_RIGHT"
    private const val GLFW_MOUSE_MIDDLE = "GLFW_MOUSE_BUTTON_MIDDLE"

    fun convert(file: File): ControlLayout? {
        return try {
            convert(file.readText(), file.nameWithoutExtension)
        } catch (_: Exception) { null }
    }

    fun convert(jsonString: String, layoutName: String = "Legacy Layout"): ControlLayout? {
        return try {
            val src = JSONObject(jsonString)
            val layoutJson = buildLayoutJson(src, layoutName)
            loadLayoutFromString(layoutJson)
        } catch (_: Exception) { null }
    }

    private fun buildLayoutJson(src: JSONObject, layoutName: String): String {
        val infoJson = src.optJSONObject("mControlInfoDataList")
        val name    = infoJson?.optString("name",    "")?.nullIfLiteralOrBlank() ?: layoutName
        val author  = infoJson?.optString("author",  "")?.nullIfLiteralOrBlank() ?: ""
        val desc    = infoJson?.optString("desc",    "")?.nullIfLiteralOrBlank() ?: ""
        val verName = infoJson?.optString("version", "")?.nullIfLiteralOrBlank() ?: ""

        val buttons = JSONArray()

        src.optJSONArray("mControlDataList")?.let { arr ->
            for (i in 0 until arr.length()) {
                arr.optJSONObject(i)?.let { buildButton(it)?.let(buttons::put) }
            }
        }

        src.optJSONArray("mDrawerDataList")?.let { arr ->
            for (i in 0 until arr.length()) {
                arr.optJSONObject(i)?.let { buildDrawerButtons(it).forEach(buttons::put) }
            }
        }

        val layer = JSONObject().apply {
            put("name", "Converted Layer")
            put("uuid", UUID.randomUUID().toString())
            put("hide", false)
            put("hideWhenMouse", false)
            put("hideWhenGamepad", false)
            put("hideWhenJoystick", false)
            put("visibilityType", "always")
            put("normalButtons", buttons)
            put("textBoxes", JSONArray())
        }

        val info = JSONObject().apply {
            put("name",        tsJson(name))
            put("author",      tsJson(author))
            put("description", tsJson(desc))
            put("versionCode", 0)
            put("versionName", verName)
        }

        return JSONObject().apply {
            put("info",          info)
            put("layers",        JSONArray().put(layer))
            put("styles",        JSONArray())
            put("special",       JSONObject())
            put("editorVersion", 11)
        }.toString()
    }

    private fun String.nullIfLiteralOrBlank(): String? =
        if (this == "null" || isBlank()) null else this

    private fun tsJson(value: String) = JSONObject().apply {
        put("default", value)
        put("matchQueue", JSONArray())
    }

    private fun buildButton(btn: JSONObject): JSONObject? {
        return try {
            val xFrac = parseExpr(btn.optString("dynamicX", "0.5 * ${screen_width}"))
            val yFrac = parseExpr(btn.optString("dynamicY", "0.5 * ${screen_height}"))
            val xPos  = (xFrac * 10000).toInt().coerceIn(0, 10000)
            val yPos  = (yFrac * 10000).toInt().coerceIn(0, 10000)
            val width  = btn.optDouble("width",  50.0).toFloat().coerceAtLeast(5f)
            val height = btn.optDouble("height", 50.0).toFloat().coerceAtLeast(5f)
            val nameText = btn.optString("name", "Button")
                .let { if (it == "null" || it.isBlank()) "Button" else it }

            val displayInGame = btn.optBoolean("displayInGame", true)
            val displayInMenu = btn.optBoolean("displayInMenu", true)
            val visType = when {
                displayInGame && displayInMenu -> "always"
                displayInGame -> "in_game"
                displayInMenu -> "in_menu"
                else -> "always"
            }

            val clickEventsArr = JSONArray()
            parseKeycodes(btn).forEach { kc ->
                keycodeToEventJson(kc)?.let(clickEventsArr::put)
            }

            JSONObject().apply {
                put("text",           tsJson(nameText))
                put("uuid",           UUID.randomUUID().toString())
                put("position",       JSONObject().apply { put("x", xPos); put("y", yPos) })
                put("buttonSize",     JSONObject().apply {
                    put("type",             "dp")
                    put("widthDp",          width.toDouble())
                    put("heightDp",         height.toDouble())
                    put("widthPercentage",  1000)
                    put("heightPercentage", 1000)
                    put("widthReference",   "screen_height")
                    put("heightReference",  "screen_height")
                })
                put("textAlignment",  "left")
                put("textBold",       false)
                put("textItalic",     false)
                put("textUnderline",  false)
                put("visibilityType", visType)
                put("clickEvents",    clickEventsArr)
                put("isSwipple",      btn.optBoolean("isSwipeable", false))
                put("isPenetrable",   false)
                put("isToggleable",   btn.optBoolean("isToggle", false))
            }
        } catch (_: Exception) { null }
    }

    private fun buildDrawerButtons(drawer: JSONObject): List<JSONObject> {
        val result = mutableListOf<JSONObject>()
        drawer.optJSONObject("properties")?.let { buildButton(it)?.let(result::add) }
        drawer.optJSONArray("buttonProperties")?.let { arr ->
            for (i in 0 until arr.length()) {
                arr.optJSONObject(i)?.let { buildButton(it)?.let(result::add) }
            }
        }
        return result
    }

    private fun parseKeycodes(btn: JSONObject): List<Int> {
        val arr = btn.optJSONArray("keycodes")
        if (arr != null) return (0 until arr.length()).mapNotNull { i -> arr.optInt(i, 0).takeIf { it != 0 } }
        val result = mutableListOf<Int>()
        if (btn.optBoolean("holdShift", false)) result.add(340) // GLFW_KEY_LEFT_SHIFT
        if (btn.optBoolean("holdCtrl",  false)) result.add(341) // GLFW_KEY_LEFT_CONTROL
        if (btn.optBoolean("holdAlt",   false)) result.add(342) // GLFW_KEY_LEFT_ALT
        val kc = btn.optInt("keycode", 0)
        if (kc != 0) result.add(kc)
        return result
    }

    private fun keycodeToEventJson(keycode: Int): JSONObject? = when (keycode) {
        SPECIALBTN_KEYBOARD    -> launcherEventJson(LAUNCHER_EVENT_SWITCH_IME)
        SPECIALBTN_TOGGLECTRL,
        SPECIALBTN_MENU        -> launcherEventJson(LAUNCHER_EVENT_SWITCH_MENU)
        SPECIALBTN_MOUSEPRI    -> launcherEventJson(GLFW_MOUSE_LEFT)
        SPECIALBTN_MOUSESEC    -> launcherEventJson(GLFW_MOUSE_RIGHT)
        SPECIALBTN_MOUSEMID    -> launcherEventJson(GLFW_MOUSE_MIDDLE)
        SPECIALBTN_SCROLLUP    -> launcherEventJson(LAUNCHER_EVENT_SCROLL_UP)
        SPECIALBTN_SCROLLDOWN  -> launcherEventJson(LAUNCHER_EVENT_SCROLL_DOWN)
        SPECIALBTN_VIRTUALMOUSE -> null
        0                      -> null
        else                   -> JSONObject().apply { put("type", "key"); put("key", keycode.toString()) }
    }

    private fun launcherEventJson(key: String) = JSONObject().apply {
        put("type", "launcher_event"); put("key", key)
    }

    /** Evaluate a Zalith 1 dynamic position expression, returning a screen fraction [0,1]. */
    private fun parseExpr(expr: String): Float {
        if (expr.isBlank()) return 0.5f
        return try {
            val processed = expr.trim()
                .replace("${screen_width}",  "1.0")
                .replace("${screen_height}", "1.0")
                .replace("${width}",   "0.0")
                .replace("${height}",  "0.0")
                .replace("${dp}",      "0.0")
                .replace("${ratio}",   "1.0")
            ExprParser(processed).parse().coerceIn(0f, 1f)
        } catch (_: Exception) { 0.5f }
    }

    /** Minimal recursive-descent arithmetic parser (no external deps). */
    private class ExprParser(private val s: String) {
        private var i = 0
        fun parse() = expr()
        private fun expr(): Float {
            var r = term(); spaces()
            while (i < s.length) {
                when (s[i]) {
                    '+' -> { i++; r += term() }
                    '-' -> { i++; r -= term() }
                    else -> break
                }
                spaces()
            }
            return r
        }
        private fun term(): Float {
            var r = factor(); spaces()
            while (i < s.length) {
                when (s[i]) {
                    '*' -> { i++; val d = factor(); r *= d }
                    '/' -> { i++; val d = factor(); if (d != 0f) r /= d }
                    else -> break
                }
                spaces()
            }
            return r
        }
        private fun factor(): Float {
            spaces()
            if (i >= s.length) return 0f
            return when {
                s[i] == '(' -> { i++; val r = expr(); spaces(); if (i < s.length && s[i] == ')') i++; r }
                s[i] == '-' -> { i++; -factor() }
                else -> num()
            }
        }
        private fun num(): Float {
            spaces(); val start = i
            while (i < s.length && (s[i].isDigit() || s[i] == '.')) i++
            return if (i > start) s.substring(start, i).toFloatOrNull() ?: 0f else 0f
        }
        private fun spaces() { while (i < s.length && s[i] == ' ') i++ }
    }
}
