package com.movtery.zalithlauncher.ui.dialog;

  import android.content.Context;

  public class KeyboardDialog {
      private final Context mContext;
      private OnKeycodeSelectListener mListener;

      public interface OnKeycodeSelectListener {
          void onKeycodeSelected(int index);
      }

      public KeyboardDialog(Context context) {
          mContext = context;
      }

      public KeyboardDialog(Context context, boolean showSpecialKeys) {
          mContext = context;
      }

      public KeyboardDialog setOnKeycodeSelectListener(OnKeycodeSelectListener listener) {
          mListener = listener;
          return this;
      }

      public KeyboardDialog show() { return this; }
      public KeyboardDialog show(int preSelectedIndex) { return show(); }
  }
  