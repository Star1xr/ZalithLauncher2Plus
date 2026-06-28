package fr.spse.gamepad_remapper;

  public class Settings {
      private static float sDeadzoneScale = 1.0f;
      public static void setDeadzoneScale(float scale) { sDeadzoneScale = scale; }
      public static float getDeadzoneScale() { return sDeadzoneScale; }
  }
  