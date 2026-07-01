package com.movtery.zalithlauncher.game.control.legacy

import android.view.MotionEvent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
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
 *  - Camera (grabbed / in-game): handled directly in [ControlLayout.dispatchTouchEvent].
 *    dispatchTouchEvent is always called by the Android View system for every touch event,
 *    regardless of whether child views consume it. This is reliable — unlike a Compose
 *    pointerInteropFilter, which stops receiving ACTION_MOVE events when it returns false
 *    on ACTION_DOWN (Compose interprets that as "gesture not claimed").
 *    [ControlLayout.isPointOverAnyChild] is used to distinguish empty-screen camera
 *    touches from touches that land on a visible button or joystick.
 *  - Cursor (not grabbed / menu): handled by [InGUIEventProcessor] inside
 *    [ControlLayout.onTouchEvent], reached for non-button touches when no child
 *    View consumes the event.
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

                    // Camera rotation is handled in ControlLayout.dispatchTouchEvent (grab mode).
                    // This processor only needs InGUIEventProcessor for menu (non-grab) mode.
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
            modifier = modifier
        )
    }
}
