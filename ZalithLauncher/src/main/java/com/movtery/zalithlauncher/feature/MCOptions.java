package com.movtery.zalithlauncher.feature;

  public class MCOptions {
      public static final MCOptions INSTANCE = new MCOptions();
      public int getMcScale() { return 2; }
      public String getValue(String key, String defaultValue) { return defaultValue; }
      public void setValue(String key, String value) {}
  }
  