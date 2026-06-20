package com.movtery.zalithlauncher.ui.screens.content

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.Animatable
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import com.movtery.zalithlauncher.ui.components.SimpleTextInputField
import com.movtery.zalithlauncher.ui.screens.content.elements.SortByDropdownMenu
import com.movtery.zalithlauncher.ui.screens.content.elements.SortByEnum
import com.movtery.zalithlauncher.ui.screens.content.versions.layouts.VersionChunkBackground
import com.movtery.zalithlauncher.ui.theme.itemColor
import com.movtery.zalithlauncher.ui.theme.onItemColor
import com.movtery.zalithlauncher.utils.animation.getAnimateTween
import java.io.File

@Composable
fun FileManagerScreen() {

    val rootDirectory = remember {
        File(getGameHome())
    }
    val context = LocalContext.current

    var currentDirectory by remember {
        mutableStateOf(rootDirectory)
    }
    var refreshCounter by remember {
        mutableStateOf(0)
    }
    var selectedFile by remember {
        mutableStateOf<File?>(null)
    }
    var showRenameDialog by remember {
        mutableStateOf(false)
    }
    var renameText by remember {
        mutableStateOf("")
    }
    var showDeleteDialog by remember {
        mutableStateOf(false)
    }
    var showPropertiesDialog by remember {
        mutableStateOf(false)
    }
    var showCreateFolderDialog by remember {
        mutableStateOf(false)
    }
    var newFolderName by remember {
        mutableStateOf("")
    }
    var searchQuery by remember {
        mutableStateOf("")
    }
    var clipboardFile by remember {
        mutableStateOf<File?>(null)
    }
    var clipboardIsCut by remember {
        mutableStateOf(false)
    }
    var sortByEnum by remember {
        mutableStateOf(SortByEnum.FileName)
    }
    var isAscending by remember {
        mutableStateOf(true)
    }

    fun importFile(uri: Uri) {

        val fileName =

            context.contentResolver
                .query(
                    uri,
                    null,
                    null,
                    null,
                    null
                )
                ?.use { cursor ->

                    val index =
                        cursor.getColumnIndex(
                            android.provider.OpenableColumns.DISPLAY_NAME
                        )

                    if (
                        cursor.moveToFirst() &&
                        index >= 0
                    ) {

                        cursor.getString(index)

                    } else {

                        null

                    }

                }

                ?: "ImportedFile"

        val destination = File(
            currentDirectory,
            fileName
        )

        context.contentResolver
            .openInputStream(uri)
            ?.use { input ->

                destination.outputStream().use { output ->

                    input.copyTo(output)

                }

            }

    }

    val importLauncher =

        rememberLauncherForActivityResult(

            contract =
            ActivityResultContracts.OpenMultipleDocuments()

        ) { uris ->

            uris.forEach {

                importFile(it)

            }

            refreshCounter++

        }

    val files = remember(currentDirectory, refreshCounter, sortByEnum, isAscending) {

        currentDirectory
            .listFiles()
            ?.sortedWith(
                Comparator { a, b ->
                    val dirCompare = b.isDirectory.compareTo(a.isDirectory)
                    if (dirCompare != 0) return@Comparator dirCompare
                    val value = when (sortByEnum) {
                        SortByEnum.FileName -> a.name.lowercase().compareTo(b.name.lowercase())
                        SortByEnum.FileModifiedTime -> b.lastModified().compareTo(a.lastModified())
                        else -> a.name.lowercase().compareTo(b.name.lowercase())
                    }
                    if (isAscending) value else -value
                }
            )
            ?: emptyList()

    }

    val displayedFiles =

        if (

            searchQuery.isBlank()

        ) {

            files

        } else {

            files.filter {

                it.name.contains(
                    searchQuery,
                    ignoreCase = true
                )

            }

        }

    Row(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {

        // ============================================================
        // SIDEBAR — unchanged
        // ============================================================

        Column(
            modifier = Modifier
                .widthIn(
                    min = 150.dp,
                    max = 180.dp
                )
                .fillMaxHeight()
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(28.dp),
                elevation = CardDefaults.cardElevation(2.dp)
            ) {

                Column(
                    modifier = Modifier.padding(16.dp)
                ) {

                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {

                        Icon(
                            painter = painterResource(
                                id = R.drawable.ic_folder_outlined
                            ),
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )

                        Spacer(
                            modifier = Modifier.width(8.dp)
                        )

                        Text(
                            text = "Current Folder",
                            style = MaterialTheme.typography.titleMedium
                        )

                    }

                    Text(
                        text = currentDirectory.name.ifEmpty {
                            "Game Folder"
                        },
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            Spacer(
                modifier = Modifier.height(16.dp)
            )

            val modsFolder =
                File(rootDirectory, "mods")

            val resourcePacksFolder =
                File(rootDirectory, "resourcepacks")

            val screenshotsFolder =
                File(rootDirectory, "screenshots")

            val gameFolderSelected =
                currentDirectory.absolutePath.startsWith(
                    rootDirectory.absolutePath
                )

            val modsSelected =
                currentDirectory.absolutePath.startsWith(
                    modsFolder.absolutePath
                )

            val resourcePacksSelected =
                currentDirectory.absolutePath.startsWith(
                    resourcePacksFolder.absolutePath
                )

            val screenshotsSelected =
                currentDirectory.absolutePath.startsWith(
                    screenshotsFolder.absolutePath
                )

            TextButton(
                onClick = {
                    currentDirectory = rootDirectory
                }
            ) {

                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {

                    Box(
                        modifier = Modifier.size(16.dp),
                        contentAlignment = Alignment.Center
                    ) {

                        RadioButton(
                            selected = gameFolderSelected,
                            onClick = null
                        )

                    }

                    Spacer(
                        modifier = Modifier.width(8.dp)
                    )

                    Text(
                        text = "Game Folder"
                    )

                }

            }

            Spacer(
                modifier = Modifier.height(4.dp)
            )

            TextButton(
                onClick = {

                    modsFolder
                        .takeIf(File::exists)
                        ?.let {
                            currentDirectory = it
                        }

                }
            ) {

                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {

                    Box(
                        modifier = Modifier.size(16.dp),
                        contentAlignment = Alignment.Center
                    ) {

                        RadioButton(
                            selected = modsSelected,
                            onClick = null
                        )

                    }

                    Spacer(
                        modifier = Modifier.width(8.dp)
                    )

                    Text("Mods")

                }

            }

            Spacer(
                modifier = Modifier.height(4.dp)
            )

            TextButton(
                onClick = {

                    resourcePacksFolder
                        .takeIf(File::exists)
                        ?.let {
                            currentDirectory = it
                        }

                }
            ) {

                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {

                    Box(
                        modifier = Modifier.size(16.dp),
                        contentAlignment = Alignment.Center
                    ) {

                        RadioButton(
                            selected = resourcePacksSelected,
                            onClick = null
                        )

                    }

                    Spacer(
                        modifier = Modifier.width(8.dp)
                    )

                    Text("Resource Packs")

                }

            }

            Spacer(
                modifier = Modifier.height(4.dp)
            )

            TextButton(
                onClick = {

                    screenshotsFolder
                        .takeIf(File::exists)
                        ?.let {
                            currentDirectory = it
                        }

                }
            ) {

                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {

                    Box(
                        modifier = Modifier.size(16.dp),
                        contentAlignment = Alignment.Center
                    ) {

                        RadioButton(
                            selected = screenshotsSelected,
                            onClick = null
                        )

                    }

                    Spacer(
                        modifier = Modifier.width(8.dp)
                    )

                    Text("Screenshots")

                }

            }

        }

        Spacer(
            modifier = Modifier.width(12.dp)
        )

        // ============================================================
        // RIGHT PANEL — rewritten to match ModsManager look
        // ============================================================

        VersionChunkBackground(
            modifier = Modifier.fillMaxSize(),
            paddingValues = PaddingValues()
        ) {

            Column(modifier = Modifier.fillMaxSize()) {

                // ------ Toolbar header (styled like ModsActionsHeader) ------

                CardTitleLayout(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp)
                            .padding(top = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {

                        // Sort button
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

                        // Search field
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

                        // Paste / Move Here button (shown only when clipboard active)
                        if (clipboardFile != null) {
                            TextButton(
                                onClick = {
                                    val source = clipboardFile ?: return@TextButton
                                    val destination = File(currentDirectory, source.name)
                                    if (clipboardIsCut) {
                                        source.renameTo(destination)
                                    } else {
                                        source.copyRecursively(
                                            destination,
                                            overwrite = true
                                        )
                                    }
                                    clipboardFile = null
                                    clipboardIsCut = false
                                    refreshCounter++
                                }
                            ) {
                                Text(
                                    text = if (clipboardIsCut) "Move Here" else "Paste Here"
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(6.dp))

                        // New Folder button
                        IconTextButton(
                            onClick = {
                                newFolderName = ""
                                showCreateFolderDialog = true
                            },
                            painter = painterResource(R.drawable.ic_folder_outlined),
                            text = "New Folder"
                        )

                        // Import Files button
                        IconTextButton(
                            onClick = {
                                importLauncher.launch(arrayOf("*/*"))
                            },
                            painter = painterResource(R.drawable.ic_upload),
                            text = "Import"
                        )

                        // Refresh button
                        IconButton(onClick = { refreshCounter++ }) {
                            Icon(
                                painter = painterResource(R.drawable.ic_refresh),
                                contentDescription = "Refresh"
                            )
                        }
                    }
                }

                // ------ Breadcrumb navigation ------

                val breadcrumbSegments =
                    try {
                        currentDirectory
                            .relativeTo(rootDirectory)
                            .invariantSeparatorsPath
                            .split("/")
                            .filter { it.isNotBlank() }
                    } catch (_: IllegalArgumentException) {
                        emptyList()
                    }

                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = { currentDirectory = rootDirectory }
                    ) {
                        Text("Game Folder")
                    }

                    if (breadcrumbSegments.isNotEmpty()) {
                        Text(text = " > ")
                    }

                    breadcrumbSegments.forEachIndexed { index, segment ->
                        val isLast = index == breadcrumbSegments.lastIndex

                        val targetDirectory = File(
                            rootDirectory,
                            breadcrumbSegments.take(index + 1).joinToString("/")
                        )

                        if (!isLast) {
                            TextButton(
                                onClick = { currentDirectory = targetDirectory }
                            ) {
                                Text(segment)
                            }
                        } else {
                            Text(
                                text = segment,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }

                        if (!isLast) {
                            Text(text = " > ")
                        }
                    }
                }

                // ------ File list ------

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
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
                                            painter = painterResource(
                                                id = R.drawable.ic_folder_outlined
                                            ),
                                            contentDescription = null,
                                            modifier = Modifier.size(48.dp),
                                            tint = LocalContentColor.current
                                        )

                                        Text(
                                            text = if (searchQuery.isBlank()) {
                                                "This folder is empty"
                                            } else {
                                                "No matching files"
                                            },
                                            style = MaterialTheme.typography.titleSmall
                                        )

                                        Text(
                                            text = if (searchQuery.isBlank()) {
                                                "Import files or create folders."
                                            } else {
                                                "Try a different search term."
                                            },
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
                                FileItemLayout(
                                    modifier = Modifier.fillMaxWidth(),
                                    file = file,
                                    onClick = {
                                        if (file.isDirectory) {
                                            currentDirectory = file
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

            // ------ Dialogs (all logic unchanged) ------

            if (showRenameDialog && selectedFile != null) {

                AlertDialog(

                    onDismissRequest = {
                        showRenameDialog = false
                    },

                    title = {
                        Text("Rename")
                    },

                    text = {

                        OutlinedTextField(
                            value = renameText,

                            onValueChange = {
                                renameText = it
                            },

                            label = {
                                Text("New name")
                            }

                        )

                    },

                    confirmButton = {

                        TextButton(

                            onClick = {

                                val renamedFile = File(
                                    selectedFile!!.parentFile,
                                    renameText
                                )

                                selectedFile!!.renameTo(
                                    renamedFile
                                )

                                refreshCounter++

                                showRenameDialog = false

                            }

                        ) {
                            Text("OK")
                        }

                    },

                    dismissButton = {

                        TextButton(

                            onClick = {
                                showRenameDialog = false
                            }

                        ) {
                            Text("Cancel")
                        }

                    }

                )
            }

            if (showDeleteDialog && selectedFile != null) {

                AlertDialog(

                    onDismissRequest = {
                        showDeleteDialog = false
                    },

                    title = {
                        Text("Delete")
                    },

                    text = {
                        Text(
                            "Delete \"${selectedFile!!.name}\"?\n\nThis cannot be undone."
                        )
                    },

                    confirmButton = {

                        TextButton(

                            onClick = {

                                selectedFile!!.deleteRecursively()

                                refreshCounter++

                                showDeleteDialog = false

                            }

                        ) {
                            Text("Delete")
                        }

                    },

                    dismissButton = {

                        TextButton(

                            onClick = {
                                showDeleteDialog = false
                            }

                        ) {
                            Text("Cancel")
                        }

                    }

                )

            }

            if (
                showPropertiesDialog &&
                selectedFile != null
            ) {

                AlertDialog(

                    onDismissRequest = {

                        showPropertiesDialog = false

                    },

                    title = {

                        Text("Properties")

                    },

                    text = {

                        Column {

                            Text(
                                "Name: ${selectedFile!!.name}"
                            )

                            Text(
                                "Type: ${
                                    if (selectedFile!!.isDirectory)
                                        "Folder"
                                    else
                                        "File"
                                }"
                            )

                            Text(
                                "Size: ${selectedFile!!.length()} bytes"
                            )

                            Text(
                                "Path:\n${selectedFile!!.absolutePath}"
                            )
                        }

                    },

                    confirmButton = {

                        TextButton(
                            onClick = {
                                showPropertiesDialog = false
                            }
                        ) {

                            Text("OK")

                        }

                    }

                )
            }

            if (showCreateFolderDialog) {

                AlertDialog(

                    onDismissRequest = {

                        showCreateFolderDialog = false

                    },

                    title = {

                        Text("Create Folder")

                    },

                    text = {

                        OutlinedTextField(

                            value = newFolderName,

                            onValueChange = {

                                newFolderName = it

                            },

                            label = {

                                Text("Folder Name")

                            },

                            singleLine = true

                        )

                    },

                    confirmButton = {

                        TextButton(

                            onClick = {

                                val folderName =
                                    newFolderName.trim()

                                if (
                                    folderName.isNotEmpty() &&
                                    !folderName.contains("/") &&
                                    !folderName.contains("\\")
                                ) {

                                    val newFolder =

                                        File(

                                            currentDirectory,

                                            folderName

                                        )

                                    if (
                                        !newFolder.exists()
                                    ) {

                                        newFolder.mkdir()

                                    }

                                    refreshCounter++

                                }

                                showCreateFolderDialog = false

                            }

                        ) {

                            Text("Create")

                        }

                    },

                    dismissButton = {

                        TextButton(

                            onClick = {

                                showCreateFolderDialog = false

                            }

                        ) {

                            Text("Cancel")

                        }

                    }

                )

            }

        }
    }
}

// ============================================================
// File item composable — styled like ModItemLayout
// ============================================================

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
    itemContentColor: androidx.compose.ui.graphics.Color = onItemColor(),
    shape: androidx.compose.ui.graphics.Shape = MaterialTheme.shapes.large
) {
    val scale = remember { Animatable(initialValue = 0.95f) }
    LaunchedEffect(Unit) {
        scale.animateTo(targetValue = 1f, animationSpec = getAnimateTween())
    }

    Surface(
        modifier = modifier
            .graphicsLayer(scaleX = scale.value, scaleY = scale.value),
        onClick = onClick,
        shape = shape,
        color = itemColor,
        contentColor = itemContentColor
    ) {
        Row(
            modifier = Modifier.padding(all = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {

            // File / folder icon
            Icon(
                painter = painterResource(id = getFileIcon(file)),
                contentDescription = if (file.isDirectory) "Folder" else "${file.extension} file",
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(10.dp)),
                tint = LocalContentColor.current
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
                    text = if (file.isDirectory) {
                        "Folder"
                    } else {
                        "${file.length()} bytes"
                    },
                    style = MaterialTheme.typography.bodySmall
                )
            }

            // Properties / Info
            IconButton(onClick = onProperties) {
                Icon(
                    painter = painterResource(R.drawable.ic_info_outlined),
                    contentDescription = "Properties"
                )
            }

            // Three-dot dropdown — Rename, Copy, Cut, Delete
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
                            Icon(
                                painter = painterResource(R.drawable.ic_edit_outlined),
                                contentDescription = null
                            )
                        },
                        text = { Text("Rename") },
                        onClick = {
                            onRename()
                            menuExpanded = false
                        }
                    )
                    DropdownMenuItem(
                        leadingIcon = {
                            Icon(
                                painter = painterResource(R.drawable.ic_copy_all_outlined),
                                contentDescription = null
                            )
                        },
                        text = { Text("Copy") },
                        onClick = {
                            onCopy()
                            menuExpanded = false
                        }
                    )
                    DropdownMenuItem(
                        leadingIcon = {
                            Icon(
                                painter = painterResource(R.drawable.ic_file_copy_filled),
                                contentDescription = null
                            )
                        },
                        text = { Text("Cut") },
                        onClick = {
                            onCut()
                            menuExpanded = false
                        }
                    )
                    DropdownMenuItem(
                        leadingIcon = {
                            Icon(
                                painter = painterResource(R.drawable.ic_delete_outlined),
                                contentDescription = null
                            )
                        },
                        text = { Text("Delete") },
                        onClick = {
                            onDelete()
                            menuExpanded = false
                        }
                    )
                }
            }
        }
    }
}

// ============================================================
// Icon resolver — unchanged
// ============================================================

private fun getFileIcon(
    file: File
): Int {

    if (file.isDirectory) {

        return when (
            file.name.lowercase()
        ) {

            "mods" ->

                R.drawable.ic_extension_outlined

            "resourcepacks" ->

                R.drawable.ic_folder_zip_outlined

            "screenshots" ->

                R.drawable.ic_image_outlined

            else ->

                R.drawable.ic_folder_outlined
        }

    }

    return when (
        file.extension.lowercase()
    ) {

        "jar" -> {

            when (
                file.parentFile?.name?.lowercase()
            ) {

                "mods" ->

                    R.drawable.ic_extension_outlined

                "libraries" ->

                    R.drawable.ic_java

                else ->

                    R.drawable.ic_java
            }

        }

        "zip" ->

            R.drawable.ic_folder_zip_outlined

        "png",
        "jpg",
        "jpeg",
        "gif",
        "webp" ->

            R.drawable.ic_image_outlined

        "txt" ->
            R.drawable.ic_article_outlined

        "toml",
        "yml",
        "yaml",
        "cfg",
        "properties" ->

            R.drawable.ic_code

        "json" ->
            R.drawable.ic_code

        "java" ->
            R.drawable.ic_java

        "log" ->
            R.drawable.ic_terminal_outlined

        "dat" -> {

            when (
                file.name.lowercase()
            ) {

                "level.dat" ->

                    R.drawable.ic_package_2_outlined

                else ->

                    R.drawable.ic_description_outlined
            }

        }

        else ->
            R.drawable.ic_description_outlined
    }
}