package net.kdt.pojavlaunch;

  import android.app.Activity;
  import android.content.Context;

  /**
   * ZL1 Legacy Backport stub for net.kdt.pojavlaunch.MainActivity.
   * Static utility methods are forwarded to the real ZL2 MainActivity.
   */
  public class MainActivity extends Activity {

      /** ZL1 Backport: toggle software keyboard state */
      public static void switchKeyboardState() {
          // No-op stub — ZL2 handles keyboard toggle via its own event system
      }

      /** ZL1 Backport: toggle virtual mouse */
      public static void toggleMouse(Context context) {
          // No-op stub — ZL2 handles mouse toggle via its own event system
      }
  }
  