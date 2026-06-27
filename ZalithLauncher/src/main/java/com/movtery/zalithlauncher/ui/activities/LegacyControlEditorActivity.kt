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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
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

private data class CanvasButton(
    val id: String = UUID.randomUUID().toString(),
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
    val isDrawerTrigger: Boolean = false,
    val isDrawerContent: Boolean = false,
    val drawerIndex: Int = -1
)

private data class DragState(val id: String, val delta: Offset)
  private data class ResizeState(val id: String, val corner: ResizeCorner, val delta: Offset)
  private enum class ResizeCorner { BottomRight, BottomLeft, TopRight, TopLeft }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LegacyControlEditorScreen(controlFile: File, onExit: () -> Unit) {
    var jsonRoot by remember { mutableStateOf<JSONObject?>(null) }
    var buttons by remember { mutableStateOf<List<CanvasButton>>(emptyList()) }
    var selectedId by remember { mutableStateOf<String?>(null) }
    var isModified by remember { mutableStateOf(false) }
    var showExitDialog by remember { mutableStateOf(false) }
    var showAddDialog by remember { mutableStateOf(false) }
    var editingButton by remember { mutableStateOf<CanvasButton?>(null) }
    var saveError by remember { mutableStateOf<String?>(null) }
    val dragState = remember { mutableStateOf<DragState?>(null) }
      val resizeState = remember { mutableStateOf<ResizeState?>(null) }
    val density = LocalDensity.current
    val selectedButton = buttons.find { it.id == selectedId }

    LaunchedEffect(Unit) {
        try {
            val root = JSONObject(controlFile.readText())
            jsonRoot = root
            buttons = parseButtons(root)
        } catch (_: Exception) {}
    }

    fun saveFile(): Boolean = try {
        val root = jsonRoot ?: JSONObject().also {
            it.put("version", 8); it.put("scaledAt", 100.0)
            it.put("mDrawerDataList", JSONArray()); it.put("mJoystickDataList", JSONArray())
        }

        // Regular buttons
        val regularButtons = buttons.filter { !it.isDrawerTrigger && !it.isDrawerContent }
        root.put("mControlDataList", JSONArray().also { a -> regularButtons.forEach { b -> a.put(buildButtonJson(b)) } })

        // Reconstruct drawer data list
        val drawerIndices = (buttons.filter { it.isDrawerTrigger || it.isDrawerContent }
            .map { it.drawerIndex }.filter { it >= 0 }.toSet().sorted())
        val drawerList = JSONArray()
        for (di in drawerIndices) {
            val drawerObj = JSONObject()
            buttons.find { it.isDrawerTrigger && it.drawerIndex == di }
                ?.let { drawerObj.put("properties", buildButtonJson(it)) }
            val btnPropsArr = JSONArray()
            buttons.filter { it.isDrawerContent && it.drawerIndex == di }
                .forEach { btnPropsArr.put(buildButtonJson(it)) }
            drawerObj.put("buttonProperties", btnPropsArr)
            // Preserve other drawer metadata from original if available
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
        isModified = false; true
    } catch (e: Exception) { saveError = e.message ?: "Error"; false }

    if (showExitDialog) {
        SimpleAlertDialog(
            title = stringResource(R.string.legacy_control_editor_unsaved_title),
            text = stringResource(R.string.legacy_control_editor_unsaved_message),
            onDismiss = { showExitDialog = false },
            onConfirm = { saveFile(); showExitDialog = false; onExit() }
        )
    }
    if (showAddDialog) {
        ButtonPropertiesDialog(
            title = stringResource(R.string.legacy_control_editor_add_button),
            initial = null, onDismiss = { showAddDialog = false },
            onConfirm = { nb -> buttons = buttons + nb; isModified = true; selectedId = nb.id; showAddDialog = false }
        )
    }
    editingButton?.let { eb ->
        ButtonPropertiesDialog(
            title = stringResource(R.string.legacy_control_editor_edit_button),
            initial = eb, onDismiss = { editingButton = null },
            onConfirm = { ub ->
                buttons = buttons.map { if (it.id == eb.id) ub.copy(id = eb.id) else it }
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
                        overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.titleMedium
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { if (isModified) showExitDialog = true else onExit() }) {
                        Icon(painterResource(R.drawable.ic_arrow_back), null)
                    }
                },
                actions = {
                    if (selectedButton != null) {
                        IconButton(onClick = { editingButton = selectedButton }) {
                            Icon(painterResource(R.drawable.ic_edit_outlined), null)
                        }
                        IconButton(onClick = {
                            buttons = buttons.filter { it.id != selectedId }
                            selectedId = null; isModified = true
                        }) { Icon(painterResource(R.drawable.ic_delete_outlined), null) }
                    }
                    IconButton(onClick = { showAddDialog = true }) {
                        Icon(painterResource(R.drawable.ic_add), null)
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
                .background(Color(0xFF0D1117))
        ) {
            val canvasWPx = with(density) { maxWidth.toPx() }
            val canvasHPx = with(density) { maxHeight.toPx() }

            Canvas(modifier = Modifier.fillMaxSize()) {
                val g = Color.White.copy(alpha = 0.05f)
                for (i in 1..9) {
                    val x = size.width * i / 10f
                    drawLine(g, Offset(x, 0f), Offset(x, size.height), 1f)
                }
                for (j in 1..5) {
                    val y = size.height * j / 6f
                    drawLine(g, Offset(0f, y), Offset(size.width, y), 1f)
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
                    val cr = (btn.cornerRadius * 0.5f).coerceIn(0f, 50f)
                    val shape = RoundedCornerShape(cr.dp)
                    val rawColor = Color(btn.bgColor)
                    val bg = rawColor.copy(alpha = (rawColor.alpha * btn.opacity).coerceIn(0f, 1f))
                    val fg = Color(btn.fgColor)

                    Box(
                        modifier = Modifier
                            .absoluteOffset { IntOffset(bxPx, byPx) }
                            .size(width = btn.widthDp.dp, height = btn.heightDp.dp)
                            .background(bg, shape)
                            .then(
                                when {
                                    isSelected -> Modifier.border(2.dp, MaterialTheme.colorScheme.primary, shape)
                                    btn.isDrawerTrigger -> Modifier.border(2.dp, Color(0xFFFF9800), shape)
                                    btn.isDrawerContent -> Modifier.border(2.dp, Color(0xFF7B61FF), shape)
                                    else -> Modifier.border(btn.strokeWidth.coerceAtLeast(0.5f).dp, fg.copy(alpha = 0.3f), shape)
                                }
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        val displayName = when {
                            btn.isDrawerTrigger -> "[D${btn.drawerIndex + 1}] ${btn.name}"
                            btn.isDrawerContent -> "[D${btn.drawerIndex + 1}C] ${btn.name}"
                            else -> btn.name
                        }
                        Text(displayName, color = fg, fontSize = 12.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    }
                  // Draw resize handles on corners of the selected button
                  if (isSelected) {
                      val handleSizeDp = 16.dp
                      val handleSizePx = with(density) { handleSizeDp.toPx() }
                      listOf(
                          ResizeCorner.TopLeft to Offset(bxPx - handleSizePx / 2f, byPx - handleSizePx / 2f),
                          ResizeCorner.TopRight to Offset(bxPx + bwPx - handleSizePx / 2f, byPx - handleSizePx / 2f),
                          ResizeCorner.BottomLeft to Offset(bxPx - handleSizePx / 2f, byPx + bhPx - handleSizePx / 2f),
                          ResizeCorner.BottomRight to Offset(bxPx + bwPx - handleSizePx / 2f, byPx + bhPx - handleSizePx / 2f)
                      ).forEach { (corner, pos) ->
                          val rs = resizeState.value
                          val dx = if (rs?.id == btn.id && rs.corner == corner) rs.delta.x else 0f
                          val dy = if (rs?.id == btn.id && rs.corner == corner) rs.delta.y else 0f
                          Box(
                              modifier = Modifier
                                  .absoluteOffset {
                                      IntOffset(
                                          (pos.x + dx).toInt(),
                                          (pos.y + dy).toInt()
                                      )
                                  }
                                  .size(handleSizeDp)
                                  .background(
                                      MaterialTheme.colorScheme.primary,
                                      androidx.compose.foundation.shape.CircleShape
                                  )
                                  .border(
                                      1.5.dp,
                                      MaterialTheme.colorScheme.onPrimary,
                                      androidx.compose.foundation.shape.CircleShape
                                  )
                          )
                      }
                  }
                }

                if (buttons.isEmpty()) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            stringResource(R.string.legacy_control_editor_no_buttons),
                            color = Color.White.copy(alpha = 0.4f),
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }

                  // Resize touch handler: drag from corners of selected button
                  selectedId?.let { sid ->
                      val sel = buttons.find { it.id == sid }
                      if (sel != null) {
                          Box(modifier = Modifier
                              .fillMaxSize()
                              .pointerInput(sid, sel.widthDp, sel.heightDp) {
                                  val selBwPx = with(density) { sel.widthDp.dp.toPx() }
                                  val selBhPx = with(density) { sel.heightDp.dp.toPx() }
                                  val selBxPx = (sel.xFrac * canvasWPx) - selBwPx / 2f
                                  val selByPx = (sel.yFrac * canvasHPx) - selBhPx / 2f
                                  val hitZone = 36f
                                  awaitEachGesture {
                                      val down = awaitFirstDown(requireUnconsumed = false)
                                      val tx = down.position.x; val ty = down.position.y
                                      fun near(cx: Float, cy: Float) = Math.abs(tx - cx) < hitZone && Math.abs(ty - cy) < hitZone
                                      val corner = when {
                                          near(selBxPx, selByPx) -> ResizeCorner.TopLeft
                                          near(selBxPx + selBwPx, selByPx) -> ResizeCorner.TopRight
                                          near(selBxPx, selByPx + selBhPx) -> ResizeCorner.BottomLeft
                                          near(selBxPx + selBwPx, selByPx + selBhPx) -> ResizeCorner.BottomRight
                                          else -> null
                                      } ?: return@awaitEachGesture
                                      down.consume()
                                      var acc = Offset.Zero
                                      var tracking = true
                                      while (tracking) {
                                          val ev = awaitPointerEvent()
                                          val ch = ev.changes.find { it.id == down.id }
                                          if (ch == null) { tracking = false; continue }
                                          val d = ch.position - ch.previousPosition
                                          acc = Offset(acc.x + d.x, acc.y + d.y)
                                          resizeState.value = ResizeState(sid, corner, acc)
                                          ch.consume()
                                          if (!ch.pressed) tracking = false
                                      }
                                      resizeState.value = null
                                      val dpX = with(density) { acc.x.toDp().value }
                                      val dpY = with(density) { acc.y.toDp().value }
                                      buttons = buttons.map { b ->
                                          if (b.id != sid) b else {
                                              when (corner) {
                                                  ResizeCorner.BottomRight -> b.copy(
                                                      widthDp = (b.widthDp + dpX).coerceAtLeast(20f),
                                                      heightDp = (b.heightDp + dpY).coerceAtLeast(20f)
                                                  )
                                                  ResizeCorner.BottomLeft -> b.copy(
                                                      widthDp = (b.widthDp - dpX).coerceAtLeast(20f),
                                                      heightDp = (b.heightDp + dpY).coerceAtLeast(20f),
                                                      xFrac = ((b.xFrac * canvasWPx + acc.x / 2f) / canvasWPx).coerceIn(0f, 1f)
                                                  )
                                                  ResizeCorner.TopRight -> b.copy(
                                                      widthDp = (b.widthDp + dpX).coerceAtLeast(20f),
                                                      heightDp = (b.heightDp - dpY).coerceAtLeast(20f),
                                                      yFrac = ((b.yFrac * canvasHPx + acc.y / 2f) / canvasHPx).coerceIn(0f, 1f)
                                                  )
                                                  ResizeCorner.TopLeft -> b.copy(
                                                      widthDp = (b.widthDp - dpX).coerceAtLeast(20f),
                                                      heightDp = (b.heightDp - dpY).coerceAtLeast(20f),
                                                      xFrac = ((b.xFrac * canvasWPx + acc.x / 2f) / canvasWPx).coerceIn(0f, 1f),
                                                      yFrac = ((b.yFrac * canvasHPx + acc.y / 2f) / canvasHPx).coerceIn(0f, 1f)
                                                  )
                                              }
                                          }
                                      }
                                      isModified = true
                                  }
                              }
                          )
                      }
                  }
            }
        }
    }
}

private fun parseButtons(root: JSONObject): List<CanvasButton> {
    val result = mutableListOf<CanvasButton>()

    // Regular buttons from mControlDataList
    root.optJSONArray("mControlDataList")?.let { arr ->
        for (i in 0 until arr.length()) {
            arr.optJSONObject(i)?.let { obj ->
                runCatching { parseSingleButton(obj, isDrawerTrigger = false, isDrawerContent = false, drawerIndex = -1) }
                    .getOrNull()?.let { result.add(it) }
            }
        }
    }

    // Drawer buttons from mDrawerDataList
    root.optJSONArray("mDrawerDataList")?.let { arr ->
        for (di in 0 until arr.length()) {
            arr.optJSONObject(di)?.let { drawer ->
                // Drawer trigger button
                drawer.optJSONObject("properties")?.let { props ->
                    runCatching { parseSingleButton(props, isDrawerTrigger = true, isDrawerContent = false, drawerIndex = di) }
                        .getOrNull()?.let { result.add(it) }
                }
                // Drawer content buttons
                drawer.optJSONArray("buttonProperties")?.let { btnArr ->
                    for (j in 0 until btnArr.length()) {
                        btnArr.optJSONObject(j)?.let { obj ->
                            runCatching { parseSingleButton(obj, isDrawerTrigger = false, isDrawerContent = true, drawerIndex = di) }
                                .getOrNull()?.let { result.add(it) }
                        }
                    }
                }
            }
        }
    }

    return result
}

private fun parseSingleButton(obj: JSONObject, isDrawerTrigger: Boolean, isDrawerContent: Boolean, drawerIndex: Int): CanvasButton {
    val w = obj.optDouble("width", 80.0).toFloat().coerceAtLeast(10f)
    val h = obj.optDouble("height", 50.0).toFloat().coerceAtLeast(10f)
    return CanvasButton(
        id = UUID.randomUUID().toString(),
        name = obj.optString("name", "Button").let { n -> if (n == "null" || n.isBlank()) "Button" else n },
        xFrac = run {
            val xLeft = evalExpr(
                obj.optString("dynamicX", "0.5 * \${screen_width}"),
                wFrac  = w / 1280f,
                hFrac  = h / 1280f,
                dpFrac = 1f / 1280f
            )
            (xLeft + w / 1280f / 2f).coerceIn(0f, 1f)
        },
        yFrac = run {
            val yTop = evalExpr(
                obj.optString("dynamicY", "0.5 * \${screen_height}"),
                wFrac  = w / 720f,
                hFrac  = h / 720f,
                dpFrac = 1f / 720f
            )
            (yTop + h / 720f / 2f).coerceIn(0f, 1f)
        },
        widthDp = w,
        heightDp = h,
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
        isDrawerTrigger = isDrawerTrigger,
        isDrawerContent = isDrawerContent,
        drawerIndex = drawerIndex
    )
}

private fun parseKeycodes(obj: JSONObject): List<Int> {
    val arr = obj.optJSONArray("keycodes") ?: return emptyList()
    return (0 until arr.length()).mapNotNull { arr.optInt(it, 0).takeIf { kc -> kc != 0 } }
}

private fun buildButtonJson(btn: CanvasButton): JSONObject = JSONObject().apply {
    put("name", btn.name)
    // ZL1 positions: left/top edge of button (center → left edge by subtracting half-size/ref)
    val refW = 1280f
    val refH = 720f
    val leftEdge = (btn.xFrac  - btn.widthDp  / refW / 2f).coerceIn(0f, 0.99f)
    val topEdge  = (btn.yFrac  - btn.heightDp / refH / 2f).coerceIn(0f, 0.99f)
    put("dynamicX", "${leftEdge} * \${screen_width}")
    put("dynamicY", "${topEdge} * \${screen_height}")
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

    /**
       * Evaluate a ZL1 position expression → screen fraction in [0, 1].
       * @param wFrac  substitute for ${width}  (button width  / reference axis)
       * @param hFrac  substitute for ${height} (button height / reference axis)
       * @param dpFrac substitute for ${dp}     (1 dp / reference axis)
       */
      private fun evalExpr(
          expr: String,
          wFrac: Float = 0f,
          hFrac: Float = 0f,
          dpFrac: Float = 0f
      ): Float {
          if (expr.isBlank()) return 0.5f
          return try {
              val s = expr.trim()
                  .replace("\${screen_width}",  "1.0")
                  .replace("\${screen_height}", "1.0")
                  .replace("\${width}",   "%.8f".format(wFrac))
                  .replace("\${height}",  "%.8f".format(hFrac))
                  .replace("\${dp}",      "%.8f".format(dpFrac))
                  .replace("\${ratio}",   "1.0")
              MiniCalc(s).eval().coerceIn(0f, 1f)
          } catch (_: Exception) { 0.5f }
}

private class MiniCalc(private val s: String) {
    private var i = 0
    fun eval(): Float = expr()
    private fun expr(): Float {
        var r = term(); skip()
        while (i < s.length) {
            when (s[i]) { '+' -> { i++; r += term() } '-' -> { i++; r -= term() } else -> break }
            skip()
        }
        return r
    }
    private fun term(): Float {
        var r = factor(); skip()
        while (i < s.length) {
            when (s[i]) { '*' -> { i++; r *= factor() } '/' -> { i++; val d = factor(); if (d != 0f) r /= d } else -> break }
            skip()
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
        skip(); val st = i
        while (i < s.length && (s[i].isDigit() || s[i] == '.')) i++
        return if (i > st) s.substring(st, i).toFloatOrNull() ?: 0f else 0f
    }
    private fun skip() { while (i < s.length && s[i] == ' ') i++ }
}

@Composable
private fun ButtonPropertiesDialog(
    title: String, initial: CanvasButton?,
    onDismiss: () -> Unit, onConfirm: (CanvasButton) -> Unit
) {
    var name by remember { mutableStateOf(initial?.name ?: "") }
    var keycodeText by remember { mutableStateOf(initial?.keycodes?.joinToString(", ") ?: "") }
    var widthDp by remember { mutableStateOf((initial?.widthDp ?: 80f).toString()) }
    var heightDp by remember { mutableStateOf((initial?.heightDp ?: 50f).toString()) }
    var cornerRadius by remember { mutableStateOf((initial?.cornerRadius ?: 0f).toString()) }
    var isToggle by remember { mutableStateOf(initial?.isToggle ?: false) }
    var displayGame by remember { mutableStateOf(initial?.displayInGame ?: true) }
    var displayMenu by remember { mutableStateOf(initial?.displayInMenu ?: true) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            shape = MaterialTheme.shapes.extraLarge, shadowElevation = 8.dp
        ) {
            Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Column(
                    modifier = Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OwnOutlinedTextField(
                        modifier = Modifier.fillMaxWidth(), value = name, onValueChange = { name = it.take(64) },
                        label = { Text(stringResource(R.string.control_manage_create_new_name)) },
                        singleLine = true, shape = MaterialTheme.shapes.large
                    )
                    OwnOutlinedTextField(
                        modifier = Modifier.fillMaxWidth(), value = keycodeText, onValueChange = { keycodeText = it.take(128) },
                        label = { Text(stringResource(R.string.legacy_control_editor_keycodes)) },
                        singleLine = true, shape = MaterialTheme.shapes.large
                    )
                    Text(
                        stringResource(R.string.legacy_control_editor_hint_keycodes),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OwnOutlinedTextField(
                            modifier = Modifier.weight(1f), value = widthDp,
                            onValueChange = { widthDp = it.filter { c -> c.isDigit() || c == '.' }.take(6) },
                            label = { Text(stringResource(R.string.legacy_control_editor_width)) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true, shape = MaterialTheme.shapes.large
                        )
                        OwnOutlinedTextField(
                            modifier = Modifier.weight(1f), value = heightDp,
                            onValueChange = { heightDp = it.filter { c -> c.isDigit() || c == '.' }.take(6) },
                            label = { Text(stringResource(R.string.legacy_control_editor_height)) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true, shape = MaterialTheme.shapes.large
                        )
                    }
                    OwnOutlinedTextField(
                        modifier = Modifier.fillMaxWidth(), value = cornerRadius,
                        onValueChange = { cornerRadius = it.filter { c -> c.isDigit() || c == '.' }.take(5) },
                        label = { Text(stringResource(R.string.legacy_control_editor_corner_radius)) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true, shape = MaterialTheme.shapes.large
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = isToggle, onCheckedChange = { isToggle = it })
                        Spacer(Modifier.width(4.dp))
                        Text(stringResource(R.string.legacy_control_editor_toggle))
                    }
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
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilledTonalButton(modifier = Modifier.weight(1f), onClick = onDismiss) {
                        Text(stringResource(R.string.generic_cancel))
                    }
                    Button(
                        modifier = Modifier.weight(1f),
                        onClick = {
                            val kcs = keycodeText.split(",").mapNotNull { it.trim().toIntOrNull() }
                            val w = widthDp.toFloatOrNull()?.coerceAtLeast(10f) ?: 80f
                            val h = heightDp.toFloatOrNull()?.coerceAtLeast(10f) ?: 50f
                            val cr = cornerRadius.toFloatOrNull()?.coerceIn(0f, 100f) ?: 0f
                            val result = initial?.copy(
                                name = name.ifBlank { "Button" }, keycodes = kcs,
                                widthDp = w, heightDp = h, cornerRadius = cr,
                                isToggle = isToggle, displayInGame = displayGame, displayInMenu = displayMenu
                            ) ?: CanvasButton(
                                name = name.ifBlank { "Button" }, xFrac = 0.5f, yFrac = 0.5f,
                                widthDp = w, heightDp = h, keycodes = kcs, isToggle = isToggle,
                                isSwipeable = false, passThruEnabled = false,
                                displayInGame = displayGame, displayInMenu = displayMenu,
                                opacity = 1f, bgColor = 0x4D000000.toInt(), fgColor = 0xFFFFFFFF.toInt(),
                                strokeWidth = 0f, cornerRadius = cr
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
    context.startActivity(Intent(context, LegacyControlEditorActivity::class.java).apply {
        putExtra(BUNDLE_LEGACY_CONTROL, file.absolutePath)
    })
}
