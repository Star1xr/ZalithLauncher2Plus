package com.movtery.zalithlauncher.task;

  import android.os.Handler;
  import android.os.Looper;
  import java.util.concurrent.Executor;
  import java.util.concurrent.Executors;

  public class TaskExecutors {
      private static final Handler UI_HANDLER = new Handler(Looper.getMainLooper());
      private static final Executor ANDROID_UI = UI_HANDLER::post;
      private static final Executor IO = Executors.newCachedThreadPool();

      public static Executor getAndroidUI() { return ANDROID_UI; }
      public static Executor getIO() { return IO; }

      public static void runInUIThread(Runnable r) { UI_HANDLER.post(r); }
  }
  