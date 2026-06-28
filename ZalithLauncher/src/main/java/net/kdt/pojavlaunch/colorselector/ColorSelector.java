package net.kdt.pojavlaunch.colorselector;

  import android.content.Context;
  import android.util.AttributeSet;
  import android.view.View;
  import android.view.ViewGroup;

  public class ColorSelector {
      public interface OnColorSelectedListener { void onColorSelected(int color); }

      private final View mRootView;

      public ColorSelector(Context context) {
          mRootView = new View(context);
      }

      public ColorSelector(Context context, AttributeSet attrs) {
          mRootView = new View(context, attrs);
      }

      public ColorSelector(Context context, ViewGroup parent, Object listener) {
          mRootView = new View(context);
      }

      public View getRootView() { return mRootView; }
      public void setOnColorSelectedListener(OnColorSelectedListener l) {}
      public void setSelectedColor(int color) {}
      public int getSelectedColor() { return 0xFFFFFFFF; }
  }
  