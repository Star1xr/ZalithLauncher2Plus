package com.movtery.zalithlauncher.task;

  import java.util.concurrent.Callable;
  import java.util.concurrent.Executor;

  public class Task<T> {
      private Runnable mRunnable;

      public static <T> Task<T> runTask(Executor executor, Callable<T> callable) {
          Task<T> task = new Task<>();
          task.mRunnable = () -> executor.execute(() -> {
              try { callable.call(); } catch (Exception ignored) {}
          });
          return task;
      }

      public static Task<Void> runTask(Executor executor, Runnable runnable) {
          Task<Void> task = new Task<>();
          task.mRunnable = () -> executor.execute(runnable);
          return task;
      }

      public Task<T> finallyTask(Runnable r) { return this; }
      public Task<T> onSuccess(Runnable r) { return this; }

      public void execute() {
          if (mRunnable != null) mRunnable.run();
      }
  }
  