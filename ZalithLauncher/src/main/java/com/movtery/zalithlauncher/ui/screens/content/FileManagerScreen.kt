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

package com.movtery.zalithlauncher.ui.screens.content

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.movtery.zalithlauncher.R
import com.movtery.zalithlauncher.game.path.getGameHome
import com.movtery.zalithlauncher.ui.components.CardTitleLayout
import com.movtery.zalithlauncher.ui.components.IconTextButton
import com.movtery.zalithlauncher.ui.components.SimpleTextInputField
import com.movtery.zalithlauncher.ui.screens.content.elements.SortByDropdownMenu
import com.movtery.zalithlauncher.ui.screens.content.elements.SortByEnum
import com.movtery.zalithlauncher.ui.screens.content.versions.layouts.VersionChunkBackground
import com.movtery.zalithlauncher.ui.theme.itemColor
import com.movtery.zalithlauncher.ui.theme.onItemColor
import com.movtery.zalithlauncher.utils.animation.getAnimateTween
import java.io.File

// ---------------------------------------------------------------------------
// Sidebar width constants
// ---------------------------------------------------------------------------
private val SIDEBAR_EXPANDED_WIDTH = 180.dp
private val SIDEBAR_COLLAPSED_WIDTH = 64.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FileManagerScreen() {

    val rootDirectory = remember { File(getGameHome()) }
    val context = LocalContext.current

    // ── Navigation & directory state ─────────────────────────────────────
    var currentDirectory by remember { mutableStateOf(rootDirectory) }
    var refreshCounter by remember { mutableStateOf(0) }

    // ── Sidebar collapse state ───────────────────────────────────────────
    var sidebarExpanded by rememberSaveable { mutableStateOf(true) }
    val sidebarWidth by animateDpAsState(
        targetValue = if (sidebarExpanded) SIDEBAR_EXPANDED_WIDTH else SIDEBAR_COLLAPSED_WIDTH,
        animationSpec = tween(durationMillis = 280),
        label = "sidebarWidth"
    )

    // ── Go-To-Path dialog ────────────────────────────────────────────────
    var showGoToPathDialog by remember { mutableStateOf(false) }
    var goToPathText by remember { mutableStateOf("") }
    var goToPathError by remember { mutableStateOf<String?>(null) }

    // ── Single-file action dialogs ───────────────────────────────────────
    var selectedFile by remember { mutableStateOf<File?>(null) }
    var showRenameDialog by remember { mutableStateOf(false) }
    var renameText by remember { mutableStateOf("") }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showPropertiesDialog by remember { mutableStateOf(false) }
    var showCreateFolderDialog by remember { mutableStateOf(false) }
    var newFolderName by remember { mutableStateOf("") }

    // ── Search & sort ────────────────────────────────────────────────────
    var searchQuery by remember { mutableStateOf("") }
    var sortByEnum by remember { mutableStateOf(SortByEnum.FileName) }
    var isAscending by remember { mutableStateOf(true) }

    // ── Clipboard ────────────────────────────────────────────────────────
    var clipboardFile by remember { mutableStateOf<File?>(null) }
    var clipboardIsCut by remember { mutableStateOf(false) }

    // ── Multi-selection mode ─────────────────────────────────────────────
    var selectionMode by remember { mutableStateOf(false) }
    var selectedPaths by remember { mutableStateOf<Set<String>>(emptySet()) }

    // ── Known sidebar destinations ───────────────────────────────────────
    val modsFolder = remember(rootDirectory) { File(rootDirectory, "mods") }
    val resourcePacksFolder = remember(rootDirectory) { File(rootDirectory, "resourcepacks") }
    val screenshotsFolder = remember(rootDirectory) { File(rootDirectory, "screenshots") }

    // ── Derived selection states ─────────────────────────────────────────
    val inModsSubtree = currentDirectory.absolutePath
        .startsWith(modsFolder.absolutePath + File.separator) ||
        currentDirectory.absolutePath == modsFolder.absolutePath
    val inResourcePacksSubtree = currentDirectory.absolutePath
        .startsWith(resourcePacksFolder.absolutePath + File.separator) ||
        currentDirectory.absolutePath == resourcePacksFolder.absolutePath
    val inScreenshotsSubtree = currentDirectory.absolutePath
        .startsWith(screenshotsFolder.absolutePath + File.separator) ||
        currentDirectory.absolutePath == screenshotsFolder.absolutePath
    val inGameRoot = !inModsSubtree && !inResourcePacksSubtree && !inScreenshotsSubtree

    // ── File import launcher ─────────────────────────────────────────────
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

    // ── Sorted + filtered file list ──────────────────────────────────────
    val files = remember(currentDirectory, refreshCounter, sortByEnum, isAscending) {
        currentDirectory.listFiles()
            ?.sortedWith(Comparator { a, b ->
                val dirCompare = b.isDirectory.compareTo(a.isDirectory)
                if (dirCompare != 0) return@Comparator dirCompare
                val value = when (sortByEnum) {
                    SortByEnum.FileName -> a.name.lowercase().compareTo(b.name.lowercase())
                    SortByEnum.FileModifiedTime -> b.lastModified().compareTo(a.lastModified())
                    else -> a.name.lowercase().compareTo(b.name.lowercase())
                }
                if (isAscending) value else -value
            }) ?: emptyList()
    }

    val displayedFiles = if (searchQuery.isBlank()) files
    else files.filter { it.name.contains(searchQuery, ignoreCase = true) }

    // ── Root layout ──────────────────────────────────────────────────────
    Row(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {

        // ================================================================
        //  SIDEBAR
        // ================================================================
        Column(
            modifier = Modifier
                .width(sidebarWidth)
                .fillMaxHeight()
        ) {

            // ── Current Folder pill (interactive → Go To Path) ───────────
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(28.dp),
                elevation = CardDefaults.cardElevation(2.dp),
                onClick = {
                    goToPathText = currentDirectory.absolutePath
                    goToPathError = null
                    showGoToPathDialog = true
                }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_folder_outlined),
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    AnimatedVisibility(
                        visible = sidebarExpanded,
                        enter = fadeIn(tween(180)) + expandHorizontally(tween(260)),
                        exit = fadeOut(tween(120)) + shrinkHorizontally(tween(200))
                    ) {
                        Column(modifier = Modifier.padding(start = 8.dp)) {
                            Text(
                                text = "Current Folder",
                                style = MaterialTheme.typography.titleSmall,
                                maxLines = 1
                            )
                            Text(
                                text = currentDirectory.name.ifEmpty { "Game Folder" },
                                style = MaterialTheme.typography.bodySmall,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // ── Nav list ─────────────────────────────────────────────────
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                // Section: GAME — header only when expanded
                if (sidebarExpanded) {
                    item(key = "header_game") {
                        SidebarSectionHeader(title = "GAME")
                    }
                }
                item(key = "nav_game") {
                    SidebarNavItem(
                        icon = R.drawable.ic_folder_outlined,
                        label = "Game Folder",
                        selected = inGameRoot,
                        expanded = sidebarExpanded,
                        onClick = { currentDirectory = rootDirectory }
                    )
                }

                // Section: CONTENT — spacer + header only when expanded
                if (sidebarExpanded) {
                    item(key = "spacer_content") { Spacer(modifier = Modifier.height(6.dp)) }
                    item(key = "header_content") { SidebarSectionHeader(title = "CONTENT") }
                } else {
                    item(key = "spacer_content") { Spacer(modifier = Modifier.height(4.dp)) }
                }
                item(key = "nav_mods") {
                    SidebarNavItem(
                        icon = R.drawable.ic_extension_outlined,
                        label = "Mods",
                        selected = inModsSubtree,
                        expanded = sidebarExpanded,
                        onClick = {
                            if (!modsFolder.exists()) modsFolder.mkdirs()
                            currentDirectory = modsFolder
                        }
                    )
                }
                item(key = "nav_resourcepacks") {
                    SidebarNavItem(
                        icon = R.drawable.ic_folder_zip_outlined,
                        label = "Resource Packs",
                        selected = inResourcePacksSubtree,
                        expanded = sidebarExpanded,
                        onClick = {
                            if (!resourcePacksFolder.exists()) resourcePacksFolder.mkdirs()
                            currentDirectory = resourcePacksFolder
                        }
                    )
                }

                // Section: MEDIA — spacer + header only when expanded
                if (sidebarExpanded) {
                    item(key = "spacer_media") { Spacer(modifier = Modifier.height(6.dp)) }
                    item(key = "header_media") { SidebarSectionHeader(title = "MEDIA") }
                } else {
                    item(key = "spacer_media") { Spacer(modifier = Modifier.height(4.dp)) }
                }
                item(key = "nav_screenshots") {
                    SidebarNavItem(
                        icon = R.drawable.ic_image_outlined,
                        label = "Screenshots",
                        selected = inScreenshotsSubtree,
                        expanded = sidebarExpanded,
                        onClick = {
                            if (!screenshotsFolder.exists()) screenshotsFolder.mkdirs()
                            currentDirectory = screenshotsFolder
                        }
                    )
                }
            }

            // ── Footer divider ───────────────────────────────────────────
            HorizontalDivider(
                modifier = Modifier.padding(vertical = 8.dp),
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
            )

            // ── Sidebar footer (expanded = storage card, collapsed = icon)
            SidebarFooter(
                rootDirectory = rootDirectory,
                expanded = sidebarExpanded
            )

            Spacer(modifier = Modifier.height(4.dp))

            // ── Collapse / expand toggle ─────────────────────────────────
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = if (sidebarExpanded) Alignment.CenterEnd else Alignment.Center
            ) {
                IconButton(onClick = { sidebarExpanded = !sidebarExpanded }) {
                    Icon(
                        painter = painterResource(
                            if (sidebarExpanded) R.drawable.ic_menu_open else R.drawable.ic_menu
                        ),
                        contentDescription = if (sidebarExpanded) "Collapse sidebar"
                        else "Expand sidebar"
                    )
                }
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        // ================================================================
        //  RIGHT PANEL
        // ================================================================
        VersionChunkBackground(
            modifier = Modifier.fillMaxSize(),
            paddingValues = PaddingValues()
        ) {
            Column(modifier = Modifier.fillMaxSize()) {

                // ── Toolbar header ───────────────────────────────────────
                CardTitleLayout(modifier = Modifier.fillMaxWidth()) {
                    if (selectionMode) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(onClick = {
                                selectionMode = false
                                selectedPaths = emptySet()
                            }) {
                                Icon(
                                    painter = painterResource(R.drawable.ic_close),
                                    contentDescription = "Exit selection"
                                )
                            }
                            Text(
                                text = "${selectedPaths.size} selected",
                                style = MaterialTheme.typography.titleSmall,
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(start = 4.dp)
                            )
                            IconTextButton(
                                onClick = {
                                    selectedPaths = displayedFiles
                                        .map { it.absolutePath }
                                        .toSet()
                                },
                                painter = painterResource(R.drawable.ic_select_all),
                                text = "All"
                            )
                            IconTextButton(
                                onClick = { selectedPaths = emptySet() },
                                painter = painterResource(R.drawable.ic_deselect),
                                text = "None",
                                enabled = selectedPaths.isNotEmpty()
                            )
                        }
                    } else {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp)
                                .padding(top = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box {
                                var sortExpanded by remember { mutableStateOf(false) }
                                IconButton(onClick = { sortExpanded = !sortExpanded }) {
                                    Icon(
                                        painter = painterResource(R.drawable.ic_sort),
                                        contentDescription = "Sort"
                                    )
                                }
                                SortByDropdownMenu(
                                    expanded = sortExpanded,
                                    onClose = { sortExpanded = false },
                                    enums = listOf(
                                        SortByEnum.FileName,
                                        SortByEnum.FileModifiedTime
                                    ),
                                    currentEnum = sortByEnum,
                                    onEnumChanged = { sortByEnum = it },
                                    isAscending = isAscending,
                                    onToggleSortOrder = { isAscending = !isAscending }
                                )
                            }

                            SimpleTextInputField(
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(start = 4.dp),
                                value = searchQuery,
                                onValueChange = { searchQuery = it },
                                hint = {
                                    Text(
                                        text = "Search files",
                                        style = TextStyle(
                                            color = LocalContentColor.current,
                                            fontSize = 12.sp
                                        )
                                    )
                                },
                                color = itemColor(),
                                contentColor = onItemColor(),
                                singleLine = true
                            )

                            IconButton(onClick = {
                                selectionMode = true
                                selectedPaths = emptySet()
                            }) {
                                Icon(
                                    painter = painterResource(R.drawable.ic_select_all),
                                    contentDescription = "Selection mode"
                                )
                            }

                            if (clipboardFile != null) {
                                TextButton(onClick = {
                                    val source = clipboardFile ?: return@TextButton
                                    val destination = File(currentDirectory, source.name)
                                    if (clipboardIsCut) source.renameTo(destination)
                                    else source.copyRecursively(destination, overwrite = true)
                                    clipboardFile = null
                                    clipboardIsCut = false
                                    refreshCounter++
                                }) {
                                    Text(if (clipboardIsCut) "Move Here" else "Paste Here")
                                }
                            }

                            Spacer(modifier = Modifier.width(6.dp))

                            IconTextButton(
                                onClick = {
                                    newFolderName = ""
                                    showCreateFolderDialog = true
                                },
                                painter = painterResource(R.drawable.ic_folder_outlined),
                                text = "New Folder"
                            )

                            IconTextButton(
                                onClick = { importLauncher.launch(arrayOf("*/*")) },
                                painter = painterResource(R.drawable.ic_upload),
                                text = "Import"
                            )

                            IconButton(onClick = { refreshCounter++ }) {
                                Icon(
                                    painter = painterResource(R.drawable.ic_refresh),
                                    contentDescription = "Refresh"
                                )
                            }
                        }
                    }
                }

                // ── Breadcrumb navigation (compact) ──────────────────────
                val breadcrumbSegments = remember(currentDirectory, rootDirectory) {
                    try {
                        currentDirectory.relativeTo(rootDirectory)
                            .invariantSeparatorsPath
                            .split("/")
                            .filter { it.isNotBlank() }
                    } catch (_: IllegalArgumentException) {
                        emptyList()
                    }
                }

                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = { currentDirectory = rootDirectory },
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "Game Folder",
                            style = MaterialTheme.typography.labelMedium
                        )
                    }

                    breadcrumbSegments.forEachIndexed { index, segment ->
                        val isLast = index == breadcrumbSegments.lastIndex
                        Text(
                            text = " › ",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        )
                        if (!isLast) {
                            val target = File(
                                rootDirectory,
                                breadcrumbSegments.take(index + 1).joinToString("/")
                            )
                            TextButton(
                                onClick = { currentDirectory = target },
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = segment,
                                    style = MaterialTheme.typography.labelMedium
                                )
                            }
                        } else {
                            Text(
                                text = segment,
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.SemiBold
                                ),
                                modifier = Modifier.padding(horizontal = 4.dp)
                            )
                        }
                    }
                }

                // ── File list ─────────────────────────────────────────────
                Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(all = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        if (displayedFiles.isEmpty()) {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 48.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(
                                            painter = painterResource(R.drawable.ic_folder_outlined),
                                            contentDescription = null,
                                            modifier = Modifier.size(48.dp),
                                            tint = LocalContentColor.current
                                        )
                                        Text(
                                            text = if (searchQuery.isBlank()) "This folder is empty"
                                            else "No matching files",
                                            style = MaterialTheme.typography.titleSmall
                                        )
                                        Text(
                                            text = if (searchQuery.isBlank()) "Import files or create folders."
                                            else "Try a different search term.",
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                    }
                                }
                            }
                        } else {
                            items(
                                items = displayedFiles,
                                key = { it.absolutePath }
                            ) { file ->
                                val isChecked = file.absolutePath in selectedPaths
                                FileItemLayout(
                                    modifier = Modifier.fillMaxWidth(),
                                    file = file,
                                    selectionMode = selectionMode,
                                    isChecked = isChecked,
                                    onCheckedChange = { checked ->
                                        selectedPaths = if (checked)
                                            selectedPaths + file.absolutePath
                                        else
                                            selectedPaths - file.absolutePath
                                    },
                                    onClick = {
                                        if (selectionMode) {
                                            selectedPaths = if (isChecked)
                                                selectedPaths - file.absolutePath
                                            else
                                                selectedPaths + file.absolutePath
                                        } else {
                                            if (file.isDirectory) currentDirectory = file
                                        }
                                    },
                                    onLongClick = {
                                        if (!selectionMode) {
                                            selectionMode = true
                                            selectedPaths = setOf(file.absolutePath)
                                        }
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
                    }
                }
            }
        }
    }

    // ================================================================
    //  DIALOGS
    // ================================================================

    // ── Go To Path ──────────────────────────────────────────────────────
    if (showGoToPathDialog) {
        val focusRequester = remember { FocusRequester() }
        AlertDialog(
            onDismissRequest = { showGoToPathDialog = false },
            title = { Text("Go to Path") },
            text = {
                OutlinedTextField(
                    value = goToPathText,
                    onValueChange = {
                        goToPathText = it
                        goToPathError = null
                    },
                    label = { Text("Directory path") },
                    singleLine = true,
                    isError = goToPathError != null,
                    supportingText = goToPathError?.let { err -> { Text(err) } },
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester)
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    val target = File(goToPathText.trim())
                    when {
                        !target.exists() -> goToPathError = "Directory does not exist"
                        !target.isDirectory -> goToPathError = "Path is not a directory"
                        !target.canRead() -> goToPathError = "Cannot access this directory"
                        else -> {
                            currentDirectory = target
                            showGoToPathDialog = false
                        }
                    }
                }) { Text("Go") }
            },
            dismissButton = {
                TextButton(onClick = { showGoToPathDialog = false }) { Text("Cancel") }
            }
        )
        LaunchedEffect(Unit) { focusRequester.requestFocus() }
    }

    // ── Rename ───────────────────────────────────────────────────────────
    if (showRenameDialog) {
        AlertDialog(
            onDismissRequest = { showRenameDialog = false },
            title = { Text("Rename") },
            text = {
                OutlinedTextField(
                    value = renameText,
                    onValueChange = { renameText = it },
                    label = { Text("New name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    selectedFile?.let { file ->
                        file.renameTo(File(file.parent, renameText))
                    }
                    refreshCounter++
                    showRenameDialog = false
                }) { Text("Rename") }
            },
            dismissButton = {
                TextButton(onClick = { showRenameDialog = false }) { Text("Cancel") }
            }
        )
    }

    // ── Delete ───────────────────────────────────────────────────────────
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete") },
            text = { Text("Delete \"${selectedFile?.name}\"? This cannot be undone.") },
            confirmButton = {
                TextButton(onClick = {
                    selectedFile?.deleteRecursively()
                    refreshCounter++
                    showDeleteDialog = false
                }) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("Cancel") }
            }
        )
    }

    // ── Properties ───────────────────────────────────────────────────────
    if (showPropertiesDialog) {
        val file = selectedFile
        if (file != null) {
            AlertDialog(
                onDismissRequest = { showPropertiesDialog = false },
                title = { Text("Properties") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        PropertyRow("Name", file.name)
                        PropertyRow("Type", if (file.isDirectory) "Folder" else "File")
                        PropertyRow("Size", formatFileSize(file))
                        PropertyRow(
                            "Modified",
                            java.text.SimpleDateFormat(
                                "yyyy-MM-dd HH:mm",
                                java.util.Locale.getDefault()
                            ).format(java.util.Date(file.lastModified()))
                        )
                        PropertyRow("Path", file.absolutePath)
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showPropertiesDialog = false }) { Text("Close") }
                }
            )
        }
    }

    // ── Create Folder ────────────────────────────────────────────────────
    if (showCreateFolderDialog) {
        AlertDialog(
            onDismissRequest = { showCreateFolderDialog = false },
            title = { Text("New Folder") },
            text = {
                OutlinedTextField(
                    value = newFolderName,
                    onValueChange = { newFolderName = it },
                    label = { Text("Folder name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    File(currentDirectory, newFolderName).mkdirs()
                    refreshCounter++
                    showCreateFolderDialog = false
                }) { Text("Create") }
            },
            dismissButton = {
                TextButton(onClick = { showCreateFolderDialog = false }) { Text("Cancel") }
            }
        )
    }
}

// ============================================================
//  SIDEBAR SECTION HEADER
// ============================================================

@Composable
private fun SidebarSectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelSmall.copy(
            letterSpacing = 1.sp,
            fontWeight = FontWeight.Medium
        ),
        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f),
        modifier = Modifier.padding(start = 16.dp, top = 2.dp, bottom = 2.dp)
    )
}

// ============================================================
//  SIDEBAR NAV ITEM
// ============================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SidebarNavItem(
    icon: Int,
    label: String,
    selected: Boolean,
    expanded: Boolean,
    onClick: () -> Unit
) {
    val backgroundColor by animateColorAsState(
        targetValue = if (selected)
            MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
        else
            Color.Transparent,
        animationSpec = tween(durationMillis = 220),
        label = "navItemBg_$label"
    )
    val accentColor by animateColorAsState(
        targetValue = if (selected)
            MaterialTheme.colorScheme.primary
        else
            Color.Transparent,
        animationSpec = tween(durationMillis = 220),
        label = "navItemAccent_$label"
    )
    val contentColor = if (selected)
        MaterialTheme.colorScheme.primary
    else
        LocalContentColor.current

    val itemModifier = Modifier
        .fillMaxWidth()
        .clip(MaterialTheme.shapes.large)
        .background(backgroundColor)
        .combinedClickable(onClick = onClick)

    val itemContent: @Composable () -> Unit = {
        if (expanded) {
            // Expanded: indicator + icon + label in a Row
            Row(
                modifier = itemModifier.padding(vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left accent bar
                Box(
                    modifier = Modifier
                        .padding(start = 4.dp)
                        .width(3.dp)
                        .height(20.dp)
                        .clip(RoundedCornerShape(50))
                        .background(accentColor)
                )
                Spacer(modifier = Modifier.width(9.dp))
                Icon(
                    painter = painterResource(id = icon),
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = contentColor
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
                    ),
                    color = contentColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
            }
        } else {
            // Collapsed: icon centered, small accent on left edge
            Box(
                modifier = itemModifier.padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                // Thin left accent bar pinned to start
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .padding(start = 2.dp)
                        .width(3.dp)
                        .height(20.dp)
                        .clip(RoundedCornerShape(50))
                        .background(accentColor)
                )
                Icon(
                    painter = painterResource(id = icon),
                    contentDescription = label,
                    modifier = Modifier.size(20.dp),
                    tint = contentColor
                )
            }
        }
    }

    if (!expanded) {
        // Tooltip wraps the item in collapsed mode
        TooltipBox(
            positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
            tooltip = { PlainTooltip { Text(label) } },
            state = rememberTooltipState()
        ) {
            itemContent()
        }
    } else {
        itemContent()
    }
}

// ============================================================
//  SIDEBAR FOOTER — storage info card
// ============================================================

@Composable
private fun SidebarFooter(rootDirectory: File, expanded: Boolean) {
    // Compute these once per rootDirectory change (not per recomposition)
    val gameDirBytes = remember(rootDirectory) {
        try {
            rootDirectory.walkTopDown().filter { it.isFile }.sumOf { it.length() }
        } catch (_: Exception) {
            0L
        }
    }
    val partitionTotal = remember(rootDirectory) {
        try { rootDirectory.totalSpace.coerceAtLeast(1L) } catch (_: Exception) { 1L }
    }
    val partitionFree = remember(rootDirectory) {
        try { rootDirectory.usableSpace } catch (_: Exception) { 0L }
    }
    val partitionUsed = (partitionTotal - partitionFree).coerceAtLeast(0L)
    val partitionProgress = (partitionUsed.toFloat() / partitionTotal.toFloat()).coerceIn(0f, 1f)

    val gameDirSizeText = formatBytesShort(gameDirBytes)
    val partitionUsedText = formatBytesShort(partitionUsed)
    val partitionTotalText = formatBytesShort(partitionTotal)

    // Abbreviated path: keep last ~28 chars if too long
    val pathText = remember(rootDirectory) {
        val p = rootDirectory.absolutePath
        if (p.length > 28) "…${p.takeLast(26)}" else p
    }

    if (expanded) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 2.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // Path row
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_folder_outlined),
                    contentDescription = null,
                    modifier = Modifier.size(11.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                )
                Text(
                    text = pathText,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Game dir size row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Game data",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                )
                Text(
                    text = gameDirSizeText,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Medium
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }

            // Storage progress bar row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Storage",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                )
                Text(
                    text = "$partitionUsedText / $partitionTotalText",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                )
            }

            LinearProgressIndicator(
                progress = { partitionProgress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.dp)
                    .clip(RoundedCornerShape(50)),
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.65f),
                trackColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
            )
        }
    } else {
        // Collapsed: single centered storage icon
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 2.dp),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_description_outlined),
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
            )
        }
    }
}

// ============================================================
//  FILE ITEM
// ============================================================

@Composable
private fun FileItemLayout(
    modifier: Modifier = Modifier,
    file: File,
    selectionMode: Boolean,
    isChecked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onRename: () -> Unit,
    onCopy: () -> Unit,
    onCut: () -> Unit,
    onProperties: () -> Unit,
    onDelete: () -> Unit
) {
    val scale = remember { Animatable(initialValue = 0.95f) }
    LaunchedEffect(Unit) {
        scale.animateTo(targetValue = 1f, animationSpec = getAnimateTween())
    }

    // Use itemColor() / onItemColor() to match the rest of the project's card style
    Surface(
        modifier = modifier
            .graphicsLayer(scaleX = scale.value, scaleY = scale.value)
            .clip(MaterialTheme.shapes.large)
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            ),
        shape = MaterialTheme.shapes.large,
        color = itemColor(),
        contentColor = onItemColor()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Checkbox — animates in when selection mode activates
            AnimatedVisibility(
                visible = selectionMode,
                enter = fadeIn(tween(180)) + expandHorizontally(tween(220)),
                exit = fadeOut(tween(140)) + shrinkHorizontally(tween(180))
            ) {
                Checkbox(
                    checked = isChecked,
                    onCheckedChange = onCheckedChange,
                    modifier = Modifier.padding(end = 4.dp)
                )
            }

            Icon(
                painter = painterResource(getFileIcon(file)),
                contentDescription = null,
                modifier = Modifier.size(32.dp),
                tint = LocalContentColor.current
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = file.name,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = if (file.isDirectory) "Folder" else formatFileSize(file),
                    style = MaterialTheme.typography.bodySmall
                )
            }

            // Action buttons — hidden while in selection mode
            AnimatedVisibility(
                visible = !selectionMode,
                enter = fadeIn(tween(180)),
                exit = fadeOut(tween(140))
            ) {
                Row {
                    IconButton(onClick = onProperties) {
                        Icon(
                            painter = painterResource(R.drawable.ic_info_outlined),
                            contentDescription = "Properties"
                        )
                    }
                    Box {
                        var menuExpanded by remember { mutableStateOf(false) }
                        IconButton(onClick = { menuExpanded = true }) {
                            Icon(
                                painter = painterResource(R.drawable.ic_more_vert),
                                contentDescription = "More options"
                            )
                        }
                        DropdownMenu(
                            expanded = menuExpanded,
                            onDismissRequest = { menuExpanded = false },
                            shape = MaterialTheme.shapes.large
                        ) {
                            DropdownMenuItem(
                                leadingIcon = {
                                    Icon(painterResource(R.drawable.ic_edit_outlined), null)
                                },
                                text = { Text("Rename") },
                                onClick = { onRename(); menuExpanded = false }
                            )
                            DropdownMenuItem(
                                leadingIcon = {
                                    Icon(painterResource(R.drawable.ic_copy_all_outlined), null)
                                },
                                text = { Text("Copy") },
                                onClick = { onCopy(); menuExpanded = false }
                            )
                            DropdownMenuItem(
                                leadingIcon = {
                                    Icon(painterResource(R.drawable.ic_file_copy_filled), null)
                                },
                                text = { Text("Cut") },
                                onClick = { onCut(); menuExpanded = false }
                            )
                            DropdownMenuItem(
                                leadingIcon = {
                                    Icon(painterResource(R.drawable.ic_delete_outlined), null)
                                },
                                text = { Text("Delete") },
                                onClick = { onDelete(); menuExpanded = false }
                            )
                        }
                    }
                }
            }
        }
    }
}

// ============================================================
//  HELPERS
// ============================================================

@Composable
private fun PropertyRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "$label:",
            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
            modifier = Modifier.width(64.dp)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.weight(1f)
        )
    }
}

/** Size of a single file in bytes, formatted for display. */
private fun formatFileSize(file: File): String {
    val bytes = file.length()
    return formatBytesShort(bytes)
}

/** Compact human-readable byte count (e.g. "1.07 GB", "234 MB", "18 KB"). */
private fun formatBytesShort(bytes: Long): String = when {
    bytes < 1_024L -> "$bytes B"
    bytes < 1_024L * 1_024L -> "%.1f KB".format(bytes / 1_024.0)
    bytes < 1_024L * 1_024L * 1_024L -> "%.1f MB".format(bytes / (1_024.0 * 1_024.0))
    else -> "%.2f GB".format(bytes / (1_024.0 * 1_024.0 * 1_024.0))
}

// ============================================================
//  ICON RESOLVER — unchanged from original
// ============================================================
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
