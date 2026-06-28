package net.kdt.pojavlaunch;

  import android.content.Context;
  import android.util.AttributeSet;
  import android.view.SurfaceView;

  import org.lwjgl.glfw.CallbackBridge;

  /**
   * Minimal MinecraftGLSurface stub for ZL1 Legacy Backport compatibility.
   */
  public class MinecraftGLSurface extends SurfaceView {
      public MinecraftGLSurface(Context context) { super(context); }
      public MinecraftGLSurface(Context context, AttributeSet attrs) { super(context, attrs); }
      public MinecraftGLSurface(Context context, AttributeSet attrs, int defStyleAttr) { super(context, attrs, defStyleAttr); }

      /**
       * ZL1 Backport: send an unconverted mouse button event.
       * Delegates to CallbackBridge for actual input dispatch.
       */
      public static boolean sendMouseButtonUnconverted(int button, boolean isPressed) {
          CallbackBridge.sendMouseButton(button, isPressed);
          return true;
      }
  }
  