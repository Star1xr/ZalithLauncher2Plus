package net.kdt.pojavlaunch.utils;

  import java.io.File;
  import java.io.FileWriter;
  import java.io.IOException;

  public class FileUtils {
      public static void write(File file, String content) throws IOException {
          if (file.getParentFile() != null) file.getParentFile().mkdirs();
          try (FileWriter w = new FileWriter(file)) { w.write(content); }
      }
  }
  