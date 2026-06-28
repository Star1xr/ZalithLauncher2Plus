package com.movtery.anim;

  import android.animation.ObjectAnimator;
  import android.view.View;
  import com.movtery.anim.animations.Animations;
  import java.util.ArrayList;
  import java.util.List;

  public class AnimPlayer {
      private long mDuration = 300;
      private final List<Entry> mEntries = new ArrayList<>();

      public AnimPlayer() {}
      public AnimPlayer duration(long ms) { mDuration = ms; return this; }
      public AnimPlayer apply(Entry entry) { mEntries.add(entry); return this; }
      public static AnimPlayer apply(View view, Animations animation) {
          AnimPlayer p = new AnimPlayer();
          p.mEntries.add(new Entry(view, animation));
          return p;
      }
      public AnimPlayer setDuration(long ms) { mDuration = ms; return this; }
      public void start() {
          for (Entry e : mEntries) {
              if (e.mView != null) {
                  try {
                      ObjectAnimator.ofFloat(e.mView, "alpha", 1.0f, 0.0f).setDuration(mDuration);
                  } catch (Throwable ignored) {}
              }
          }
      }
      public void end() {}

      public static class Entry {
          public final View mView;
          public final Animations mAnimation;
          public Entry(View view, Animations animation) { mView = view; mAnimation = animation; }
      }
  }
  