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

package com.movtery.zalithlauncher.game.download.assets

import android.content.Context
import com.movtery.zalithlauncher.R
import com.movtery.zalithlauncher.coroutine.Task
import com.movtery.zalithlauncher.coroutine.TaskSystem
import com.movtery.zalithlauncher.game.download.assets.platform.PlatformVersion
import com.movtery.zalithlauncher.game.download.assets.platform.mcim.mapMCIMMirrorUrls
import com.movtery.zalithlauncher.game.version.installed.Version
import com.movtery.zalithlauncher.path.PathManager
import com.movtery.zalithlauncher.utils.file.ensureParentDirectory
import com.movtery.zalithlauncher.utils.file.formatFileSize
import com.movtery.zalithlauncher.utils.logging.Logger
import com.movtery.zalithlauncher.utils.network.downloadFromMirrorListSuspend
import com.movtery.zalithlauncher.utils.network.toLocal
import com.movtery.zalithlauncher.utils.network.withSpeedReport
import com.movtery.zalithlauncher.viewmodel.ErrorViewModel
import io.ktor.client.plugins.HttpRequestTimeoutException
import io.ktor.client.plugins.ResponseException
import okio.IOException
import org.apache.commons.io.FileUtils
import java.io.File
import java.net.ConnectException
import java.net.UnknownHostException
import java.nio.channels.UnresolvedAddressException

private const val TAG = "DownloadSingle"

/**
 * 为一些版本下载单独的资源文件
 * @param version 要下载单独资源版本信息
 * @param versions 为哪些游戏版本下载
 * @param folder 版本游戏目录下的相对路径
 * @param onFileCopied 文件已成功复制到版本游戏目录后 单独回调
 * @param onFileCancelled 文件安装已取消 单独回调
 */
fun downloadSingleForVersions(
    context: Context,
    version: PlatformVersion,
    versions: List<Version>,
    folder: String,
    onFileCopied: suspend (zip: File, folder: File) -> Unit = { _, _ -> },
    onFileCancelled: (zip: File, folder: File) -> Unit = { _, _ -> },
    submitError: (ErrorViewModel.ThrowableMessage) -> Unit
) {
    val cacheFile = File(File(PathManager.DIR_CACHE, "assets"), version.platformSha1() ?: version.platformFileName())

    downloadSingleFile(
        version = version,
        file = cacheFile,
        onDownloaded = { task ->
            task.updateProgress(-1f, R.string.download_assets_install_progress_installing, version.platformFileName())
            versions.forEach { ver ->
                val targetFolder = File(ver.getGameDir(), folder)
                val targetFile = File(targetFolder, version.platformFileName())
                if (targetFile.exists() && !targetFile.delete()) throw IOException("Failed to properly delete the existing target file.")
                cacheFile.copyTo(targetFile)
                onFileCopied(targetFile, targetFolder) //文件已复制回调
            }
        },
        onError = { e ->
            Logger.warning(TAG, "An error occurred while downloading the resource files.", e)
            val message = mapExceptionToMessage(e).let { pair ->
                val args = pair.second
                if (args != null) {
                    context.getString(pair.first, *args)
                } else {
                    context.getString(pair.first)
                }
            }
            submitError(
                ErrorViewModel.ThrowableMessage(
                    title = context.getString(R.string.download_assets_install_failed),
                    message = message
                )
            )
        },
        onCancel = {
            FileUtils.deleteQuietly(cacheFile)
            versions.forEach { ver ->
                val targetFolder = File(ver.getGameDir(), folder)
                val targetFile = File(targetFolder, version.platformFileName())
                if (targetFile.exists()) FileUtils.deleteQuietly(targetFile)
                onFileCancelled(targetFile, targetFolder) //文件已取消回调
            }
        },
        onFinally = {
            Logger.info(TAG, "Attempting to clear cached resource files.")
            FileUtils.deleteQuietly(cacheFile)
        }
    )
}

private fun downloadSingleFile(
    version: PlatformVersion,
    file: File,
    onDownloaded: suspend (Task) -> Unit,
    onError: (Throwable) -> Unit = {},
    onCancel: () -> Unit = {},
    onFinally: () -> Unit = {}
) {
    TaskSystem.submitTask(
        Task.runTask(
            id = version.platformSha1() ?: version.platformFileName(),
            task = { task ->
                val totalFileSize = version.platformFileSize()
                var downloadedSize = 0L

                //更新下载任务进度
                fun updateProgress() {
                    task.updateProgress(
                        (downloadedSize.toDouble() / totalFileSize.toDouble()).toFloat(),
                        R.string.download_assets_install_progress_downloading,
                        version.platformFileName(),
                        formatFileSize(downloadedSize),
                        formatFileSize(totalFileSize),
                    )
                }
                updateProgress()

                withSpeedReport(
                    onSpeedReport = { bytes ->
                        task.updateSpeed(bytes)
                    },
                    onClear = {
                        task.clearSpeed()
                    }
                ) { report ->
                    downloadFromMirrorListSuspend(
                        urls = version
                            .platformDownloadUrl()
                            .mapMCIMMirrorUrls(),
                        sha1 = version.platformSha1(),
                        outputFile = file.ensureParentDirectory(),
                        sizeCallback = { size ->
                            downloadedSize += size
                            updateProgress()
                            report(size)
                        }
                    )
                }

                onDownloaded(task)
            },
            onError = onError,
            onCancel = onCancel,
            onFinally = onFinally
        )
    )
}

fun mapExceptionToMessage(e: Throwable): Pair<Int, Array<Any>?> {
    return when (e) {
        is HttpRequestTimeoutException -> Pair(R.string.error_timeout, null)
        is UnknownHostException, is UnresolvedAddressException -> Pair(R.string.error_network_unreachable, null)
        is ConnectException -> Pair(R.string.error_connection_failed, null)
        is ResponseException -> e.toLocal()
        else -> {
            val errorMessage = e.localizedMessage ?: e::class.simpleName ?: "Unknown error"
            Pair(R.string.empty_holder, arrayOf(errorMessage))
        }
    }
}
  /**
   * Batch-downloads a list of dependency projects into the given game versions.
   *
   * For each dependency:
   * 1. Fetches all available platform versions.
   * 2. Picks the best version – prefers versions compatible with the installed
   *    Minecraft versions; falls back to the most recently published one.
   * 3. Initialises the version's file metadata (may require an extra network call).
   * 4. Submits a background download task via [downloadSingleForVersions].
   *
   * Pre-download failures (network error fetching versions, no version found,
   * init failure) are reported via [onEachError]. Errors that occur during the
   * actual file download are forwarded to [submitError] as usual.
   *
   * @param deps     dependency entries — (dependency metadata, project metadata)
   * @param gameVersions installed game versions to install each dep into
   * @param folder   relative folder under each version's game dir (e.g. "mods")
   * @param onEachError called with (name, errorMessage) for pre-download failures
   */
  suspend fun downloadDependenciesBatch(
      context: Context,
      deps: List<Pair<com.movtery.zalithlauncher.game.download.assets.platform.PlatformVersion.PlatformDependency, com.movtery.zalithlauncher.game.download.assets.platform.PlatformProject>>,
      gameVersions: List<Version>,
      folder: String,
      submitError: (ErrorViewModel.ThrowableMessage) -> Unit,
      onEachError: (name: String, error: String) -> Unit
  ) = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
      val mcVersions = gameVersions
          .mapNotNull { it.getVersionInfo()?.minecraftVersion }
          .distinct()

      for ((dep, project) in deps) {
          val name = project.platformTitle()
          runCatching {
              // Fetch all versions for this dependency project
              val allVersions = com.movtery.zalithlauncher.game.download.assets.platform.getVersions(
                  projectID = dep.projectId,
                  platform = dep.platform
              )

              // Pick best version: prefer MC-version-compatible, fallback to most recent
              val best = if (mcVersions.isEmpty()) {
                  allVersions.maxByOrNull { it.platformDatePublished() }
              } else {
                  allVersions
                      .filter { v -> v.platformGameVersion().any { it in mcVersions } }
                      .maxByOrNull { it.platformDatePublished() }
                      ?: allVersions.maxByOrNull { it.platformDatePublished() }
              } ?: throw java.io.IOException("No available version found for dependency: $name")

              // Initialise file metadata (some platforms require an extra network call)
              check(best.initFile(dep.projectId)) {
                  "Failed to initialise version info for dependency: $name"
              }

              // Submit the download task (runs in TaskSystem background)
              downloadSingleForVersions(
                  context = context,
                  version = best,
                  versions = gameVersions,
                  folder = folder,
                  submitError = submitError
              )
          }.onFailure { e ->
              if (e !is kotlinx.coroutines.CancellationException) {
                  Logger.warning(TAG, "Failed to prepare batch download for dependency: $name", e)
                  val msg = mapExceptionToMessage(e).let { (resId, args) ->
                      if (args != null) context.getString(resId, *args) else context.getString(resId)
                  }
                  onEachError(name, msg)
              }
          }
      }
  }
  