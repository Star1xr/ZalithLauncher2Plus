package com.movtery.zalithlauncher.context;

  import android.content.Context;

  public class ContextExecutor {
      private static Context sContext;
      public static void setContext(Context ctx) { sContext = ctx; }
      public static String getString(int id) {
          if (sContext == null) return "";
          try { return sContext.getString(id); } catch (Throwable t) { return ""; }
      }
      public static Context getContext() { return sContext; }
  }
  