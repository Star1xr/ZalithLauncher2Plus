package com.movtery.zalithlauncher.event.value;
  public class HotbarChangeEvent {
      public final int value;
      private final int width;
      private final int height;
      public HotbarChangeEvent(int value) { this.value = value; this.width = 0; this.height = 0; }
      public HotbarChangeEvent(int width, int height) { this.value = 0; this.width = width; this.height = height; }
      public int getWidth() { return width; }
      public int getHeight() { return height; }
  }
  