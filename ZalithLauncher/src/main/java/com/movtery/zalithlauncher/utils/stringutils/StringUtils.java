package com.movtery.zalithlauncher.utils.stringutils;

  /**
   * String utilities for the ZL1 Legacy Backport.
   */
  public class StringUtils {

      /**
       * Insert a value into a template. If the template contains %s, uses String.format;
       * otherwise appends with a space.
       */
      public static String insertSpace(String template, String value) {
          if (template == null) return value != null ? value : "";
          if (template.contains("%s")) return String.format(template, value);
          return template + " " + value;
      }

      public static boolean isNullOrEmpty(String s) {
          return s == null || s.isEmpty();
      }

      private StringUtils() {}
  }
  