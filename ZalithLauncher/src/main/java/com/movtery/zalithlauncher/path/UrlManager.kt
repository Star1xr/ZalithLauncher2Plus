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

package com.movtery.zalithlauncher.path

import com.movtery.zalithlauncher.BuildConfig
import com.movtery.zalithlauncher.BuildKeys
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.HttpRequestPipeline
import io.ktor.client.request.header
import io.ktor.http.HttpHeaders
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import java.util.concurrent.TimeUnit

val URL_USER_AGENT: String = "${BuildKeys.LAUNCHER_SHORT_NAME}/Android_${BuildConfig.VERSION_NAME}"
val TIME_OUT = TimeUnit.SECONDS.toMillis(30L)

const val HOST_CURSEFORGE_API = "api.curseforge.com"
const val HOST_CURSEFORGE_EDGE = "edge.forgecdn.net"

const val URL_MCMOD: String = "https://www.mcmod.cn/"
const val URL_MINECRAFT_VERSION_REPOS: String = "https://piston-meta.mojang.com/mc/game/version_manifest_v2.json"
const val URL_MINECRAFT_ASSETS_INDEX: String = "https://launchermeta.mojang.com/v1/packages"
const val URL_MINECRAFT_PURCHASE = "https://www.xbox.com/games/store/minecraft-java-bedrock-edition-for-pc/9nxp44l49shj"
const val URL_PROJECT: String = "https://github.com/johnrenonasuncion-Ramil/Zeryth-Launcher"
const val URL_RAMI1L: String = "https://github.com/johnrenonasuncion-Ramil/Zeryth-Launcher"
const val URL_ORIGINAL_PROJECT: String = "https://github.com/ZalithLauncher/ZalithLauncher2"
const val URL_STAR1XR: String = "https://github.com/Star1xr"
const val URL_PROJECT_INFO: String = "https://api.github.com/repos/Star1xr/ZalithLauncher2Plus/contents/v2"
const val URL_ORIGINAL_PROJECT_INFO: String = "https://api.github.com/repos/ZalithLauncher/ZalithLauncher2/contents/v2"
const val URL_PROJECT_RELEASES_LATEST: String = "https://api.github.com/repos/johnrenonasuncion-Ramil/Zeryth-Launcher/releases/latest"
const val URL_COMMUNITY: String = "https://github.com/johnrenonasuncion-Ramil/Zeryth-Launcher/graphs/contributors"
const val URL_WEBLATE: String = "https://hosted.weblate.org/projects/zalithlauncher2"
const val URL_SUPPORT: String = "https://ifdian.net/a/MovTery"
const val URL_ELY_BY_AUTH: String = "https://account.ely.by/api/authlib-injector/"
const val URL_EASYTIER: String = "https://easytier.cn/"

const val URL_GITHUB_RENDERER_PLUGINS = "https://github.com/ShirosakiMio/FCLRendererPlugin/releases/tag/Renderer"
const val URL_GITHUB_DRIVER_PLUGINS = "https://github.com/FCL-Team/FCLDriverPlugin/releases/tag/Turnip"
const val URL_GITHUB_NATIVE_LIB_PLUGINS = "https://github.com/ZalithLauncher/NativeLibPlugin/releases"

const val URL_CLOUD_RENDERER_PLUGINS = "https://www.123865.com/s/YLIUVv-hae0v"
const val URL_CLOUD_DRIVE_DRIVER_PLUGINS = "https://www.123865.com/s/YLIUVv-3ae0v"
const val URL_CLOUD_NATIVE_LIB_PLUGINS = "https://www.123865.com/s/YLIUVv-Hae0v"

val GLOBAL_JSON = Json {
    ignoreUnknownKeys = true
    explicitNulls = true
    coerceInputValues = true
}

val GLOBAL_CLIENT = HttpClient(CIO) {
    install(HttpTimeout) {
        requestTimeoutMillis = TIME_OUT
    }
    install(ContentNegotiation) {
        json(GLOBAL_JSON)
    }
    expectSuccess = true

    defaultRequest {
        header(HttpHeaders.UserAgent, URL_USER_AGENT)
    }
}.apply {
    requestPipeline.intercept(HttpRequestPipeline.State) {
        // æ£æ¥ host æ¯å¦ä¸º CurseForge
        // èªå¨æ·»å  CurseForge ç api å¯é¥
        val host = context.url.host
        if (host == HOST_CURSEFORGE_API || host == HOST_CURSEFORGE_EDGE) {
            val apiKey = BuildKeys.CURSEFORGE_API
            if (apiKey.isNotBlank()) {
                context.header("x-api-key", apiKey)
            }
        }
    }
}

/**
 * An [Interceptor] for CurseForge API requests.
 *
 * It automatically injects the `x-api-key` header when the request host matches
 * [HOST_CURSEFORGE_API] or [HOST_CURSEFORGE_EDGE], provided the API key is not blank.
 */
private val CURSEFORGE_INTERCEPTOR = Interceptor { chain ->
    val request = chain.request()
    val host = request.url.host
    if (host == HOST_CURSEFORGE_API || host == HOST_CURSEFORGE_EDGE) {
        val apiKey = BuildKeys.CURSEFORGE_API
        if (apiKey.isNotBlank()) {
            val newRequest = request.newBuilder()
                .header("x-api-key", apiKey)
                .build()
            return@Interceptor chain.proceed(newRequest)
        }
    }
    chain.proceed(request)
}

fun createRequestBuilder(url: String): Request.Builder {
    return createRequestBuilder(url, null)
}

fun createRequestBuilder(url: String, body: RequestBody?): Request.Builder {
    val request = Request.Builder().url(url).header("User-Agent", URL_USER_AGENT)
    body?.let{ request.post(it) }
    return request
}

fun createOkHttpClient(): OkHttpClient = createOkHttpClientBuilder().build()

/**
 * åå»ºä¸ä¸ªOkHttpClientï¼å¯èªå®ä¹ä¸äºåå®¹
 */
fun createOkHttpClientBuilder(action: (OkHttpClient.Builder) -> Unit = { }): OkHttpClient.Builder {
    return OkHttpClient.Builder()
        .callTimeout(TIME_OUT, TimeUnit.MILLISECONDS)
        .addInterceptor(CURSEFORGE_INTERCEPTOR)
        .apply(action)
}

/**
 * åå»ºç¨äºæä»¶ä¸è½½ç OkHttpClientã
 * ä¸æ®é API è°ç¨ä¸åï¼æä»¶ä¸è½½éè¦æ´é¿çè¶æ¶éç½®ï¼ä¸ä¸è®¾ callTimeout
 * ï¼å ä¸ºæä»¶å¤§å°å·®å¼å¾å¤§ï¼ä¸è½ç¨ä¸ä¸ªåºå®å¼éå¶æ´ä½ä¸è½½æ¶é´ï¼ã
 *
 * ä½¿ç¨ OkHttp æ¿ä»£ HttpURLConnection çä¸»è¦åå æ¯ï¼
 * OkHttp ä½¿ç¨èªå®ç°ç AsyncTimeout æºå¶ï¼æ¯ä¾èµæä½ç³»ç» socket è¶æ¶ç
 * HttpURLConnection å¨ Android ä¸æ´å å¯é ï¼è½ææé¿å"å¡ 0b/s"é®é¢ã
 */
val DOWNLOAD_OKHTTP_CLIENT: OkHttpClient by lazy {
    OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .addInterceptor(CURSEFORGE_INTERCEPTOR)
        .build()
        // æ³¨æï¼ä¸è®¾ç½® callTimeoutï¼å ä¸ºæä»¶å¤§å°å·®å¼æå¤§
        // åç¨å±ç withTimeout æä¾æ´ä½ååºä¿æ¤
}