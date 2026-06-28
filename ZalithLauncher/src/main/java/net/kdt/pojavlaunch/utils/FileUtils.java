package net.kdt.pojavlaunch.utils;

  import java.io.File;
  import java.io.FileWriter;
  import java.io.IOException;

  public class FileUtils {
      public static void write(File file, String content) throws IOException {
          if (file.getParentFile() != null) file.getParentFile().mkdirs();
          try (FileWriter w = new FileWriter(file)) { w.write(content); }
      }

      public static void ensureParentDirectory(File file) throws IOException {
          File parent = file.getParentFile();
          if (parent != null && !parent.exists()) {
              if (!parent.mkdirs()) throw new IOException("Could not create directory: " + parent);
          }
      }
  }
  