package com.movtery.zalithlauncher.ui.screens.content.elements

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import kotlin.math.roundToInt
import androidx.compose.ui.window.DialogProperties
import androidx.compose.foundation.Canvas as ComposeCanvas
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.IntSize
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.platform.LocalContext
import com.movtery.zalithlauncher.R
import com.movtery.zalithlauncher.game.account.AccountsManager
import com.movtery.zalithlauncher.game.account.wardrobe.AccountCapeCollection
import com.movtery.zalithlauncher.game.account.wardrobe.CapeEntry

import com.movtery.zalithlauncher.ui.screens.content.settings.layouts.CardPosition
import com.movtery.zalithlauncher.ui.screens.content.settings.layouts.SettingsCard
import java.io.File
import java.io.FileOutputStream

@Composable
fun CapeSelectorDialog(
    accountUUID: String,
    onDismiss: () -> Unit,
    onCapeActivated: () -> Unit,
    onCapeDeleted: () -> Unit = {}
) {
    var manifest by remember(accountUUID) { mutableStateOf(AccountCapeCollection.loadManifest(accountUUID)) }
    val sortedCapes = remember(manifest) {
        manifest.capes.sortedByDescending { it.favorite }
    }
    var confirmDeleteId by remember { mutableStateOf<String?>(null) }
    val context = LocalContext.current
    var isImporting by remember { mutableStateOf(false) }

    val capePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        isImporting = true
        try {
            val inputStream = context.contentResolver.openInputStream(uri) ?: return@rememberLauncherForActivityResult
            val tempFile = File(context.cacheDir, "import_cape_${System.currentTimeMillis()}")
            FileOutputStream(tempFile).use { output ->
                inputStream.copyTo(output)
            }
            inputStream.close()
            val ext = when {
                uri.toString().endsWith(".webp", true) -> "webp"
                uri.toString().endsWith(".jpg", true) || uri.toString().endsWith(".jpeg", true) -> "jpg"
                else -> "png"
            }
            AccountCapeCollection.addCape(
                accountUUID = accountUUID,
                textureFile = tempFile,
                name = tempFile.nameWithoutExtension,
                source = "Imported",
                ext = ext
            )
            tempFile.delete()
            manifest = AccountCapeCollection.loadManifest(accountUUID)
            Toast.makeText(context, context.getString(R.string.account_capes_saved_toast), Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(context, context.getString(R.string.account_change_cape_failed_to_import, e.message ?: ""), Toast.LENGTH_SHORT).show()
        } finally {
            isImporting = false
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.7f)
                .fillMaxSize()
        ) {
            androidx.compose.material3.Card(
                modifier = Modifier.fillMaxSize(),
                shape = MaterialTheme.shapes.extraLarge,
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                ) {
                    Text(
                        text = stringResource(R.string.account_capes_select_title),
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    val hasActiveCape = manifest.activeCapeId != null

                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        item(key = "__no_cape__") {
                            CapeEntryCard(
                                accountUUID = accountUUID,
                                entry = null,
                                isActive = !hasActiveCape,
                                onActivate = {
                                    AccountCapeCollection.clearActiveCape(accountUUID)
                                    manifest = AccountCapeCollection.loadManifest(accountUUID)
                                    onCapeActivated()
                                },
                                onToggleFavorite = {},
                                onRename = {},
                                onDelete = {}
                            )
                        }

                        items(sortedCapes, key = { it.id }) { entry ->
                            CapeEntryCard(
                                accountUUID = accountUUID,
                                entry = entry,
                                isActive = entry.id == manifest.activeCapeId,
                                onActivate = {
                                    AccountCapeCollection.setActiveCape(accountUUID, entry.id)
                                    manifest = AccountCapeCollection.loadManifest(accountUUID)
                                    onCapeActivated()
                                },
                                onToggleFavorite = {
                                    AccountCapeCollection.toggleFavorite(accountUUID, entry.id)
                                    manifest = AccountCapeCollection.loadManifest(accountUUID)
                                },
                                onRename = { newName ->
                                    AccountCapeCollection.renameCape(accountUUID, entry.id, newName)
                                    manifest = AccountCapeCollection.loadManifest(accountUUID)
                                },
                                onDelete = {
                                    confirmDeleteId = entry.id
                                }
                            )
                        }

                        if (sortedCapes.isEmpty()) {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 32.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = stringResource(R.string.account_capes_no_capes),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                    )
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End)
                    ) {
                        Button(
                            enabled = !isImporting,
                            onClick = { capePicker.launch(arrayOf("image/png", "image/webp", "image/jpeg")) }
                        ) {
                            Text(stringResource(R.string.account_change_cape_upload))
                        }
                        Button(onClick = onDismiss) {
                            Text(stringResource(R.string.generic_close))
                        }
                    }
                }
            }
        }
    }

    confirmDeleteId?.let { id ->
        val entry = manifest.capes.find { it.id == id }
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { confirmDeleteId = null },
            title = { Text(stringResource(R.string.account_capes_delete_title)) },
            text = {
                Text(stringResource(R.string.account_capes_delete_message, entry?.name ?: ""))
            },
            confirmButton = {
                Button(onClick = {
                    AccountCapeCollection.removeCape(accountUUID, id)
                    manifest = AccountCapeCollection.loadManifest(accountUUID)
                    confirmDeleteId = null
                    onCapeDeleted()
                }) {
                    Text(stringResource(R.string.generic_delete))
                }
            },
            dismissButton = {
                androidx.compose.material3.TextButton(onClick = { confirmDeleteId = null }) {
                    Text(stringResource(R.string.generic_cancel))
                }
            }
        )
    }
}

@Composable
private fun CapeEntryCard(
    accountUUID: String,
    entry: CapeEntry?,
    isActive: Boolean,
    onActivate: () -> Unit,
    onToggleFavorite: () -> Unit,
    onRename: (String) -> Unit,
    onDelete: () -> Unit
) {
    val isNoCape = entry == null
    var editing by remember { mutableStateOf(false) }
    var editName by remember(entry?.name) { mutableStateOf(entry?.name ?: "") }

    SettingsCard(
        modifier = Modifier.fillMaxWidth(),
        position = CardPosition.Single,
        onClick = {
            if (!editing) onActivate()
        }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .width(80.dp)
                    .height(40.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                if (isNoCape) {
                    Text(
                        text = "✕",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                    )
                } else {
                    val capeFile = File(AccountCapeCollection.getCollectionDir(accountUUID), "${entry.id}.${entry.ext}")
                    val density = LocalDensity.current
                    val targetH = with(density) { 40.dp.toPx() }.roundToInt()
                    val capeBitmap = remember(capeFile, density) {
                        runCatching {
                            val opts = BitmapFactory.Options().apply { inScaled = false }
                            val bitmap = BitmapFactory.decodeFile(capeFile.absolutePath, opts) ?: return@runCatching null
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
                            scaled
                        }.getOrNull()
                    }
                    val bmp = capeBitmap
                    if (bmp != null) {
                        ComposeCanvas(modifier = Modifier.fillMaxSize()) {
                            drawImage(
                                image = bmp.asImageBitmap(),
                                dstSize = IntSize(size.width.toInt(), size.height.toInt()),
                                filterQuality = FilterQuality.High
                            )
                        }
                    }
                }
            }

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = if (isNoCape) stringResource(R.string.account_capes_none)
                        else entry!!.name,
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    if (!isNoCape) {
                        IconButton(
                            onClick = {
                                editName = entry.name
                                editing = true
                            },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = stringResource(R.string.generic_rename),
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                            )
                        }
                    }
                }

                if (!isNoCape && editing) {
                    OutlinedTextField(
                        value = editName,
                        onValueChange = { editName = it },
                        singleLine = true,
                        label = { Text(stringResource(R.string.account_capes_rename_hint)) },
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(
                            onDone = {
                                onRename(editName)
                                editing = false
                            }
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Text(
                    text = if (isActive) "● Active" else if (isNoCape) "" else "● ${entry.source}",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isActive) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                )
            }

            if (!isNoCape) {
                IconButton(onClick = onToggleFavorite) {
                    Icon(
                        imageVector = if (entry!!.favorite) Icons.Filled.Star else Icons.Outlined.StarBorder,
                        contentDescription = stringResource(
                            if (entry.favorite) R.string.account_capes_unfavorite
                            else R.string.account_capes_favorite
                        ),
                        tint = if (entry.favorite) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                }

                IconButton(onClick = onDelete) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = stringResource(R.string.generic_delete),
                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f)
                    )
                }
            }
        }
    }
}
