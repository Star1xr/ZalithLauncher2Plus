package com.movtery.zalithlauncher.game.control.legacy

import android.graphics.RectF
import java.util.concurrent.CopyOnWriteArrayList

/**
 * Thread-safe registry of visible Legacy control button bounds, expressed in the
 * ControlLayout local coordinate space (= full-screen PojavControlLayout pixels).
 *
 * Updated by [net.kdt.pojavlaunch.customcontrols.ControlLayout] on every new touch;
 * read by [com.movtery.zalithlauncher.game.support.touch_controller.touchControllerTouchModifier]
 * to exclude button-owned pointers from Touch Controller mod reporting, so that pressing
 * a Legacy button does not simultaneously trigger camera movement or other gameplay gestures.
 */
object LegacyButtonTracker {
    private val buttonRects = CopyOnWriteArrayList<RectF>()

    /**
     * Replace the current set of button rects with [rects].
     * Called from the UI thread by ControlLayout on each ACTION_DOWN.
     */
    fun updateButtonRects(rects: List<RectF>) {
        buttonRects.clear()
        buttonRects.addAll(rects)
    }

    /** Remove all tracked rects (called when the Legacy layout is unloaded). */
    fun clear() {
        buttonRects.clear()
    }

    /**
     * Returns true if the screen-local point ([x], [y]) falls within any visible
     * Legacy control button.  Safe to call from any thread.
     */
    fun isOnButton(x: Float, y: Float): Boolean =
        buttonRects.any { it.contains(x, y) }
}
