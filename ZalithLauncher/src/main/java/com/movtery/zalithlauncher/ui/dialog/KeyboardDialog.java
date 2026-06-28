package com.movtery.zalithlauncher.ui.dialog;

  import android.content.Context;

  /**
   * Keyboard dialog stub for ZL1 Legacy Backport compatibility.
   * Provides a keycode selection dialog interface.
   */
  public class KeyboardDialog {
      private final Context mContext;
      private OnKeycodeSelectListener mListener;

      public interface OnKeycodeSelectListener {
          void onKeycodeSelected(int index);
      }

      public KeyboardDialog(Context context) {
          mContext = context;
      }

      public KeyboardDialog setOnKeycodeSelectListener(OnKeycodeSelectListener listener) {
          mListener = listener;
          return this;
      }

      /** Show the keyboard dialog. */
      public KeyboardDialog show() {
          // Stub: no-op; in a full implementation this would show a dialog.
          return this;
      }

      /** Show the keyboard dialog with a pre-selected index. */
      public KeyboardDialog show(int preSelectedIndex) {
          return show();
      }
  }
  