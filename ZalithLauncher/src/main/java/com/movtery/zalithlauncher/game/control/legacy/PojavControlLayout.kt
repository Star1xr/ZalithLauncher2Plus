package com.movtery.zalithlauncher.game.control.legacy

import android.os.Handler
import android.os.Looper
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
import net.kdt.pojavlaunch.customcontrols.mouse.LeftClickGesture
import net.kdt.pojavlaunch.customcontrols.mouse.RightClickGesture
import net.kdt.pojavlaunch.customcontrols.mouse.TouchEventProcessor
import org.lwjgl.glfw.CallbackBridge
import java.io.File

/**
 * Gesture-only touch processor for grab (in-game) mode.
 *
 * Camera rotation is handled separately in [ControlLayout.dispatchTouchEvent], so this
 * processor MUST NOT send any cursor position or delta — doing so would cause double
 * camera movement.
 *
 * Responsibilities:
 *  - [LeftClickGesture]  : hold finger still → GLFW_MOUSE_BUTTON_LEFT  (break block)
 *  - [RightClickGesture] : quick tap         → GLFW_MOUSE_BUTTON_RIGHT (use / interact)
 *
 * Motion deltas fed to each gesture are sensitivity-scaled screen-pixel deltas, matching
 * [InGameEventProcessor]'s scale so the "finger still" threshold (9 dp) behaves identically
 * to ZL2 mode.
 */
private class InGameGestureProcessor : TouchEventProcessor {
    private val mHandler = Handler(Looper.getMainLooper())
    private val mLeftClick = LeftClickGesture(mHandler)
    private val mRightClick = RightClickGesture(mHandler)

    /** Prevents RightClickGesture firing on stale events immediately after grab activates. */
    private var mEventTransitioned = true

    private var mLastX = 0f
    private var mLastY = 0f

    override fun processTouchEvent(event: MotionEvent): Boolean {
        val disabled = AllSettings.getDisableGestures().value
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                mLastX = event.getX(0)
                mLastY = event.getY(0)
                if (!disabled) {
                    mEventTransitioned = false
                    mLeftClick.inputEvent()
                    if (!mEventTransitioned) mRightClick.inputEvent()
                }
            }

            MotionEvent.ACTION_MOVE -> {
                if (!disabled) {
                    // Scale by sensitivity so fast swipes cancel the gesture the same way
                    // InGameEventProcessor does (gesture uses scaled deltas for still-finger check).
                    val sensitivity =
                        (AllSettings.getMouseSpeed().value as Number).toFloat() / 100f
                    val dx = (event.getX(0) - mLastX) * sensitivity
                    val dy = (event.getY(0) - mLastY) * sensitivity
                    mLastX = event.getX(0)
                    mLastY = event.getY(0)

                    // Inform gesture trackers of accumulated motion — no cursor delta sent here;
                    // camera movement is already done by ControlLayout.dispatchTouchEvent.
                    mLeftClick.setMotion(dx, dy)
                    mRightClick.setMotion(dx, dy)

                    mLeftClick.inputEvent()
                    if (!mEventTransitioned) mRightClick.inputEvent()
                }
            }

            MotionEvent.ACTION_UP,
            MotionEvent.ACTION_CANCEL -> {
                // isSwitching=false → RightClickGesture.onGestureCancelled fires the click if
                // the tap was quick and the finger was still (short tap = right click / interact).
                mEventTransitioned = true
                mLeftClick.cancel(false)
                mRightClick.cancel(false)
            }
        }
        return true
    }

    override fun cancelPendingActions() {
        // isSwitching=true → suppresses RightClickGesture firing (mode is switching).
        mLeftClick.cancel(true)
        mRightClick.cancel(true)
    }
}

/**
 * Composable that renders PojavLauncher's native [ControlLayout] (View-based)
 * for the Legacy (Zalith 1) control mode.
 *
 * Touch routing:
 *  - Camera (grabbed / in-game): [ControlLayout.dispatchTouchEvent] — always called by
 *    the Android View system, regardless of whether child views consume the event.
 *    [ControlLayout.isPointOverAnyChild] guards against camera-tracking button touches.
 *  - Tap / long press (grabbed / in-game): [InGameGestureProcessor] via
 *    [ControlLayout.onTouchEvent], reached for empty-screen touches no child consumed.
 *    Fires GLFW_MOUSE_BUTTON_LEFT (hold still) and GLFW_MOUSE_BUTTON_RIGHT (quick tap).
 *  - Cursor (not grabbed / menu): [InGUIEventProcessor] via [ControlLayout.onTouchEvent].
 */
@Composable
fun PojavControlLayout(
    modifier: Modifier = Modifier,
    legacyFile: File,
    isGrabbing: Boolean,
    onMenuButtonClicked: () -> Unit
) {
    val currentOnMenu by rememberUpdatedState(onMenuButtonClicked)

    key(legacyFile.absolutePath) {
        AndroidView(
            factory = { context ->
                Tools.currentDisplayMetrics.setTo(context.resources.displayMetrics)
                CallbackBridge.physicalWidth = context.resources.displayMetrics.widthPixels
                CallbackBridge.physicalHeight = context.resources.displayMetrics.heightPixels
                ControlLayout(context).also { layout ->
                    layout.setMenuListener(ControlButtonMenuListener { currentOnMenu() })
                    runCatching { layout.loadLayout(legacyFile.absolutePath) }
                    layout.setControlVisible(true)

                    val inGUIProc = InGUIEventProcessor()
                    val inGameGestureProc = InGameGestureProcessor()
                    layout.setGameTouchProcessor(object : TouchEventProcessor {
                        override fun processTouchEvent(event: MotionEvent): Boolean =
                            if (CallbackBridge.isGrabbing()) inGameGestureProc.processTouchEvent(event)
                            else inGUIProc.processTouchEvent(event)

                        override fun cancelPendingActions() {
                            inGameGestureProc.cancelPendingActions()
                            inGUIProc.cancelPendingActions()
                        }
                    })
                }
            },
            update = { layout ->
                @Suppress("UNUSED_EXPRESSION")
                isGrabbing
                layout.setControlVisible(true)
            },
            modifier = modifier
        )
    }
}
