package com.movtery.zalithlauncher.ui.dialog;

  import android.app.AlertDialog;
  import android.content.Context;
  import java.io.File;

  public class SelectControlsDialog extends AlertDialog {
      public interface OnFileSelectedListener {
          void onFileSelected(File file);
      }
      public SelectControlsDialog(Context context, OnFileSelectedListener listener) {
          super(context);
      }
      public void setTitleText(CharSequence text) {}
      public void setTitleText(int resId) {}
  }
  