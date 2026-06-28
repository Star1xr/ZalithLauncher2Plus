package com.movtery.zalithlauncher.ui.dialog;

  import android.app.AlertDialog;
  import android.content.Context;

  public class TipDialog extends AlertDialog {
      protected TipDialog(Context context) { super(context); }

      public static class Builder {
          private final Context mCtx;
          public Builder(Context context) { mCtx = context; }
          public Builder setTitle(CharSequence t) { return this; }
          public Builder setTitle(int r) { return this; }
          public Builder setMessage(CharSequence m) { return this; }
          public Builder setMessage(int r) { return this; }
          public Builder setWarning(CharSequence w) { return this; }
          public Builder setConfirm(CharSequence l, Runnable a) { return this; }
          public Builder setCancel(CharSequence l, Runnable a) { return this; }
          public Builder setConfirmClickListener(Runnable a) { return this; }
          public Builder setCancelClickListener(Runnable a) { return this; }
          public Builder setShowCancel(boolean s) { return this; }
          public Builder setShowConfirm(boolean s) { return this; }
          public TipDialog build() { return new TipDialog(mCtx); }
          public void buildDialog() { build().show(); }
      }
  }
  