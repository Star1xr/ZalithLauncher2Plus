package net.kdt.pojavlaunch.colorselector;

  import android.content.Context;
  import android.util.AttributeSet;
  import android.view.View;

  public class ColorSelector extends View {
      public interface OnColorSelectedListener { void onColorSelected(int color); }
      public ColorSelector(Context context) { super(context); }
      public ColorSelector(Context context, AttributeSet attrs) { super(context, attrs); }
      public void setOnColorSelectedListener(OnColorSelectedListener l) {}
      public void setSelectedColor(int color) {}
      public int getSelectedColor() { return 0xFFFFFFFF; }
  }
  