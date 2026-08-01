package com.movtery.layer_controller.layout

import android.util.DisplayMetrics
import com.movtery.layer_controller.data.ButtonPosition
import com.movtery.layer_controller.data.ButtonShape
import com.movtery.layer_controller.data.ButtonSize
import com.movtery.layer_controller.data.ButtonStyle
import com.movtery.layer_controller.data.NormalData
import com.movtery.layer_controller.data.TextAlignment
import com.movtery.layer_controller.data.VisibilityType
import com.movtery.layer_controller.data.lang.TranslatableString
import com.movtery.layer_controller.event.ClickEvent
import androidx.compose.ui.graphics.Color
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.long
import java.util.UUID

private const val POJAV_MARGIN_DP = 2f
private const val EDITOR_VERSION = 11
private val DEFAULT_BG = Color(red = 0f, green = 0f, blue = 0f, alpha = 0.3f)
private val DEFAULT_PRESSED_BG = Color(red = 0f, green = 0f, blue = 0f, alpha = 0.5f)
private val DEFAULT_FG = Color.White

private fun randomUUID(length: Int = 12): String =
    UUID.randomUUID().toString().replace("-", "").take(length)

fun isPojavJson(jsonString: String): Boolean =
    jsonString.contains("\"mControlDataList\"")

fun parsePojavLayout(jsonString: String): ControlLayout {
    val dm: DisplayMetrics
    try {
        dm = android.content.res.Resources.getSystem().displayMetrics
    } catch (_: Exception) {
        return ControlLayout(
            info = ControlLayout.Info(
                name = TranslatableString("Pojav", emptyList()),
                author = TranslatableString("PojavLauncherTeam", emptyList()),
                description = TranslatableString("PojavLauncher touch controls", emptyList()),
                versionCode = 1,
                versionName = "1.0"
            ),
            layers = emptyList(),
            styles = emptyList(),
            editorVersion = EDITOR_VERSION
        )
    }
    return convertPojavToZalith(jsonString, dm)
}

fun convertPojavToZalith(jsonString: String, displayMetrics: DisplayMetrics): ControlLayout {
    val root = kotlinx.serialization.json.Json.parseToJsonElement(jsonString).jsonObject

    val screenWidthDp = displayMetrics.widthPixels / displayMetrics.density
    val screenHeightDp = displayMetrics.heightPixels / displayMetrics.density

    val buttons = root["mControlDataList"]?.jsonArray ?: emptyList()
    val drawers = root["mDrawerDataList"]?.jsonArray ?: emptyList()
    val joysticks = root["mJoystickDataList"]?.jsonArray ?: emptyList()
    val infoData = root["mControlInfoDataList"]?.jsonObject

    val styleCache = mutableMapOf<String, ButtonStyle>()
    val allButtons = mutableListOf<NormalData>()

    val preferredScale = (root["scaledAt"]?.jsonPrimitive?.int ?: 100).toFloat() / 100f

    for (btnObj in buttons) {
        convertPojavButton(btnObj, screenWidthDp, screenHeightDp, preferredScale)?.let { (data, style) ->
            allButtons.add(data)
            if (style != null) styleCache[style.uuid] = style
        }
    }

    for (drawerObj in drawers) {
        convertPojavDrawer(drawerObj, screenWidthDp, screenHeightDp, preferredScale)?.let { items ->
            for ((data, style) in items) {
                allButtons.add(data)
                if (style != null) styleCache[style.uuid] = style
            }
        }
    }

    for (joyObj in joysticks) {
        convertPojavJoystick(joyObj, screenWidthDp, screenHeightDp, preferredScale)?.let { (data, style) ->
            allButtons.add(data)
            if (style != null) styleCache[style.uuid] = style
        }
    }

    val author = infoData?.let { parseJsonString(it["author"]) } ?: "PojavLauncherTeam"
    val name = infoData?.let { parseJsonString(it["name"]) } ?: "Pojav"

    return ControlLayout(
        info = ControlLayout.Info(
            name = TranslatableString(name, emptyList()),
            author = TranslatableString(author, emptyList()),
            description = TranslatableString("PojavLauncher touch controls", emptyList()),
            versionCode = 1,
            versionName = "1.0"
        ),
        layers = listOf(
            ControlLayer(
                name = "controls",
                uuid = randomUUID(),
                hide = false,
                hideWhenMouse = true,
                hideWhenGamepad = true,
                visibilityType = VisibilityType.ALWAYS,
                normalButtons = allButtons,
                textBoxes = emptyList()
            )
        ),
        styles = styleCache.values.toList(),
        editorVersion = EDITOR_VERSION
    )
}

private fun convertPojavButton(
    btnObj: JsonElement,
    sw: Float, sh: Float, scale: Float
): Pair<NormalData, ButtonStyle?>? {
    val btn = btnObj.jsonObject
    val name = btn["name"]?.jsonPrimitive?.content ?: return null

    val w = (btn["width"]?.jsonPrimitive?.int ?: 50).toFloat()
    val h = (btn["height"]?.jsonPrimitive?.int ?: 50).toFloat()
    if (w <= 0 || h <= 0) return null

    val rawX = btn["dynamicX"]?.jsonPrimitive?.content ?: "\${margin}"
    val rawY = btn["dynamicY"]?.jsonPrimitive?.content ?: "\${margin}"

    if (rawX.contains("Infinity") || rawY.contains("Infinity")) return null

    val xDp = evalPojavExpr(rawX, w, h, sw, sh)
    val yDp = evalPojavExpr(rawY, w, h, sw, sh)

    val xPct = ((xDp / sw) * 10000).toInt().coerceIn(0, 10000)
    val yPct = ((yDp / sh) * 10000).toInt().coerceIn(0, 10000)

    val keycodes = btn["keycodes"]?.jsonArray?.map { it.jsonPrimitive.int } ?: emptyList()
    val events = keycodes.flatMap { pojavKeyToClickEvents(it) }.ifEmpty {
        listOf(ClickEvent(ClickEvent.Type.Key, "GLFW_KEY_UNKNOWN"))
    }

    val displayInGame = btn["displayInGame"]?.jsonPrimitive?.boolean ?: true
    val displayInMenu = btn["displayInMenu"]?.jsonPrimitive?.boolean ?: true
    if (!displayInGame && !displayInMenu) return null

    val visibility = when {
        displayInGame && displayInMenu -> VisibilityType.ALWAYS
        displayInGame -> VisibilityType.IN_GAME
        else -> VisibilityType.IN_MENU
    }

    val opacity = (btn["opacity"]?.jsonPrimitive?.int ?: 100).coerceIn(0, 100) / 100f
    val bgColor = btn["bgColor"]?.jsonPrimitive?.long ?: 0x4D000000L
    val cornerRadius = (btn["cornerRadius"]?.jsonPrimitive?.int ?: 0).toFloat()
    val strokeWidth = (btn["strokeWidth"]?.jsonPrimitive?.int ?: 0).toFloat()
    val strokeColor = btn["strokeColor"]?.jsonPrimitive?.long ?: if (strokeWidth > 0) 0xFFFFFFFFL else 0L
    val isSwipeable = btn["isSwipeable"]?.jsonPrimitive?.boolean ?: false
    val isToggle = btn["isToggle"]?.jsonPrimitive?.boolean ?: false
    val isPenetrable = btn["passThruEnabled"]?.jsonPrimitive?.boolean ?: false

    val renderedRadius = cornerRadius.coerceIn(0f, 100f)
    val shape = ButtonShape(renderedRadius)

    val color = Color(
        red = ((bgColor shr 16) and 0xFF).toFloat() / 255f,
        green = ((bgColor shr 8) and 0xFF).toFloat() / 255f,
        blue = (bgColor and 0xFF).toFloat() / 255f,
        alpha = ((bgColor shr 24) and 0xFF).toFloat() / 255f
    )
    val borderColor = Color(
        red = ((strokeColor shr 16) and 0xFF).toFloat() / 255f,
        green = ((strokeColor shr 8) and 0xFF).toFloat() / 255f,
        blue = (strokeColor and 0xFF).toFloat() / 255f,
        alpha = ((strokeColor shr 24) and 0xFF).toFloat() / 255f
    )

    val styleUuid = makeStyleHash(
        opacity, bgColor, cornerRadius, strokeWidth, strokeColor
    )

    val data = NormalData(
        text = TranslatableString(name, emptyList()),
        uuid = randomUUID(),
        position = ButtonPosition(x = xPct, y = yPct),
        buttonSize = ButtonSize(
            type = ButtonSize.Type.Dp,
            widthDp = w.coerceAtLeast(5f),
            heightDp = h.coerceAtLeast(5f),
            widthPercentage = 1400,
            heightPercentage = 1400,
            widthReference = ButtonSize.Reference.ScreenHeight,
            heightReference = ButtonSize.Reference.ScreenHeight
        ),
        buttonStyle = styleUuid,
        textAlignment = TextAlignment.Center,
        visibilityType = visibility,
        _clickEvents = events,
        isSwipple = isSwipeable,
        isPenetrable = isPenetrable,
        isToggleable = isToggle
    )

    val style = ButtonStyle(
        name = name.take(16),
        uuid = styleUuid,
        animateSwap = false,
        commonStyle = true,
        lightStyle = ButtonStyle.StyleConfig(
            alpha = opacity,
            pressedAlpha = opacity,
            backgroundColor = color,
            pressedBackgroundColor = if (bgColor == 0L) DEFAULT_PRESSED_BG
                else darkenColor(color, 0.3f),
            contentColor = DEFAULT_FG,
            pressedContentColor = DEFAULT_FG,
            fontSize = null,
            pressedFontSize = null,
            borderWidth = strokeWidth.toInt(),
            pressedBorderWidth = strokeWidth.toInt(),
            borderColor = borderColor,
            pressedBorderColor = borderColor,
            borderRadius = shape,
            pressedBorderRadius = shape
        ),
        darkStyle = ButtonStyle.StyleConfig(
            alpha = opacity,
            pressedAlpha = opacity,
            backgroundColor = color,
            pressedBackgroundColor = if (bgColor == 0L) DEFAULT_PRESSED_BG
                else darkenColor(color, 0.3f),
            contentColor = DEFAULT_FG,
            pressedContentColor = DEFAULT_FG,
            fontSize = null,
            pressedFontSize = null,
            borderWidth = strokeWidth.toInt(),
            pressedBorderWidth = strokeWidth.toInt(),
            borderColor = borderColor,
            pressedBorderColor = borderColor,
            borderRadius = shape,
            pressedBorderRadius = shape
        )
    )

    return Pair(data, style)
}

private fun convertPojavDrawer(
    drawerObj: JsonElement,
    sw: Float, sh: Float, scale: Float
): List<Pair<NormalData, ButtonStyle?>>? {
    val drawer = drawerObj.jsonObject
    val properties = drawer["properties"]?.jsonObject ?: return null
    val orientation = drawer["orientation"]?.jsonPrimitive?.content ?: "LEFT"
    val buttonProps = drawer["buttonProperties"]?.jsonArray ?: return null

    val dx = properties["dynamicX"]?.jsonPrimitive?.content ?: "\${margin}"
    val dy = properties["dynamicY"]?.jsonPrimitive?.content ?: "\${margin}"
    val dw = (properties["width"]?.jsonPrimitive?.int ?: 50).toFloat()
    val dh = (properties["height"]?.jsonPrimitive?.int ?: 50).toFloat()
    if (dw <= 0 || dh <= 0) return null

    val dX = evalPojavExpr(dx, dw, dh, sw, sh)
    val dY = evalPojavExpr(dy, dw, dh, sw, sh)

    val result = mutableListOf<Pair<NormalData, ButtonStyle?>>()

    val drawerMain = convertPojavButton(properties, sw, sh, scale)
    if (drawerMain != null) {
        result.add(drawerMain)
    }

    for ((i, btnEl) in buttonProps.withIndex()) {
        val btn = convertPojavButton(btnEl, sw, sh, scale) ?: continue
        val btnData = btn.first
        val btnStyle = btn.second

        val offsetX = when (orientation.uppercase()) {
            "RIGHT" -> dw * (i + 1)
            "LEFT" -> -(i + 1) * dw
            else -> 0f
        }
        val offsetY = when (orientation.uppercase()) {
            "DOWN" -> dh * (i + 1)
            "UP" -> -(i + 1) * dh
            else -> 0f
        }

        val newXPct = ((dX + offsetX) / sw * 10000).toInt().coerceIn(0, 10000)
        val newYPct = ((dY + offsetY) / sh * 10000).toInt().coerceIn(0, 10000)

        result.add(
            btnData.copy(
                position = ButtonPosition(newXPct, newYPct)
            ) to btnStyle
        )
    }

    return result
}

private fun convertPojavJoystick(
    joyObj: JsonElement,
    sw: Float, sh: Float, scale: Float
): Pair<NormalData, ButtonStyle?>? {
    val joy = joyObj.jsonObject
    val name = (joy["name"]?.jsonPrimitive?.content) ?: "Joystick"

    val w = (joy["width"]?.jsonPrimitive?.int ?: 80).toFloat()
    val h = (joy["height"]?.jsonPrimitive?.int ?: 80).toFloat()
    if (w <= 0 || h <= 0) return null

    val rawX = joy["dynamicX"]?.jsonPrimitive?.content ?: "\${margin}"
    val rawY = joy["dynamicY"]?.jsonPrimitive?.content ?: "\${margin}"

    if (rawX.contains("Infinity") || rawY.contains("Infinity")) return null

    val xDp = evalPojavExpr(rawX, w, h, sw, sh)
    val yDp = evalPojavExpr(rawY, w, h, sw, sh)

    val xPct = ((xDp / sw) * 10000).toInt().coerceIn(0, 10000)
    val yPct = ((yDp / sh) * 10000).toInt().coerceIn(0, 10000)

    val displayInGame = joy["displayInGame"]?.jsonPrimitive?.boolean ?: true
    val displayInMenu = joy["displayInMenu"]?.jsonPrimitive?.boolean ?: true
    if (!displayInGame && !displayInMenu) return null

    val visibility = when {
        displayInGame && displayInMenu -> VisibilityType.ALWAYS
        displayInGame -> VisibilityType.IN_GAME
        else -> VisibilityType.IN_MENU
    }

    val opacity = (joy["opacity"]?.jsonPrimitive?.int ?: 100).coerceIn(0, 100) / 100f

    val styleUuid = makeStyleHash(opacity, 0x4D000000, 35f, 0f, 0L)

    val data = NormalData(
        text = TranslatableString(name, emptyList()),
        uuid = randomUUID(),
        position = ButtonPosition(x = xPct, y = yPct),
        buttonSize = ButtonSize(
            type = ButtonSize.Type.Dp,
            widthDp = w.coerceAtLeast(5f),
            heightDp = h.coerceAtLeast(5f),
            widthPercentage = 1400,
            heightPercentage = 1400,
            widthReference = ButtonSize.Reference.ScreenHeight,
            heightReference = ButtonSize.Reference.ScreenHeight
        ),
        buttonStyle = styleUuid,
        textAlignment = TextAlignment.Center,
        visibilityType = visibility,
        _clickEvents = listOf(
            ClickEvent(ClickEvent.Type.Key, "GLFW_KEY_W"),
            ClickEvent(ClickEvent.Type.Key, "GLFW_KEY_A"),
            ClickEvent(ClickEvent.Type.Key, "GLFW_KEY_S"),
            ClickEvent(ClickEvent.Type.Key, "GLFW_KEY_D")
        ),
        isSwipple = true,
        isPenetrable = false,
        isToggleable = false
    )

    val shape = ButtonShape(35f)
    val color = Color(red = 0f, green = 0f, blue = 0f, alpha = 0.3f)

    val style = ButtonStyle(
        name = name.take(16),
        uuid = styleUuid,
        animateSwap = false,
        commonStyle = true,
        lightStyle = ButtonStyle.StyleConfig(
            alpha = opacity,
            pressedAlpha = opacity,
            backgroundColor = color,
            pressedBackgroundColor = DEFAULT_PRESSED_BG,
            contentColor = DEFAULT_FG,
            pressedContentColor = DEFAULT_FG,
            fontSize = null,
            pressedFontSize = null,
            borderWidth = 0,
            pressedBorderWidth = 0,
            borderColor = Color.Transparent,
            pressedBorderColor = Color.Transparent,
            borderRadius = shape,
            pressedBorderRadius = shape
        ),
        darkStyle = ButtonStyle.StyleConfig(
            alpha = opacity,
            pressedAlpha = opacity,
            backgroundColor = color,
            pressedBackgroundColor = DEFAULT_PRESSED_BG,
            contentColor = DEFAULT_FG,
            pressedContentColor = DEFAULT_FG,
            fontSize = null,
            pressedFontSize = null,
            borderWidth = 0,
            pressedBorderWidth = 0,
            borderColor = Color.Transparent,
            pressedBorderColor = Color.Transparent,
            borderRadius = shape,
            pressedBorderRadius = shape
        )
    )

    return Pair(data, style)
}

private fun makeStyleHash(
    opacity: Float, bgColor: Long, cornerRadius: Float,
    strokeWidth: Float, strokeColor: Long
): String {
    val raw = "$opacity|$bgColor|$cornerRadius|$strokeWidth|$strokeColor"
    return "pojav_" + raw.hashCode().let {
        if (it == Int.MIN_VALUE) "default" else it.toString(36)
    }
}

private fun darkenColor(color: Color, factor: Float): Color {
    return Color(
        red = (color.red * (1f - factor)).coerceIn(0f, 1f),
        green = (color.green * (1f - factor)).coerceIn(0f, 1f),
        blue = (color.blue * (1f - factor)).coerceIn(0f, 1f),
        alpha = color.alpha
    )
}

private fun parseJsonString(element: JsonElement?): String? {
    return when {
        element?.jsonPrimitive?.isString == true -> element.jsonPrimitive.content
        element?.jsonObject != null -> element.jsonObject["default"]?.jsonPrimitive?.content
        else -> null
    }
}

private fun evalPojavExpr(expr: String, btnW: Float, btnH: Float, sw: Float, sh: Float): Float {
    val margin = POJAV_MARGIN_DP
    var s = expr
    s = s.replace("\${margin}", margin.toString())
    s = s.replace("\${width}", btnW.toString())
    s = s.replace("\${height}", btnH.toString())
    s = s.replace("\${screen_width}", sw.toString())
    s = s.replace("\${screen_height}", sh.toString())
    s = s.replace("\${right}", (sw - btnW - margin).toString())
    s = s.replace("\${bottom}", (sh - btnH - margin).toString())
    s = s.replace("\${top}", "0")
    s = s.replace("\${left}", "0")
    s = s.replace("\${preferred_scale}", "1")
    s = s.replace(Regex("dp\\(([^)]+)\\)")) { match ->
        val inner = match.groupValues[1].trim()
        (evalPojavExpr(inner, btnW, btnH, sw, sh)).toString()
    }
    s = s.replace(Regex("px\\(([^)]+)\\)")) { match ->
        val inner = match.groupValues[1].trim()
        (evalPojavExpr(inner, btnW, btnH, sw, sh)).toString()
    }
    return evaluateSimple(s)
}

private fun evaluateSimple(expr: String): Float {
    val cleaned = expr.replace(" ", "")
    if (cleaned.all { it.isDigit() || it == '.' }) return cleaned.toFloat()

    return try {
        val tokens = mutableListOf<String>()
        val current = StringBuilder()
        for (c in cleaned) {
            if (c in "+-*/()") {
                if (current.isNotEmpty()) { tokens.add(current.toString()); current.clear() }
                tokens.add(c.toString())
            } else {
                current.append(c)
            }
        }
        if (current.isNotEmpty()) tokens.add(current.toString())

        ExprParser(tokens).parse()
    } catch (_: Exception) {
        0f
    }
}

private class ExprParser(private val tokens: List<String>) {
    private var idx = 0

    fun parse(): Float = parseExpr()

    private fun parseExpr(): Float {
        var result = parseTerm()
        while (idx < tokens.size && (tokens[idx] == "+" || tokens[idx] == "-")) {
            val op = tokens[idx++]
            val rhs = parseTerm()
            result = if (op == "+") result + rhs else result - rhs
        }
        return result
    }

    private fun parseTerm(): Float {
        var result = parseFactor()
        while (idx < tokens.size && (tokens[idx] == "*" || tokens[idx] == "/")) {
            val op = tokens[idx++]
            val rhs = parseFactor()
            result = if (op == "*") result * rhs else result / rhs
        }
        return result
    }

    private fun parseFactor(): Float {
        if (idx >= tokens.size) return 0f
        val t = tokens[idx]
        if (t == "(") { idx++; val v = parseExpr(); idx++; return v }
        if (t == "-") { idx++; return -parseFactor() }
        idx++
        return t.toFloatOrNull() ?: 0f
    }
}

private fun pojavKeyToClickEvents(keycode: Int): List<ClickEvent> {
    return when (keycode) {
        -1 -> listOf(ClickEvent(ClickEvent.Type.LauncherEvent, "launcher.event.switch_ime"))
        -2 -> listOf(ClickEvent(ClickEvent.Type.LauncherEvent, "launcher.event.switch_menu"))
        -3 -> listOf(ClickEvent(ClickEvent.Type.LauncherEvent, "GLFW_MOUSE_BUTTON_LEFT"))
        -4 -> listOf(ClickEvent(ClickEvent.Type.LauncherEvent, "GLFW_MOUSE_BUTTON_RIGHT"))
        -5 -> listOf(ClickEvent(ClickEvent.Type.LauncherEvent, "launcher.event.switch_menu"))
        -6 -> listOf(ClickEvent(ClickEvent.Type.LauncherEvent, "GLFW_MOUSE_BUTTON_MIDDLE"))
        -7 -> listOf(ClickEvent(ClickEvent.Type.LauncherEvent, "launcher.event.scroll_up"))
        -8 -> listOf(ClickEvent(ClickEvent.Type.LauncherEvent, "launcher.event.scroll_down"))
        -9 -> listOf(ClickEvent(ClickEvent.Type.LauncherEvent, "launcher.event.switch_menu"))
        32 -> listOf(ClickEvent(ClickEvent.Type.Key, "GLFW_KEY_SPACE"))
        48 -> listOf(ClickEvent(ClickEvent.Type.Key, "GLFW_KEY_0"))
        49 -> listOf(ClickEvent(ClickEvent.Type.Key, "GLFW_KEY_1"))
        50 -> listOf(ClickEvent(ClickEvent.Type.Key, "GLFW_KEY_2"))
        51 -> listOf(ClickEvent(ClickEvent.Type.Key, "GLFW_KEY_3"))
        52 -> listOf(ClickEvent(ClickEvent.Type.Key, "GLFW_KEY_4"))
        53 -> listOf(ClickEvent(ClickEvent.Type.Key, "GLFW_KEY_5"))
        54 -> listOf(ClickEvent(ClickEvent.Type.Key, "GLFW_KEY_6"))
        55 -> listOf(ClickEvent(ClickEvent.Type.Key, "GLFW_KEY_7"))
        56 -> listOf(ClickEvent(ClickEvent.Type.Key, "GLFW_KEY_8"))
        57 -> listOf(ClickEvent(ClickEvent.Type.Key, "GLFW_KEY_9"))
        65 -> listOf(ClickEvent(ClickEvent.Type.Key, "GLFW_KEY_A"))
        66 -> listOf(ClickEvent(ClickEvent.Type.Key, "GLFW_KEY_B"))
        67 -> listOf(ClickEvent(ClickEvent.Type.Key, "GLFW_KEY_C"))
        68 -> listOf(ClickEvent(ClickEvent.Type.Key, "GLFW_KEY_D"))
        69 -> listOf(ClickEvent(ClickEvent.Type.Key, "GLFW_KEY_E"))
        70 -> listOf(ClickEvent(ClickEvent.Type.Key, "GLFW_KEY_F"))
        71 -> listOf(ClickEvent(ClickEvent.Type.Key, "GLFW_KEY_G"))
        72 -> listOf(ClickEvent(ClickEvent.Type.Key, "GLFW_KEY_H"))
        73 -> listOf(ClickEvent(ClickEvent.Type.Key, "GLFW_KEY_I"))
        74 -> listOf(ClickEvent(ClickEvent.Type.Key, "GLFW_KEY_J"))
        75 -> listOf(ClickEvent(ClickEvent.Type.Key, "GLFW_KEY_K"))
        76 -> listOf(ClickEvent(ClickEvent.Type.Key, "GLFW_KEY_L"))
        77 -> listOf(ClickEvent(ClickEvent.Type.Key, "GLFW_KEY_M"))
        78 -> listOf(ClickEvent(ClickEvent.Type.Key, "GLFW_KEY_N"))
        79 -> listOf(ClickEvent(ClickEvent.Type.Key, "GLFW_KEY_O"))
        80 -> listOf(ClickEvent(ClickEvent.Type.Key, "GLFW_KEY_P"))
        81 -> listOf(ClickEvent(ClickEvent.Type.Key, "GLFW_KEY_Q"))
        82 -> listOf(ClickEvent(ClickEvent.Type.Key, "GLFW_KEY_R"))
        83 -> listOf(ClickEvent(ClickEvent.Type.Key, "GLFW_KEY_S"))
        84 -> listOf(ClickEvent(ClickEvent.Type.Key, "GLFW_KEY_T"))
        85 -> listOf(ClickEvent(ClickEvent.Type.Key, "GLFW_KEY_U"))
        86 -> listOf(ClickEvent(ClickEvent.Type.Key, "GLFW_KEY_V"))
        87 -> listOf(ClickEvent(ClickEvent.Type.Key, "GLFW_KEY_W"))
        88 -> listOf(ClickEvent(ClickEvent.Type.Key, "GLFW_KEY_X"))
        89 -> listOf(ClickEvent(ClickEvent.Type.Key, "GLFW_KEY_Y"))
        90 -> listOf(ClickEvent(ClickEvent.Type.Key, "GLFW_KEY_Z"))
        256 -> listOf(ClickEvent(ClickEvent.Type.Key, "GLFW_KEY_ESCAPE"))
        257 -> listOf(ClickEvent(ClickEvent.Type.Key, "GLFW_KEY_ENTER"))
        258 -> listOf(ClickEvent(ClickEvent.Type.Key, "GLFW_KEY_TAB"))
        259 -> listOf(ClickEvent(ClickEvent.Type.Key, "GLFW_KEY_BACKSPACE"))
        260 -> listOf(ClickEvent(ClickEvent.Type.Key, "GLFW_KEY_INSERT"))
        261 -> listOf(ClickEvent(ClickEvent.Type.Key, "GLFW_KEY_DELETE"))
        262 -> listOf(ClickEvent(ClickEvent.Type.Key, "GLFW_KEY_RIGHT"))
        263 -> listOf(ClickEvent(ClickEvent.Type.Key, "GLFW_KEY_LEFT"))
        264 -> listOf(ClickEvent(ClickEvent.Type.Key, "GLFW_KEY_DOWN"))
        265 -> listOf(ClickEvent(ClickEvent.Type.Key, "GLFW_KEY_UP"))
        280 -> listOf(ClickEvent(ClickEvent.Type.Key, "GLFW_KEY_CAPS_LOCK"))
        290 -> listOf(ClickEvent(ClickEvent.Type.Key, "GLFW_KEY_F1"))
        291 -> listOf(ClickEvent(ClickEvent.Type.Key, "GLFW_KEY_F2"))
        292 -> listOf(ClickEvent(ClickEvent.Type.Key, "GLFW_KEY_F3"))
        293 -> listOf(ClickEvent(ClickEvent.Type.Key, "GLFW_KEY_F4"))
        294 -> listOf(ClickEvent(ClickEvent.Type.Key, "GLFW_KEY_F5"))
        295 -> listOf(ClickEvent(ClickEvent.Type.Key, "GLFW_KEY_F6"))
        296 -> listOf(ClickEvent(ClickEvent.Type.Key, "GLFW_KEY_F7"))
        297 -> listOf(ClickEvent(ClickEvent.Type.Key, "GLFW_KEY_F8"))
        298 -> listOf(ClickEvent(ClickEvent.Type.Key, "GLFW_KEY_F9"))
        299 -> listOf(ClickEvent(ClickEvent.Type.Key, "GLFW_KEY_F10"))
        300 -> listOf(ClickEvent(ClickEvent.Type.Key, "GLFW_KEY_F11"))
        301 -> listOf(ClickEvent(ClickEvent.Type.Key, "GLFW_KEY_F12"))
        340 -> listOf(ClickEvent(ClickEvent.Type.Key, "GLFW_KEY_LEFT_SHIFT"))
        341 -> listOf(ClickEvent(ClickEvent.Type.Key, "GLFW_KEY_LEFT_CONTROL"))
        342 -> listOf(ClickEvent(ClickEvent.Type.Key, "GLFW_KEY_LEFT_ALT"))
        343 -> listOf(ClickEvent(ClickEvent.Type.Key, "GLFW_KEY_RIGHT_SHIFT"))
        344 -> listOf(ClickEvent(ClickEvent.Type.Key, "GLFW_KEY_RIGHT_CONTROL"))
        345 -> listOf(ClickEvent(ClickEvent.Type.Key, "GLFW_KEY_RIGHT_ALT"))
        347 -> listOf(ClickEvent(ClickEvent.Type.Key, "GLFW_KEY_RIGHT_CONTROL"))
        348 -> listOf(ClickEvent(ClickEvent.Type.Key, "GLFW_KEY_RIGHT_ALT"))
        else -> listOf(ClickEvent(ClickEvent.Type.Key, "GLFW_KEY_UNKNOWN"))
    }
}
