package com.movtery.zalithlauncher.ui.activities

import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import com.movtery.zalithlauncher.game.account.AccountsManager
import android.content.Intent
import android.os.Bundle
import com.movtery.zalithlauncher.game.launch.LaunchGame
import com.movtery.zalithlauncher.game.version.installed.VersionsManager
import com.movtery.zalithlauncher.ui.base.BaseAppCompatActivity

class VersionShortcutActivity : BaseAppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val versionName = intent.getStringExtra(EXTRA_LAUNCH_VERSION)

        if (versionName == null) {
            Toast.makeText(
                this,
                "Shortcut: versionName NULL",
                Toast.LENGTH_LONG
            ).show()

            openLauncher()
            return
        }

        lifecycleScope.launch {
            
            VersionsManager.refresh(
                tag = "VersionShortcutActivity",
                trySetVersion = versionName
            )

            VersionsManager.waitForRefresh()

            val version = VersionsManager.getVersion(versionName)

            if (version == null) {
                runOnUiThread {
                    Toast.makeText(
                        this@VersionShortcutActivity,
                        "Shortcut: version refresh failed",
                        Toast.LENGTH_LONG
                    ).show()
                }

                openLauncher()
                return@launch
            }

            VersionsManager.saveVersion(version)

AccountsManager.initialize(this@VersionShortcutActivity)
        AccountsManager.suspendReloadAccounts()

        LaunchGame.launchGame(
            context = this@VersionShortcutActivity,
            version = version,
            
            exitActivity = {
                finish()
            },
            
            waitForVulkanChecker = {
                // Skip Vulkan checker for shortcuts
            },
            submitError = {
                runOnUiThread {
                    Toast.makeText(
                        this@VersionShortcutActivity,
                        "Shortcut: LaunchGame error",
                        Toast.LENGTH_LONG
                    ).show()
                }

                openLauncher()
            }
        )
    }
}

    private fun openLauncher() {
        startActivity(Intent(this, SplashActivity::class.java))
        finish()
    }
}
