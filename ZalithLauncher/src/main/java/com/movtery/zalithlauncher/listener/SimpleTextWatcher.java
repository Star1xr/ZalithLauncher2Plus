package com.movtery.zalithlauncher.listener;

  import android.text.Editable;
  import android.text.TextWatcher;

  @FunctionalInterface
  public interface SimpleTextWatcher extends TextWatcher {
      @Override
      void afterTextChanged(Editable s);

      @Override
      default void beforeTextChanged(CharSequence s, int start, int count, int after) {}

      @Override
      default void onTextChanged(CharSequence s, int start, int before, int count) {}
  }
  