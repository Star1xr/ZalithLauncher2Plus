package net.kdt.pojavlaunch;

import android.content.Context;
import android.util.AttributeSet;
import android.view.SurfaceView;

/**
 * Minimal MinecraftGLSurface stub for ZL1 Legacy Backport compatibility.
 * The real surface is only needed during gameplay, not in the control editor.
 */
public class MinecraftGLSurface extends SurfaceView {
    public MinecraftGLSurface(Context context) { super(context); }
    public MinecraftGLSurface(Context context, AttributeSet attrs) { super(context, attrs); }
    public MinecraftGLSurface(Context context, AttributeSet attrs, int defStyleAttr) { super(context, attrs, defStyleAttr); }
}
