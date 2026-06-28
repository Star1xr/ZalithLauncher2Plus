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

  /**
   * Tools stub for ZL1 Legacy Backport compatibility.
   */
  public class Tools {

      public static DisplayMetrics currentDisplayMetrics = new DisplayMetrics();

      public static final Gson GLOBAL_GSON = new Gson();

      public static void showError(Context ctx, Throwable e) {
          showError(ctx, e, false);
      }

      public static void showError(Context ctx, Throwable e, boolean fatal) {
          if (ctx == null) return;
          try {
              new AlertDialog.Builder(ctx)
                  .setTitle("Error")
                  .setMessage(e != null ? e.toString() : "Unknown error")
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
  