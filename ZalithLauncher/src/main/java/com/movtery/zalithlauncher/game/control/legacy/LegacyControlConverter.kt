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

      /**
       * Assumed display density (dp to pixel ratio) for ZL1 layout positions.
       * ZL1 uses pixel-based coordinates at the device density; most Android devices are XHDPI (2.0).
       * This is used to correctly normalise the dp/px() variable in position expressions.
       */
      private const val DEFAULT_DENSITY = 2f

      /**
       * LWJGL2 integer keycode -> GLFW key name string mapping.
       *
       * ZL1 stores keycodes as LWJGL2 integers (from org.lwjgl.input.Keyboard).
       * The ZL2 runtime's ControlEventKeycode.getKeycodeFromEvent() expects GLFW key name strings
       * (e.g. "GLFW_KEY_A"). This map provides the complete conversion table.
       */
      private val lwjgl2ToGlfw: Map<Int, String> = mapOf(
          // Letters A-Z
          0x1E to "GLFW_KEY_A",
          0x30 to "GLFW_KEY_B",
          0x2E to "GLFW_KEY_C",
          0x20 to "GLFW_KEY_D",
          0x12 to "GLFW_KEY_E",
          0x21 to "GLFW_KEY_F",
          0x22 to "GLFW_KEY_G",
          0x23 to "GLFW_KEY_H",
          0x17 to "GLFW_KEY_I",
          0x24 to "GLFW_KEY_J",
          0x25 to "GLFW_KEY_K",
          0x26 to "GLFW_KEY_L",
          0x32 to "GLFW_KEY_M",
          0x31 to "GLFW_KEY_N",
          0x18 to "GLFW_KEY_O",
          0x19 to "GLFW_KEY_P",
          0x10 to "GLFW_KEY_Q",
          0x13 to "GLFW_KEY_R",
          0x1F to "GLFW_KEY_S",
          0x14 to "GLFW_KEY_T",
          0x16 to "GLFW_KEY_U",
          0x2F to "GLFW_KEY_V",
          0x11 to "GLFW_KEY_W",
          0x2D to "GLFW_KEY_X",
          0x15 to "GLFW_KEY_Y",
          0x2C to "GLFW_KEY_Z",
          // Number row
          0x02 to "GLFW_KEY_1",
          0x03 to "GLFW_KEY_2",
          0x04 to "GLFW_KEY_3",
          0x05 to "GLFW_KEY_4",
          0x06 to "GLFW_KEY_5",
          0x07 to "GLFW_KEY_6",
          0x08 to "GLFW_KEY_7",
          0x09 to "GLFW_KEY_8",
          0x0A to "GLFW_KEY_9",
          0x0B to "GLFW_KEY_0",
          // Special / control keys
          0x01 to "GLFW_KEY_ESCAPE",
          0x0E to "GLFW_KEY_BACKSPACE",
          0x0F to "GLFW_KEY_TAB",
          0x1C to "GLFW_KEY_ENTER",
          0x39 to "GLFW_KEY_SPACE",
          // Punctuation
          0x0C to "GLFW_KEY_MINUS",
          0x0D to "GLFW_KEY_EQUAL",
          0x1A to "GLFW_KEY_LEFT_BRACKET",
          0x1B to "GLFW_KEY_RIGHT_BRACKET",
          0x2B to "GLFW_KEY_BACKSLASH",
          0x27 to "GLFW_KEY_SEMICOLON",
          0x28 to "GLFW_KEY_APOSTROPHE",
          0x29 to "GLFW_KEY_GRAVE_ACCENT",
          0x33 to "GLFW_KEY_COMMA",
          0x34 to "GLFW_KEY_PERIOD",
          0x35 to "GLFW_KEY_SLASH",
          // Modifiers
          0x2A to "GLFW_KEY_LEFT_SHIFT",
          0x36 to "GLFW_KEY_RIGHT_SHIFT",
          0x1D to "GLFW_KEY_LEFT_CONTROL",
          0x9D to "GLFW_KEY_RIGHT_CONTROL",
          0x38 to "GLFW_KEY_LEFT_ALT",
          0xB8 to "GLFW_KEY_RIGHT_ALT",
          0x3A to "GLFW_KEY_CAPS_LOCK",
          // Function keys F1-F12
          0x3B to "GLFW_KEY_F1",
          0x3C to "GLFW_KEY_F2",
          0x3D to "GLFW_KEY_F3",
          0x3E to "GLFW_KEY_F4",
          0x3F to "GLFW_KEY_F5",
          0x40 to "GLFW_KEY_F6",
          0x41 to "GLFW_KEY_F7",
          0x42 to "GLFW_KEY_F8",
          0x43 to "GLFW_KEY_F9",
          0x44 to "GLFW_KEY_F10",
          0x57 to "GLFW_KEY_F11",
          0x58 to "GLFW_KEY_F12",
          // Extended function keys F13-F19
          0x64 to "GLFW_KEY_F13",
          0x65 to "GLFW_KEY_F14",
          0x66 to "GLFW_KEY_F15",
          0x67 to "GLFW_KEY_F16",
          0x68 to "GLFW_KEY_F17",
          0x69 to "GLFW_KEY_F18",
          0x71 to "GLFW_KEY_F19",
          // Navigation cluster
          0xC7 to "GLFW_KEY_HOME",
          0xCF to "GLFW_KEY_END",
          0xC9 to "GLFW_KEY_PAGE_UP",
          0xD1 to "GLFW_KEY_PAGE_DOWN",
          0xD2 to "GLFW_KEY_INSERT",
          0xD3 to "GLFW_KEY_DELETE",
          // Arrow keys
          0xC8 to "GLFW_KEY_UP",
          0xD0 to "GLFW_KEY_DOWN",
          0xCB to "GLFW_KEY_LEFT",
          0xCD to "GLFW_KEY_RIGHT",
          // Lock keys
          0x45 to "GLFW_KEY_NUM_LOCK",
          0x46 to "GLFW_KEY_SCROLL_LOCK",
          // Numpad arithmetic
          0x37 to "GLFW_KEY_KP_MULTIPLY",
          0x4A to "GLFW_KEY_KP_SUBTRACT",
          0x4E to "GLFW_KEY_KP_ADD",
          0x9C to "GLFW_KEY_KP_ENTER",
          0x53 to "GLFW_KEY_KP_DECIMAL",
          0x8D to "GLFW_KEY_KP_EQUAL",
          0xB5 to "GLFW_KEY_KP_DIVIDE",
          // Numpad digits
          0x52 to "GLFW_KEY_KP_0",
          0x4F to "GLFW_KEY_KP_1",
          0x50 to "GLFW_KEY_KP_2",
          0x51 to "GLFW_KEY_KP_3",
          0x4B to "GLFW_KEY_KP_4",
          0x4C to "GLFW_KEY_KP_5",
          0x4D to "GLFW_KEY_KP_6",
          0x47 to "GLFW_KEY_KP_7",
          0x48 to "GLFW_KEY_KP_8",
          0x49 to "GLFW_KEY_KP_9",
          // Misc system keys
          0xB7 to "GLFW_KEY_PRINT_SCREEN",
          0xC5 to "GLFW_KEY_PAUSE",
          0xDB to "GLFW_KEY_LEFT_SUPER",
          0xDC to "GLFW_KEY_RIGHT_SUPER",
      )

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

        val wFracX = width  / REF_W
        val hFracX = height / REF_W
        val wFracY = width  / REF_H
        val hFracY = height / REF_H
        // dp fraction: ZL1 uses density as a px-per-dp multiplier.
        // DEFAULT_DENSITY (2.0 = XHDPI) correctly scales px() and the ${dp} variable.
        val dpFracX = DEFAULT_DENSITY / REF_W
        val dpFracY = DEFAULT_DENSITY / REF_H

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

    /**
     * Extract LWJGL2 integer keycodes from a ZL1 button JSON object.
     *
     * ZL1 has two storage formats:
     *  - New: "keycodes" array of LWJGL2 integers
     *  - Old: single "keycode" integer + boolean modifier flags
     *
     * The holdShift/holdCtrl/holdAlt flags map to LWJGL2 codes 0x2A/0x1D/0x38.
     */
    private fun parseKeycodes(btn: JSONObject): List<Int> {
        val arr = btn.optJSONArray("keycodes")
        if (arr != null) return (0 until arr.length()).mapNotNull { i -> arr.optInt(i, 0).takeIf { it != 0 } }
        val result = mutableListOf<Int>()
        // Use LWJGL2 codes so they pass through lwjgl2ToGlfw correctly.
        if (btn.optBoolean("holdShift", false)) result.add(0x2A) // KEY_LSHIFT
        if (btn.optBoolean("holdCtrl",  false)) result.add(0x1D) // KEY_LCONTROL
        if (btn.optBoolean("holdAlt",   false)) result.add(0x38) // KEY_LMENU
        val kc = btn.optInt("keycode", 0)
        if (kc != 0) result.add(kc)
        return result
    }

    /**
     * Convert a ZL1 LWJGL2 keycode integer to a ZL2 click event JSON object.
     *
     * Negative values are ZL1 special button codes mapped to launcher events.
     * Positive values are LWJGL2 keycodes converted via lwjgl2ToGlfw to GLFW key name strings.
     * Unknown LWJGL2 codes are silently dropped (return null).
     */
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
        else -> {
            val glfwName = lwjgl2ToGlfw[keycode]
            if (glfwName != null) {
                JSONObject().apply { put("type", "key"); put("key", glfwName) }
            } else {
                null
            }
        }
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
