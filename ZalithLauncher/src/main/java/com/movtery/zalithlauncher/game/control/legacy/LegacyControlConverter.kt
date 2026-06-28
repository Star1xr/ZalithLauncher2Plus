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
 *
 * ZL1 coordinate system: dynamicX/Y evaluate to the LEFT/TOP edge of the button.
 * Variables screen_width, screen_height are actual screen pixels at runtime; width/height are
 * button pixels; dp = display density; preferred_scale = layout scale (100 = default).
 *
 * Conversion: normalise each expression to [0,1] fraction, add half-button-size to get center,
 * then multiply by 10000 for the LayerController coordinate space.
 */
object LegacyControlConverter {

    private const val SPECIALBTN_KEYBOARD     = -1
    private const val SPECIALBTN_TOGGLECTRL   = -2
    private const val SPECIALBTN_MOUSEPRI     = -3
    private const val SPECIALBTN_MOUSESEC     = -4
    private const val SPECIALBTN_VIRTUALMOUSE = -5
    private const val SPECIALBTN_MOUSEMID     = -6
    private const val SPECIALBTN_SCROLLUP     = -7
    private const val SPECIALBTN_SCROLLDOWN   = -8
    private const val SPECIALBTN_MENU         = -9

    private const val GLFW_MOUSE_LEFT   = "GLFW_MOUSE_BUTTON_LEFT"
    private const val GLFW_MOUSE_RIGHT  = "GLFW_MOUSE_BUTTON_RIGHT"
    private const val GLFW_MOUSE_MIDDLE = "GLFW_MOUSE_BUTTON_MIDDLE"

    private const val REF_W = 1280f
    private const val REF_H = 720f

    fun convert(file: File): ControlLayout? =
        try { convert(file.readText(), file.nameWithoutExtension) } catch (_: Exception) { null }

    fun convertToJson(file: File): String? = try {
        buildLayoutJson(JSONObject(file.readText()), file.nameWithoutExtension)
    } catch (_: Exception) { null }

    fun convert(jsonString: String, layoutName: String = "Legacy Layout"): ControlLayout? = try {
        loadLayoutFromString(buildLayoutJson(JSONObject(jsonString), layoutName))
    } catch (_: Exception) { null }

    private fun buildLayoutJson(src: JSONObject, layoutName: String): String {
        val infoJson = src.optJSONObject("mControlInfoDataList")
        val name    = infoJson?.optString("name",    "")?.nullIfLiteralOrBlank() ?: layoutName
        val author  = infoJson?.optString("author",  "")?.nullIfLiteralOrBlank() ?: ""
        val desc    = infoJson?.optString("desc",    "")?.nullIfLiteralOrBlank() ?: ""
        val verName = infoJson?.optString("version", "")?.nullIfLiteralOrBlank() ?: ""

        val mainButtons = JSONArray()
        val extraLayers = JSONArray()

        src.optJSONArray("mControlDataList")?.let { arr ->
            for (i in 0 until arr.length()) {
                arr.optJSONObject(i)?.let { buildButton(it)?.let(mainButtons::put) }
            }
        }

        src.optJSONArray("mDrawerDataList")?.let { arr ->
            for (i in 0 until arr.length()) {
                arr.optJSONObject(i)?.let { drawer ->
                    val drawerLayerUuid = UUID.randomUUID().toString()
                    val drawerButtons = JSONArray()
                    drawer.optJSONArray("buttonProperties")?.let { btnArr ->
                        for (j in 0 until btnArr.length()) {
                            btnArr.optJSONObject(j)?.let { buildButton(it)?.let(drawerButtons::put) }
                        }
                    }
                    val drawerLayer = JSONObject().apply {
                        put("name", "Drawer " + (i + 1))
                        put("uuid", drawerLayerUuid)
                        put("hide", true)
                        put("hideWhenMouse", false)
                        put("hideWhenGamepad", false)
                        put("hideWhenJoystick", false)
                        put("visibilityType", "always")
                        put("normalButtons", drawerButtons)
                        put("textBoxes", JSONArray())
                    }
                    extraLayers.put(drawerLayer)

                    val triggerBtn = drawer.optJSONObject("properties")?.let { buildButton(it) }
                    if (triggerBtn != null) {
                        val switchEvent = JSONObject().apply {
                            put("type", "switch_layer")
                            put("key", drawerLayerUuid)
                        }
                        val events = triggerBtn.optJSONArray("clickEvents") ?: JSONArray()
                        val newEvents = JSONArray().put(switchEvent)
                        for (k in 0 until events.length()) newEvents.put(events.getJSONObject(k))
                        triggerBtn.put("clickEvents", newEvents)
                        mainButtons.put(triggerBtn)
                    } else {
                        for (j in 0 until drawerButtons.length()) mainButtons.put(drawerButtons.getJSONObject(j))
                    }
                }
            }
        }

        val mainLayer = JSONObject().apply {
            put("name", "Converted Layer")
            put("uuid", UUID.randomUUID().toString())
            put("hide", false)
            put("hideWhenMouse", false)
            put("hideWhenGamepad", false)
            put("hideWhenJoystick", false)
            put("visibilityType", "always")
            put("normalButtons", mainButtons)
            put("textBoxes", JSONArray())
        }
        val allLayers = JSONArray().put(mainLayer)
        for (k in 0 until extraLayers.length()) allLayers.put(extraLayers.getJSONObject(k))

        val info = JSONObject().apply {
            put("name",        tsJson(name))
            put("author",      tsJson(author))
            put("description", tsJson(desc))
            put("versionCode", 0)
            put("versionName", verName)
        }
        return JSONObject().apply {
            put("info",          info)
            put("layers",        allLayers)
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

    private fun buildButton(btn: JSONObject): JSONObject? = try {
        val width  = btn.optDouble("width",  50.0).toFloat().coerceAtLeast(5f)
        val height = btn.optDouble("height", 50.0).toFloat().coerceAtLeast(5f)

        val wFracX = width  / REF_W; val hFracX = height / REF_W; val dpFracX = 1f / REF_W
        val wFracY = width  / REF_H; val hFracY = height / REF_H; val dpFracY = 1f / REF_H

        val xLeft = parseExpr(btn.optString("dynamicX", ""), wFracX, hFracX, dpFracX)
        val yTop  = parseExpr(btn.optString("dynamicY", ""), wFracY, hFracY, dpFracY)

        val xCenter = (xLeft + wFracX / 2f).coerceIn(0f, 1f)
        val yCenter = (yTop  + hFracY / 2f).coerceIn(0f, 1f)
        val xPos = (xCenter * 10000).toInt().coerceIn(0, 10000)
        val yPos = (yCenter * 10000).toInt().coerceIn(0, 10000)

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
        parseKeycodes(btn).forEach { kc -> keycodeToEventJson(kc)?.let(clickEventsArr::put) }

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
                put("widthReference",   "screen_width")
                put("heightReference",  "screen_height")
            })
            put("textAlignment",  "Left")
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

    private fun parseKeycodes(btn: JSONObject): List<Int> {
        val arr = btn.optJSONArray("keycodes")
        if (arr != null) return (0 until arr.length()).mapNotNull { i -> arr.optInt(i, 0).takeIf { it != 0 } }
        val result = mutableListOf<Int>()
        if (btn.optBoolean("holdShift", false)) result.add(340)
        if (btn.optBoolean("holdCtrl",  false)) result.add(341)
        if (btn.optBoolean("holdAlt",   false)) result.add(342)
        val kc = btn.optInt("keycode", 0)
        if (kc != 0) result.add(kc)
        return result
    }

    private fun keycodeToEventJson(keycode: Int): JSONObject? = when (keycode) {
        SPECIALBTN_KEYBOARD     -> launcherEventJson(LAUNCHER_EVENT_SWITCH_IME)
        SPECIALBTN_TOGGLECTRL,
        SPECIALBTN_MENU         -> launcherEventJson(LAUNCHER_EVENT_SWITCH_MENU)
        SPECIALBTN_MOUSEPRI     -> launcherEventJson(GLFW_MOUSE_LEFT)
        SPECIALBTN_MOUSESEC     -> launcherEventJson(GLFW_MOUSE_RIGHT)
        SPECIALBTN_MOUSEMID     -> launcherEventJson(GLFW_MOUSE_MIDDLE)
        SPECIALBTN_SCROLLUP     -> launcherEventJson(LAUNCHER_EVENT_SCROLL_UP)
        SPECIALBTN_SCROLLDOWN   -> launcherEventJson(LAUNCHER_EVENT_SCROLL_DOWN)
        SPECIALBTN_VIRTUALMOUSE -> null
        0                       -> null
        else -> JSONObject().apply { put("type", "key"); put("key", keycode.toString()) }
    }

    private fun launcherEventJson(key: String) = JSONObject().apply {
        put("type", "launcher_event"); put("key", key)
    }

    /**
     * Evaluate a ZL1 dynamic position expression to a screen fraction in [0, 1].
     *
     * All known variable tokens are substituted before the arithmetic parser runs.
     * The dollar-variable trick avoids Kotlin string interpolation of the variable names.
     * Any unrecognised tokens are replaced with "0.0" as a safe fallback.
     */
    private fun parseExpr(expr: String, wFrac: Float = 0f, hFrac: Float = 0f, dpFrac: Float = 0f): Float {
        if (expr.isBlank()) return 0.5f
        return try {
            // Use a local val for "$" to avoid Kotlin string interpolation of variable names.
            val d = "$"
            var s = expr.trim()
                .replace(d + "{screen_width}",    "1.0")
                .replace(d + "{screen_height}",   "1.0")
                .replace(d + "{width}",           "%.8f".format(wFrac))
                .replace(d + "{height}",          "%.8f".format(hFrac))
                .replace(d + "{dp}",              "%.8f".format(dpFrac))
                .replace(d + "{preferred_scale}", "100.0")
                .replace(d + "{ratio}",           "1.0")
                .replace(d + "{margin}",          "0.0")
                .replace(d + "{right}",           "(1.0 - %.8f)".format(wFrac))
                .replace(d + "{bottom}",          "(1.0 - %.8f)".format(hFrac))

            // Replace px(value) -> value * dpFrac (dp-to-px normalised).
            // Scan manually to avoid Regex dollar-sign issues.
            s = replacePxCalls(s, dpFrac)

            // Strip any remaining unresolved variable tokens.
            s = stripUnresolved(s, d)

            ExprParser(s).parse().coerceIn(0f, 1f)
        } catch (_: Exception) { 0.5f }
    }

    /** Replace px(number) calls in [expr] with the dp value times [dpFrac]. */
    private fun replacePxCalls(expr: String, dpFrac: Float): String {
        val sb = StringBuilder()
        var i = 0
        while (i < expr.length) {
            if (expr.startsWith("px(", i)) {
                val start = i + 3
                val end   = expr.indexOf(')', start)
                if (end > start) {
                    val dpVal = expr.substring(start, end).toFloatOrNull() ?: 0f
                    sb.append("%.8f".format(dpVal * dpFrac))
                    i = end + 1
                    continue
                }
            }
            sb.append(expr[i])
            i++
        }
        return sb.toString()
    }

    /** Replace any remaining variable tokens that look like dollar+{name} with "0.0". */
    private fun stripUnresolved(expr: String, dollar: String): String {
        val sb = StringBuilder()
        var i = 0
        while (i < expr.length) {
            if (expr.startsWith(dollar + "{", i)) {
                val end = expr.indexOf('}', i + dollar.length + 1)
                if (end >= 0) { sb.append("0.0"); i = end + 1; continue }
            }
            sb.append(expr[i])
            i++
        }
        return sb.toString()
    }

    /** Minimal recursive-descent arithmetic parser supporting +, -, *, /, (), unary minus, floats. */
    private class ExprParser(private val s: String) {
        private var i = 0
        fun parse() = expr()
        private fun expr(): Float {
            var r = term(); spaces()
            while (i < s.length) { when (s[i]) { '+' -> { i++; r += term() }; '-' -> { i++; r -= term() }; else -> break }; spaces() }
            return r
        }
        private fun term(): Float {
            var r = factor(); spaces()
            while (i < s.length) { when (s[i]) { '*' -> { i++; r *= factor() }; '/' -> { i++; val d2 = factor(); if (d2 != 0f) r /= d2 }; else -> break }; spaces() }
            return r
        }
        private fun factor(): Float {
            spaces(); if (i >= s.length) return 0f
            return when { s[i] == '(' -> { i++; val r = expr(); spaces(); if (i < s.length && s[i] == ')') i++; r }; s[i] == '-' -> { i++; -factor() }; else -> num() }
        }
        private fun num(): Float {
            spaces(); val start = i
            while (i < s.length && (s[i].isDigit() || s[i] == '.')) i++
            return if (i > start) s.substring(start, i).toFloatOrNull() ?: 0f else 0f
        }
        private fun spaces() { while (i < s.length && s[i] == ' ') i++ }
    }
}
