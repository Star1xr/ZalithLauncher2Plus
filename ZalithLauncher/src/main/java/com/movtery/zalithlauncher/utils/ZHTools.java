package com.movtery.zalithlauncher.utils;

  import android.content.Context;
  import android.util.DisplayMetrics;

  public class ZHTools {
      public static float dipToPx(Context context, float dip) {
          DisplayMetrics m = context.getResources().getDisplayMetrics();
          return dip * m.density;
      }
      public static int dp2px(Context context, int dp) { return (int) dipToPx(context, dp); }
  }
  