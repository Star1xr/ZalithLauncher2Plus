package net.kdt.pojavlaunch.utils;

  public class MathUtils {
      public static float dist(float x1, float y1, float x2, float y2) {
          float dx = x2 - x1, dy = y2 - y1;
          return (float) Math.sqrt(dx * dx + dy * dy);
      }
      public static float clamp(float v, float min, float max) { return Math.max(min, Math.min(max, v)); }
      public static double toDeadzone(double value, double deadzone) {
          if (Math.abs(value) < deadzone) return 0;
          return value;
      }
  }
  