/*
 * Zeryth Launcher
 * Copyright (C) 2026 Rami1l <johnrenonasuncion@gmail.com> and contributors
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
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
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
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

// ---------------------------------------------------------------------------
// Sidebar width
// ---------------------------------------------------------------------------
private val SIDEBAR_WIDTH = 180.dp

// ---------------------------------------------------------------------------
// Animation specs — typed and defined once to avoid per-frame allocations.
//
// IntOffset is used by slideIn/OutHorizontally; IntSize is used by
// expandVertically/shrinkVertically; Float is used by fade animations.
// ---------------------------------------------------------------------------
private val SidebarSlideEnterSpec: FiniteAnimationSpec<IntOffset> =
    tween(320, easing = FastOutSlowInEasing)
private val SidebarSlideExitSpec: FiniteAnimationSpec<IntOffset> =
    tween(280, easing = FastOutSlowInEasing)
private val SectionExpandSpec: FiniteAnimationSpec<IntSize> =
    tween(260, easing = FastOutSlowInEasing)
private val SectionShrinkSpec: FiniteAnimationSpec<IntSize> =
    tween(220, easing = FastOutSlowInEasing)
private val SectionFadeInSpec: FiniteAnimationSpec<Float> =
    tween(200, delayMillis = 50)
private val SectionFadeOutSpec: FiniteAnimationSpec<Float> =
    tween(160)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FileManagerScreen(
    initialPath: String? = null
) {

    val rootDirectory = remember { File(getGameHome()) }
    val context = LocalContext.current

    // ── Navigation & directory state ─────────────────────────────────────
    var currentDirectory by remember {
        mutableStateOf(
            initialPath?.let { File(it) }?.takeIf { it.exists() && it.isDirectory } ?: rootDirectory
        )
    }
    // When navigated to with a new initialPath (e.g. via Quick Actions for a
    // different version folder), reset the current directory so the user
    // actually lands in the requested folder instead of seeing a stale one.
    LaunchedEffect(initialPath) {
        val requested = initialPath?.let { File(it) }?.takeIf { it.exists() && it.isDirectory }
        if (requested != null) currentDirectory = requested
    }
    var refreshCounter by remember { mutableStateOf(0) }

    // ── Sidebar visibility ────────────────────────────────────────────────
    var sidebarVisible by rememberSaveable { mutableStateOf(true) }

    // ── Section collapse states — GAME starts expanded (default landing is
    //    Game Folder); CONTENT and MEDIA start collapsed. All states are
    //    persisted via rememberSaveable so they survive rotation/recreation.
    var gameSectionExpanded by rememberSaveable { mutableStateOf(true) }
    var contentSectionExpanded by rememberSaveable { mutableStateOf(false) }
    var mediaSectionExpanded by rememberSaveable { mutableStateOf(false) }

    // ── Go-To-Path dialog ────────────────────────────────────────────────
    var showGoToPathDialog by remember { mutableStateOf(false) }
    var goToPathText by remember { mutableStateOf("") }
    var goToPathError by remember { mutableStateOf<String?>(null) }

    // ── Dialogs ───────────────────────────────────────────────────────────
    // selectedFile is used exclusively for the rename dialog (single file).
    var selectedFile by remember { mutableStateOf<File?>(null) }
    var showRenameDialog by remember { mutableStateOf(false) }
    var renameText by remember { mutableStateOf("") }

    // Single-file delete (from per-item context menu)
    var fileToDelete by remember { mutableStateOf<File?>(null) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    // Bulk delete (from multi-selection action bar)
    var showBulkDeleteDialog by remember { mutableStateOf(false) }

    // Properties — shared by both single-file and multi-selection paths
    var propertiesFiles by remember { mutableStateOf<List<File>>(emptyList()) }
    var showPropertiesDialog by remember { mutableStateOf(false) }

    var showCreateFolderDialog by remember { mutableStateOf(false) }
    var newFolderName by remember { mutableStateOf("") }

    // ── Search & sort ────────────────────────────────────────────────────
    var searchQuery by remember { mutableStateOf("") }
    var sortByEnum by remember { mutableStateOf(SortByEnum.FileName) }
    var isAscending by remember { mutableStateOf(true) }

    // ── Clipboard — stores a list so multi-select cut/copy works ─────────
    var clipboardFiles by remember { mutableStateOf<List<File>>(emptyList()) }
    var clipboardIsCut by remember { mutableStateOf(false) }
    var showClipboardDialog by remember { mutableStateOf(false) }

    // ── Multi-selection ───────────────────────────────────────────────────
    var selectionMode by remember { mutableStateOf(false) }
    var selectedPaths by remember { mutableStateOf<Set<String>>(emptySet()) }

    // ── Known sidebar destinations ───────────────────────────────────────
    val modsFolder = remember(rootDirectory) { File(rootDirectory, "mods") }
    val resourcePacksFolder = remember(rootDirectory) { File(rootDirectory, "resourcepacks") }
    val screenshotsFolder = remember(rootDirectory) { File(rootDirectory, "screenshots") }

    // ── Derived selection states (pure string comparison, no IO) ─────────
    val inModsSubtree = remember(currentDirectory, modsFolder) {
        val base = modsFolder.absolutePath
        currentDirectory.absolutePath == base ||
            currentDirectory.absolutePath.startsWith(base + File.separator)
    }
    val inResourcePacksSubtree = remember(currentDirectory, resourcePacksFolder) {
        val base = resourcePacksFolder.absolutePath
        currentDirectory.absolutePath == base ||
            currentDirectory.absolutePath.startsWith(base + File.separator)
    }
    val inScreenshotsSubtree = remember(currentDirectory, screenshotsFolder) {
        val base = screenshotsFolder.absolutePath
        currentDirectory.absolutePath == base ||
            currentDirectory.absolutePath.startsWith(base + File.separator)
    }
    val inGameRoot = !inModsSubtree && !inResourcePacksSubtree && !inScreenshotsSubtree

    // ── File list — loaded asynchronously on IO thread ───────────────────
    var files by remember { mutableStateOf<List<File>>(emptyList()) }
    var isLoadingFiles by remember { mutableStateOf(true) }

    LaunchedEffect(currentDirectory, refreshCounter, sortByEnum, isAscending) {
        isLoadingFiles = true
        val result = withContext(Dispatchers.IO) {
            currentDirectory.listFiles()
                ?.sortedWith(Comparator { a, b ->
                    val dirFirst = b.isDirectory.compareTo(a.isDirectory)
                    if (dirFirst != 0) return@Comparator dirFirst
                    val cmp = when (sortByEnum) {
                        SortByEnum.FileName ->
                            a.name.lowercase().compareTo(b.name.lowercase())
                        SortByEnum.FileModifiedTime ->
                            b.lastModified().compareTo(a.lastModified())
                        else ->
                            a.name.lowercase().compareTo(b.name.lowercase())
                    }
                    if (isAscending) cmp else -cmp
                }) ?: emptyList()
        }
        files = result
        isLoadingFiles = false
    }

    val displayedFiles = remember(files, searchQuery) {
        if (searchQuery.isBlank()) files
        else files.filter { it.name.contains(searchQuery, ignoreCase = true) }
    }

    // Convenience: the actual File objects currently selected
    val selectedFiles = remember(selectedPaths, displayedFiles) {
        displayedFiles.filter { it.absolutePath in selectedPaths }
    }

    // ── File import launcher ─────────────────────────────────────────────
    fun importFile(uri: Uri) {
        val fileName = context.contentResolver
            .query(uri, null, null, null, null)
            ?.use { cursor ->
                val col = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                if (cursor.moveToFirst() && col >= 0) cursor.getString(col) else null
            } ?: "ImportedFile"
        val dest = File(currentDirectory, fileName)
        context.contentResolver.openInputStream(uri)?.use { inp ->
            dest.outputStream().use { out -> inp.copyTo(out) }
        }
    }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uris ->
        uris.forEach { importFile(it) }
        refreshCounter++
    }

    // ── Root layout: sidebar + main panel ────────────────────────────────
    Row(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {

        // ================================================================
        //  SIDEBAR
        //
        //  Layout stability note: the inner Row is given fillMaxHeight() so
        //  that the sidebar Column's fillMaxHeight() resolves against the
        //  outer Row's bounded height rather than its own intrinsic height.
        //  The nav sections live inside Box(weight(1f)) which absorbs all
        //  height changes from expandVertically/shrinkVertically. Those
        //  changes update the scroll max only — the Box itself never
        //  re-reports a different size to its parent, so the outer Row and
        //  the main panel are never measured again during section animations.
        // ================================================================
        AnimatedVisibility(
            visible = sidebarVisible,
            enter = slideInHorizontally(SidebarSlideEnterSpec) { -it } +
                fadeIn(tween(260, delayMillis = 80)),
            exit = fadeOut(tween(160)) +
                slideOutHorizontally(SidebarSlideExitSpec) { -it }
        ) {
            Row(modifier = Modifier.fillMaxHeight()) {
                Column(
                    modifier = Modifier
                        .width(SIDEBAR_WIDTH)
                        .fillMaxHeight()
                ) {

                    // ── Contextual sidebar pill ───────────────────────────
                    // • Empty clipboard → "Current Folder" (opens Go To Path)
                    // • Active clipboard → "Clipboard" (opens clipboard dialog)
                    // Crossfade crossfades content; the Card shell stays stable
                    // so there is no container size animation to coordinate.
                    val hasClipboard = clipboardFiles.isNotEmpty()
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(28.dp),
                        elevation = CardDefaults.cardElevation(2.dp),
                        onClick = {
                            if (hasClipboard) {
                                showClipboardDialog = true
                            } else {
                                goToPathText = currentDirectory.absolutePath
                                goToPathError = null
                                showGoToPathDialog = true
                            }
                        }
                    ) {
                        Crossfade(
                            targetState = hasClipboard,
                            label = "sidebar_pill_crossfade"
                        ) { isClipboardActive ->
                            Row(
                                modifier = Modifier.padding(
                                    horizontal = 14.dp, vertical = 12.dp
                                ),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (isClipboardActive) {
                                    Icon(
                                        painter = painterResource(
                                            if (clipboardIsCut) R.drawable.ic_file_copy_filled
                                            else R.drawable.ic_copy_all_outlined
                                        ),
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Column {
                                        Text(
                                            text = "Clipboard",
                                            style = MaterialTheme.typography.titleSmall,
                                            maxLines = 1,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        Text(
                                            text = buildClipboardLabel(
                                                clipboardFiles, clipboardIsCut
                                            ),
                                            style = MaterialTheme.typography.bodySmall,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                } else {
                                    Icon(
                                        painter = painterResource(R.drawable.ic_folder_outlined),
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Column {
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
                    }

                    Spacer(Modifier.height(12.dp))

                    // ── Collapsible nav sections ──────────────────────────
                    // Wrapped in a fixed-height Box (weight(1f)) so that
                    // height changes from section animations are absorbed
                    // here and never propagate to the parent Column or Row.
                    // The inner Column scrolls freely within the Box.
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {

                            // ── GAME ──────────────────────────────────────
                            CollapsibleSidebarSection(
                                title = "GAME",
                                expanded = gameSectionExpanded,
                                onToggle = { gameSectionExpanded = !gameSectionExpanded }
                            ) {
                                SidebarNavItem(
                                    icon = R.drawable.ic_folder_outlined,
                                    label = "Game Folder",
                                    selected = inGameRoot,
                                    onClick = { currentDirectory = rootDirectory }
                                )
                            }

                            Spacer(Modifier.height(4.dp))

                            // ── CONTENT ───────────────────────────────────
                            CollapsibleSidebarSection(
                                title = "CONTENT",
                                expanded = contentSectionExpanded,
                                onToggle = { contentSectionExpanded = !contentSectionExpanded }
                            ) {
                                SidebarNavItem(
                                    icon = R.drawable.ic_extension_outlined,
                                    label = "Mods",
                                    selected = inModsSubtree,
                                    onClick = {
                                        if (!modsFolder.exists()) modsFolder.mkdirs()
                                        currentDirectory = modsFolder
                                    }
                                )
                                SidebarNavItem(
                                    icon = R.drawable.ic_folder_zip_outlined,
                                    label = "Resource Packs",
                                    selected = inResourcePacksSubtree,
                                    onClick = {
                                        if (!resourcePacksFolder.exists())
                                            resourcePacksFolder.mkdirs()
                                        currentDirectory = resourcePacksFolder
                                    }
                                )
                            }

                            Spacer(Modifier.height(4.dp))

                            // ── MEDIA ─────────────────────────────────────
                            CollapsibleSidebarSection(
                                title = "MEDIA",
                                expanded = mediaSectionExpanded,
                                onToggle = { mediaSectionExpanded = !mediaSectionExpanded }
                            ) {
                                SidebarNavItem(
                                    icon = R.drawable.ic_image_outlined,
                                    label = "Screenshots",
                                    selected = inScreenshotsSubtree,
                                    onClick = {
                                        if (!screenshotsFolder.exists())
                                            screenshotsFolder.mkdirs()
                                        currentDirectory = screenshotsFolder
                                    }
                                )
                            }

                            Spacer(Modifier.height(4.dp))
                        }
                    }

                    // ── Footer divider ────────────────────────────────────
                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 8.dp),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                    )

                    // ── Storage footer ────────────────────────────────────
                    SidebarStorageFooter(rootDirectory = rootDirectory)

                    Spacer(Modifier.height(8.dp))
                }

                Spacer(Modifier.width(12.dp))
            }
        }

        // ================================================================
        //  MAIN PANEL
        // ================================================================
        VersionChunkBackground(
            modifier = Modifier.fillMaxSize(),
            paddingValues = PaddingValues()
        ) {
            Column(modifier = Modifier.fillMaxSize()) {

                // ── Toolbar ──────────────────────────────────────────────
                CardTitleLayout(modifier = Modifier.fillMaxWidth()) {
                    if (selectionMode) {

                        // ── SELECTION MODE action bar ─────────────────────
                        // Shows Cut, Copy, Delete, Rename (disabled when
                        // >1 item), Properties, Select All, Deselect All.
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 4.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Sidebar toggle
                            IconButton(onClick = { sidebarVisible = !sidebarVisible }) {
                                Icon(
                                    painter = painterResource(
                                        if (sidebarVisible) R.drawable.ic_menu_open
                                        else R.drawable.ic_menu
                                    ),
                                    contentDescription = if (sidebarVisible)
                                        "Hide sidebar" else "Show sidebar"
                                )
                            }
                            // Exit selection mode
                            IconButton(onClick = {
                                selectionMode = false
                                selectedPaths = emptySet()
                            }) {
                                Icon(
                                    painter = painterResource(R.drawable.ic_close),
                                    contentDescription = "Exit selection"
                                )
                            }
                            // Count badge
                            Text(
                                text = buildString {
                                    val n = selectedPaths.size
                                    append(n)
                                    append(if (n == 1) " item selected" else " items selected")
                                },
                                style = MaterialTheme.typography.titleSmall,
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(start = 2.dp),
                                maxLines = 1
                            )

                            // ── Cut ──────────────────────────────────────
                            val hasSelection = selectedPaths.isNotEmpty()
                            val singleSelection = selectedPaths.size == 1

                            IconButton(
                                onClick = {
                                    clipboardFiles = selectedFiles
                                    clipboardIsCut = true
                                    selectionMode = false
                                    selectedPaths = emptySet()
                                },
                                enabled = hasSelection
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.ic_file_copy_filled),
                                    contentDescription = "Cut",
                                    tint = if (hasSelection) LocalContentColor.current
                                    else LocalContentColor.current.copy(alpha = 0.38f)
                                )
                            }
                            // ── Copy ─────────────────────────────────────
                            IconButton(
                                onClick = {
                                    clipboardFiles = selectedFiles
                                    clipboardIsCut = false
                                    selectionMode = false
                                    selectedPaths = emptySet()
                                },
                                enabled = hasSelection
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.ic_copy_all_outlined),
                                    contentDescription = "Copy",
                                    tint = if (hasSelection) LocalContentColor.current
                                    else LocalContentColor.current.copy(alpha = 0.38f)
                                )
                            }
                            // ── Delete ────────────────────────────────────
                            IconButton(
                                onClick = { showBulkDeleteDialog = true },
                                enabled = hasSelection
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.ic_delete_outlined),
                                    contentDescription = "Delete",
                                    tint = if (hasSelection)
                                        MaterialTheme.colorScheme.error
                                    else LocalContentColor.current.copy(alpha = 0.38f)
                                )
                            }
                            // ── Rename (single selection only) ────────────
                            IconButton(
                                onClick = {
                                    val f = selectedFiles.firstOrNull() ?: return@IconButton
                                    selectedFile = f
                                    renameText = f.name
                                    showRenameDialog = true
                                },
                                enabled = singleSelection
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.ic_edit_outlined),
                                    contentDescription = "Rename",
                                    tint = if (singleSelection) LocalContentColor.current
                                    else LocalContentColor.current.copy(alpha = 0.38f)
                                )
                            }
                            // ── Properties ────────────────────────────────
                            IconButton(
                                onClick = {
                                    propertiesFiles = selectedFiles
                                    showPropertiesDialog = true
                                },
                                enabled = hasSelection
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.ic_info_outlined),
                                    contentDescription = "Properties",
                                    tint = if (hasSelection) LocalContentColor.current
                                    else LocalContentColor.current.copy(alpha = 0.38f)
                                )
                            }
                            // ── Select All ────────────────────────────────
                            IconButton(onClick = {
                                selectedPaths = displayedFiles.map { it.absolutePath }.toSet()
                            }) {
                                Icon(
                                    painter = painterResource(R.drawable.ic_select_all),
                                    contentDescription = "Select all"
                                )
                            }
                            // ── Deselect All ──────────────────────────────
                            IconButton(
                                onClick = { selectedPaths = emptySet() },
                                enabled = hasSelection
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.ic_deselect),
                                    contentDescription = "Deselect all",
                                    tint = if (hasSelection) LocalContentColor.current
                                    else LocalContentColor.current.copy(alpha = 0.38f)
                                )
                            }
                        }

                    } else {

                        // ── NORMAL MODE toolbar ───────────────────────────
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp)
                                .padding(top = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Sidebar toggle
                            IconButton(onClick = { sidebarVisible = !sidebarVisible }) {
                                Icon(
                                    painter = painterResource(
                                        if (sidebarVisible) R.drawable.ic_menu_open
                                        else R.drawable.ic_menu
                                    ),
                                    contentDescription = if (sidebarVisible)
                                        "Hide sidebar" else "Show sidebar"
                                )
                            }

                            // Sort
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

                            // Search
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

                            // Enter selection mode
                            IconButton(onClick = {
                                selectionMode = true
                                selectedPaths = emptySet()
                            }) {
                                Icon(
                                    painter = painterResource(R.drawable.ic_select_all),
                                    contentDescription = "Selection mode"
                                )
                            }

                            // Paste (shown when clipboard is non-empty)
                            if (clipboardFiles.isNotEmpty()) {
                                TextButton(onClick = {
                                    clipboardFiles.forEach { source ->
                                        val dest = File(currentDirectory, source.name)
                                        if (clipboardIsCut) source.renameTo(dest)
                                        else source.copyRecursively(dest, overwrite = true)
                                    }
                                    if (clipboardIsCut) clipboardFiles = emptyList()
                                    clipboardIsCut = false
                                    refreshCounter++
                                }) {
                                    Text(if (clipboardIsCut) "Move Here" else "Paste Here")
                                }
                            }

                            Spacer(Modifier.width(6.dp))

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

                // ── Breadcrumb row with Up One Folder button ─────────────
                // Layout: [Up btn (animated)] [← scrollable trail →]
                //
                // The Up button is pinned outside the scroll container so it
                // is always accessible. Using ic_arrow_back rotated +90° in
                // Compose's math-convention rotationZ (positive = CCW) gives ↑.
                // The scrollable trail fills the remaining space; auto-scroll
                // drives the current folder into view on every navigation.
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

                val atRoot = remember(currentDirectory, rootDirectory) {
                    currentDirectory.absolutePath == rootDirectory.absolutePath
                }

                val breadcrumbScrollState = rememberScrollState()
                LaunchedEffect(currentDirectory) {
                    breadcrumbScrollState.animateScrollTo(Int.MAX_VALUE)
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Up One Folder — slides in/out; never visible at root
                    AnimatedVisibility(
                        visible = !atRoot,
                        enter = fadeIn(tween(180)) + expandHorizontally(
                            animationSpec = tween(200, easing = FastOutSlowInEasing),
                            expandFrom = Alignment.Start
                        ),
                        exit = fadeOut(tween(140)) + shrinkHorizontally(
                            animationSpec = tween(180, easing = FastOutSlowInEasing),
                            shrinkTowards = Alignment.Start
                        )
                    ) {
                        IconButton(
                            onClick = {
                                val parent = currentDirectory.parentFile
                                if (parent != null) currentDirectory = parent
                            },
                            modifier = Modifier
                                .padding(start = 4.dp)
                                .size(36.dp)
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.ic_arrow_back),
                                contentDescription = "Up one folder",
                                modifier = Modifier
                                    .size(18.dp)
                                    .graphicsLayer { rotationZ = 90f }
                            )
                        }
                    }

                    // Horizontally scrollable trail — fills remaining width
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .horizontalScroll(breadcrumbScrollState)
                            .padding(start = 4.dp, end = 12.dp, top = 2.dp, bottom = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(
                            onClick = { currentDirectory = rootDirectory },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "Game Folder",
                                style = MaterialTheme.typography.labelMedium,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                        breadcrumbSegments.forEachIndexed { index, segment ->
                            val isLast = index == breadcrumbSegments.lastIndex
                            Text(
                                text = " › ",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                maxLines = 1,
                                softWrap = false
                            )
                            if (!isLast) {
                                val target = File(
                                    rootDirectory,
                                    breadcrumbSegments.take(index + 1).joinToString("/")
                                )
                                TextButton(
                                    onClick = { currentDirectory = target },
                                    contentPadding = PaddingValues(
                                        horizontal = 8.dp, vertical = 2.dp
                                    )
                                ) {
                                    Text(
                                        text = segment,
                                        style = MaterialTheme.typography.labelMedium,
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                }
                            } else {
                                Text(
                                    text = segment,
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = FontWeight.SemiBold
                                    ),
                                    modifier = Modifier.padding(horizontal = 4.dp),
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                        }
                    }
                }

                // ── File list ────────────────────────────────────────────
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    if (isLoadingFiles) {
                        CircularProgressIndicator(modifier = Modifier.size(36.dp))
                    } else if (displayedFiles.isEmpty()) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.ic_folder_outlined),
                                contentDescription = null,
                                modifier = Modifier.size(48.dp),
                                tint = LocalContentColor.current.copy(alpha = 0.35f)
                            )
                            Text(
                                text = if (searchQuery.isBlank()) "This folder is empty"
                                else "No matching files",
                                style = MaterialTheme.typography.titleSmall
                            )
                            Text(
                                text = if (searchQuery.isBlank())
                                    "Import files or create folders."
                                else "Try a different search term.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                            )
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(all = 12.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
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
                                        clipboardFiles = listOf(file)
                                        clipboardIsCut = false
                                    },
                                    onCut = {
                                        clipboardFiles = listOf(file)
                                        clipboardIsCut = true
                                    },
                                    onProperties = {
                                        propertiesFiles = listOf(file)
                                        showPropertiesDialog = true
                                    },
                                    onDelete = {
                                        fileToDelete = file
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

    // ── Go To Path ───────────────────────────────────────────────────────
    if (showGoToPathDialog) {
        val focusRequester = remember { FocusRequester() }
        AlertDialog(
            onDismissRequest = { showGoToPathDialog = false },
            title = { Text("Go to Path") },
            text = {
                OutlinedTextField(
                    value = goToPathText,
                    onValueChange = { goToPathText = it; goToPathError = null },
                    label = { Text("Directory path") },
                    singleLine = true,
                    isError = goToPathError != null,
                    supportingText = goToPathError?.let { err -> { Text(err) } },
                    modifier = Modifier.fillMaxWidth().focusRequester(focusRequester)
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    val target = File(goToPathText.trim())
                    when {
                        !target.exists() -> goToPathError = "Directory does not exist"
                        !target.isDirectory -> goToPathError = "Path is not a directory"
                        !target.canRead() -> goToPathError = "Cannot access this directory"
                        else -> { currentDirectory = target; showGoToPathDialog = false }
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
                    selectedFile?.renameTo(File(selectedFile!!.parent, renameText))
                    refreshCounter++
                    showRenameDialog = false
                }) { Text("Rename") }
            },
            dismissButton = {
                TextButton(onClick = { showRenameDialog = false }) { Text("Cancel") }
            }
        )
    }

    // ── Single-file delete (from per-item context menu) ──────────────────
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete") },
            text = { Text("Delete \"${fileToDelete?.name}\"? This cannot be undone.") },
            confirmButton = {
                TextButton(onClick = {
                    fileToDelete?.deleteRecursively()
                    fileToDelete = null
                    refreshCounter++
                    showDeleteDialog = false
                }) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("Cancel") }
            }
        )
    }

    // ── Bulk delete (from multi-selection action bar) ────────────────────
    if (showBulkDeleteDialog) {
        val count = selectedPaths.size
        AlertDialog(
            onDismissRequest = { showBulkDeleteDialog = false },
            title = { Text("Delete items") },
            text = {
                Text(
                    "Permanently delete $count ${if (count == 1) "item" else "items"}? " +
                        "This cannot be undone."
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    selectedFiles.forEach { it.deleteRecursively() }
                    selectionMode = false
                    selectedPaths = emptySet()
                    refreshCounter++
                    showBulkDeleteDialog = false
                }) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showBulkDeleteDialog = false }) { Text("Cancel") }
            }
        )
    }

    // ── Properties ───────────────────────────────────────────────────────
    if (showPropertiesDialog && propertiesFiles.isNotEmpty()) {
        PropertiesDialog(
            files = propertiesFiles,
            onDismiss = { showPropertiesDialog = false }
        )
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

    // ── Clipboard ─────────────────────────────────────────────────────────
    // Shows operation type, item list (up to 5), destination, and actions:
    //   Paste Here / Clear Clipboard / Cancel
    if (showClipboardDialog && clipboardFiles.isNotEmpty()) {
        AlertDialog(
            onDismissRequest = { showClipboardDialog = false },
            icon = {
                Icon(
                    painter = painterResource(
                        if (clipboardIsCut) R.drawable.ic_file_copy_filled
                        else R.drawable.ic_copy_all_outlined
                    ),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            },
            title = {
                Text(if (clipboardIsCut) "Cut — pending move" else "Copy — pending paste")
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = buildClipboardLabel(clipboardFiles, clipboardIsCut),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(4.dp))
                    // Preview — up to 5 items
                    clipboardFiles.take(5).forEach { file ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                painter = painterResource(getFileIcon(file)),
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = LocalContentColor.current.copy(alpha = 0.7f)
                            )
                            Text(
                                text = file.name,
                                style = MaterialTheme.typography.bodySmall,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                    if (clipboardFiles.size > 5) {
                        Text(
                            text = "… and ${clipboardFiles.size - 5} more",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = "Destination: ${
                            currentDirectory.name.ifEmpty { "Game Folder" }
                        }",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    clipboardFiles.forEach { source ->
                        val dest = File(currentDirectory, source.name)
                        if (clipboardIsCut) source.renameTo(dest)
                        else source.copyRecursively(dest, overwrite = true)
                    }
                    if (clipboardIsCut) clipboardFiles = emptyList()
                    clipboardIsCut = false
                    refreshCounter++
                    showClipboardDialog = false
                }) {
                    Text("Paste Here")
                }
            },
            dismissButton = {
                Row {
                    TextButton(onClick = {
                        clipboardFiles = emptyList()
                        clipboardIsCut = false
                        showClipboardDialog = false
                    }) { Text("Clear") }
                    TextButton(onClick = { showClipboardDialog = false }) { Text("Cancel") }
                }
            }
        )
    }
}

// ============================================================
//  COLLAPSIBLE SIDEBAR SECTION
//
//  The chevron reuses ic_arrow_back (points ←) and rotates it:
//    • 180° → points right: section is collapsed
//    •  -90° → points down: section is expanded
//
//  expandVertically / shrinkVertically animate height while the
//  parent Box(weight(1f)) absorbs the height delta — so zero
//  layout changes escape to the outer Row or main panel.
// ============================================================

@Composable
private fun CollapsibleSidebarSection(
    title: String,
    expanded: Boolean,
    onToggle: () -> Unit,
    content: @Composable () -> Unit
) {
    Column {
        // Header row — always visible; click anywhere to toggle
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(MaterialTheme.shapes.medium)
                .clickable(onClick = onToggle)
                .padding(start = 16.dp, end = 8.dp, top = 5.dp, bottom = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall.copy(
                    letterSpacing = 1.sp,
                    fontWeight = FontWeight.Medium
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f)
            )

            // ic_arrow_back rotated: right = collapsed, down = expanded
            val chevronRotation by animateFloatAsState(
                targetValue = if (expanded) -90f else 180f,
                animationSpec = tween(240, easing = FastOutSlowInEasing),
                label = "chevron_$title"
            )
            Icon(
                painter = painterResource(R.drawable.ic_arrow_back),
                contentDescription = if (expanded) "Collapse $title" else "Expand $title",
                modifier = Modifier
                    .size(14.dp)
                    .graphicsLayer { rotationZ = chevronRotation },
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f)
            )
        }

        // Animated items — height-animates, fade-animates; no outer layout impact
        AnimatedVisibility(
            visible = expanded,
            enter = expandVertically(
                animationSpec = SectionExpandSpec,
                expandFrom = Alignment.Top
            ) + fadeIn(SectionFadeInSpec),
            exit = shrinkVertically(
                animationSpec = SectionShrinkSpec,
                shrinkTowards = Alignment.Top
            ) + fadeOut(SectionFadeOutSpec)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                content()
            }
        }
    }
}

// ============================================================
//  SIDEBAR NAV ITEM
// ============================================================

@Composable
private fun SidebarNavItem(
    icon: Int,
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    val backgroundColor by animateColorAsState(
        targetValue = if (selected)
            MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
        else Color.Transparent,
        animationSpec = tween(220),
        label = "navBg_$label"
    )
    val accentColor by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.primary else Color.Transparent,
        animationSpec = tween(220),
        label = "navAccent_$label"
    )
    val contentColor =
        if (selected) MaterialTheme.colorScheme.primary else LocalContentColor.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.large)
            .background(backgroundColor)
            .combinedClickable(onClick = onClick)
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .padding(start = 4.dp)
                .width(3.dp)
                .height(20.dp)
                .clip(RoundedCornerShape(50))
                .background(accentColor)
        )
        Spacer(Modifier.width(9.dp))
        Icon(
            painter = painterResource(id = icon),
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            tint = contentColor
        )
        Spacer(Modifier.width(10.dp))
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
}

// ============================================================
//  SIDEBAR STORAGE FOOTER
//  All heavy IO (walkTopDown, totalSpace, usableSpace) is on
//  Dispatchers.IO so the initial render is never blocked.
// ============================================================

@Composable
private fun SidebarStorageFooter(rootDirectory: File) {
    var gameDirBytes by remember(rootDirectory) { mutableLongStateOf(0L) }
    var partitionTotal by remember(rootDirectory) { mutableLongStateOf(1L) }
    var partitionFree by remember(rootDirectory) { mutableLongStateOf(0L) }

    LaunchedEffect(rootDirectory) {
        withContext(Dispatchers.IO) {
            gameDirBytes = try {
                rootDirectory.walkTopDown().filter { it.isFile }.sumOf { it.length() }
            } catch (_: Exception) { 0L }
            partitionTotal = try {
                rootDirectory.totalSpace.coerceAtLeast(1L)
            } catch (_: Exception) { 1L }
            partitionFree = try {
                rootDirectory.usableSpace
            } catch (_: Exception) { 0L }
        }
    }

    val partitionUsed = (partitionTotal - partitionFree).coerceAtLeast(0L)
    val partitionProgress =
        (partitionUsed.toFloat() / partitionTotal.toFloat()).coerceIn(0f, 1f)

    val pathText = remember(rootDirectory) {
        val p = rootDirectory.absolutePath
        if (p.length > 28) "…${p.takeLast(26)}" else p
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 2.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
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
                text = formatBytesShort(gameDirBytes),
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Medium
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
        }

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
                text = "${formatBytesShort(partitionUsed)} / ${formatBytesShort(partitionTotal)}",
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

    Surface(
        modifier = modifier
            .graphicsLayer(scaleX = scale.value, scaleY = scale.value)
            .clip(MaterialTheme.shapes.large)
            .combinedClickable(onClick = onClick, onLongClick = onLongClick),
        shape = MaterialTheme.shapes.large,
        color = itemColor(),
        contentColor = onItemColor()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
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

            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = file.name,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = if (file.isDirectory) "Folder"
                    else formatBytesShort(file.length()),
                    style = MaterialTheme.typography.bodySmall
                )
            }

            // Per-item actions: only shown when NOT in selection mode
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
//  PROPERTIES DIALOG
//
//  Single file: full details — name, type, location, size,
//  modification date, read/write permissions.
//  Multiple files: aggregated — file count, folder count,
//  combined size, common location.
// ============================================================

@Composable
private fun PropertiesDialog(files: List<File>, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                if (files.size == 1) "Properties"
                else "Properties — ${files.size} items"
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                if (files.size == 1) {
                    val f = files.first()
                    PropertyRow("Name", f.name)
                    PropertyRow("Type", if (f.isDirectory) "Folder" else "File")
                    PropertyRow("Location", f.parentFile?.absolutePath ?: "—")
                    PropertyRow(
                        "Size",
                        if (f.isDirectory) "Folder" else formatBytesShort(f.length())
                    )
                    PropertyRow("Modified", formatFileDate(f.lastModified()))
                    PropertyRow("Readable", if (f.canRead()) "Yes" else "No")
                    PropertyRow("Writable", if (f.canWrite()) "Yes" else "No")
                    PropertyRow("Executable", if (f.canExecute()) "Yes" else "No")
                } else {
                    val fileCount = files.count { !it.isDirectory }
                    val folderCount = files.count { it.isDirectory }
                    val totalSize = files.filter { !it.isDirectory }.sumOf { it.length() }
                    val parents = files.mapNotNull { it.parentFile?.absolutePath }.toSet()
                    val locationLabel = when {
                        parents.isEmpty() -> "—"
                        parents.size == 1 -> parents.first()
                        else -> "${parents.size} locations"
                    }

                    if (fileCount > 0)
                        PropertyRow("Files", "$fileCount")
                    if (folderCount > 0)
                        PropertyRow("Folders", "$folderCount")
                    PropertyRow("Total size", formatBytesShort(totalSize))
                    PropertyRow("Location", locationLabel)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        }
    )
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
            modifier = Modifier.width(72.dp)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.weight(1f)
        )
    }
}

/** Human-readable byte count. Pure computation — no IO. */
private fun formatBytesShort(bytes: Long): String = when {
    bytes < 1_024L -> "$bytes B"
    bytes < 1_024L * 1_024L -> "%.1f KB".format(bytes / 1_024.0)
    bytes < 1_024L * 1_024L * 1_024L -> "%.1f MB".format(bytes / (1_024.0 * 1_024.0))
    else -> "%.2f GB".format(bytes / (1_024.0 * 1_024.0 * 1_024.0))
}

/** Format a Unix timestamp for display in the Properties dialog. */
private fun formatFileDate(millis: Long): String =
    SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date(millis))

/**
 * Human-readable clipboard summary for the sidebar pill and dialog subtitle.
 *
 * Examples:
 *   1 file copied
 *   3 files cut
 *   1 folder cut
 *   2 folders copied
 *   2 files and 1 folder copied
 *   1 file and 3 folders cut
 */
private fun buildClipboardLabel(files: List<File>, isCut: Boolean): String {
    if (files.isEmpty()) return ""
    val op = if (isCut) "cut" else "copied"
    val fileCount = files.count { !it.isDirectory }
    val folderCount = files.count { it.isDirectory }
    return when {
        fileCount > 0 && folderCount > 0 -> {
            val filePart = "$fileCount ${if (fileCount == 1) "file" else "files"}"
            val folderPart = "$folderCount ${if (folderCount == 1) "folder" else "folders"}"
            "$filePart and $folderPart $op"
        }
        fileCount > 0 ->
            "$fileCount ${if (fileCount == 1) "file" else "files"} $op"
        else ->
            "$folderCount ${if (folderCount == 1) "folder" else "folders"} $op"
    }
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
