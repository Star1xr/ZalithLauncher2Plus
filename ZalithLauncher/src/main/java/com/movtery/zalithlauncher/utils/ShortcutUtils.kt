package com.movtery.zalithlauncher.utils

import android.content.Context
import android.content.Intent
import android.content.pm.ShortcutInfo
import android.content.pm.ShortcutManager
import android.graphics.drawable.Icon
import android.os.Build
import com.movtery.zalithlauncher.R
import com.movtery.zalithlauncher.game.version.installed.Version
import com.movtery.zalithlauncher.ui.activities.EXTRA_LAUNCH_VERSION
import com.movtery.zalithlauncher.ui.activities.SplashActivity

object ShortcutUtils {
    fun pinVersion(context: Context, version: Version) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(ShortcutManager::class.java) ?: return
        if (!manager.isRequestPinShortcutSupported) return

        val intent = Intent(context, SplashActivity::class.java).apply {
            action = Intent.ACTION_VIEW

            putExtra(EXTRA_LAUNCH_VERSION, version.getVersionName())

            addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK or
                Intent.FLAG_ACTIVITY_CLEAR_TOP or
                Intent.FLAG_ACTIVITY_SINGLE_TOP
            )

            `package` = context.packageName
        }

        val shortcut = ShortcutInfo.Builder(context, "version_${version.getVersionName()}")
            .setShortLabel(version.getVersionName())
            .setLongLabel(version.getVersionName())
            .setIcon(Icon.createWithResource(context, R.drawable.ic_rocket_launch_filled))
            .setIntent(intent)
            .build()

        manager.requestPinShortcut(shortcut, null)
    }
}
