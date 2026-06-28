package com.movtery.zalithlauncher.utils.path;

  /**
   * Compatibility shim for ZL1 code that imports com.movtery.zalithlauncher.utils.path.PathManager.
   * Delegates to the real Zeryth path manager at runtime.
   */
  public class PathManager {
      public static String DIR_DATA = "";
      public static String DIR_CTRLMAP_PATH = "";

      static {
          try {
              com.movtery.zalithlauncher.path.PathManager pm =
                  com.movtery.zalithlauncher.path.PathManager.INSTANCE;
              DIR_DATA = pm.getDirData();
              DIR_CTRLMAP_PATH = pm.getDirCtrlmapPath();
          } catch (Throwable ignored) {}
      }
  }
  