package com.movtery.zalithlauncher.game.control.legacy

  import android.view.MotionEvent
  import androidx.compose.runtime.Composable
  import androidx.compose.runtime.getValue
  import androidx.compose.runtime.key
  import androidx.compose.runtime.remember
  import androidx.compose.runtime.rememberUpdatedState
  import androidx.compose.ui.ExperimentalComposeUiApi
  import androidx.compose.ui.Modifier
  import androidx.compose.ui.input.pointer.pointerInteropFilter
  import androidx.compose.ui.viewinterop.AndroidView
  import com.movtery.zalithlauncher.setting.AllSettings
  import net.kdt.pojavlaunch.Tools
  import net.kdt.pojavlaunch.customcontrols.ControlButtonMenuListener
  import net.kdt.pojavlaunch.customcontrols.ControlLayout
  import net.kdt.pojavlaunch.customcontrols.mouse.InGUIEventProcessor
  import net.kdt.pojavlaunch.customcontrols.mouse.TouchEventProcessor
  import org.lwjgl.glfw.CallbackBridge
  import java.io.File

  /**
   * Composable that renders PojavLauncher's native [ControlLayout] (View-based)
   * for the Legacy (Zalith 1) control mode.
   *
   * Touch routing:
   *  - Camera (grabbed / in-game): handled by the [pointerInteropFilter] modifier.
   *    For each MOVE of the designated camera pointer, [CallbackBridge.sendCursorDelta]
   *    is called directly, giving sub-frame latency and no gesture-detection delay.
   *    The filter always returns false so the AndroidView also receives every event,
   *    keeping button and joystick OnTouchListeners fully functional.
   *    [ControlLayout.isPointOverAnyChild] is used to distinguish empty-screen camera
   *    touches from touches that land on a visible button or joystick.
   *  - Cursor (not grabbed / menu): handled by [InGUIEventProcessor] inside
   *    [ControlLayout.onTouchEvent], reached for non-button touches when no child
   *    View consumes the event.
   */
  @OptIn(ExperimentalComposeUiApi::class)
  @Composable
  fun PojavControlLayout(
      modifier: Modifier = Modifier,
      legacyFile: File,
      isGrabbing: Boolean,
      onMenuButtonClicked: () -> Unit
  ) {
      val currentOnMenu by rememberUpdatedState(onMenuButtonClicked)

      // key() forces AndroidView to be destroyed and recreated when legacyFile changes
      // (e.g. user swaps layout via the in-game menu). Without this, AndroidView.factory
      // only runs once per composition lifetime, so switching layouts would have no effect.
      key(legacyFile.absolutePath) {
          // Simple mutable holder for camera tracking state.
          // Plain class (not Compose state) so MOVE events never trigger recomposition.
          val tracker = remember { CameraTracker() }

          // Holds the ControlLayout reference so the pointerInteropFilter lambda can call
          // isPointOverAnyChild without capturing a potentially stale factory closure.
          val layoutHolder = remember { LayoutHolder() }

          AndroidView(
              factory = { context ->
                  // CRITICAL: initialize Tools.currentDisplayMetrics before any dp/px conversion
                  // in the ZL1 control system (density=0 makes all button sizes 0px).
                  Tools.currentDisplayMetrics.setTo(context.resources.displayMetrics)
                  // Seed physicalWidth/Height BEFORE loadLayout so button dynamic-expression
                  // positions evaluate correctly on the very first render pass.
                  CallbackBridge.physicalWidth = context.resources.displayMetrics.widthPixels
                  CallbackBridge.physicalHeight = context.resources.displayMetrics.heightPixels
                  ControlLayout(context).also { layout ->
                      layoutHolder.layout = layout

                      layout.setMenuListener(ControlButtonMenuListener { currentOnMenu() })
                      runCatching {
                          layout.loadLayout(legacyFile.absolutePath)
                      }
                      layout.setControlVisible(true)

                      // In grab mode camera is fully handled by the pointerInteropFilter above,
                      // so the processor only needs InGUIEventProcessor for menu (non-grab) mode.
                      val inGUIProc = InGUIEventProcessor()
                      layout.setGameTouchProcessor(object : TouchEventProcessor {
                          override fun processTouchEvent(event: MotionEvent): Boolean {
                              if (CallbackBridge.isGrabbing()) return true
                              return inGUIProc.processTouchEvent(event)
                          }
                          override fun cancelPendingActions() = inGUIProc.cancelPendingActions()
                      })
                  }
              },
              update = { layout ->
                  // Capturing isGrabbing triggers re-invocation on grab-state change,
                  // ensuring setControlVisible re-evaluates displayInGame / displayInMenu flags.
                  @Suppress("UNUSED_EXPRESSION")
                  isGrabbing
                  layout.setControlVisible(true)
              },
              modifier = modifier.pointerInteropFilter { event ->
                  if (CallbackBridge.isGrabbing()) {
                      val layout = layoutHolder.layout
                      if (layout != null) {
                          tracker.handleEvent(event, layout)
                      }
                  }
                  // Always return false: buttons and joysticks live inside the AndroidView and
                  // must still receive every event via their child OnTouchListeners.
                  false
              }
          )
      }
  }

  /**
   * Tracks which pointer is driving camera rotation and accumulates position for delta computation.
   * Held in [remember] so it lives for the lifetime of the AndroidView without causing recomposition.
   */
  private class CameraTracker {
      var pointerId: Int = -1
      var lastX: Float = 0f
      var lastY: Float = 0f

      fun handleEvent(event: MotionEvent, layout: ControlLayout) {
          // Read sensitivity on every MOVE so in-session setting changes take effect immediately.
          val sensitivity = AllSettings.getMouseSpeed().getValue().toFloat() / 100f
          when (event.actionMasked) {
              MotionEvent.ACTION_DOWN -> {
                  val x = event.getX(0)
                  val y = event.getY(0)
                  if (!layout.isPointOverAnyChild(x, y)) {
                      pointerId = event.getPointerId(0)
                      lastX = x
                      lastY = y
                  }
              }
              MotionEvent.ACTION_POINTER_DOWN -> {
                  if (pointerId == -1) {
                      val idx = event.actionIndex
                      val x = event.getX(idx)
                      val y = event.getY(idx)
                      if (!layout.isPointOverAnyChild(x, y)) {
                          pointerId = event.getPointerId(idx)
                          lastX = x
                          lastY = y
                      }
                  }
              }
              MotionEvent.ACTION_MOVE -> {
                  if (pointerId != -1) {
                      val idx = event.findPointerIndex(pointerId)
                      if (idx != -1) {
                          val x = event.getX(idx)
                          val y = event.getY(idx)
                          val dx = (x - lastX) * sensitivity
                          val dy = (y - lastY) * sensitivity
                          lastX = x
                          lastY = y
                          CallbackBridge.sendCursorDelta(dx, dy)
                      }
                  }
              }
              MotionEvent.ACTION_POINTER_UP -> {
                  if (pointerId != -1 &&
                      event.getPointerId(event.actionIndex) == pointerId) {
                      pointerId = -1
                  }
              }
              MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                  pointerId = -1
              }
          }
      }
  }

  /** Thin reference holder so the [pointerInteropFilter] lambda can access the [ControlLayout]. */
  private class LayoutHolder {
      var layout: ControlLayout? = null
  }
  