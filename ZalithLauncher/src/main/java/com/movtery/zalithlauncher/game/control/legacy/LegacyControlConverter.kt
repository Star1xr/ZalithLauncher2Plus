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
 * ZL1 coordinate system:
 *   - dynamicX/dynamicY evaluate to the LEFT/TOP edge of the button on a 1280x720 dp reference screen.
 *   - Variables: ${screen_width}=actual_screen_px, ${screen_height}=actual_screen_px,
 *     ${width}=button_width_px, ${height}=button_height_px, ${dp}=density.
 *   - When normalised by screen dimension: fractions suitable for conversion.
 *
 * LayerController coordinate system:
 *   - ButtonPosition {x, y} in 0-10000 represents the CENTER of the button as a percentage
 *     (5000 = 50% = screen center).
 *
 * Conversion: left_edge_fraction + half_button_fraction -> center_fraction x 10000.
 *
 * Expression accuracy improvements:
 *   - ${preferred_scale} is resolved to 100.0 (editor default).
 *   - px(value) function calls are evaluated as value * dpFrac (1 dp normalised).
 *   - ${right}/${bottom} anchors are expanded before evaluation.
 *   - Any remaining unresolved ${...} tokens fall back to 0.0 instead of crashing the parser.
 */
object LegacyControlConverter {

    // Zalith 1 special button keycodes
    private const val SPECIALBTN_KEYBOARD     = -1
    private const val SPECIALBTN_TOGGLECTRL   = -2
    private const val SPECIALBTN_MOUSEPRI     = -3
    private const val SPECIALBTN_MOUSESEC     = -4
    private const val SPECIALBTN_VIRTUALMOUSE = -5
    private const val SPECIALBTN_MOUSEMID     = -6
    private const val SPECIALBTN_SCROLLUP     = -7
    private const val SPECIALBTN_SCROLLDOWN   = -8
    private const val SPECIALBTN_MENU         = -9

    // GLFW mouse button event strings
    private const val GLFW_MOUSE_LEFT   = "GLFW_MOUSE_BUTTON_LEFT"
    private const val GLFW_MOUSE_RIGHT  = "GLFW_MOUSE_BUTTON_RIGHT"
    private const val GLFW_MOUSE_MIDDLE = "GLFW_MOUSE_BUTTON_MIDDLE"

    // ZL1 reference landscape screen dimensions (dp).
    // Positions are interpreted relative to a 1280x720 dp canvas.
    private const val REF_W = 1280f
    private const val REF_H = 720f

    fun convert(file: File): ControlLayout? {
        return try { convert(file.readText(), file.nameWithoutExtension) } catch (_: Exception) { null }
    }

    /**
     * Converts a ZL1 legacy control layout file to a LayerController-format JSON string.
     * Returns null if the file cannot be read or parsed.
     */
    fun convertToJson(file: File): String? {
        return try {
            val src = JSONObject(file.readText())
            buildLayoutJson(src, file.nameWithoutExtension)
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

        val mainButtons = JSONArray()
        val extraLayers = JSONArray()

        // Regular buttons
        src.optJSONArray("mControlDataList")?.let { arr ->
            for (i in 0 until arr.length()) {
                arr.optJSONObject(i)?.let { buildButton(it)?.let(mainButtons::put) }
            }
        }

        // Drawer controls: each drawer gets its own hidden layer with a trigger button
        src.optJSONArray("mDrawerDataList")?.let { arr ->
            for (i in 0 until arr.length()) {
                arr.optJSONObject(i)?.let { drawer ->
                    val drawerLayerUuid = UUID.randomUUID().toString()

                    // Build drawer content layer (hidden by default)
                    val drawerButtons = JSONArray()
                    drawer.optJSONArray("buttonProperties")?.let { btnArr ->
                        for (j in 0 until btnArr.length()) {
                            btnArr.optJSONObject(j)?.let { buildButton(it)?.let(drawerButtons::put) }
                        }
                    }
                    val drawerLayer = JSONObject().apply {
                        put("name", "Drawer ${i + 1}")
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

                    // Build drawer trigger button with SwitchLayer event
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
                        // No trigger properties: flatten drawer contents into main layer
                        for (j in 0 until drawerButtons.length()) {
                            mainButtons.put(drawerButtons.getJSONObject(j))
                        }
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

    private fun buildButton(btn: JSONObject): JSONObject? {
        return try {
            val width  = btn.optDouble("width",  50.0).toFloat().coerceAtLeast(5f)
            val height = btn.optDouble("height", 50.0).toFloat().coerceAtLeast(5f)

            // Fractions for X expressions (reference axis = REF_W = screen width)
            val wFracX  = width  / REF_W   // ${width}  in X context
            val hFracX  = height / REF_W   // ${height} in X context
            val dpFracX = 1f     / REF_W   // ${dp}     in X context: 1 dp as fraction of ref width

            // Fractions for Y expressions (reference axis = REF_H = screen height)
            val wFracY  = width  / REF_H   // ${width}  in Y context
            val hFracY  = height / REF_H   // ${height} in Y context
            val dpFracY = 1f     / REF_H   // ${dp}     in Y context: 1 dp as fraction of ref height

            // ZL1 dynamicX/Y = LEFT / TOP edge of button as a [0,1] screen fraction
            val xLeft = parseExpr(btn.optString("dynamicX", ""), wFracX, hFracX, dpFracX)
            val yTop  = parseExpr(btn.optString("dynamicY", ""), wFracY, hFracY, dpFracY)

            // LayerController ButtonPosition = CENTER of button (0-10000)
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
    }

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
        else -> JSONObject().apply { put("type", "key"); put("key", keycode.toString()) }
    }

    private fun launcherEventJson(key: String) = JSONObject().apply {
        put("type", "launcher_event"); put("key", key)
    }

    /**
     * Evaluate a ZL1 dynamic position expression to a screen fraction in [0, 1].
     *
     * Handles all known ZL1/ZL2 expression variables:
     *   ${screen_width}, ${screen_height} -> 1.0
     *   ${width}  -> wFrac  (button width  as fraction of reference axis)
     *   ${height} -> hFrac  (button height as fraction of reference axis)
     *   ${dp}     -> dpFrac (1 dp as fraction of reference axis)
     *   ${preferred_scale} -> 100.0
     *   ${ratio}  -> 1.0
     *   ${margin} -> 0.0
     *   ${right}  -> (1.0 - wFrac) -- right edge anchor
     *   ${bottom} -> (1.0 - hFrac) -- bottom edge anchor
     *   px(value)  -> value * dpFrac (dp-to-px in normalised space)
     *   Any remaining unresolved ${...} -> 0.0 (safe fallback)
     */
    private fun parseExpr(
        expr: String,
        wFrac: Float = 0f,
        hFrac: Float = 0f,
        dpFrac: Float = 0f
    ): Float {
        if (expr.isBlank()) return 0.5f
        return try {
            var processed = expr.trim()
                // Core screen variables
                .replace("${screen_width}",  "1.0")
                .replace("${screen_height}", "1.0")
                // Button dimension variables
                .replace("${width}",   "%.8f".format(wFrac))
                .replace("${height}",  "%.8f".format(hFrac))
                .replace("${dp}",      "%.8f".format(dpFrac))
                // Layout/editor variables
                .replace("${preferred_scale}", "100.0")
                .replace("${ratio}",   "1.0")
                .replace("${margin}",  "0.0")
                // Edge anchors (expanded before evaluation)
                .replace("${right}",   "(1.0 - %.8f)".format(wFrac))
                .replace("${bottom}",  "(1.0 - %.8f)".format(hFrac))

            // Evaluate px(value) function calls: px(dp) -> dp * dpFrac in normalised space
            val pxPattern = Regex("px\(([0-9.]+)\)")
            processed = pxPattern.replace(processed) { m ->
                val dp = m.groupValues[1].toFloatOrNull() ?: 0f
                "%.8f".format(dp * dpFrac)
            }

            // Safety: replace any remaining unresolved ${...} with 0.0
            processed = processed.replace(Regex("\$\{[^}]+\}"), "0.0")

            ExprParser(processed).parse().coerceIn(0f, 1f)
        } catch (_: Exception) { 0.5f }
    }

    /** Minimal recursive-descent arithmetic parser. Handles +, -, *, /, (), unary minus, numbers. */
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
                    '*' -> { i++; val d2 = factor(); r *= d2 }
                    '/' -> { i++; val d2 = factor(); if (d2 != 0f) r /= d2 }
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
