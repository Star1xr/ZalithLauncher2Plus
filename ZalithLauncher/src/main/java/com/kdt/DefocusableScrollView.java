package com.kdt;

  import android.annotation.SuppressLint;
  import android.content.Context;
  import android.util.AttributeSet;
  import android.widget.ScrollView;

  @SuppressLint("AppCompatCustomView")
  public class DefocusableScrollView extends ScrollView {
      public DefocusableScrollView(Context context) { super(context); }
      public DefocusableScrollView(Context context, AttributeSet attrs) { super(context, attrs); }
      public DefocusableScrollView(Context context, AttributeSet attrs, int s) { super(context, attrs, s); }
  }
  