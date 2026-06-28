package com.movtery.zalithlauncher.feature.log;

  import android.util.Log;

  public class Logging {
      public static void i(String tag, String msg) { Log.i(tag, msg != null ? msg : "null"); }
      public static void d(String tag, String msg) { Log.d(tag, msg != null ? msg : "null"); }
      public static void v(String tag, String msg) { Log.v(tag, msg != null ? msg : "null"); }
      public static void w(String tag, String msg) { Log.w(tag, msg != null ? msg : "null"); }
      public static void e(String tag, String msg) { Log.e(tag, msg != null ? msg : "null"); }
      public static void e(String tag, String msg, Throwable t) { Log.e(tag, msg != null ? msg : "null", t); }
      public static void w(String tag, String msg, Throwable t) { Log.w(tag, msg != null ? msg : "null", t); }
  }
  