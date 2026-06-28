package com.movtery.zalithlauncher.utils.path;

  import android.os.Environment;

  /**
   * Compatibility shim for ZL1 code. Provides stand-in paths that mirror
   * what the real Zeryth PathManager sets up at runtime.
   * These are initialized lazily so they pick up the real values when available.
   */
  public class PathManager {
      private static String sDirData = null;
      private static String sDirCtrlmapPath = null;

      public static String getDirData() {
          if (sDirData == null) {
              sDirData = Environment.getExternalStorageDirectory().getAbsolutePath()
                      + "/games/PojavLauncher";
          }
          return sDirData;
      }

      public static String getDirCtrlmapPath() {
          if (sDirCtrlmapPath == null) {
              sDirCtrlmapPath = getDirData() + "/controlmap";
          }
          return sDirCtrlmapPath;
      }

      public static void setDirData(String dirData) {
          sDirData = dirData;
      }

      public static void setDirCtrlmapPath(String dirCtrlmapPath) {
          sDirCtrlmapPath = dirCtrlmapPath;
      }

      // ZL1-compatible static fields (populated at class load via getDirXxx())
      public static String DIR_DATA = getDirData();
      public static String DIR_CTRLMAP_PATH = getDirCtrlmapPath();
  }
  