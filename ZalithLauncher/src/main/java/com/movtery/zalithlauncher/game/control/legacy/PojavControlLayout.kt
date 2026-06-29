package com.movtery.zalithlauncher.game.control.legacy

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import net.kdt.pojavlaunch.Tools
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
            ControlLayout(context).also { layout ->
                layout.setMenuListener(ControlButtonMenuListener { currentOnMenu() })
                runCatching {
                    layout.loadLayout(legacyFile.absolutePath)
                }
                layout.setControlVisible(true)
            }
        },
        update = { layout ->
            // Called on every recomposition that captures isGrabbing.
            // ControlLayout.setControlVisible re-reads CallbackBridge.isGrabbing() internally
            // to decide which buttons (displayInGame / displayInMenu) should be visible.
            layout.setControlVisible(true)
        },
        modifier = modifier
    )
}

