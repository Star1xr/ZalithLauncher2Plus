package io.github.controlwear.virtual.joystick.android;

  import android.annotation.SuppressLint;
  import android.content.Context;
  import android.util.AttributeSet;
  import android.view.View;

  @SuppressLint("ViewConstructor")
  public class JoystickView extends View {
      public interface OnMoveListener { void onMove(int angle, int strength); }
      public JoystickView(Context context) { super(context); }
      public JoystickView(Context context, AttributeSet attrs) { super(context, attrs); }
      public JoystickView(Context context, AttributeSet attrs, int s) { super(context, attrs, s); }
      public void setOnMoveListener(OnMoveListener l) {}
      public void setOnMoveListener(OnMoveListener l, int loopInterval) {}
      public int getAngle() { return 0; }
      public int getStrength() { return 0; }
      public void setButtonRadius(int r) {}
      public void setEnabled(boolean e) { super.setEnabled(e); }
  }
  