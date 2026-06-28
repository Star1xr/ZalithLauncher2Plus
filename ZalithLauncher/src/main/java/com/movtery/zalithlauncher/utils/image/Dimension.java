package com.movtery.zalithlauncher.utils.image;

  public class Dimension {
      public final int width;
      public final int height;
      private Dimension(int w, int h) { width = w; height = h; }
      public static Dimension of(int w, int h) { return new Dimension(w, h); }
      public static Dimension square(int s) { return new Dimension(s, s); }
  }
  