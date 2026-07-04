package com.movtery.zalithlauncher.utils;

  import android.content.Context;
  import android.graphics.Bitmap;
  import android.graphics.BitmapFactory;
  import android.graphics.drawable.BitmapDrawable;
  import android.graphics.drawable.Drawable;
  import android.util.DisplayMetrics;

  import androidx.core.content.res.ResourcesCompat;

  import com.movtery.zalithlauncher.R;
  import com.movtery.zalithlauncher.path.PathManager;
  import com.movtery.zalithlauncher.setting.AllSettings;
  import com.movtery.zalithlauncher.ui.control.mouse.CursorHotspot;

  import java.io.File;

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
       * Returns the mouse cursor drawable used by Zalith 2's virtual mouse (arrow pointer),
       * so the Legacy (Zalith 1) touchpad cursor matches it exactly, including any custom
       * pointer image the user has set via ZL2's mouse pointer settings.
       */
      public static Drawable customMouse(Context context) {
          try {
              File customFile = new File(PathManager.Companion.getDIR_MOUSE_POINTER(), "default_pointer.image");
              if (customFile.exists()) {
                  Bitmap bitmap = BitmapFactory.decodeFile(customFile.getAbsolutePath());
                  if (bitmap != null) {
                      return new BitmapDrawable(context.getResources(), bitmap);
                  }
              }
          } catch (Throwable ignored) {}

          try {
              return ResourcesCompat.getDrawable(context.getResources(),
                      R.drawable.img_mouse_pointer_arrow, context.getTheme());
          } catch (Throwable t) {
              return null;
          }
      }

      /**
       * Returns the virtual mouse cursor size (in dp), matching ZL2's {@code mouseSize} setting.
       */
      public static int getMouseSizeDp() {
          try {
              return AllSettings.INSTANCE.getMouseSize().getValue();
          } catch (Throwable ignored) {
              return 24;
          }
      }

      /**
       * Returns the arrow cursor's hotspot (percentage offset applied against the cursor's
       * own size to determine where the "tip" of the pointer sits), matching ZL2's
       * {@code arrowMouseHotspot} setting.
       */
      public static CursorHotspot getArrowMouseHotspot() {
          try {
              return AllSettings.INSTANCE.getArrowMouseHotspot().getValue();
          } catch (Throwable ignored) {
              return new CursorHotspot(0, 0);
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
  