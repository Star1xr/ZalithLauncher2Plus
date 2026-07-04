package com.movtery.zalithlauncher.game.control.legacy

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.view.MotionEvent
import android.widget.FrameLayout
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.movtery.zalithlauncher.setting.AllSettings
import com.movtery.zalithlauncher.setting.enums.MouseControlMode
import net.kdt.pojavlaunch.Tools
import net.kdt.pojavlaunch.customcontrols.ControlButtonMenuListener
import net.kdt.pojavlaunch.customcontrols.ControlLayout
import net.kdt.pojavlaunch.customcontrols.LegacySpecialButtonListener
import net.kdt.pojavlaunch.customcontrols.mouse.InGUIEventProcessor
import net.kdt.pojavlaunch.customcontrols.mouse.LeftClickGesture
import net.kdt.pojavlaunch.customcontrols.mouse.RightClickGesture
import net.kdt.pojavlaunch.customcontrols.mouse.Touchpad
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
        val disabled = AllSettings.getDisableGestures().getValue()
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
                // Always update mLastX/Y so that toggling gestures on mid-touch doesn't
                // produce a coordinate spike from stale values.
                val sensitivity =
                    (AllSettings.getMouseSpeed().getValue() as Number).toFloat() / 100f
                val dx = (event.getX(0) - mLastX) * sensitivity
                val dy = (event.getY(0) - mLastY) * sensitivity
                mLastX = event.getX(0)
                mLastY = event.getY(0)

                if (!disabled) {
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
        // Mirror InGameEventProcessor: set mEventTransitioned=true BEFORE cancelling so
        // that any stale MOVE events arriving after a mode switch cannot re-arm
        // mRightClick via the !mEventTransitioned guard.
        mEventTransitioned = true
        // isSwitching=true → suppresses RightClickGesture firing (mode is switching).
        mLeftClick.cancel(true)
        mRightClick.cancel(true)
    }
}

/**
 * Container view wrapping the native [ControlLayout] together with a [Touchpad] overlay.
 *
 * The touchpad is a plain (non-clickable) [android.view.View], so it never intercepts touch
 * input — it is purely a visual + logic overlay driven by [InGUIEventProcessor] through the
 * [net.kdt.pojavlaunch.customcontrols.mouse.AbstractTouchpad] interface. All real touch
 * handling continues to flow through [controlLayout] beneath it.
 */
private class LegacyControlContainer(context: Context) : FrameLayout(context) {
    val controlLayout = ControlLayout(context)
    val touchpad = Touchpad(context)

    /** Tracks the last [MouseControlMode] applied, so we only re-sync on actual changes. */
    var lastAppliedMode: MouseControlMode? = null

    /** Tracks the last consumed toggle request counter from the floating quick menu. */
    var lastToggleRequest = 0

    var onCursorStateChanged: ((Boolean) -> Unit)? = null

    init {
        addView(controlLayout, FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT))
        touchpad.isClickable = false
        touchpad.isFocusable = false
        addView(touchpad, FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT))
    }

    /** Manually flips the virtual mouse cursor on/off — used by both the dedicated
     *  in-layout special button and the floating quick-menu action, so they always
     *  stay in sync (both operate on the same underlying [touchpad] state). */
    fun toggleMouseCursor() {
        touchpad.switchState()
        onCursorStateChanged?.invoke(touchpad.displayState)
    }

    /** Applies the Tap/Swipe mouse control mode as the touchpad's default display state. */
    fun applyMouseControlMode(mode: MouseControlMode) {
        if (mode == lastAppliedMode) return
        lastAppliedMode = mode
        when (mode) {
            MouseControlMode.SLIDE -> touchpad.enable(true)
            MouseControlMode.CLICK -> touchpad.disable()
        }
        onCursorStateChanged?.invoke(touchpad.displayState)
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
 *    Behaves as Tap (direct positioning) or Swipe (offset cursor via the [Touchpad] overlay)
 *    depending on [AllSettings.mouseControlMode], or whichever the user last toggled with
 *    the dedicated mouse cursor button / floating quick-menu action.
 *
 * @param mouseCursorToggleRequest bump this counter (e.g. from the floating quick menu) to
 *   toggle the virtual mouse cursor on/off, mirroring the dedicated in-layout special button.
 * @param onKeyboardButtonClicked invoked when the on-screen keyboard special button is pressed.
 * @param onMouseCursorStateChanged reports the virtual mouse cursor's display state after any
 *   change (mode switch or manual toggle), so other UI (e.g. the floating quick menu) can stay
 *   in sync.
 */
@Composable
fun PojavControlLayout(
    modifier: Modifier = Modifier,
    legacyFile: File,
    isGrabbing: Boolean,
    mouseCursorToggleRequest: Int = 0,
    onMenuButtonClicked: () -> Unit,
    onKeyboardButtonClicked: () -> Unit = {},
    onMouseCursorStateChanged: (Boolean) -> Unit = {}
) {
    val currentOnMenu by rememberUpdatedState(onMenuButtonClicked)
    val currentOnKeyboard by rememberUpdatedState(onKeyboardButtonClicked)
    val currentOnCursorStateChanged by rememberUpdatedState(onMouseCursorStateChanged)
    val mouseControlMode = AllSettings.mouseControlMode.state

    key(legacyFile.absolutePath) {
        AndroidView(
            factory = { context ->
                Tools.currentDisplayMetrics.setTo(context.resources.displayMetrics)
                CallbackBridge.physicalWidth = context.resources.displayMetrics.widthPixels
                CallbackBridge.physicalHeight = context.resources.displayMetrics.heightPixels
                LegacyControlContainer(context).also { container ->
                    container.onCursorStateChanged = { state -> currentOnCursorStateChanged(state) }

                    val layout = container.controlLayout
                    layout.setMenuListener(ControlButtonMenuListener { currentOnMenu() })
                    layout.setSpecialButtonListener(object : LegacySpecialButtonListener {
                        override fun onKeyboardToggle() {
                            currentOnKeyboard()
                        }

                        override fun onMouseCursorToggle() {
                            container.toggleMouseCursor()
                        }
                    })
                    runCatching { layout.loadLayout(legacyFile.absolutePath) }
                    layout.setControlVisible(true)

                    val inGUIProc = InGUIEventProcessor()
                    inGUIProc.setAbstractTouchpad(container.touchpad)
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
            update = { container ->
                container.controlLayout.setControlVisible(true)
                container.touchpad.onGrabState(isGrabbing)
                container.applyMouseControlMode(mouseControlMode)
                if (mouseCursorToggleRequest != container.lastToggleRequest) {
                    container.lastToggleRequest = mouseCursorToggleRequest
                    container.toggleMouseCursor()
                }
            },
            modifier = modifier
        )
    }
}
