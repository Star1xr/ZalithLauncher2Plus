package com.movtery.zalithlauncher.utils;

  import android.content.Context;
  import android.graphics.drawable.Drawable;
  import android.util.DisplayMetrics;

  import androidx.core.content.res.ResourcesCompat;

  import com.movtery.zalithlauncher.R;
  import com.movtery.zalithlauncher.setting.AllSettings;

  /**
   * ZHTools stub — provides utility methods used by ZL1 backport controls.
   */
  public class ZHTools {

      public static float dipToPx(Context context, float dip) {
          DisplayMetrics m = context.getResources().getDisplayMetrics();
          return dip * m.density;
      }

      public static int dp2px(Context context, int dp) {
          return (int) dipToPx(context, dp);
      }

      /**
       * Returns the custom mouse cursor drawable (or a system default).
       */
      public static Drawable customMouse(Context context) {
          try {
              return ResourcesCompat.getDrawable(context.getResources(),
                      android.R.drawable.ic_input_add, context.getTheme());
          } catch (Throwable t) {
              return null;
          }
      }

      /**
       * Returns the current mouse speed setting value (0-200 range typical).
       * Delegates to AllSettings.mouseCaptureSensitivity.
       */
      public static int getMouseSpeed() {
          try {
              Object setting = AllSettings.INSTANCE.getMouseCaptureSensitivity();
              if (setting != null) {
                  java.lang.reflect.Method m = setting.getClass().getMethod("getValue");
                  Object v = m.invoke(setting);
                  if (v instanceof Number) return ((Number) v).intValue();
              }
          } catch (Throwable ignored) {}
          return 100;
      }

      /**
       * Returns whether touch gestures are disabled.
       */
      public static boolean getDisableGestures() {
          return false;
      }
  }
  