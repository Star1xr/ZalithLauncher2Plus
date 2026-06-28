package com.movtery.zalithlauncher.utils.stringutils;

  /**
   * String utilities — provides ZL1 Backport methods and supports StringUtilsKt.java.
   */
  public class StringUtils {

      /**
       * ZL1 Backport: insert value into template. If template contains %s uses String.format;
       * otherwise appends with a space.
       */
      public static String insertSpace(String template, String value) {
          if (template == null) return value != null ? value : "";
          if (template.contains("%s")) return String.format(template, value);
          return template + " " + value;
      }

      /** Remove suffix from s if s ends with suffix; otherwise return s unchanged. */
      public static String removeSuffix(String s, String suffix) {
          if (s == null) return null;
          if (suffix == null || !s.endsWith(suffix)) return s;
          return s.substring(0, s.length() - suffix.length());
      }

      public static boolean isNullOrEmpty(String s) {
          return s == null || s.isEmpty();
      }

      private StringUtils() {}
  }
  