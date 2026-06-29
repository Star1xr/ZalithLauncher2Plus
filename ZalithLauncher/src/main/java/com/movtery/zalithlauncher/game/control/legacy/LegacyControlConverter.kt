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
   *   - dynamicX/Y expressions evaluate to pixel positions (LEFT/TOP edge of the button).
   *   - Variables: screen_width/screen_height = physical screen pixels; width/height = button
   *     size in PIXELS (stored as dp in JSON, converted at runtime via density).
   *   - dp = display density (px per dp); px(v) converts dp→px; dp(v) converts px→dp.
   *   - margin = a small padding constant (8dp).
   *   - right = screen_width - button_width_px; bottom = screen_height - button_height_px.
   *
   * Conversion approach:
   *   1. Substitute all variables with pixel values using REF_W x REF_H reference screen.
   *   2. Evaluate the resulting arithmetic expression to get a pixel LEFT/TOP position.
   *   3. Divide by REF_W (X) or REF_H (Y) to normalise to [0,1].
   *   4. Add half the button size fraction to obtain the CENTER position.
   *   5. Multiply by 10000 for the LayerController coordinate space.
   *
   * ZL1 keycodes: the 'keycodes' JSON array stores GLFW integer values directly
   * (e.g. GLFW_KEY_A=65, GLFW_KEY_ESCAPE=256) — NOT LWJGL2 scan-codes.
   * glfwIntToName provides the complete integer→string-name conversion.
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

      /** Reference screen resolution used for position normalisation. */
      private const val REF_W = 1280f
      private const val REF_H = 720f

      /**
       * Reference display density (dp→px multiplier) assumed when converting ZL1 layouts.
       * ZL1 stores button sizes in dp; at XHDPI (density 2.0) 50dp = 100px.
       * Using 2.0 matches the most common Android device class.
       */
      private const val DEFAULT_DENSITY = 2f

      /**
       * Standard margin distance used by ZL1 (ControlInterface.getMarginDistance()).
       * Typically 8dp converted to pixels = 8 * density.
       */
      private const val MARGIN_DP = 8f

      // ─────────────────────────────────────────────────────────────────────────
      // ZL1 keycodes are stored as GLFW integer values (the same constants as
      // org.lwjgl.glfw.GLFW / LwjglGlfwKeycode).  This map converts those integers
      // to the GLFW key-name strings expected by ControlEventKeycode.getKeycodeFromEvent().
      // ─────────────────────────────────────────────────────────────────────────
      private val glfwIntToName: Map<Int, String> = mapOf(
          // Printable keys
          32  to "GLFW_KEY_SPACE",
          39  to "GLFW_KEY_APOSTROPHE",
          44  to "GLFW_KEY_COMMA",
          45  to "GLFW_KEY_MINUS",
          46  to "GLFW_KEY_PERIOD",
          47  to "GLFW_KEY_SLASH",
          48  to "GLFW_KEY_0",
          49  to "GLFW_KEY_1",
          50  to "GLFW_KEY_2",
          51  to "GLFW_KEY_3",
          52  to "GLFW_KEY_4",
          53  to "GLFW_KEY_5",
          54  to "GLFW_KEY_6",
          55  to "GLFW_KEY_7",
          56  to "GLFW_KEY_8",
          57  to "GLFW_KEY_9",
          59  to "GLFW_KEY_SEMICOLON",
          61  to "GLFW_KEY_EQUAL",
          65  to "GLFW_KEY_A",
          66  to "GLFW_KEY_B",
          67  to "GLFW_KEY_C",
          68  to "GLFW_KEY_D",
          69  to "GLFW_KEY_E",
          70  to "GLFW_KEY_F",
          71  to "GLFW_KEY_G",
          72  to "GLFW_KEY_H",
          73  to "GLFW_KEY_I",
          74  to "GLFW_KEY_J",
          75  to "GLFW_KEY_K",
          76  to "GLFW_KEY_L",
          77  to "GLFW_KEY_M",
          78  to "GLFW_KEY_N",
          79  to "GLFW_KEY_O",
          80  to "GLFW_KEY_P",
          81  to "GLFW_KEY_Q",
          82  to "GLFW_KEY_R",
          83  to "GLFW_KEY_S",
          84  to "GLFW_KEY_T",
          85  to "GLFW_KEY_U",
          86  to "GLFW_KEY_V",
          87  to "GLFW_KEY_W",
          88  to "GLFW_KEY_X",
          89  to "GLFW_KEY_Y",
          90  to "GLFW_KEY_Z",
          91  to "GLFW_KEY_LEFT_BRACKET",
          92  to "GLFW_KEY_BACKSLASH",
          93  to "GLFW_KEY_RIGHT_BRACKET",
          96  to "GLFW_KEY_GRAVE_ACCENT",
          161 to "GLFW_KEY_WORLD_1",
          162 to "GLFW_KEY_WORLD_2",
          // Function / control keys
          256 to "GLFW_KEY_ESCAPE",
          257 to "GLFW_KEY_ENTER",
          258 to "GLFW_KEY_TAB",
          259 to "GLFW_KEY_BACKSPACE",
          260 to "GLFW_KEY_INSERT",
          261 to "GLFW_KEY_DELETE",
          262 to "GLFW_KEY_RIGHT",
          263 to "GLFW_KEY_LEFT",
          264 to "GLFW_KEY_DOWN",
          265 to "GLFW_KEY_UP",
          266 to "GLFW_KEY_PAGE_UP",
          267 to "GLFW_KEY_PAGE_DOWN",
          268 to "GLFW_KEY_HOME",
          269 to "GLFW_KEY_END",
          280 to "GLFW_KEY_CAPS_LOCK",
          281 to "GLFW_KEY_SCROLL_LOCK",
          282 to "GLFW_KEY_NUM_LOCK",
          283 to "GLFW_KEY_PRINT_SCREEN",
          284 to "GLFW_KEY_PAUSE",
          // F-keys
          290 to "GLFW_KEY_F1",
          291 to "GLFW_KEY_F2",
          292 to "GLFW_KEY_F3",
          293 to "GLFW_KEY_F4",
          294 to "GLFW_KEY_F5",
          295 to "GLFW_KEY_F6",
          296 to "GLFW_KEY_F7",
          297 to "GLFW_KEY_F8",
          298 to "GLFW_KEY_F9",
          299 to "GLFW_KEY_F10",
          300 to "GLFW_KEY_F11",
          301 to "GLFW_KEY_F12",
          302 to "GLFW_KEY_F13",
          303 to "GLFW_KEY_F14",
          304 to "GLFW_KEY_F15",
          305 to "GLFW_KEY_F16",
          306 to "GLFW_KEY_F17",
          307 to "GLFW_KEY_F18",
          308 to "GLFW_KEY_F19",
          309 to "GLFW_KEY_F20",
          310 to "GLFW_KEY_F21",
          311 to "GLFW_KEY_F22",
          312 to "GLFW_KEY_F23",
          313 to "GLFW_KEY_F24",
          314 to "GLFW_KEY_F25",
          // Numpad
          320 to "GLFW_KEY_KP_0",
          321 to "GLFW_KEY_KP_1",
          322 to "GLFW_KEY_KP_2",
          323 to "GLFW_KEY_KP_3",
          324 to "GLFW_KEY_KP_4",
          325 to "GLFW_KEY_KP_5",
          326 to "GLFW_KEY_KP_6",
          327 to "GLFW_KEY_KP_7",
          328 to "GLFW_KEY_KP_8",
          329 to "GLFW_KEY_KP_9",
          330 to "GLFW_KEY_KP_DECIMAL",
          331 to "GLFW_KEY_KP_DIVIDE",
          332 to "GLFW_KEY_KP_MULTIPLY",
          333 to "GLFW_KEY_KP_SUBTRACT",
          334 to "GLFW_KEY_KP_ADD",
          335 to "GLFW_KEY_KP_ENTER",
          336 to "GLFW_KEY_KP_EQUAL",
          // Modifiers
          340 to "GLFW_KEY_LEFT_SHIFT",
          341 to "GLFW_KEY_LEFT_CONTROL",
          342 to "GLFW_KEY_LEFT_ALT",
          343 to "GLFW_KEY_LEFT_SUPER",
          344 to "GLFW_KEY_RIGHT_SHIFT",
          345 to "GLFW_KEY_RIGHT_CONTROL",
          346 to "GLFW_KEY_RIGHT_ALT",
          347 to "GLFW_KEY_RIGHT_SUPER",
          348 to "GLFW_KEY_MENU",
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
          val scaledAt = src.optDouble("scaledAt", 100.0).toFloat()

          val mainButtons = JSONArray()
          val extraLayers = JSONArray()

          // ── Regular buttons ──────────────────────────────────────────────────
          src.optJSONArray("mControlDataList")?.let { arr ->
              for (i in 0 until arr.length()) {
                  arr.optJSONObject(i)?.let { buildButton(it, scaledAt)?.let(mainButtons::put) }
              }
          }

          // ── Drawer controls ──────────────────────────────────────────────────
          src.optJSONArray("mDrawerDataList")?.let { arr ->
              for (i in 0 until arr.length()) {
                  arr.optJSONObject(i)?.let { drawer ->
                      val drawerLayerUuid = UUID.randomUUID().toString()
                      val drawerButtons = JSONArray()
                      drawer.optJSONArray("buttonProperties")?.let { btnArr ->
                          for (j in 0 until btnArr.length()) {
                              btnArr.optJSONObject(j)?.let { buildButton(it, scaledAt)?.let(drawerButtons::put) }
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

                      val triggerBtn = drawer.optJSONObject("properties")?.let { buildButton(it, scaledAt) }
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

          // ── Joystick controls ─────────────────────────────────────────────────
          // ZL1 joysticks are analog directional controls that send WASD-style keycodes.
          // ZL2's ControlLayer has no native analog joystick widget, so we represent each
          // joystick as four swipeable directional buttons arranged in a D-pad pattern.
          src.optJSONArray("mJoystickDataList")?.let { arr ->
              for (i in 0 until arr.length()) {
                  arr.optJSONObject(i)?.let { joystick ->
                      buildJoystickButtons(joystick, scaledAt).forEach(mainButtons::put)
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

      // ─────────────────────────────────────────────────────────────────────────
      // Button conversion
      // ─────────────────────────────────────────────────────────────────────────

      private fun buildButton(btn: JSONObject, preferredScale: Float = 100f): JSONObject? = try {
          // ZL1 JSON stores width/height in dp.  Convert dp→px for position evaluation.
          val widthDp  = btn.optDouble("width",  50.0).toFloat().coerceAtLeast(5f)
          val heightDp = btn.optDouble("height", 50.0).toFloat().coerceAtLeast(5f)
          val widthPx  = widthDp  * DEFAULT_DENSITY
          val heightPx = heightDp * DEFAULT_DENSITY

          // Parse dynamicX/Y (pixel expressions) → normalised [0,1] fraction.
          val xLeftNorm = parseExpr(btn.optString("dynamicX", ""), widthPx, heightPx, REF_W, preferredScale)
          val yTopNorm  = parseExpr(btn.optString("dynamicY", ""), widthPx, heightPx, REF_H, preferredScale)

          // Convert left/top fraction to center position fraction, then to [0,10000].
          val xCenter = (xLeftNorm + widthPx  / REF_W / 2f).coerceIn(0f, 1f)
          val yCenter = (yTopNorm  + heightPx / REF_H / 2f).coerceIn(0f, 1f)
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
                  put("widthDp",          widthDp.toDouble())
                  put("heightDp",         heightDp.toDouble())
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

      // ─────────────────────────────────────────────────────────────────────────
      // Joystick → D-pad button group conversion
      //
      // ZL1 joysticks hard-code W/A/S/D as the directional keys by default.
      // Each joystick becomes four swipeable directional buttons placed around
      // the joystick's centre position.  Button size matches the joystick size.
      // ─────────────────────────────────────────────────────────────────────────

      // Default ZL1 joystick GLFW keycodes (forward/backward/left/right)
      private const val JOYSTICK_FORWARD  = 87  // GLFW_KEY_W
      private const val JOYSTICK_BACKWARD = 83  // GLFW_KEY_S
      private const val JOYSTICK_LEFT     = 65  // GLFW_KEY_A
      private const val JOYSTICK_RIGHT    = 68  // GLFW_KEY_D

      private fun buildJoystickButtons(joystick: JSONObject, preferredScale: Float): List<JSONObject> {
          val widthDp  = joystick.optDouble("width",  100.0).toFloat().coerceAtLeast(20f)
          val heightDp = joystick.optDouble("height", 100.0).toFloat().coerceAtLeast(20f)
          val widthPx  = widthDp  * DEFAULT_DENSITY
          val heightPx = heightDp * DEFAULT_DENSITY

          val xCenterNorm = run {
              val xLeft = parseExpr(joystick.optString("dynamicX", ""), widthPx, heightPx, REF_W, preferredScale)
              (xLeft + widthPx / REF_W / 2f).coerceIn(0f, 1f)
          }
          val yCenterNorm = run {
              val yTop = parseExpr(joystick.optString("dynamicY", ""), widthPx, heightPx, REF_H, preferredScale)
              (yTop + heightPx / REF_H / 2f).coerceIn(0f, 1f)
          }

          val xCenter = (xCenterNorm * 10000).toInt()
          val yCenter = (yCenterNorm * 10000).toInt()

          // Button size for each D-pad direction: half the joystick size
          val btnWDp = (widthDp  / 2f).coerceAtLeast(15f)
          val btnHDp = (heightDp / 2f).coerceAtLeast(15f)
          val btnWPx = btnWDp * DEFAULT_DENSITY
          val btnHPx = btnHDp * DEFAULT_DENSITY

          // Offset from center in normalised units to place each direction button
          val offsetX = ((widthPx / 2f + btnWPx / 2f) / REF_W * 10000).toInt()
          val offsetY = ((heightPx / 2f + btnHPx / 2f) / REF_H * 10000).toInt()

          val displayInGame = joystick.optBoolean("displayInGame", true)
          val displayInMenu = joystick.optBoolean("displayInMenu", true)
          val visType = when {
              displayInGame && displayInMenu -> "always"
              displayInGame -> "in_game"
              displayInMenu -> "in_menu"
              else -> "always"
          }

          // Read custom keycodes if available; fall back to W/A/S/D for any missing slot.
          // ZL1 joysticks store [forward, backward, left, right] but may have fewer than 4 entries.
          val keycodes = run {
              val defaults = intArrayOf(JOYSTICK_FORWARD, JOYSTICK_BACKWARD, JOYSTICK_LEFT, JOYSTICK_RIGHT)
              val arr = joystick.optJSONArray("keycodes")
              if (arr != null && arr.length() > 0) {
                  IntArray(4) { i -> arr.optInt(i, 0).let { v -> if (v != 0) v else defaults[i] } }
              } else {
                  defaults
              }
          }

          fun dirBtn(label: String, x: Int, y: Int, keycode: Int): JSONObject {
              val events = JSONArray()
              if (keycode != 0) keycodeToEventJson(keycode)?.let(events::put)
              return JSONObject().apply {
                  put("text",           tsJson(label))
                  put("uuid",           UUID.randomUUID().toString())
                  put("position",       JSONObject().apply { put("x", x.coerceIn(0,10000)); put("y", y.coerceIn(0,10000)) })
                  put("buttonSize",     JSONObject().apply {
                      put("type",             "dp")
                      put("widthDp",          btnWDp.toDouble())
                      put("heightDp",         btnHDp.toDouble())
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
                  put("clickEvents",    events)
                  put("isSwipple",      true)
                  put("isPenetrable",   false)
                  put("isToggleable",   false)
              }
          }

          return listOf(
              dirBtn("↑", xCenter,           yCenter - offsetY, keycodes[0]),  // Forward
              dirBtn("↓", xCenter,           yCenter + offsetY, keycodes[1]),  // Backward
              dirBtn("←", xCenter - offsetX, yCenter,           keycodes[2]),  // Left
              dirBtn("→", xCenter + offsetX, yCenter,           keycodes[3]),  // Right
          )
      }

      // ─────────────────────────────────────────────────────────────────────────
      // Keycode parsing helpers
      // ─────────────────────────────────────────────────────────────────────────

      /**
       * Extract GLFW integer keycodes from a ZL1 button JSON object.
       *
       * ZL1 has two storage formats:
       *  - New: "keycodes" array of GLFW integers (directly usable with glfwIntToName).
       *  - Old: single "keycode" GLFW integer + boolean modifier flags.
       *
       * The holdShift/holdCtrl/holdAlt flags map to GLFW modifier key codes:
       *   LEFT_SHIFT=340, LEFT_CONTROL=341, LEFT_ALT=342.
       */
      private fun parseKeycodes(btn: JSONObject): List<Int> {
          val arr = btn.optJSONArray("keycodes")
          if (arr != null) {
              // New format: array of GLFW integers. Filter out zero (GLFW_KEY_UNKNOWN).
              return (0 until arr.length()).mapNotNull { i -> arr.optInt(i, 0).takeIf { it != 0 } }
          }
          // Old format: single keycode + modifier booleans
          val result = mutableListOf<Int>()
          if (btn.optBoolean("holdShift", false)) result.add(340) // GLFW_KEY_LEFT_SHIFT
          if (btn.optBoolean("holdCtrl",  false)) result.add(341) // GLFW_KEY_LEFT_CONTROL
          if (btn.optBoolean("holdAlt",   false)) result.add(342) // GLFW_KEY_LEFT_ALT
          val kc = btn.optInt("keycode", 0)
          if (kc != 0) result.add(kc)
          return result
      }

      /**
       * Convert a ZL1 keycode integer to a ZL2 click event JSON object.
       *
       * Negative values are ZL1 special-button codes mapped to launcher events.
       * Non-negative values are GLFW integer codes converted via glfwIntToName.
       * Unknown GLFW codes (not in glfwIntToName) are silently dropped.
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
              val glfwName = glfwIntToName[keycode]
              if (glfwName != null) {
                  JSONObject().apply { put("type", "key"); put("key", glfwName) }
              } else {
                  null // Unknown GLFW code — drop silently
              }
          }
      }

      private fun launcherEventJson(key: String) = JSONObject().apply {
          put("type", "launcher_event"); put("key", key)
      }

      // ─────────────────────────────────────────────────────────────────────────
      // Position expression evaluator
      // ─────────────────────────────────────────────────────────────────────────

      /**
       * Evaluate a ZL1 dynamic position expression and return a normalised [0,1] fraction.
       *
       * @param expr          The dynamicX or dynamicY expression string from the ZL1 JSON.
       * @param widthPx       Button width in pixels at the reference density.
       * @param heightPx      Button height in pixels at the reference density.
       * @param screenDimPx   REF_W for an X expression, REF_H for a Y expression.
       * @param preferredScale The layout's scaledAt value (default 100).
       *
       * All variable tokens are substituted with their pixel equivalents, the
       * arithmetic is evaluated, and the result is divided by [screenDimPx] to
       * obtain a [0,1] fraction for the button's LEFT (or TOP) edge.
       */
      private fun parseExpr(
          expr: String,
          widthPx: Float,
          heightPx: Float,
          screenDimPx: Float,
          preferredScale: Float = 100f
      ): Float {
          if (expr.isBlank()) return 0f
          return try {
              val d = "$"
              val marginPx = MARGIN_DP * DEFAULT_DENSITY

              // Substitute variable tokens with their pixel-value equivalents.
              // Order matters: replace longer/more-specific names before shorter ones
              // (e.g. screen_width before width) to avoid partial replacements.
              var s = expr.trim()
                  .replace(d + "{screen_width}",    "%.4f".format(REF_W))
                  .replace(d + "{screen_height}",   "%.4f".format(REF_H))
                  .replace(d + "{right}",           "(%.4f-%.4f)".format(REF_W, widthPx))
                  .replace(d + "{bottom}",          "(%.4f-%.4f)".format(REF_H, heightPx))
                  .replace(d + "{width}",           "%.4f".format(widthPx))
                  .replace(d + "{height}",          "%.4f".format(heightPx))
                  .replace(d + "{dp}",              "%.4f".format(DEFAULT_DENSITY))
                  .replace(d + "{preferred_scale}", "%.2f".format(preferredScale))
                  .replace(d + "{ratio}",           "1.0")
                  .replace(d + "{margin}",          "%.4f".format(marginPx))

              // Replace px(value) calls: converts dp→px = value * DEFAULT_DENSITY.
              s = replacePxCalls(s, DEFAULT_DENSITY)
              // Replace dp(value) calls: converts px→dp = value / DEFAULT_DENSITY.
              s = replaceDpCalls(s, DEFAULT_DENSITY)
              // Strip any remaining unresolved tokens.
              s = stripUnresolved(s, d)

              // Evaluate arithmetic expression (result is in pixels).
              // Divide by screen dimension to get normalised [0,1] fraction.
              (ExprParser(s).parse() / screenDimPx).coerceIn(0f, 1f)
          } catch (_: Exception) { 0f }
      }

      /**
       * Replace px(number) calls: px(v) converts dp value v to pixels = v * density.
       */
      private fun replacePxCalls(expr: String, density: Float): String {
          val sb = StringBuilder()
          var i = 0
          while (i < expr.length) {
              if (expr.startsWith("px(", i)) {
                  val start = i + 3
                  val end   = expr.indexOf(')', start)
                  if (end > start) {
                      val dpVal = expr.substring(start, end).toFloatOrNull() ?: 0f
                      sb.append("%.4f".format(dpVal * density))
                      i = end + 1
                      continue
                  }
              }
              sb.append(expr[i])
              i++
          }
          return sb.toString()
      }

      /**
       * Replace dp(number) calls: dp(v) converts pixel value v to dp = v / density.
       */
      private fun replaceDpCalls(expr: String, density: Float): String {
          val sb = StringBuilder()
          var i = 0
          while (i < expr.length) {
              if (expr.startsWith("dp(", i)) {
                  val start = i + 3
                  val end   = expr.indexOf(')', start)
                  if (end > start) {
                      val pxVal = expr.substring(start, end).toFloatOrNull() ?: 0f
                      sb.append("%.4f".format(if (density != 0f) pxVal / density else 0f))
                      i = end + 1
                      continue
                  }
              }
              sb.append(expr[i])
              i++
          }
          return sb.toString()
      }

      /**
       * Replace any remaining variable tokens (dollar + brace-enclosed name) with "0.0".
       */
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

      // ─────────────────────────────────────────────────────────────────────────
      // Minimal recursive-descent arithmetic parser
      // Supports: +, -, *, /, (, ), unary minus, float literals.
      // ─────────────────────────────────────────────────────────────────────────

      private class ExprParser(private val s: String) {
          private var i = 0
          fun parse() = expr()
          private fun expr(): Float {
              var r = term(); spaces()
              while (i < s.length) {
                  val op = s[i]
                  if (op == '+' || op == '-') { i++; spaces(); r = if (op == '+') r + term() else r - term(); spaces() } else break
              }
              return r
          }
          private fun term(): Float {
              var r = unary(); spaces()
              while (i < s.length) {
                  val op = s[i]
                  if (op == '*' || op == '/') { i++; spaces(); val d = unary(); r = if (op == '*') r * d else if (d != 0f) r / d else 0f; spaces() } else break
              }
              return r
          }
          private fun unary(): Float {
              spaces()
              return if (i < s.length && s[i] == '-') { i++; -unary() }
              else if (i < s.length && s[i] == '+') { i++; unary() }
              else primary()
          }
          private fun primary(): Float {
              spaces()
              if (i < s.length && s[i] == '(') {
                  i++; val v = expr(); spaces(); if (i < s.length && s[i] == ')') i++; return v
              }
              val start = i
              if (i < s.length && (s[i] == '-' || s[i] == '+')) i++
              while (i < s.length && (s[i].isDigit() || s[i] == '.' || s[i] == 'E' || s[i] == 'e' ||
                         ((s[i] == '+' || s[i] == '-') && i > 0 && (s[i-1] == 'E' || s[i-1] == 'e')))) i++
              return s.substring(start, i).toFloatOrNull() ?: 0f
          }
          private fun spaces() { while (i < s.length && s[i].isWhitespace()) i++ }
      }
  }
  