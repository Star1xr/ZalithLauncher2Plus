package com.movtery.zalithlauncher.game.control.legacy

  import android.view.MotionEvent
  import androidx.compose.runtime.Composable
  import androidx.compose.runtime.getValue
  import androidx.compose.runtime.key
  import androidx.compose.runtime.rememberUpdatedState
  import androidx.compose.ui.Modifier
  import androidx.compose.ui.viewinterop.AndroidView
  import com.movtery.zalithlauncher.setting.AllSettings
  import net.kdt.pojavlaunch.Tools
  import net.kdt.pojavlaunch.customcontrols.ControlButtonMenuListener
  import net.kdt.pojavlaunch.customcontrols.ControlLayout
  import net.kdt.pojavlaunch.customcontrols.mouse.InGUIEventProcessor
  import net.kdt.pojavlaunch.customcontrols.mouse.InGameEventProcessor
  import net.kdt.pojavlaunch.customcontrols.mouse.TouchEventProcessor
  import org.lwjgl.glfw.CallbackBridge
  import java.io.File

  /**
   * Composable that renders PojavLauncher's native [ControlLayout] (View-based)
   * for the Legacy (Zalith 1) control mode.
   *
   * Touch routing follows ZL1 native exactly:
   *  - Button/joystick area: the button's OnTouchListener (injected by
   *    [net.kdt.pojavlaunch.customcontrols.buttons.ControlInterface.injectTouchEventBehavior])
   *    returns true in game mode, consuming the event and sending key/mouse calls via
   *    [CallbackBridge]. [ControlLayout.onTouchEvent] is NOT called.
   *  - Game surface area: no button child claims the event -> Android calls
   *    [ControlLayout.onTouchEvent] -> delegating [TouchEventProcessor]:
   *      - Grabbed (in-game): [InGameEventProcessor] -- relative cursor delta movement.
   *      - Not grabbed (menu): [InGUIEventProcessor] -- absolute cursor position mapping.
   */
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
                      layout.setMenuListener(ControlButtonMenuListener { currentOnMenu() })
                      runCatching {
                          layout.loadLayout(legacyFile.absolutePath)
                      }
                      layout.setControlVisible(true)

                      // Wire ZL1-native touch processors for game-surface (non-button) touches.
                      // Sensitivity is read from AllSettings.mouseCaptureSensitivity (same
                      // setting ZL2 uses for grabbed-mode cursor speed; default = 100 -> 1.0x).
                      // The delegating wrapper checks CallbackBridge.isGrabbing() at runtime so
                      // grab-state transitions are handled without any recomposition.
                      val sensitivity = AllSettings.getMouseSpeed().getValue().toDouble() / 100.0
                      val inGameProc = InGameEventProcessor(sensitivity)
                      val inGUIProc  = InGUIEventProcessor()
                      layout.setGameTouchProcessor(object : TouchEventProcessor {
                          override fun processTouchEvent(event: MotionEvent): Boolean =
                              if (CallbackBridge.isGrabbing()) inGameProc.processTouchEvent(event)
                              else inGUIProc.processTouchEvent(event)

                          override fun cancelPendingActions() {
                              inGameProc.cancelPendingActions()
                              inGUIProc.cancelPendingActions()
                          }
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
              modifier = modifier
          )
      }
  }
