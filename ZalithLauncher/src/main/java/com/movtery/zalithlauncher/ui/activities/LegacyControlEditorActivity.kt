package com.movtery.zalithlauncher.ui.activities

  import android.content.Context
  import android.content.Intent
  import android.os.Bundle
  import androidx.activity.compose.setContent
  import androidx.compose.foundation.Canvas
  import androidx.compose.foundation.background
  import androidx.compose.foundation.border
  import androidx.compose.foundation.gestures.awaitEachGesture
  import androidx.compose.foundation.gestures.awaitFirstDown
  import androidx.compose.foundation.layout.Arrangement
  import androidx.compose.foundation.layout.Box
  import androidx.compose.foundation.layout.BoxWithConstraints
  import androidx.compose.foundation.layout.Column
  import androidx.compose.foundation.layout.Row
  import androidx.compose.foundation.layout.Spacer
  import androidx.compose.foundation.layout.absoluteOffset
  import androidx.compose.foundation.layout.fillMaxSize
  import androidx.compose.foundation.layout.fillMaxWidth
  import androidx.compose.foundation.layout.padding
  import androidx.compose.foundation.layout.size
  import androidx.compose.foundation.layout.width
  import androidx.compose.foundation.rememberScrollState
  import androidx.compose.foundation.shape.CircleShape
  import androidx.compose.foundation.shape.RoundedCornerShape
  import androidx.compose.foundation.text.KeyboardOptions
  import androidx.compose.foundation.verticalScroll
  import androidx.compose.material3.AlertDialog
  import androidx.compose.material3.Button
  import androidx.compose.material3.Checkbox
  import androidx.compose.material3.DropdownMenu
  import androidx.compose.material3.DropdownMenuItem
  import androidx.compose.material3.ExperimentalMaterial3Api
  import androidx.compose.material3.FilledTonalButton
  import androidx.compose.material3.HorizontalDivider
  import androidx.compose.material3.Icon
  import androidx.compose.material3.IconButton
  import androidx.compose.material3.MaterialTheme
  import androidx.compose.material3.Scaffold
  import androidx.compose.material3.Surface
  import androidx.compose.material3.Text
  import androidx.compose.material3.TextButton
  import androidx.compose.material3.TopAppBar
  import androidx.compose.runtime.Composable
  import androidx.compose.runtime.LaunchedEffect
  import androidx.compose.runtime.getValue
  import androidx.compose.runtime.mutableStateOf
  import androidx.compose.runtime.remember
  import androidx.compose.runtime.setValue
  import androidx.compose.ui.Alignment
  import androidx.compose.ui.Modifier
  import androidx.compose.ui.geometry.Offset
  import androidx.compose.ui.graphics.Color
  import androidx.compose.ui.input.pointer.pointerInput
  import androidx.compose.ui.platform.LocalDensity
  import androidx.compose.ui.res.painterResource
  import androidx.compose.ui.res.stringResource
  import androidx.compose.ui.text.input.KeyboardType
  import androidx.compose.ui.text.style.TextOverflow
  import androidx.compose.ui.unit.IntOffset
  import androidx.compose.ui.unit.dp
  import androidx.compose.ui.unit.sp
  import androidx.compose.ui.window.Dialog
  import com.movtery.zalithlauncher.R
  import com.movtery.zalithlauncher.game.control.legacy.LegacyControlManager
  import com.movtery.zalithlauncher.ui.base.BaseAppCompatActivity
  import com.movtery.zalithlauncher.ui.components.OwnOutlinedTextField
  import com.movtery.zalithlauncher.ui.components.SimpleAlertDialog
  import com.movtery.zalithlauncher.ui.theme.ZalithLauncherTheme
  import dagger.hilt.android.AndroidEntryPoint
  import org.json.JSONArray
  import org.json.JSONObject
  import java.io.File
  import java.util.UUID
  import kotlin.math.roundToInt

  private const val BUNDLE_LEGACY_CONTROL = "BUNDLE_LEGACY_CONTROL"

  @AndroidEntryPoint
  class LegacyControlEditorActivity : BaseAppCompatActivity() {
      override fun onCreate(savedInstanceState: Bundle?) {
          super.onCreate(savedInstanceState)
          val path = intent.extras?.getString(BUNDLE_LEGACY_CONTROL) ?: run { finish(); return }
          val file = File(path).takeIf { it.exists() && it.isFile } ?: run { finish(); return }
          setContent {
              ZalithLauncherTheme {
                  LegacyControlEditorScreen(controlFile = file, onExit = { finish() })
              }
          }
      }
  }

  private enum class ButtonType {
      REGULAR, JOYSTICK, DRAWER_TRIGGER, DRAWER_CONTENT
  }

  private data class CanvasButton(
      val id: String = UUID.randomUUID().toString(),
      val type: ButtonType = ButtonType.REGULAR,
      val name: String,
      val xFrac: Float,
      val yFrac: Float,
      val widthDp: Float,
      val heightDp: Float,
      val keycodes: List<Int>,
      val isToggle: Boolean,
      val isSwipeable: Boolean,
      val passThruEnabled: Boolean,
      val displayInGame: Boolean,
      val displayInMenu: Boolean,
      val opacity: Float,
      val bgColor: Int,
      val fgColor: Int,
      val strokeWidth: Float,
      val cornerRadius: Float,
      val drawerIndex: Int = -1,
      val forwardLock: Boolean = false,
      val absolute: Boolean = false
  ) {
      val isDrawerTrigger get() = type == ButtonType.DRAWER_TRIGGER
      val isDrawerContent get() = type == ButtonType.DRAWER_CONTENT
      val isJoystick get() = type == ButtonType.JOYSTICK
  }

  private data class DragState(val id: String, val delta: Offset)
  private enum class AddItemType { NONE, BUTTON, JOYSTICK, DRAWER }

  @OptIn(ExperimentalMaterial3Api::class)
  @Composable
  private fun LegacyControlEditorScreen(controlFile: File, onExit: () -> Unit) {
      var jsonRoot by remember { mutableStateOf<JSONObject?>(null) }
      var buttons by remember { mutableStateOf<List<CanvasButton>>(emptyList()) }
      var selectedId by remember { mutableStateOf<String?>(null) }
      var isModified by remember { mutableStateOf(false) }
      var showExitDialog by remember { mutableStateOf(false) }
      var addItemType by remember { mutableStateOf(AddItemType.NONE) }
      var showAddMenu by remember { mutableStateOf(false) }
      var editingButton by remember { mutableStateOf<CanvasButton?>(null) }
      var saveError by remember { mutableStateOf<String?>(null) }
      val dragState = remember { mutableStateOf<DragState?>(null) }
      val density = LocalDensity.current
      val selectedButton = buttons.find { it.id == selectedId }

      LaunchedEffect(Unit) {
          try {
              val root = JSONObject(controlFile.readText())
              jsonRoot = root
              buttons = parseAllControls(root)
          } catch (_: Exception) {}
      }

      fun saveFile(): Boolean = try {
          val root = jsonRoot ?: JSONObject().also {
              it.put("version", 8); it.put("scaledAt", 100.0)
              it.put("mDrawerDataList", JSONArray())
              it.put("mJoystickDataList", JSONArray())
          }

          // Regular buttons
          val regularButtons = buttons.filter { it.type == ButtonType.REGULAR }
          root.put("mControlDataList", JSONArray().also { a ->
              regularButtons.forEach { b -> a.put(buildButtonJson(b)) }
          })

          // Joysticks
          val joysticks = buttons.filter { it.type == ButtonType.JOYSTICK }
          root.put("mJoystickDataList", JSONArray().also { a ->
              joysticks.forEach { j -> a.put(buildJoystickJson(j)) }
          })

          // Drawers
          val drawerIndices = buttons
              .filter { it.isDrawerTrigger || it.isDrawerContent }
              .map { it.drawerIndex }.filter { it >= 0 }.toSet().sorted()
          val drawerList = JSONArray()
          for (di in drawerIndices) {
              val drawerObj = JSONObject()
              buttons.find { it.isDrawerTrigger && it.drawerIndex == di }
                  ?.let { drawerObj.put("properties", buildButtonJson(it)) }
              val btnPropsArr = JSONArray()
              buttons.filter { it.isDrawerContent && it.drawerIndex == di }
                  .forEach { btnPropsArr.put(buildButtonJson(it)) }
              drawerObj.put("buttonProperties", btnPropsArr)
              root.optJSONArray("mDrawerDataList")?.optJSONObject(di)?.let { orig ->
                  orig.keys().forEach { key ->
                      if (key != "properties" && key != "buttonProperties") {
                          drawerObj.put(key, orig.get(key))
                      }
                  }
              }
              drawerList.put(drawerObj)
          }
          root.put("mDrawerDataList", drawerList)

          controlFile.writeText(root.toString(2))
          LegacyControlManager.refresh()
          isModified = false
          true
      } catch (e: Exception) {
          saveError = e.message ?: "Save failed"
          false
      }

      if (showExitDialog) {
          SimpleAlertDialog(
              title = stringResource(R.string.legacy_control_editor_unsaved_title),
              text = stringResource(R.string.legacy_control_editor_unsaved_message),
              onDismiss = { showExitDialog = false },
              onConfirm = { saveFile(); showExitDialog = false; onExit() }
          )
      }

      when (addItemType) {
          AddItemType.BUTTON -> ButtonPropertiesDialog(
              title = stringResource(R.string.legacy_control_editor_add_button),
              initial = null, isJoystick = false,
              onDismiss = { addItemType = AddItemType.NONE },
              onConfirm = { nb ->
                  buttons = buttons + nb; isModified = true
                  selectedId = nb.id; addItemType = AddItemType.NONE
              }
          )
          AddItemType.JOYSTICK -> {
              val defaultJoy = CanvasButton(
                  type = ButtonType.JOYSTICK, name = "Joystick",
                  xFrac = 0.15f, yFrac = 0.7f, widthDp = 120f, heightDp = 120f,
                  keycodes = listOf(87, 83, 65, 68),
                  isToggle = false, isSwipeable = false, passThruEnabled = false,
                  displayInGame = true, displayInMenu = false,
                  opacity = 1f, bgColor = 0x4D000000.toInt(), fgColor = 0xFFFFFFFF.toInt(),
                  strokeWidth = 0f, cornerRadius = 50f
              )
              ButtonPropertiesDialog(
                  title = stringResource(R.string.legacy_control_editor_add_joystick),
                  initial = defaultJoy, isJoystick = true,
                  onDismiss = { addItemType = AddItemType.NONE },
                  onConfirm = { nb ->
                      buttons = buttons + nb; isModified = true
                      selectedId = nb.id; addItemType = AddItemType.NONE
                  }
              )
          }
          AddItemType.DRAWER -> {
              val nextIdx = (buttons.mapNotNull {
                  if (it.isDrawerTrigger || it.isDrawerContent) it.drawerIndex else null
              }.maxOrNull() ?: -1) + 1
              val trigger = CanvasButton(
                  type = ButtonType.DRAWER_TRIGGER, name = "Drawer",
                  xFrac = 0.5f, yFrac = 0.5f, widthDp = 80f, heightDp = 50f,
                  keycodes = emptyList(), isToggle = false, isSwipeable = false,
                  passThruEnabled = false, displayInGame = true, displayInMenu = true,
                  opacity = 1f, bgColor = 0x4D000000.toInt(), fgColor = 0xFFFFFFFF.toInt(),
                  strokeWidth = 0f, cornerRadius = 0f, drawerIndex = nextIdx
              )
              val content = CanvasButton(
                  type = ButtonType.DRAWER_CONTENT, name = "Btn",
                  xFrac = 0.55f, yFrac = 0.5f, widthDp = 50f, heightDp = 50f,
                  keycodes = emptyList(), isToggle = false, isSwipeable = false,
                  passThruEnabled = false, displayInGame = true, displayInMenu = true,
                  opacity = 1f, bgColor = 0x4D000000.toInt(), fgColor = 0xFFFFFFFF.toInt(),
                  strokeWidth = 0f, cornerRadius = 0f, drawerIndex = nextIdx
              )
              buttons = buttons + trigger + content
              isModified = true; selectedId = trigger.id; addItemType = AddItemType.NONE
          }
          AddItemType.NONE -> {}
      }

      editingButton?.let { eb ->
          ButtonPropertiesDialog(
              title = stringResource(R.string.legacy_control_editor_edit_button),
              initial = eb, isJoystick = eb.isJoystick,
              onDismiss = { editingButton = null },
              onConfirm = { ub ->
                  buttons = buttons.map {
                      if (it.id == eb.id) ub.copy(id = eb.id, type = eb.type, drawerIndex = eb.drawerIndex) else it
                  }
                  isModified = true; editingButton = null
              }
          )
      }

      saveError?.let { err ->
          AlertDialog(
              onDismissRequest = { saveError = null },
              title = { Text(stringResource(R.string.generic_error)) },
              text = { Text(err) },
              confirmButton = { TextButton(onClick = { saveError = null }) { Text("OK") } }
          )
      }

      Scaffold(
          topBar = {
              TopAppBar(
                  title = {
                      Text(
                          controlFile.nameWithoutExtension, maxLines = 1,
                          overflow = TextOverflow.Ellipsis,
                          style = MaterialTheme.typography.titleMedium
                      )
                  },
                  navigationIcon = {
                      IconButton(onClick = { if (isModified) showExitDialog = true else onExit() }) {
                          Icon(painterResource(R.drawable.ic_arrow_back), contentDescription = null)
                      }
                  },
                  actions = {
                      if (selectedButton != null) {
                          IconButton(onClick = { editingButton = selectedButton }) {
                              Icon(painterResource(R.drawable.ic_edit_outlined), contentDescription = null)
                          }
                          IconButton(onClick = {
                              buttons = buttons.filter { it.id != selectedId }
                              selectedId = null; isModified = true
                          }) {
                              Icon(painterResource(R.drawable.ic_delete_outlined), contentDescription = null)
                          }
                      }
                      Box {
                          IconButton(onClick = { showAddMenu = true }) {
                              Icon(painterResource(R.drawable.ic_add), contentDescription = null)
                          }
                          DropdownMenu(
                              expanded = showAddMenu,
                              onDismissRequest = { showAddMenu = false }
                          ) {
                              DropdownMenuItem(
                                  text = { Text(stringResource(R.string.legacy_control_editor_add_button)) },
                                  leadingIcon = {
                                      Icon(painterResource(R.drawable.ic_add), contentDescription = null)
                                  },
                                  onClick = { showAddMenu = false; addItemType = AddItemType.BUTTON }
                              )
                              DropdownMenuItem(
                                  text = { Text(stringResource(R.string.legacy_control_editor_add_joystick)) },
                                  leadingIcon = {
                                      Icon(painterResource(R.drawable.ic_add), contentDescription = null)
                                  },
                                  onClick = { showAddMenu = false; addItemType = AddItemType.JOYSTICK }
                              )
                              DropdownMenuItem(
                                  text = { Text(stringResource(R.string.legacy_control_editor_add_drawer)) },
                                  leadingIcon = {
                                      Icon(painterResource(R.drawable.ic_add), contentDescription = null)
                                  },
                                  onClick = { showAddMenu = false; addItemType = AddItemType.DRAWER }
                              )
                          }
                      }
                      TextButton(onClick = { saveFile() }, enabled = isModified) {
                          Text(stringResource(R.string.generic_save))
                      }
                  }
              )
          }
      ) { innerPadding ->
          BoxWithConstraints(
              modifier = Modifier
                  .fillMaxSize()
                  .padding(innerPadding)
                  .background(MaterialTheme.colorScheme.background)
          ) {
              val canvasWPx = with(density) { maxWidth.toPx() }
              val canvasHPx = with(density) { maxHeight.toPx() }

              // Grid
              val gridLineColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.06f)
              Canvas(modifier = Modifier.fillMaxSize()) {
                  for (i in 1..9) {
                      val x = size.width * i / 10f
                      drawLine(gridLineColor, Offset(x, 0f), Offset(x, size.height), 1f)
                  }
                  for (j in 1..5) {
                      val y = size.height * j / 6f
                      drawLine(gridLineColor, Offset(0f, y), Offset(size.width, y), 1f)
                  }
              }

              Box(
                  modifier = Modifier
                      .fillMaxSize()
                      .pointerInput(buttons, canvasWPx, canvasHPx) {
                          awaitEachGesture {
                              val down = awaitFirstDown(requireUnconsumed = false)
                              val tx = down.position.x
                              val ty = down.position.y
                              val hit = buttons.lastOrNull { btn ->
                                  val bwPx = with(density) { btn.widthDp.dp.toPx() }
                                  val bhPx = with(density) { btn.heightDp.dp.toPx() }
                                  val bx = btn.xFrac * canvasWPx - bwPx / 2f
                                  val by = btn.yFrac * canvasHPx - bhPx / 2f
                                  tx >= bx && tx <= bx + bwPx && ty >= by && ty <= by + bhPx
                              }
                              selectedId = hit?.id
                              if (hit != null) {
                                  var acc = Offset.Zero
                                  var tracking = true
                                  while (tracking) {
                                      val ev = awaitPointerEvent()
                                      val ch = ev.changes.find { it.id == down.id }
                                      if (ch == null) {
                                          tracking = false
                                      } else {
                                          val d = ch.position - ch.previousPosition
                                          acc = Offset(acc.x + d.x, acc.y + d.y)
                                          dragState.value = DragState(hit.id, acc)
                                          ch.consume()
                                          if (!ch.pressed) tracking = false
                                      }
                                  }
                                  val slop = viewConfiguration.touchSlop
                                  if (acc.x * acc.x + acc.y * acc.y > slop * slop) {
                                      val nx = ((hit.xFrac * canvasWPx + acc.x) / canvasWPx).coerceIn(0f, 1f)
                                      val ny = ((hit.yFrac * canvasHPx + acc.y) / canvasHPx).coerceIn(0f, 1f)
                                      buttons = buttons.map { b ->
                                          if (b.id == hit.id) b.copy(xFrac = nx, yFrac = ny) else b
                                      }
                                      isModified = true
                                  }
                                  dragState.value = null
                              }
                          }
                      }
              ) {
                  val ds = dragState.value
                  buttons.forEach { btn ->
                      val ex = if (ds?.id == btn.id) ds.delta.x else 0f
                      val ey = if (ds?.id == btn.id) ds.delta.y else 0f
                      val bwPx = with(density) { btn.widthDp.dp.toPx() }
                      val bhPx = with(density) { btn.heightDp.dp.toPx() }
                      val bxPx = (btn.xFrac * canvasWPx + ex - bwPx / 2f).roundToInt()
                      val byPx = (btn.yFrac * canvasHPx + ey - bhPx / 2f).roundToInt()
                      val isSelected = btn.id == selectedId
                      val cr = if (btn.isJoystick) 50f else (btn.cornerRadius * 0.5f).coerceIn(0f, 50f)
                      val shape = RoundedCornerShape(cr.dp)
                      val rawColor = Color(btn.bgColor)
                      val bg = rawColor.copy(alpha = (rawColor.alpha * btn.opacity).coerceIn(0f, 1f))
                      val fg = Color(btn.fgColor)
                      val borderColor = when {
                          isSelected -> MaterialTheme.colorScheme.primary
                          btn.isJoystick -> Color(0xFF4CAF50)
                          btn.isDrawerTrigger -> Color(0xFFFF9800)
                          btn.isDrawerContent -> Color(0xFF7B61FF)
                          else -> fg.copy(alpha = 0.4f)
                      }
                      val borderWidth = if (isSelected || btn.isJoystick || btn.isDrawerTrigger || btn.isDrawerContent) 2.dp
                                        else btn.strokeWidth.coerceAtLeast(0.5f).dp

                      Box(
                          modifier = Modifier
                              .absoluteOffset { IntOffset(bxPx, byPx) }
                              .size(width = btn.widthDp.dp, height = btn.heightDp.dp)
                              .background(bg, shape)
                              .border(borderWidth, borderColor, shape),
                          contentAlignment = Alignment.Center
                      ) {
                          val label = when {
                              btn.isJoystick -> "[Joy] " + btn.name
                              btn.isDrawerTrigger -> "[D" + (btn.drawerIndex + 1) + "] " + btn.name
                              btn.isDrawerContent -> "[D" + (btn.drawerIndex + 1) + "C] " + btn.name
                              else -> btn.name
                          }
                          Text(
                              text = label,
                              color = fg,
                              fontSize = 11.sp,
                              maxLines = 2,
                              overflow = TextOverflow.Ellipsis
                          )
                      }
                      if (isSelected) {
                          val handleSizeDp = 16.dp
                          val handleSizePx = with(density) { handleSizeDp.toPx() }
                          val corners = listOf(
                              Offset(bxPx - handleSizePx / 2f, byPx - handleSizePx / 2f),
                              Offset(bxPx + bwPx - handleSizePx / 2f, byPx - handleSizePx / 2f),
                              Offset(bxPx - handleSizePx / 2f, byPx + bhPx - handleSizePx / 2f),
                              Offset(bxPx + bwPx - handleSizePx / 2f, byPx + bhPx - handleSizePx / 2f)
                          )
                          corners.forEach { pos ->
                              Box(
                                  modifier = Modifier
                                      .absoluteOffset { IntOffset(pos.x.toInt(), pos.y.toInt()) }
                                      .size(handleSizeDp)
                                      .background(MaterialTheme.colorScheme.primary, CircleShape)
                                      .border(1.5.dp, MaterialTheme.colorScheme.onPrimary, CircleShape)
                              )
                          }
                      }
                  }

                  if (buttons.isEmpty()) {
                      Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                          Text(
                              stringResource(R.string.legacy_control_editor_no_buttons),
                              color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f),
                              style = MaterialTheme.typography.bodyLarge
                          )
                      }
                  }
              }

              // Legend bar at bottom
              Row(
                  modifier = Modifier
                      .align(Alignment.BottomEnd)
                      .padding(12.dp)
                      .background(
                          MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f),
                          RoundedCornerShape(8.dp)
                      )
                      .padding(horizontal = 10.dp, vertical = 6.dp),
                  horizontalArrangement = Arrangement.spacedBy(10.dp)
              ) {
                  EditorLegendItem(MaterialTheme.colorScheme.outline, "Button")
                  EditorLegendItem(Color(0xFF4CAF50), "Joystick")
                  EditorLegendItem(Color(0xFFFF9800), "Drawer")
                  EditorLegendItem(Color(0xFF7B61FF), "Content")
              }
          }
      }
  }

  @Composable
  private fun EditorLegendItem(color: Color, label: String) {
      Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(4.dp)
      ) {
          Box(Modifier.size(8.dp).background(color, CircleShape))
          Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp)
      }
  }

  private fun parseAllControls(root: JSONObject): List<CanvasButton> {
      val result = mutableListOf<CanvasButton>()

      root.optJSONArray("mControlDataList")?.let { arr ->
          for (i in 0 until arr.length()) {
              arr.optJSONObject(i)?.let { obj ->
                  runCatching { parseSingleButton(obj, ButtonType.REGULAR) }
                      .getOrNull()?.let { result.add(it) }
              }
          }
      }

      root.optJSONArray("mJoystickDataList")?.let { arr ->
          for (i in 0 until arr.length()) {
              arr.optJSONObject(i)?.let { obj ->
                  runCatching {
                      parseSingleButton(obj, ButtonType.JOYSTICK).copy(
                          cornerRadius = 50f,
                          forwardLock = obj.optBoolean("forwardLock", false),
                          absolute = obj.optBoolean("absolute", false)
                      )
                  }.getOrNull()?.let { result.add(it) }
              }
          }
      }

      root.optJSONArray("mDrawerDataList")?.let { arr ->
          for (di in 0 until arr.length()) {
              arr.optJSONObject(di)?.let { drawer ->
                  drawer.optJSONObject("properties")?.let { props ->
                      runCatching { parseSingleButton(props, ButtonType.DRAWER_TRIGGER, di) }
                          .getOrNull()?.let { result.add(it) }
                  }
                  drawer.optJSONArray("buttonProperties")?.let { btnArr ->
                      for (j in 0 until btnArr.length()) {
                          btnArr.optJSONObject(j)?.let { obj ->
                              runCatching { parseSingleButton(obj, ButtonType.DRAWER_CONTENT, di) }
                                  .getOrNull()?.let { result.add(it) }
                          }
                      }
                  }
              }
          }
      }

      return result
  }

  private fun parseSingleButton(
      obj: JSONObject,
      type: ButtonType,
      drawerIndex: Int = -1
  ): CanvasButton {
      val w = obj.optDouble("width", 80.0).toFloat().coerceAtLeast(10f)
      val h = obj.optDouble("height", 50.0).toFloat().coerceAtLeast(10f)
      // dpFrac: ZL1 ${dp} is display density (~2.0 px/dp). Normalised = density / screen_dim.
      val dpFracX = 2f / 1280f
      val dpFracY = 2f / 720f
      val xLeft = evalExpr(
          obj.optString("dynamicX", "0.5 * \${screen_width}"),
          wFrac = w / 1280f, hFrac = h / 720f, dpFrac = dpFracX
      )
      val yTop = evalExpr(
          obj.optString("dynamicY", "0.5 * \${screen_height}"),
          wFrac = w / 1280f, hFrac = h / 720f, dpFrac = dpFracY
      )
      val rawName = obj.optString("name", "Button")
      return CanvasButton(
          id = UUID.randomUUID().toString(),
          type = type,
          name = if (rawName == "null" || rawName.isBlank()) "Button" else rawName,
          xFrac = (xLeft + w / 1280f / 2f).coerceIn(0f, 1f),
          yFrac = (yTop + h / 720f / 2f).coerceIn(0f, 1f),
          widthDp = w, heightDp = h,
          keycodes = parseKeycodes(obj),
          isToggle = obj.optBoolean("isToggle", false),
          isSwipeable = obj.optBoolean("isSwipeable", false),
          passThruEnabled = obj.optBoolean("passThruEnabled", false),
          displayInGame = obj.optBoolean("displayInGame", true),
          displayInMenu = obj.optBoolean("displayInMenu", true),
          opacity = obj.optDouble("opacity", 1.0).toFloat().coerceIn(0f, 1f),
          bgColor = obj.optInt("bgColor", 0x4D000000.toInt()),
          fgColor = obj.optInt("strokeColor", 0xFFFFFFFF.toInt()),
          strokeWidth = obj.optDouble("strokeWidth", 0.0).toFloat(),
          cornerRadius = obj.optDouble("cornerRadius", 0.0).toFloat(),
          drawerIndex = drawerIndex,
          forwardLock = obj.optBoolean("forwardLock", false),
          absolute = obj.optBoolean("absolute", false)
      )
  }

  private fun parseKeycodes(obj: JSONObject): List<Int> {
      val arr = obj.optJSONArray("keycodes")
      if (arr != null) {
          return (0 until arr.length()).mapNotNull { arr.optInt(it, 0).takeIf { kc -> kc != 0 } }
      }
      // Old ZL1 single-keycode format: "keycode" integer + optional modifier booleans
      val result = mutableListOf<Int>()
      if (obj.optBoolean("holdShift", false)) result.add(340) // GLFW_KEY_LEFT_SHIFT
      if (obj.optBoolean("holdCtrl",  false)) result.add(341) // GLFW_KEY_LEFT_CONTROL
      if (obj.optBoolean("holdAlt",   false)) result.add(342) // GLFW_KEY_LEFT_ALT
      val kc = obj.optInt("keycode", 0)
      if (kc != 0) result.add(kc)
      return result
  }

  private fun buildButtonJson(btn: CanvasButton): JSONObject {
      val refW = 1280f; val refH = 720f
      val leftEdge = (btn.xFrac - btn.widthDp / refW / 2f).coerceIn(0f, 1f)
      val topEdge = (btn.yFrac - btn.heightDp / refH / 2f).coerceIn(0f, 1f)
      return JSONObject().apply {
          put("name", btn.name)
          put("dynamicX", leftEdge.toString() + " * \${screen_width}")
          put("dynamicY", topEdge.toString() + " * \${screen_height}")
          put("width", btn.widthDp.toDouble())
          put("height", btn.heightDp.toDouble())
          put("keycodes", JSONArray().also { a -> btn.keycodes.forEach { a.put(it) } })
          put("isToggle", btn.isToggle)
          put("passThruEnabled", btn.passThruEnabled)
          put("isSwipeable", btn.isSwipeable)
          put("displayInGame", btn.displayInGame)
          put("displayInMenu", btn.displayInMenu)
          put("opacity", btn.opacity.toDouble())
          put("bgColor", btn.bgColor)
          put("strokeColor", btn.fgColor)
          put("strokeWidth", btn.strokeWidth.toDouble())
          put("cornerRadius", btn.cornerRadius.toDouble())
      }
  }

  private fun buildJoystickJson(btn: CanvasButton): JSONObject =
      buildButtonJson(btn).apply {
          put("forwardLock", btn.forwardLock)
          put("absolute", btn.absolute)
      }

  private fun evalExpr(
      expr: String,
      wFrac: Float = 0f,
      hFrac: Float = 0f,
      dpFrac: Float = 0f
  ): Float {
      if (expr.isBlank()) return 0.5f
      return try {
          val marginFrac = 8f / 1280f
          var s = expr.trim()
              .replace("\${screen_width}", "1.0")
              .replace("\${screen_height}", "1.0")
              .replace("\${width}", "%.8f".format(wFrac))
              .replace("\${height}", "%.8f".format(hFrac))
              .replace("\${dp}", "%.8f".format(dpFrac))
              .replace("\${ratio}", "1.0")
              .replace("\${margin}", "%.8f".format(marginFrac))
              .replace("\${right}", "(1.0 - %.8f)".format(wFrac))
              .replace("\${bottom}", "(1.0 - %.8f)".format(hFrac))
              .replace("\${preferred_scale}", "100.0")
          s = evalReplacePx(s, dpFrac)
          MiniCalc(s).eval().coerceIn(0f, 1f)
      } catch (_: Exception) { 0.5f }
  }

  private fun evalReplacePx(expr: String, dpFrac: Float): String {
      val sb = StringBuilder(); var i = 0
      while (i < expr.length) {
          if (expr.startsWith("px(", i)) {
              val st = i + 3; val en = expr.indexOf(')', st)
              if (en > st) {
                  val dpVal = expr.substring(st, en).toFloatOrNull() ?: 0f
                  sb.append("%.8f".format(dpVal * dpFrac))
                  i = en + 1; continue
              }
          }
          sb.append(expr[i]); i++
      }
      return sb.toString()
  }

  private class MiniCalc(private val s: String) {
      private var i = 0
      fun eval(): Float = expr()
      private fun expr(): Float {
          var r = term(); skip()
          while (i < s.length) {
              when (s[i]) {
                  '+' -> { i++; r += term() }
                  '-' -> { i++; r -= term() }
                  else -> break
              }; skip()
          }
          return r
      }
      private fun term(): Float {
          var r = factor(); skip()
          while (i < s.length) {
              when (s[i]) {
                  '*' -> { i++; r *= factor() }
                  '/' -> { i++; val d = factor(); if (d != 0f) r /= d }
                  else -> break
              }; skip()
          }
          return r
      }
      private fun factor(): Float {
          skip(); if (i >= s.length) return 0f
          return when {
              s[i] == '(' -> { i++; val r = expr(); skip(); if (i < s.length && s[i] == ')') i++; r }
              s[i] == '-' -> { i++; -factor() }
              else -> num()
          }
      }
      private fun num(): Float {
          skip(); val start = i
          while (i < s.length && (s[i].isDigit() || s[i] == '.')) i++
          return if (i > start) s.substring(start, i).toFloatOrNull() ?: 0f else 0f
      }
      private fun skip() { while (i < s.length && s[i] == ' ') i++ }
  }

  @Composable
  private fun ButtonPropertiesDialog(
      title: String,
      initial: CanvasButton?,
      isJoystick: Boolean,
      onDismiss: () -> Unit,
      onConfirm: (CanvasButton) -> Unit
  ) {
      var name by remember { mutableStateOf(initial?.name ?: "") }
      var keycodeText by remember { mutableStateOf(initial?.keycodes?.joinToString(", ") ?: "") }
      var widthDp by remember { mutableStateOf((initial?.widthDp ?: 80f).toString()) }
      var heightDp by remember { mutableStateOf((initial?.heightDp ?: 50f).toString()) }
      var cornerRadius by remember { mutableStateOf((initial?.cornerRadius ?: 0f).toString()) }
      var isToggle by remember { mutableStateOf(initial?.isToggle ?: false) }
      var displayGame by remember { mutableStateOf(initial?.displayInGame ?: true) }
      var displayMenu by remember { mutableStateOf(initial?.displayInMenu ?: true) }
      var forwardLock by remember { mutableStateOf(initial?.forwardLock ?: false) }
      var absolute by remember { mutableStateOf(initial?.absolute ?: false) }

      Dialog(onDismissRequest = onDismiss) {
          Surface(
              modifier = Modifier.fillMaxWidth().padding(16.dp),
              shape = MaterialTheme.shapes.extraLarge,
              shadowElevation = 8.dp
          ) {
              Column(
                  modifier = Modifier.padding(20.dp),
                  verticalArrangement = Arrangement.spacedBy(12.dp)
              ) {
                  Text(title, style = MaterialTheme.typography.titleMedium)
                  Column(
                      modifier = Modifier
                          .weight(1f, fill = false)
                          .verticalScroll(rememberScrollState()),
                      verticalArrangement = Arrangement.spacedBy(10.dp)
                  ) {
                      OwnOutlinedTextField(
                          modifier = Modifier.fillMaxWidth(),
                          value = name,
                          onValueChange = { name = it.take(64) },
                          label = { Text(stringResource(R.string.control_manage_create_new_name)) },
                          singleLine = true,
                          shape = MaterialTheme.shapes.large
                      )
                      OwnOutlinedTextField(
                          modifier = Modifier.fillMaxWidth(),
                          value = keycodeText,
                          onValueChange = { keycodeText = it.take(128) },
                          label = { Text(stringResource(R.string.legacy_control_editor_keycodes)) },
                          singleLine = true,
                          shape = MaterialTheme.shapes.large
                      )
                      Text(
                          text = if (isJoystick)
                              stringResource(R.string.legacy_control_editor_hint_joystick_keycodes)
                          else
                              stringResource(R.string.legacy_control_editor_hint_keycodes),
                          style = MaterialTheme.typography.bodySmall,
                          color = MaterialTheme.colorScheme.onSurfaceVariant
                      )
                      Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                          OwnOutlinedTextField(
                              modifier = Modifier.weight(1f),
                              value = widthDp,
                              onValueChange = { widthDp = it.filter { c -> c.isDigit() || c == '.' }.take(6) },
                              label = { Text(stringResource(R.string.legacy_control_editor_width)) },
                              keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                              singleLine = true,
                              shape = MaterialTheme.shapes.large
                          )
                          OwnOutlinedTextField(
                              modifier = Modifier.weight(1f),
                              value = heightDp,
                              onValueChange = { heightDp = it.filter { c -> c.isDigit() || c == '.' }.take(6) },
                              label = { Text(stringResource(R.string.legacy_control_editor_height)) },
                              keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                              singleLine = true,
                              shape = MaterialTheme.shapes.large
                          )
                      }
                      if (!isJoystick) {
                          OwnOutlinedTextField(
                              modifier = Modifier.fillMaxWidth(),
                              value = cornerRadius,
                              onValueChange = { cornerRadius = it.filter { c -> c.isDigit() || c == '.' }.take(5) },
                              label = { Text(stringResource(R.string.legacy_control_editor_corner_radius)) },
                              keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                              singleLine = true,
                              shape = MaterialTheme.shapes.large
                          )
                          Row(verticalAlignment = Alignment.CenterVertically) {
                              Checkbox(checked = isToggle, onCheckedChange = { isToggle = it })
                              Spacer(Modifier.width(4.dp))
                              Text(stringResource(R.string.legacy_control_editor_toggle))
                          }
                      }
                      if (isJoystick) {
                          HorizontalDivider()
                          Text(
                              stringResource(R.string.legacy_control_editor_joystick_options),
                              style = MaterialTheme.typography.labelMedium,
                              color = MaterialTheme.colorScheme.primary
                          )
                          Row(verticalAlignment = Alignment.CenterVertically) {
                              Checkbox(checked = forwardLock, onCheckedChange = { forwardLock = it })
                              Spacer(Modifier.width(4.dp))
                              Text(stringResource(R.string.legacy_control_editor_forward_lock))
                          }
                          Row(verticalAlignment = Alignment.CenterVertically) {
                              Checkbox(checked = absolute, onCheckedChange = { absolute = it })
                              Spacer(Modifier.width(4.dp))
                              Text(stringResource(R.string.legacy_control_editor_absolute_mode))
                          }
                      }
                      HorizontalDivider()
                      Row(verticalAlignment = Alignment.CenterVertically) {
                          Checkbox(checked = displayGame, onCheckedChange = { displayGame = it })
                          Spacer(Modifier.width(4.dp))
                          Text(stringResource(R.string.legacy_control_editor_display_game))
                      }
                      Row(verticalAlignment = Alignment.CenterVertically) {
                          Checkbox(checked = displayMenu, onCheckedChange = { displayMenu = it })
                          Spacer(Modifier.width(4.dp))
                          Text(stringResource(R.string.legacy_control_editor_display_menu))
                      }
                  }
                  Row(
                      Modifier.fillMaxWidth(),
                      horizontalArrangement = Arrangement.spacedBy(8.dp)
                  ) {
                      FilledTonalButton(modifier = Modifier.weight(1f), onClick = onDismiss) {
                          Text(stringResource(R.string.generic_cancel))
                      }
                      Button(
                          modifier = Modifier.weight(1f),
                          onClick = {
                              val kcs = keycodeText.split(",").mapNotNull { it.trim().toIntOrNull() }
                              val w = widthDp.toFloatOrNull()?.coerceAtLeast(10f) ?: 80f
                              val h = heightDp.toFloatOrNull()?.coerceAtLeast(10f) ?: 50f
                              val cr = if (isJoystick) 50f
                                       else (cornerRadius.toFloatOrNull()?.coerceIn(0f, 100f) ?: 0f)
                              val result = initial?.copy(
                                  name = name.ifBlank { "Button" },
                                  keycodes = kcs, widthDp = w, heightDp = h, cornerRadius = cr,
                                  isToggle = isToggle, displayInGame = displayGame,
                                  displayInMenu = displayMenu,
                                  forwardLock = forwardLock, absolute = absolute
                              ) ?: CanvasButton(
                                  type = if (isJoystick) ButtonType.JOYSTICK else ButtonType.REGULAR,
                                  name = name.ifBlank { "Button" },
                                  xFrac = 0.5f, yFrac = 0.5f,
                                  widthDp = w, heightDp = h,
                                  keycodes = kcs, isToggle = isToggle,
                                  isSwipeable = false, passThruEnabled = false,
                                  displayInGame = displayGame, displayInMenu = displayMenu,
                                  opacity = 1f, bgColor = 0x4D000000.toInt(),
                                  fgColor = 0xFFFFFFFF.toInt(),
                                  strokeWidth = 0f, cornerRadius = cr,
                                  forwardLock = forwardLock, absolute = absolute
                              )
                              onConfirm(result)
                          },
                          enabled = name.isNotBlank()
                      ) { Text(stringResource(R.string.generic_save)) }
                  }
              }
          }
      }
  }

  fun startLegacyEditorActivity(context: Context, file: File) {
      context.startActivity(
          Intent(context, LegacyControlEditorActivity::class.java).apply {
              putExtra(BUNDLE_LEGACY_CONTROL, file.absolutePath)
          }
      )
  }
  