package com.movtery.zalithlauncher.ui.screens.content

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.widget.Toast
import androidx.compose.foundation.Canvas as ComposeCanvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.NavigateNext
import androidx.compose.material.icons.filled.NavigateBefore
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt
import com.movtery.zalithlauncher.R
import com.movtery.zalithlauncher.game.account.labynet.LabyCapeApi
import com.movtery.zalithlauncher.game.account.labynet.OfficialCape
import com.movtery.zalithlauncher.game.account.wardrobe.AccountCapeCollection
import com.movtery.zalithlauncher.ui.base.BaseScreen
import com.movtery.zalithlauncher.ui.components.BackgroundCard
import com.movtery.zalithlauncher.ui.components.CardTitleLayout

import com.movtery.zalithlauncher.ui.screens.NormalNavKey
import com.movtery.zalithlauncher.viewmodel.ScreenBackStackViewModel
import io.ktor.client.HttpClient
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.header
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.io.File

@Composable
fun CapeGalleryScreen(
    key: NormalNavKey.CapeGallery,
    backStackViewModel: ScreenBackStackViewModel
) {
    val context = LocalContext.current
    val client = remember {
        HttpClient {
            install(ContentNegotiation) {
                json(Json {
                    ignoreUnknownKeys = true
                    isLenient = true
                })
            }
            defaultRequest {
                header("User-Agent", "ZalithLauncher/2.0")
            }
        }
    }
    val scope = rememberCoroutineScope()

    var capes by remember { mutableStateOf<List<OfficialCape>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var downloading by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        try {
            val result = withContext(Dispatchers.IO) {
                LabyCapeApi.fetchOfficialCapes(client)
            }
            capes = result.filter { !it.texture.isNullOrBlank() }
        } catch (e: Exception) {
            error = "${e::class.simpleName}: ${e.message ?: "Unknown"}"
        } finally {
            loading = false
        }
    }

    BaseScreen(
        screenKey = key,
        currentKey = backStackViewModel.mainScreen.currentKey
    ) {
        BackgroundCard(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            shape = MaterialTheme.shapes.extraLarge
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                CardTitleLayout {
                    Text(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                        text = stringResource(R.string.account_capes_labynet_title),
                        style = MaterialTheme.typography.titleMedium
                    )
                }

                when {
                    loading -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    }

                    error != null -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = stringResource(R.string.account_capes_labynet_failed, error ?: ""),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.error,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(24.dp)
                            )
                        }
                    }

                    capes.isEmpty() -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = stringResource(R.string.account_capes_labynet_no_capes),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(24.dp)
                            )
                        }
                    }

                    else -> {
                        LazyVerticalGrid(
                            columns = GridCells.Adaptive(minSize = 150.dp),
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(8.dp),
                            contentPadding = PaddingValues(8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(capes, key = { it.name }) { cape ->
                                OfficialCapeCard(
                                    cape = cape,
                                    isDownloading = downloading,
                                    onDownload = {
                                        val textureUrl = cape.texture ?: return@OfficialCapeCard
                                        downloading = true
                                        scope.launch(Dispatchers.IO) {
                                            try {
                                                val ext = "png"
                                                val tempFile = File.createTempFile("cape_", ".$ext")
                                                try {
                                                    LabyCapeApi.downloadCapeImage(client, textureUrl, tempFile)
                                                    AccountCapeCollection.addCape(
                                                        accountUUID = key.accountUUID,
                                                        textureFile = tempFile,
                                                        name = cape.alias.ifBlank { cape.name },
                                                        source = "Official",
                                                        ext = ext
                                                    )
                                                    withContext(Dispatchers.Main) {
                                                        Toast.makeText(
                                                            context,
                                                            context.getString(R.string.account_capes_labynet_downloaded, cape.alias.ifBlank { cape.name }),
                                                            Toast.LENGTH_SHORT
                                                        ).show()
                                                    }
                                                } finally {
                                                    tempFile.delete()
                                                }
                                            } catch (e: Exception) {
                                                withContext(Dispatchers.Main) {
                                                    Toast.makeText(
                                                        context,
                                                        context.getString(R.string.account_capes_labynet_download_failed, e.message ?: ""),
                                                        Toast.LENGTH_SHORT
                                                    ).show()
                                                }
                                            } finally {
                                                downloading = false
                                            }
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun OfficialCapeCard(
    cape: OfficialCape,
    isDownloading: Boolean,
    onDownload: () -> Unit
) {
    var capeBitmap by remember { mutableStateOf<Bitmap?>(null) }
    val density = LocalDensity.current
    val targetH = with(density) { 90.dp.toPx() }.roundToInt()

    LaunchedEffect(cape.texture) {
        capeBitmap = null
        if (cape.texture != null) {
            try {
                val bytes = withContext(Dispatchers.IO) {
                    val url = java.net.URL(cape.texture)
                    val connection = url.openConnection()
                    connection.setRequestProperty("User-Agent", "ZalithLauncher/2.0")
                    connection.connectTimeout = 5000
                    connection.getInputStream().readBytes()
                }
                val opts = BitmapFactory.Options().apply { inScaled = false }
                val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, opts)
                if (bitmap != null) {
                    val scaleFactor = bitmap.width / 64f
                    val start = (1 * scaleFactor).roundToInt()
                    val capeW = (10 * scaleFactor).roundToInt()
                    val capeH = (16 * scaleFactor).roundToInt()
                    val targetW = (targetH.toFloat() * capeW / capeH).roundToInt()
                    val scaled = Bitmap.createBitmap(targetW, targetH, Bitmap.Config.ARGB_8888)
                    Canvas(scaled).drawBitmap(
                        bitmap,
                        Rect(start, start, start + capeW, start + capeH),
                        RectF(0f, 0f, targetW.toFloat(), targetH.toFloat()),
                        Paint().apply { isFilterBitmap = true }
                    )
                    if (bitmap !== scaled) bitmap.recycle()
                    capeBitmap = scaled
                }
            } catch (_: Exception) { }
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(90.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                val bitmap = capeBitmap
                if (bitmap != null) {
                    ComposeCanvas(modifier = Modifier.fillMaxSize()) {
                        drawImage(
                            image = bitmap.asImageBitmap(),
                            dstSize = IntSize(size.width.toInt(), size.height.toInt()),
                            filterQuality = FilterQuality.High
                        )
                    }
                } else {
                    Text(
                        text = "?",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                    )
                }
            }

            Text(
                text = cape.alias.ifBlank { cape.name },
                style = MaterialTheme.typography.labelMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )

            Button(
                onClick = onDownload,
                enabled = !isDownloading && cape.texture != null,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (isDownloading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp
                    )
                    Spacer(Modifier.width(4.dp))
                } else {
                    Icon(
                        imageVector = Icons.Default.Download,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                }
                Text(
                    text = stringResource(R.string.generic_download),
                    style = MaterialTheme.typography.labelSmall
                )
            }
        }
    }
}
