package com.movtery.zalithlauncher.utils.stringutils;

  public class StringUtils {
      public static String removeSuffix(String s, String suffix) {
          if (s != null && suffix != null && s.endsWith(suffix))
              return s.substring(0, s.length() - suffix.length());
          return s != null ? s : "";
      }
      public static boolean isBlank(String s) { return s == null || s.trim().isEmpty(); }
      public static boolean isEmpty(String s) { return s == null || s.isEmpty(); }
  }
  