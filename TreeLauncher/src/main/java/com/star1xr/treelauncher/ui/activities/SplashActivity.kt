/*
 * Zalith Launcher 2
 * Copyright (C) 2025 MovTery <movtery228@qq.com> and contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/gpl-3.0.txt>.
 */

package com.star1xr.treelauncher.ui.activities

import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.lifecycleScope
import com.star1xr.treelauncher.ui.base.BaseAppCompatActivity
import com.star1xr.treelauncher.ui.screens.splash.SplashScreen
import com.star1xr.treelauncher.ui.theme.ZalithLauncherTheme
import com.star1xr.treelauncher.ui.theme.backgroundColor
import com.star1xr.treelauncher.ui.theme.onBackgroundColor
import com.star1xr.treelauncher.utils.logging.Logger.lInfo
import com.star1xr.treelauncher.utils.logging.Logger.lWarning
import com.star1xr.treelauncher.viewmodel.SplashBackStackViewModel
import dagger.hilt.android.AndroidEntryPoint
import com.star1xr.treelauncher.components.UnpackTasksManager
import com.star1xr.treelauncher.setting.AllSettings

const val EXTRA_IMPORT_ACTION = "EXTRA_IMPORT_ACTION"
const val EXTRA_IMPORT_URI    = "EXTRA_IMPORT_URI"
const val EXTRA_IMPORT_TYPE   = "EXTRA_IMPORT_TYPE"

const val IMPORT_TYPE_MODPACK = "modpack"
const val IMPORT_TYPE_CONTROLS = "controls"
const val IMPORT_TYPE_UNKNOWN = "unknown"

@SuppressLint("CustomSplashScreen")
@AndroidEntryPoint
class SplashActivity : BaseAppCompatActivity(refreshData = false) {

    private val backStackViewModel: SplashBackStackViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        UnpackTasksManager.initUnpackItems(this)
        if (checkTasksToMain()) return
        setContent {
            ZalithLauncherTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = backgroundColor(), contentColor = onBackgroundColor()) {
                    SplashScreen(
                        startAllTask = { UnpackTasksManager.startAllTask(lifecycleScope) { swapToMain() } },
                        unpackItems = UnpackTasksManager.unpackItems,
                        screenViewModel = backStackViewModel
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        if (!UnpackTasksManager.areAllTasksFinished()) return
        if (isImportIntent(intent) && !isLauncherIntent(intent)) {
            handleImportIntent(intent); finish()
        }
    }

    private fun checkTasksToMain(): Boolean {
        if (!AllSettings.setupCompleted.getValue()) { swapToMain(); return true }
        
        // If setup is done, always proceed to Main immediately.
        // We start unpacking in background just in case, but WITHOUT showing UI.
        if (!UnpackTasksManager.areAllTasksFinished()) {
            UnpackTasksManager.startAllTask(lifecycleScope)
        }

        if (isImportIntent(intent) && !isLauncherIntent(intent)) {
            if (handleImportIntent(intent)) { finish(); return true }
        }
        swapToMain(); return true
    }

    private fun swapToMain() {
        val forward = Intent(this, MainActivity::class.java).apply {
            if (intent.action == Intent.ACTION_VIEW && intent.scheme == "treelauncher") {
                data = intent.data
            }
        }
        startActivity(forward); finish()
    }

    private fun handleImportIntent(source: Intent): Boolean {
        if (!isImportIntent(source)) return false
        val uri: Uri? = when (source.action) {
            Intent.ACTION_SEND -> source.clipData?.getItemAt(0)?.uri ?: source.getParcelableExtra(Intent.EXTRA_STREAM)
            Intent.ACTION_VIEW -> source.data
            else -> null
        }
        if (uri == null) return false
        try { contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION) } catch (_: Exception) {}
        val forward = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra(EXTRA_IMPORT_ACTION, source.action)
            putExtra(EXTRA_IMPORT_URI, uri)
            putExtra(EXTRA_IMPORT_TYPE, resolveImportType(source))
        }
        startActivity(forward); return true
    }

    private fun resolveImportType(intent: Intent): String {
        val comp = intent.component ?: return IMPORT_TYPE_UNKNOWN
        val info = packageManager.getActivityInfo(comp, PackageManager.GET_META_DATA)
        return info.metaData?.getString("import_type") ?: IMPORT_TYPE_UNKNOWN
    }

    private fun isLauncherIntent(intent: Intent?): Boolean {
        return intent?.action == Intent.ACTION_MAIN && intent.categories?.contains(Intent.CATEGORY_LAUNCHER) == true
    }

    private fun isImportIntent(intent: Intent?): Boolean {
        val comp = intent?.component ?: return false
        val info = packageManager.getActivityInfo(comp, PackageManager.GET_META_DATA)
        return info.metaData?.getString("import_type") != null
    }
}
