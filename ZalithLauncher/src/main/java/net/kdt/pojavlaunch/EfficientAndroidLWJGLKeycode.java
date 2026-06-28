package net.kdt.pojavlaunch;

  import android.view.KeyEvent;
  import java.util.List;

  /**
   * Compatibility shim: delegates to com.movtery.zalithlauncher.game.input.EfficientAndroidLWJGLKeycode
   */
  public class EfficientAndroidLWJGLKeycode {
      public static boolean containsIndex(int index) {
          return com.movtery.zalithlauncher.game.input.EfficientAndroidLWJGLKeycode.containsIndex(index);
      }
      public static void execKey(KeyEvent event, int valueIndex) {
          com.movtery.zalithlauncher.game.input.EfficientAndroidLWJGLKeycode.execKey(event, valueIndex);
      }
      public static void execKeyIndex(int index) {
          com.movtery.zalithlauncher.game.input.EfficientAndroidLWJGLKeycode.execKeyIndex(index);
      }
      public static short getValueByIndex(int index) {
          return com.movtery.zalithlauncher.game.input.EfficientAndroidLWJGLKeycode.getValueByIndex(index);
      }
      public static int getIndexByKey(int key) {
          return com.movtery.zalithlauncher.game.input.EfficientAndroidLWJGLKeycode.getIndexByKey(key);
      }
      public static int getIndexByValue(int lwjglKey) {
          return com.movtery.zalithlauncher.game.input.EfficientAndroidLWJGLKeycode.getIndexByValue(lwjglKey);
      }
      public static List<String> generateKeyName() {
          return com.movtery.zalithlauncher.game.input.EfficientAndroidLWJGLKeycode.generateKeyName();
      }
  }
  