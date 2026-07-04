package net.kdt.pojavlaunch.customcontrols;

/**
 * Listener for Legacy (Zalith 1) special control buttons that need to reach outside the
 * native {@link ControlLayout} view hierarchy — namely the keyboard toggle and the
 * dedicated mouse cursor toggle button. Mirrors the existing {@link ControlButtonMenuListener}
 * pattern used for the menu button.
 */
public interface LegacySpecialButtonListener {
    /** Called when the on-screen keyboard special button is pressed. */
    void onKeyboardToggle();

    /** Called when the virtual mouse cursor special button is pressed. */
    void onMouseCursorToggle();
}
