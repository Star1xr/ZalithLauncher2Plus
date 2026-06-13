package com.movtery.zalithlauncher.ui.activities

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
            openLauncher()
            return
        }

        val version = VersionsManager.getVersion(versionName)

        if (version == null) {
            openLauncher()
            return
        }

        VersionsManager.saveVersion(version)

        LaunchGame.launchGame(
            context = this,
            version = version,
            exitActivity = {
                finish()
            },
            waitForVulkanChecker = {
                // Skip Vulkan checker for shortcuts
            },
            submitError = {
                openLauncher()
            }
        )
    }

    private fun openLauncher() {
        startActivity(Intent(this, SplashActivity::class.java))
        finish()
    }
}
