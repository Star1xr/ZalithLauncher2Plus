package net.kdt.pojavlaunch;

import android.app.AlertDialog;
import android.content.Context;
import android.util.DisplayMetrics;

/**
 * Minimal Tools stub for ZL1 Legacy Backport compatibility.
 * Only exposes the members required by the customcontrols package.
 */
public class Tools {

    /** Display metrics populated at runtime by the activity. */
    public static DisplayMetrics currentDisplayMetrics = new DisplayMetrics();

    public static void showError(Context ctx, Throwable e) {
        showError(ctx, e, false);
    }

    public static void showError(Context ctx, Throwable e, boolean fatal) {
        if (ctx == null) return;
        try {
            new AlertDialog.Builder(ctx)
                .setTitle("Error")
                .setMessage(e != null ? e.toString() : "Unknown error")
                .setPositiveButton(android.R.string.ok, null)
                .show();
        } catch (Throwable ignored) {}
    }
}
