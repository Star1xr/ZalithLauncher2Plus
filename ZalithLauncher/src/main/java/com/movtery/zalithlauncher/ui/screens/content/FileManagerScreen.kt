package com.movtery.zalithlauncher.ui.screens.content

import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.platform.LocalContext
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.Button
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Icon
import androidx.compose.ui.res.painterResource
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.ui.Alignment
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.material3.IconButton
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.foundation.combinedClickable
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.TextButton
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

import com.movtery.zalithlauncher.game.path.getGameHome
import com.movtery.zalithlauncher.R

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
    val files = remember(currentDirectory, refreshCounter) {

        currentDirectory
            .listFiles()
            ?.sortedWith(
                compareBy<File>(
                    { !it.isDirectory },
                    { it.name.lowercase() }
                )
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
        Column(
            modifier = Modifier
                .widthIn(
                    min = 220.dp,
                    max = 280.dp
                )
                .fillMaxHeight()
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(28.dp),
                elevation = CardDefaults.cardElevation(2.dp)
            ) {

                Column(
                    modifier = Modifier.padding(20.dp)
                ) {

                    Text(
                        text = "Current Folder",
                        style = MaterialTheme.typography.titleMedium
                    )

                    Text(
                        text = currentDirectory.name.ifEmpty {
                            "Game Folder"
                        },
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            Spacer(
                modifier = Modifier.height(24.dp)
            )

            val modsFolder =
                File(rootDirectory, "mods")

            val resourcePacksFolder =
                File(rootDirectory, "resourcepacks")

            val screenshotsFolder =
                File(rootDirectory, "screenshots")

            TextButton(
                onClick = {
                    currentDirectory = rootDirectory
                }
            ) {

                Text(
                    text =
                        if (
                            currentDirectory ==
                            rootDirectory
                        ) {
                            "● Game Folder"
                        } else {
                            "○ Game Folder"
                        }
                )

            }

            TextButton(
                onClick = {

                    modsFolder
                        .takeIf(File::exists)
                        ?.let {
                            currentDirectory = it
                        }

                }
            ) {

                Text(

                    text =
                        if (

                            currentDirectory
                                .absolutePath
                                .startsWith(
                                    modsFolder.absolutePath
                                )

                        ) {

                            "● Mods"

                        } else {

                            "○ Mods"

                        }

                )

            }

            TextButton(
                onClick = {

                    resourcePacksFolder
                        .takeIf(File::exists)
                        ?.let {
                            currentDirectory = it
                        }

                }
            ) {

                Text(

                    text =
                        if (

                            currentDirectory
                                .absolutePath
                                .startsWith(
                                    resourcePacksFolder.absolutePath
                                )

                        ) {

                            "● Resource Packs"

                        } else {

                             "○ Resource Packs"

                        }

                )

            }
            
            TextButton(
                onClick = {

                    screenshotsFolder
                        .takeIf(File::exists)
                        ?.let {
                            currentDirectory = it
                        }

                }
            ) {

                Text(

                    text =
                        if (

                            currentDirectory
                                .absolutePath
                                .startsWith(
                                    screenshotsFolder.absolutePath
                                )

                        ) {

                            "● Screenshots"

                        } else {

                            "○ Screenshots"

                        }

                )

            }

            Spacer(
                modifier = Modifier.weight(1f)
            )

            Button(

                onClick = {

                    newFolderName = ""

                    showCreateFolderDialog = true

                },

                modifier = Modifier.fillMaxWidth()

            ) {

                Text("New Folder")

            }

            Spacer(
                modifier = Modifier.height(12.dp)
            )

            Button(

                onClick = {

                    importLauncher.launch(
                        arrayOf("*/*")
                    )

                },

                modifier = Modifier.fillMaxWidth()

            ) {

                Text("Import Files")

            }
            
        }

        Spacer(
            modifier = Modifier.width(12.dp)
        )

        Column(
            modifier = Modifier.weight(1f)
        ) {

            TextField(

                value = searchQuery,

                onValueChange = {

                    searchQuery = it

                },

                placeholder = {

                    Text("Search")

                },

                singleLine = true,

                shape = RoundedCornerShape(50.dp),

                modifier = Modifier.fillMaxWidth(),

                colors = TextFieldDefaults.colors(

                    focusedContainerColor =
                        MaterialTheme.colorScheme.surfaceVariant,

                    unfocusedContainerColor =
                        MaterialTheme.colorScheme.surfaceVariant,
        
                    focusedIndicatorColor =
                        androidx.compose.ui.graphics.Color.Transparent,

                    unfocusedIndicatorColor =
                        androidx.compose.ui.graphics.Color.Transparent
            
                )

            )
            
            Spacer(
                modifier = Modifier.height(12.dp)
            )

            val breadcrumbSegments =

                try {

                    currentDirectory
                        .relativeTo(rootDirectory)
                        .invariantSeparatorsPath
                        .split("/")
                        .filter {
                            it.isNotBlank()
                        }

                } catch (_: IllegalArgumentException) {

                    emptyList()

                }

            Row(

                verticalAlignment =
                    Alignment.CenterVertically

            ) {

                TextButton(

                    onClick = {

                        currentDirectory =
                            rootDirectory

                    }

                ) {

                    Text("Game Folder")

                }

                if (
                    breadcrumbSegments.isNotEmpty()
                ) {

                    Text(
                         text = " > "
                    )

                }

                breadcrumbSegments.forEachIndexed {

                    index,
                    segment ->

                    val isLast =
                    
                        index ==
                            breadcrumbSegments.lastIndex

                    
                    val targetDirectory =

                        File(

                            rootDirectory,

                            breadcrumbSegments
                                .take(index + 1)
                                .joinToString("/")

                        )

                    if (!isLast) {

                        TextButton(

                            onClick = {

                                currentDirectory =
                                    targetDirectory

                            }

                        ) {

                            Text(segment)

                        }
                        
                    }
                    else {

                        Text(

                            text = segment,

                            style =
                                MaterialTheme
                                    .typography
                                    .bodySmall

                        )

                    }

                    if (!isLast) {

                        Text(
                            text = " > "
                        )

                    }

                }
                
            }
                
            if (clipboardFile != null) {

                TextButton(

                    onClick = {

                        if (clipboardFile != null) {

                            val source = clipboardFile ?: return@TextButton

                            val destination = File(
                                currentDirectory,
                                source.name
                            )

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

                    }

                ) {

                    Text(
                        text = if (clipboardIsCut) {
                            "📌 Move Here"
                        } else {
                            "📌 Paste Here"
                        }
                    )

                }

            }
        
            LazyColumn {
            
                if (displayedFiles.isEmpty()) {

                    item {

                        Card(

                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 24.dp),

                            shape = RoundedCornerShape(28.dp),

                            elevation = CardDefaults.cardElevation(
                                defaultElevation = 2.dp
                            )

                        ) {

                            Column(

                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(32.dp),

                                horizontalAlignment =
                                    Alignment.CenterHorizontally

                            ) {

                                Icon(
                                    painter = painterResource(
                                        id = R.drawable.ic_folder_outlined
                                    ),
                                    contentDescription = null,
                                    modifier = Modifier.size(30.dp),
                                    tint = LocalContentColor.current
                                )
                                
                                Spacer(
                                    modifier =
                                        Modifier.height(12.dp)
                                )

                                Text(

                                    text =

                                        if (

                                            searchQuery.isBlank()

                                        ) {

                                            "This folder is empty"

                                        } else {

                                            "No matching files"

                                        }

                                )

                                Spacer(
                                    modifier =
                                        Modifier.height(4.dp)
                                )

                                Text(

                                    text =

                                        if (

                                            searchQuery.isBlank()

                                        ) {

                                            "Import files or create folders."

                                        } else {

                                            "Try a different search term."

                                        }

                                )

                            }

                        }

                    }
                    
                }
                else {

                    items(displayedFiles) { file ->

                        var menuExpanded by remember(file) {
                            mutableStateOf(false)
                        }

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp),

                            shape = RoundedCornerShape(28.dp),

                            elevation = CardDefaults.cardElevation(
                                defaultElevation = 2.dp
                            )
                        ) {

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .combinedClickable(
                                        onClick = {
                                            if (file.isDirectory) {
                                                currentDirectory = file
                                            }
                                        }
                                    )
                                    .padding(
                                        horizontal = 18.dp,
                                        vertical = 12.dp
                                    ),
                                verticalAlignment = Alignment.CenterVertically
                            ) {

                                Icon(
                                    painter = painterResource(
                                        id = getFileIcon(file)
                                    ),
                                    contentDescription =
                                        if (file.isDirectory) {
                                            "Folder"
                                        } else {
                                            "${file.extension} file"
                                        },
                                    modifier = Modifier.size(36.dp),
                                    tint = LocalContentColor.current
                                )

                                Spacer(
                                    modifier = Modifier.width(16.dp)
                                )

                                Column(
                                    modifier = Modifier.weight(1f)
                                ) {

                                    Text(
                                        text = file.name,
                                        style = MaterialTheme.typography.titleMedium
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

                                Box(

                                    contentAlignment =
                                        Alignment.TopEnd

                                ) {

                                    IconButton(
                                        onClick = {

                                            selectedFile = file

                                            menuExpanded = true

                                        }
                                    ) {

                                        Text(
                                            "⋮",
                                            style = MaterialTheme.typography.headlineSmall
                                        )
                                    }

                                    DropdownMenu(

                                        expanded = menuExpanded,

                                        shape = MaterialTheme.shapes.large,

                                        shadowElevation = 3.dp,

                                        onDismissRequest = {

                                            menuExpanded = false

                                        }
    
                                    ) {

                                        DropdownMenuItem(

                                            leadingIcon = {

                                                  Icon(

                                                      painter = painterResource(
                                                          id = R.drawable.ic_edit_outlined
                                                      ),

                                                      contentDescription = null

                                                  )

                                            },

                                            text = {

                                                Text("Rename")

                                            },

                                            onClick = {

                                                renameText = file.name

                                                selectedFile = file

                                                menuExpanded = false

                                                showRenameDialog = true

                                            }

                                        )

                                        DropdownMenuItem(

                                            leadingIcon = {

                                                Icon(

                                                    painter = painterResource(
                                                        id = R.drawable.ic_copy_all_outlined
                                                    ),

                                                    contentDescription = null

                                                )

                                            },

                                            text = {

                                                Text("Copy")

                                            },

                                            onClick = {

                                                clipboardFile = file

                                                clipboardIsCut = false

                                                menuExpanded = false

                                            }

                                        )

                                        DropdownMenuItem(

                                            leadingIcon = {

                                                Icon(

                                                    painter = painterResource(
                                                        id = R.drawable.ic_file_copy_filled
                                                    ),

                                                    contentDescription = null

                                                )

                                            },

                                            text = {

                                                Text("Cut")

                                            },

                                            onClick = {

                                                clipboardFile = file

                                                clipboardIsCut = true

                                                menuExpanded = false

                                            }

                                        )

                                        DropdownMenuItem(

                                            leadingIcon = {

                                                Icon(

                                                    painter = painterResource(
                                                        id = R.drawable.ic_info_outlined
                                                    ),

                                                    contentDescription = null

                                                )

                                            },

                                            text = {

                                                Text("Properties")

                                            },

                                            onClick = {

                                                selectedFile = file

                                                menuExpanded = false

                                                showPropertiesDialog = true

                                            }

                                        )

                                        DropdownMenuItem(

                                            leadingIcon = {

                                                Icon(

                                                    painter = painterResource(
                                                        id = R.drawable.ic_delete_outlined
                                                    ),

                                                    contentDescription = null

                                                )

                                            },

                                            text = {

                                                Text("Delete")

                                            },

                                            onClick = {

                                                selectedFile = file

                                                menuExpanded = false

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
