package com.movtery.zalithlauncher.ui.dialog;

  import android.app.AlertDialog;
  import android.content.Context;
  import com.movtery.zalithlauncher.ui.subassembly.customcontrols.ControlInfoData;

  public class EditControlInfoDialog extends AlertDialog {
      public interface OnConfirmClickListener {
          void onConfirm(String fileName, ControlInfoData controlInfoData);
      }

      public EditControlInfoDialog(Context context, boolean isEdit, String filename, ControlInfoData data) {
          super(context);
      }

      public void setOnConfirmClickListener(OnConfirmClickListener listener) {}

      @Override
      public void setTitle(CharSequence title) { super.setTitle(title); }

      @Override
      public void setTitle(int titleId) { super.setTitle(titleId); }
  }
  