package fr.spse.gamepad_remapper;

  import android.view.KeyEvent;
  import android.view.MotionEvent;

  public interface GamepadHandler {
      boolean onKeyEvent(KeyEvent event);
      boolean onMotionEvent(MotionEvent event);
  }
  