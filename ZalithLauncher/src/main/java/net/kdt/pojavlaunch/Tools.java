package net.kdt.pojavlaunch;

  import android.app.AlertDialog;
  import android.content.Context;
  import android.util.DisplayMetrics;

  import com.google.gson.Gson;

  import java.io.BufferedReader;
  import java.io.File;
  import java.io.FileInputStream;
  import java.io.FileWriter;
  import java.io.IOException;
  import java.io.InputStreamReader;

  public class Tools {

      public static DisplayMetrics currentDisplayMetrics = new DisplayMetrics();

      public static final Gson GLOBAL_GSON = new Gson();

      public static void showError(Context ctx, Throwable e) {
          showError(ctx, e.toString(), e);
      }

      public static void showError(Context ctx, Throwable e, boolean fatal) {
          showError(ctx, e.toString(), e);
      }

      public static void showError(Context ctx, String message, Throwable e) {
          if (ctx == null) return;
          try {
              new AlertDialog.Builder(ctx)
                  .setTitle("Error")
                  .setMessage(message + (e != null ? ("\n" + e.toString()) : ""))
                  .setPositiveButton(android.R.string.ok, null)
                  .show();
          } catch (Throwable ignored) {}
      }

      public static float dpToPx(float dp) {
          return dp * currentDisplayMetrics.density;
      }

      public static float dpToPx(int dp) {
          return dp * currentDisplayMetrics.density;
      }

      public static float pxToDp(float px) {
          if (currentDisplayMetrics.density == 0) return px;
          return px / currentDisplayMetrics.density;
      }

      public static boolean isValidString(String s) {
          return s != null && !s.isEmpty();
      }

      public static String read(String path) throws IOException {
          return read(new File(path));
      }

      public static String read(File file) throws IOException {
          StringBuilder sb = new StringBuilder();
          try (BufferedReader br = new BufferedReader(
                  new InputStreamReader(new FileInputStream(file), "UTF-8"))) {
              String line;
              while ((line = br.readLine()) != null) {
                  sb.append(line).append("\n");
              }
          }
          return sb.toString();
      }

      public static void write(String path, String content) throws IOException {
          File file = new File(path);
          if (file.getParentFile() != null) file.getParentFile().mkdirs();
          try (FileWriter fw = new FileWriter(file)) {
              fw.write(content);
          }
      }
  }
  