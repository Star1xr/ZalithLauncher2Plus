package com.movtery.zalithlauncher.game.version.mod.update

import com.movtery.zalithlauncher.game.version.installed.Version
import com.movtery.zalithlauncher.game.version.installed.VersionFolders
import com.movtery.zalithlauncher.game.version.installed.VersionsManager
import com.movtery.zalithlauncher.game.version.mod.AllModReader
import com.movtery.zalithlauncher.setting.launcherMMKV
import com.movtery.zalithlauncher.utils.logging.Logger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit

private const val TAG = "ModUpdateChecker"

private fun updateCountKey(versionName: String) = "mod_update_count_$versionName"

object ModUpdateChecker {

    /** Cached update count for a version. -1 = not checked yet. */
    fun getUpdateCount(versionName: String): Int =
        launcherMMKV().getInt(updateCountKey(versionName), -1)

    fun clearUpdateCount(versionName: String) {
        launcherMMKV().remove(updateCountKey(versionName))
    }

    /** Run background update check for all versions. Called once at app start. */
    fun checkAllInBackground(scope: CoroutineScope = CoroutineScope(Dispatchers.IO)) {
        scope.launch(Dispatchers.IO) {
            VersionsManager.versions.forEach { version -> checkVersion(version) }
        }
    }

    /** Check a single version and store result in MMKV. */
    suspend fun checkVersion(version: Version) {
        val versionInfo = version.getVersionInfo() ?: return
        val minecraft = versionInfo.minecraftVersion
        val modLoader = versionInfo.loaderInfo?.loader ?: return
        val modsDir = VersionFolders.MOD.getDir(version.getGameDir())
        if (!modsDir.exists()) return

        runCatching {
            val mods = AllModReader(modsDir).readAllForRemote()
            if (mods.isEmpty()) {
                launcherMMKV().putInt(updateCountKey(version.getVersionName()), 0).apply()
                return@runCatching
            }

            val semaphore = Semaphore(5)
            val updateCount = coroutineScope {
                mods.map { mod ->
                    async(Dispatchers.IO) {
                        semaphore.withPermit {
                            runCatching { mod.load(loadFromCache = true) }.getOrNull()
                            val modFile = mod.remoteFile ?: return@withPermit 0
                            val project = mod.projectInfo ?: return@withPermit 0
                            val data = ModData(
                                file = mod.localMod.file,
                                modFile = modFile,
                                project = project,
                                mcMod = mod.mcMod
                            )
                            if (data.checkUpdate(minecraft, modLoader) != null) 1 else 0
                        }
                    }
                }.awaitAll().sum()
            }

            launcherMMKV().putInt(updateCountKey(version.getVersionName()), updateCount).apply()
            Logger.info(TAG, "${version.getVersionName()}: $updateCount mod updates available")
        }.onFailure {
            Logger.warning(TAG, "Update check failed for ${version.getVersionName()}", it)
        }
    }
}
