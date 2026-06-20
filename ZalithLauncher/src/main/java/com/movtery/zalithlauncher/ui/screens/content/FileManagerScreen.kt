package com.movtery.zalithlauncher.ui.screens.content

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.movtery.zalithlauncher.R
import com.movtery.zalithlauncher.game.path.getGameHome
import com.movtery.zalithlauncher.ui.components.CardTitleLayout
import com.movtery.zalithlauncher.ui.components.IconTextButton
import com.movtery.zalithlauncher.ui.components.ScalingLabel
import com.movtery.zalithlauncher.ui.components.SimpleTextInputField
import com.movtery.zalithlauncher.ui.screens.content.versions.layouts.VersionChunkBackground
import com.movtery.zalithlauncher.ui.theme.itemColor
import com.movtery.zalithlauncher.ui.theme.onItemColor
import com.movtery.zalithlauncher.utils.animation.getAnimateTween
import java.io.File

@Composable
fun FileManagerScreen() {

    val rootDirectory = remember { File(getGameHome()) }
    val context = LocalContext.current

    var currentDirectory by remember { mutableStateOf(rootDirectory) }
    var refreshCounter by remember { mutableStateOf(0) }
    var selectedFile by remember { mutableStateOf<File?>(null) }
    var showRenameDialog by remember { mutableStateOf(false) }
    var renameText by remember { mutableStateOf("") }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showPropertiesDialog by remember { mutableStateOf(false) }
    var showCreateFolderDialog by remember { mutableStateOf(false) }
    var newFolderName by remember { mutableStateOf("") }
    var searchQuery by remember { mutableStateOf("") }
    var clipboardFile by remember { mutableStateOf<File?>(null) }
    var clipboardIsCut by remember { mutableStateOf(false) }

    fun importFile(uri: Uri) {
        val fileName = context.contentResolver
            .query(uri, null, null, null, null)
            ?.use { cursor ->
                val index = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                if (cursor.moveToFirst() && index >= 0) cursor.getString(index) else null
            } ?: "ImportedFile"
        val destination = File(currentDirectory, fileName)
        context.contentResolver.openInputStream(uri)?.use { input ->
            destination.outputStream().use { output -> input.copyTo(output) }
        }
    }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uris ->
        uris.forEach { importFile(it) }
        refreshCounter++
    }

    val files = remember(currentDirectory, refreshCounter) {
        currentDirectory.listFiles()
            ?.sortedWith(compareBy<File>({ !it.isDirectory }, { it.name.lowercase() }))
            ?: emptyList()
    }

    val displayedFiles = if (searchQuery.isBlank()) {
        files
    } else {
        files.filter { it.name.contains(searchQuery, ignoreCase = true) }
    }

    val modsFolder = File(rootDirectory, "mods")
    val resourcePacksFolder = File(rootDirectory, "resourcepacks")
    val screenshotsFolder = File(rootDirectory, "screenshots")

    val gameFolderSelected = currentDirectory.absolutePath.startsWith(rootDirectory.absolutePath)
    val modsSelected = currentDirectory.absolutePath.startsWith(modsFolder.absolutePath)
    val resourcePacksSelected = currentDirectory.absolutePath.startsWith(resourcePacksFolder.absolutePath)
    val screenshotsSelected = currentDirectory.absolutePath.startsWith(screenshotsFolder.absolutePath)

    Row(
        modifier = Modifier
            .fillMaxSize()
            .padding(all = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // ── Left sidebar ────────────────────────────────────────────────────
        VersionChunkBackground(
            modifier = Modifier
                .widthIn(min = 200.dp, max = 260.dp)
                .fillMaxHeight(),
            paddingValues = PaddingValues()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(vertical = 8.dp)
            ) {
                // Current Folder header
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 10.dp),
                    shape = MaterialTheme.shapes.large,
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_folder_outlined),
                            contentDescription = null,
                            modifier = Modifier.size(22.dp)
                        )
                        Column {
                            Text(
                                text = "Current Folder",
                                style = MaterialTheme.typography.labelSmall,
                                color = LocalContentColor.current.copy(alpha = 0.7f)
                            )
                            // Crossfade when the folder name changes
                            Crossfade(
                                targetState = currentDirectory.name.ifEmpty { "Game Folder" },
                                animationSpec = tween(200),
                                label = "SidebarFolderName"
                            ) { name ->
                                Text(
                                    text = name,
                                    style = MaterialTheme.typography.titleSmall,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(2.dp))

                // Nav items
                SidebarNavItem(
                    icon = R.drawable.ic_folder_outlined,
                    label = "Game Folder",
                    selected = gameFolderSelected,
                    onClick = { currentDirectory = rootDirectory }
                )
                SidebarNavItem(
                    icon = R.drawable.ic_extension_outlined,
                    label = "Mods",
                    selected = modsSelected,
                    onClick = {
                        modsFolder.takeIf(File::exists)?.let { currentDirectory = it }
                    }
                )
                SidebarNavItem(
                    icon = R.drawable.ic_folder_zip_outlined,
                    label = "Resource Packs",
                    selected = resourcePacksSelected,
                    onClick = {
                        resourcePacksFolder.takeIf(File::exists)?.let { currentDirectory = it }
                    }
                )
                SidebarNavItem(
                    icon = R.drawable.ic_image_outlined,
                    label = "Screenshots",
                    selected = screenshotsSelected,
                    onClick = {
                        screenshotsFolder.takeIf(File::exists)?.let { currentDirectory = it }
                    }
                )

                Spacer(modifier = Modifier.weight(1f))

                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 12.dp),
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Action buttons
                IconTextButton(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp),
                    onClick = {
                        newFolderName = ""
                        showCreateFolderDialog = true
                    },
                    painter = painterResource(id = R.drawable.ic_add_box_outlined),
                    text = "New Folder"
                )

                Spacer(modifier = Modifier.height(8.dp))

                IconTextButton(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp),
                    onClick = { importLauncher.launch(arrayOf("*/*")) },
                    painter = painterResource(id = R.drawable.ic_upload),
                    text = "Import Files"
                )

                Spacer(modifier = Modifier.height(8.dp))
            }
        }

        // ── Right main panel ────────────────────────────────────────────────
        VersionChunkBackground(
            modifier = Modifier.fillMaxSize(),
            paddingValues = PaddingValues()
        ) {
            Column(modifier = Modifier.fillMaxSize()) {

                // Header toolbar (matches ModsActionsHeader style)
                CardTitleLayout(modifier = Modifier.fillMaxWidth()) {
                    BoxWithConstraints(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp)
                            .padding(top = 4.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Search field (weight 1, like ModsActionsHeader)
                            SimpleTextInputField(
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(start = 4.dp),
                                value = searchQuery,
                                onValueChange = { searchQuery = it },
                                hint = {
                                    Text(
                                        text = "Search files",
                                        style = TextStyle(color = LocalContentColor.current).copy(fontSize = 12.sp)
                                    )
                                },
                                color = itemColor(),
                                contentColor = onItemColor(),
                                singleLine = true
                            )

                            Spacer(modifier = Modifier.width(6.dp))

                            // Paste / Move button (visible only when clipboard has a file)
                            if (clipboardFile != null) {
                                VerticalDivider(
                                    modifier = Modifier
                                        .fillMaxHeight()
                                        .padding(vertical = 12.dp),
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                                )
                                IconTextButton(
                                    onClick = {
                                        val source = clipboardFile ?: return@IconTextButton
                                        val destination = File(currentDirectory, source.name)
                                        if (clipboardIsCut) {
                                            source.renameTo(destination)
                                        } else {
                                            source.copyRecursively(destination, overwrite = true)
                                        }
                                        clipboardFile = null
                                        clipboardIsCut = false
                                        refreshCounter++
                                    },
                                    painter = painterResource(id = R.drawable.ic_file_copy_filled),
                                    text = if (clipboardIsCut) "Move Here" else "Paste Here"
                                )
                            }

                            VerticalDivider(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .padding(vertical = 12.dp),
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                            )

                            // Refresh button
                            IconButton(onClick = { refreshCounter++ }) {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_refresh),
                                    contentDescription = "Refresh"
                                )
                            }
                        }
                    }

                    // Breadcrumb row
                    val breadcrumbSegments = try {
                        currentDirectory.relativeTo(rootDirectory)
                            .invariantSeparatorsPath
                            .split("/")
                            .filter { it.isNotBlank() }
                    } catch (_: IllegalArgumentException) {
                        emptyList()
                    }

                    val isAtRoot = currentDirectory.absolutePath == rootDirectory.absolutePath

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp)
                            .padding(bottom = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Up one level button — slides in from the left when entering a subfolder
                        AnimatedVisibility(
                            visible = !isAtRoot,
                            enter = fadeIn() + slideInHorizontally(initialOffsetX = { -it }),
                            exit = fadeOut() + slideOutHorizontally(targetOffsetX = { -it })
                        ) {
                            IconButton(
                                onClick = {
                                    val parent = currentDirectory.parentFile
                                    if (parent != null) {
                                        currentDirectory = if (
                                            parent.absolutePath.startsWith(rootDirectory.absolutePath)
                                        ) parent else rootDirectory
                                    }
                                }
                            ) {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_arrow_back),
                                    contentDescription = "Go up one level",
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }

                        TextButton(onClick = { currentDirectory = rootDirectory }) {
                            Text(
                                text = "Game Folder",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                        // Animated breadcrumb trail — slides right when going deeper, left when going up
                        val previousDepth = remember { androidx.compose.runtime.mutableIntStateOf(0) }
                        val goingDeeper = breadcrumbSegments.size >= previousDepth.intValue
                        LaunchedEffect(breadcrumbSegments.size) {
                            previousDepth.intValue = breadcrumbSegments.size
                        }

                        AnimatedContent(
                            targetState = breadcrumbSegments,
                            transitionSpec = {
                                if (goingDeeper) {
                                    (fadeIn(tween(200)) + slideInHorizontally(tween(200)) { it / 3 })
                                        .togetherWith(fadeOut(tween(150)) + slideOutHorizontally(tween(150)) { -it / 3 })
                                } else {
                                    (fadeIn(tween(200)) + slideInHorizontally(tween(200)) { -it / 3 })
                                        .togetherWith(fadeOut(tween(150)) + slideOutHorizontally(tween(150)) { it / 3 })
                                }
                            },
                            label = "BreadcrumbTrail"
                        ) { segments ->
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                segments.forEachIndexed { index, segment ->
                                    val isLast = index == segments.lastIndex
                                    Text(
                                        text = " > ",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = LocalContentColor.current.copy(alpha = 0.4f)
                                    )
                                    val targetDirectory = File(
                                        rootDirectory,
                                        segments.take(index + 1).joinToString("/")
                                    )
                                    if (!isLast) {
                                        TextButton(onClick = { currentDirectory = targetDirectory }) {
                                            Text(
                                                text = segment,
                                                style = MaterialTheme.typography.bodySmall
                                            )
                                        }
                                    } else {
                                        Text(
                                            text = segment,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = LocalContentColor.current.copy(alpha = 0.75f)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // File list
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(all = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(displayedFiles, key = { it.absolutePath }) { file ->
                            FileItemLayout(
                                modifier = Modifier.fillMaxWidth(),
                                file = file,
                                onClick = {
                                    if (file.isDirectory) currentDirectory = file
                                },
                                onRename = {
                                    renameText = file.name
                                    selectedFile = file
                                    showRenameDialog = true
                                },
                                onCopy = {
                                    clipboardFile = file
                                    clipboardIsCut = false
                                },
                                onCut = {
                                    clipboardFile = file
                                    clipboardIsCut = true
                                },
                                onProperties = {
                                    selectedFile = file
                                    showPropertiesDialog = true
                                },
                                onDelete = {
                                    selectedFile = file
                                    showDeleteDialog = true
                                }
                            )
                        }
                    }

                    if (displayedFiles.isEmpty()) {
                        ScalingLabel(
                            text = if (searchQuery.isBlank()) "This folder is empty" else "No matching files"
                        )
                    }
                }
            }
        }
    }

    // ── Dialogs ─────────────────────────────────────────────────────────────

    if (showRenameDialog && selectedFile != null) {
        AlertDialog(
            onDismissRequest = { showRenameDialog = false },
            title = { Text("Rename") },
            text = {
                OutlinedTextField(
                    value = renameText,
                    onValueChange = { renameText = it },
                    label = { Text("New name") },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    val renamedFile = File(selectedFile!!.parentFile, renameText)
                    selectedFile!!.renameTo(renamedFile)
                    refreshCounter++
                    showRenameDialog = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showRenameDialog = false }) { Text("Cancel") }
            }
        )
    }

    if (showDeleteDialog && selectedFile != null) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete") },
            text = { Text("Delete \"${selectedFile!!.name}\"?\n\nThis cannot be undone.") },
            confirmButton = {
                TextButton(onClick = {
                    selectedFile!!.deleteRecursively()
                    refreshCounter++
                    showDeleteDialog = false
                }) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("Cancel") }
            }
        )
    }

    if (showPropertiesDialog && selectedFile != null) {
        AlertDialog(
            onDismissRequest = { showPropertiesDialog = false },
            title = { Text("Properties") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Name: ${selectedFile!!.name}")
                    Text("Type: ${if (selectedFile!!.isDirectory) "Folder" else "File"}")
                    Text("Size: ${selectedFile!!.length()} bytes")
                    Text("Path:\n${selectedFile!!.absolutePath}")
                }
            },
            confirmButton = {
                TextButton(onClick = { showPropertiesDialog = false }) { Text("OK") }
            }
        )
    }

    if (showCreateFolderDialog) {
        AlertDialog(
            onDismissRequest = { showCreateFolderDialog = false },
            title = { Text("Create Folder") },
            text = {
                OutlinedTextField(
                    value = newFolderName,
                    onValueChange = { newFolderName = it },
                    label = { Text("Folder Name") },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    val folderName = newFolderName.trim()
                    if (folderName.isNotEmpty() && !folderName.contains("/") && !folderName.contains("\\")) {
                        val newFolder = File(currentDirectory, folderName)
                        if (!newFolder.exists()) newFolder.mkdir()
                        refreshCounter++
                    }
                    showCreateFolderDialog = false
                }) { Text("Create") }
            },
            dismissButton = {
                TextButton(onClick = { showCreateFolderDialog = false }) { Text("Cancel") }
            }
        )
    }
}

// ── Sidebar nav item ───────────────────────────────────────────────────────

@Composable
private fun SidebarNavItem(
    icon: Int,
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    val bgColor by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.secondaryContainer
                      else androidx.compose.ui.graphics.Color.Transparent,
        animationSpec = tween(200),
        label = "NavItemBg"
    )
    val contentColor by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.onSecondaryContainer
                      else LocalContentColor.current,
        animationSpec = tween(200),
        label = "NavItemContent"
    )
    val indicatorWidth by animateDpAsState(
        targetValue = if (selected) 3.dp else 0.dp,
        animationSpec = tween(200),
        label = "NavItemIndicator"
    )

    Surface(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 2.dp),
        shape = MaterialTheme.shapes.medium,
        color = bgColor,
        contentColor = contentColor
    ) {
        Row(
            modifier = Modifier.padding(start = 4.dp, end = 12.dp, top = 10.dp, bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Animated left selection indicator bar — grows in width from 0 → 3dp
            Box(
                modifier = Modifier
                    .width(indicatorWidth)
                    .height(20.dp)
                    .background(
                        color = MaterialTheme.colorScheme.primary,
                        shape = RoundedCornerShape(topEnd = 4.dp, bottomEnd = 4.dp)
                    )
            )

            Spacer(modifier = Modifier.width(12.dp))

            Icon(
                painter = painterResource(id = icon),
                contentDescription = null,
                modifier = Modifier.size(20.dp)
            )

            Spacer(modifier = Modifier.width(10.dp))

            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

// ── File list item (matches ModItemLayout style) ───────────────────────────

@Composable
private fun FileItemLayout(
    modifier: Modifier = Modifier,
    file: File,
    onClick: () -> Unit,
    onRename: () -> Unit,
    onCopy: () -> Unit,
    onCut: () -> Unit,
    onProperties: () -> Unit,
    onDelete: () -> Unit,
    itemColor: androidx.compose.ui.graphics.Color = itemColor(),
    itemContentColor: androidx.compose.ui.graphics.Color = onItemColor()
) {
    val scale = remember { Animatable(initialValue = 0.95f) }
    LaunchedEffect(Unit) {
        scale.animateTo(targetValue = 1f, animationSpec = getAnimateTween())
    }

    var menuExpanded by remember(file) { mutableStateOf(false) }

    Surface(
        modifier = modifier.graphicsLayer(scaleX = scale.value, scaleY = scale.value),
        onClick = onClick,
        shape = MaterialTheme.shapes.large,
        color = itemColor,
        contentColor = itemContentColor
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // File / folder icon
            Icon(
                painter = painterResource(id = getFileIcon(file)),
                contentDescription = if (file.isDirectory) "Folder" else "${file.extension} file",
                modifier = Modifier.size(36.dp)
            )

            // Name + subtitle
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = file.name,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1
                )
                Text(
                    text = if (file.isDirectory) "Folder" else "${file.length()} bytes",
                    style = MaterialTheme.typography.bodySmall,
                    color = itemContentColor.copy(alpha = 0.6f),
                    maxLines = 1
                )
            }

            // More options menu
            Box(contentAlignment = Alignment.TopEnd) {
                IconButton(onClick = { menuExpanded = true }) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_more_vert),
                        contentDescription = "More options"
                    )
                }

                DropdownMenu(
                    expanded = menuExpanded,
                    onDismissRequest = { menuExpanded = false },
                    shape = MaterialTheme.shapes.large,
                    shadowElevation = 3.dp
                ) {
                    DropdownMenuItem(
                        leadingIcon = {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_edit_outlined),
                                contentDescription = null
                            )
                        },
                        text = { Text("Rename") },
                        onClick = {
                            menuExpanded = false
                            onRename()
                        }
                    )
                    DropdownMenuItem(
                        leadingIcon = {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_copy_all_outlined),
                                contentDescription = null
                            )
                        },
                        text = { Text("Copy") },
                        onClick = {
                            menuExpanded = false
                            onCopy()
                        }
                    )
                    DropdownMenuItem(
                        leadingIcon = {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_file_copy_filled),
                                contentDescription = null
                            )
                        },
                        text = { Text("Cut") },
                        onClick = {
                            menuExpanded = false
                            onCut()
                        }
                    )
                    DropdownMenuItem(
                        leadingIcon = {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_info_outlined),
                                contentDescription = null
                            )
                        },
                        text = { Text("Properties") },
                        onClick = {
                            menuExpanded = false
                            onProperties()
                        }
                    )
                    DropdownMenuItem(
                        leadingIcon = {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_delete_outlined),
                                contentDescription = null
                            )
                        },
                        text = { Text("Delete") },
                        onClick = {
                            menuExpanded = false
                            onDelete()
                        }
                    )
                }
            }
        }
    }
}

// ── File icon helper (unchanged) ───────────────────────────────────────────

private fun getFileIcon(file: File): Int {
    if (file.isDirectory) {
        return when (file.name.lowercase()) {
            "mods" -> R.drawable.ic_extension_outlined
            "resourcepacks" -> R.drawable.ic_folder_zip_outlined
            "screenshots" -> R.drawable.ic_image_outlined
            else -> R.drawable.ic_folder_outlined
        }
    }
    return when (file.extension.lowercase()) {
        "jar" -> when (file.parentFile?.name?.lowercase()) {
            "mods" -> R.drawable.ic_extension_outlined
            "libraries" -> R.drawable.ic_java
            else -> R.drawable.ic_java
        }
        "zip" -> R.drawable.ic_folder_zip_outlined
        "png", "jpg", "jpeg", "gif", "webp" -> R.drawable.ic_image_outlined
        "txt" -> R.drawable.ic_article_outlined
        "toml", "yml", "yaml", "cfg", "properties" -> R.drawable.ic_code
        "json" -> R.drawable.ic_code
        "java" -> R.drawable.ic_java
        "log" -> R.drawable.ic_terminal_outlined
        "dat" -> when (file.name.lowercase()) {
            "level.dat" -> R.drawable.ic_package_2_outlined
            else -> R.drawable.ic_description_outlined
        }
        else -> R.drawable.ic_description_outlined
    }
}
