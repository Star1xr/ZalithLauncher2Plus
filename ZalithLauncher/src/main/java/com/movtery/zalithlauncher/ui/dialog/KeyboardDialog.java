package com.movtery.zalithlauncher.ui.dialog;

  import android.app.AlertDialog;
  import android.content.Context;
  import net.kdt.pojavlaunch.GrabListener;

  public class KeyboardDialog extends AlertDialog {
      public KeyboardDialog(Context context) { super(context); }
      public KeyboardDialog(Context context, GrabListener listener) { super(context); }
      public void setCurrentKey(int keyCode) {}
      public void setOnKeySelectedListener(OnKeySelectedListener l) {}

      public interface OnKeySelectedListener {
          void onKeySelected(int keyCode);
      }
  }
  