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

package com.movtery.zalithlauncher.utils.driver

import android.content.Context
import android.widget.Toast
import com.movtery.zalithlauncher.R
import com.movtery.zalithlauncher.coroutine.Task
import com.movtery.zalithlauncher.coroutine.TaskSystem
import com.movtery.zalithlauncher.game.plugin.driver.DriverPluginManager
import com.movtery.zalithlauncher.path.PathManager
import com.movtery.zalithlauncher.upgrade.GithubReleaseApi
import com.movtery.zalithlauncher.utils.file.extractFromZip
import com.movtery.zalithlauncher.utils.logging.Logger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okio.buffer
import okio.sink
import java.io.File
import java.util.zip.ZipFile
import kotlinx.serialization.json.Json

data class TurnipRelease(
    val tagName: String,
    val assets: List<GithubReleaseApi.Asset>
)

object TurnipDownloader {
    private const val TAG = "TurnipDownloader"
    private val client = OkHttpClient()
    private val json = Json { ignoreUnknownKeys = true }
    private const val REPO_API = "https://api.github.com/repos/K11MCH1/AdrenoToolsDrivers/releases"

    private fun parseVersion(tag: String): Int {
        val digits = tag.removePrefix("V").takeWhile { it.isDigit() || it == '.' }
            .split(".").firstOrNull()?.toIntOrNull() ?: 0
        return digits
    }

    suspend fun fetchAllReleases(): List<TurnipRelease> {
        val result = mutableListOf<TurnipRelease>()
        var page = 1
        var hasMore = true

        while (hasMore) {
            val url = "$REPO_API?per_page=100&page=$page"
            val request = Request.Builder().url(url).build()
            val response = withContext(Dispatchers.IO) { client.newCall(request).execute() }
            if (!response.isSuccessful) break

            val body = response.body?.string() ?: break
            val releases = json.decodeFromString<List<GithubReleaseApi>>(body)

            if (releases.isEmpty()) {
                hasMore = false
                break
            }

            releases.forEach { release ->
                val version = parseVersion(release.tagName)
                if (version >= 23) {
                    val zipAssets = release.assets.filter { it.name.endsWith(".zip", ignoreCase = true) }
                    if (zipAssets.isNotEmpty()) {
                        result.add(TurnipRelease(release.tagName, zipAssets))
                    }
                } else if (version < 23) {
                    hasMore = false
                }
            }

            page++
        }

        if (result.isEmpty()) throw Exception("No Turnip releases found (V23+)")
        return result
    }

    fun downloadAsset(context: Context, asset: GithubReleaseApi.Asset) {
        val task = Task.runTask(
            id = "download_turnip_driver_${asset.name}",
            task = { it ->
                
                val zipName = asset.name
                val canonicalZipName = zipName.substringBeforeLast(".zip")
                val currentDir = PathManager.DIR_DRIVERS
                val forbidden = listOf("/","\")
                
                if ((currentDir.listFiles()?.any{it.isDirectory && it.name == canonicalZipName} ?: false) && (canonicalZipName != "." && canonicalZipName != ".." && canonicalZipName.isNotEmpty() && forbidden.none{canonicalZipName.contains(it)})) {
                   Toast.makeText(context.applicationContext, "Driver already exists, skipping...", Toast.LENGTH_SHORT ).show()
                   return
                   }
                
                it.updateMessage(R.string.settings_renderer_turnip_downloading)
                it.updateProgress(-1f)

                val downloadFile = File(PathManager.DIR_CACHE, asset.name)
                val downloadRequest = Request.Builder().url(asset.browserDownloadUrl).build()

                withContext(Dispatchers.IO) {
                    client.newCall(downloadRequest).execute().use { downloadResponse ->
                        if (!downloadResponse.isSuccessful) throw Exception("Failed to download driver")
                        val source = downloadResponse.body?.source() ?: throw Exception("Empty download body")
                        val totalSize = downloadResponse.body?.contentLength() ?: -1L

                        downloadFile.sink().buffer().use { sink ->
                            val buffer = ByteArray(8192)
                            var bytesRead: Long = 0
                            var read: Int
                            while (source.read(buffer).also { read = it } != -1) {
                                sink.write(buffer, 0, read)
                                bytesRead += read
                                if (totalSize > 0) {
                                    it.updateProgress(bytesRead.toFloat() / totalSize)
                                }
                            }
                        }
                    }
                }

                it.updateMessage(R.string.settings_renderer_turnip_extracting)
                it.updateProgress(-1f)

                val extractDir = File(PathManager.DIR_DRIVERS, asset.name.removeSuffix(".zip"))
                if (extractDir.exists()) {
                    extractDir.deleteRecursively()
                }
                extractDir.mkdirs()

                withContext(Dispatchers.IO) {
                    ZipFile(downloadFile).use { zip ->
                        zip.extractFromZip("", extractDir)
                    }
                }

                downloadFile.delete()

                withContext(Dispatchers.Main) {
                    DriverPluginManager.scanExternalDrivers(context)
                    Toast.makeText(context, R.string.settings_renderer_turnip_success, Toast.LENGTH_SHORT).show()
                }
            },
            onError = { th ->
                Logger.error(TAG, "Failed to download Turnip driver", th)
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Failed: ${th.message}", Toast.LENGTH_LONG).show()
                }
            }
        )
        TaskSystem.submitTask(task)
    }
    
    fun downloadUrl(context: Context, downloadUrl: String) {
        val task = Task.runTask(
            id = "download_turnip_driver",
            task = { it ->
                it.updateMessage(R.string.settings_renderer_turnip_downloading)
                
                val zipName = downloadUrl.substringAfterLast("/").substringBeforeLast("?")
                val canonicalZipName = zipName.substringBeforeLast(".zip")
                val currentDir = PathManager.DIR_DRIVERS
                val forbidden = listOf("/","\")
                
                if ((currentDir.listFiles()?.any{it.isDirectory && it.name == canonicalZipName} ?: false) && (canonicalZipName != "." && canonicalZipName != ".." && canonicalZipName.isNotEmpty() && forbidden.none{canonicalZipName.contains(it)})) {
                   Toast.makeText(context.applicationContext, "Driver already exists, skipping...", Toast.LENGTH_SHORT ).show()
                   return
                   }
                
                val downloadFile = File(PathManager.DIR_CACHE, zipName)
                val downloadRequest = Request.Builder().url(downloadUrl).build()
                
                withContext(Dispatchers.IO) {
                    client.newCall(downloadRequest).execute().use { downloadResponse ->
                        if (!downloadResponse.isSuccessful) throw Exception("Failed to download driver")
                        val source = downloadResponse.body?.source() ?: throw Exception("Empty download body")
                        val totalSize = downloadResponse.body?.contentLength() ?: -1L
                        
                        downloadFile.sink().buffer().use { sink ->
                            val buffer = ByteArray(8192)
                            var bytesRead: Long = 0
                            var read: Int
                            while (source.read(buffer).also { read = it } != -1) {
                                sink.write(buffer, 0, read)
                                bytesRead += read
                                if (totalSize > 0) {
                                    it.updateProgress(bytesRead.toFloat() / totalSize)
                                }
                            }
                        }
                    }
                }
                
                it.updateMessage(R.string.settings_renderer_turnip_extracting)
                it.updateProgress(-1f)
                
                val prefix = "unnamed-driver-"
                
                val safeDirName = if ( canonicalZipName.isEmpty() || canonicalZipName == "." || canonicalZipName == ".." ) { "$prefix${(currentDir.listFiles()?.map {it.name}?.filter {it.startsWith(prefix)}?.mapNotNull {it.substring(prefix.length).toIntOrNull() }?.maxOrNull() ?:0) +1}" } else {
                                                     canonicalZipName.replace(Regex("[/\\\\]+"), "_")}
                
                val extractDir = File(PathManager.DIR_DRIVERS, safeDirName)
                if (extractDir.exists()) {
                    extractDir.deleteRecursively()
                }
                extractDir.mkdirs()
                
                withContext(Dispatchers.IO) {
                    ZipFile(downloadFile).use { zip ->
                        if (!downloadFile.extension.equals("zip", ignoreCase = true)) {
                            throw IllegalStateException("Driver file is not a zip: ${downloadFile.name}")
                        } else { zip.extractFromZip("", extractDir) }
                    }
                }
                
                downloadFile.delete()
                
                withContext(Dispatchers.Main) {
                    DriverPluginManager.scanExternalDrivers(context)
                    Toast.makeText(context, R.string.settings_renderer_turnip_success, Toast.LENGTH_SHORT).show()
                }
             },
             onError = { th ->
                Logger.error(TAG, "Failed to download Turnip driver", th)
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Failed: ${th.message}", Toast.LENGTH_LONG).show()
                }
            }
         )
         TaskSystem.submitTask(task)
    }
            }
