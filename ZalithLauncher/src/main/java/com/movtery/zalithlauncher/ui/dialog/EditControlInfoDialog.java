package com.movtery.zalithlauncher.ui.dialog;

  import android.app.AlertDialog;
  import android.content.Context;
  import com.movtery.zalithlauncher.ui.subassembly.customcontrols.ControlInfoData;

  public class EditControlInfoDialog extends AlertDialog {
      public EditControlInfoDialog(Context context, boolean isEdit, String filename, ControlInfoData data) {
          super(context);
      }
  }
  