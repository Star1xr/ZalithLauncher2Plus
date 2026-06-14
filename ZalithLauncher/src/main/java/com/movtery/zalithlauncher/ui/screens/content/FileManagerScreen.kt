package com.movtery.zalithlauncher.ui.screens.content

import androidx.compose.foundation.layout.widthIn
import androidx.compose.ui.Alignment
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.material3.IconButton
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.foundation.combinedClickable
import androidx.compose.material3.AlertDialog
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

import java.io.File

@Composable
fun FileManagerScreen() {

    val rootDirectory = remember {
        File(getGameHome())
    }
    var currentDirectory by remember {
        mutableStateOf(rootDirectory)
    }
    var refreshCounter by remember {
        mutableStateOf(0)
    }
    var selectedFile by remember {
        mutableStateOf<File?>(null)
    }
    var showFileMenu by remember {
        mutableStateOf(false)
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
    var clipboardFile by remember {
        mutableStateOf<File?>(null)
    }
    var clipboardIsCut by remember {
        mutableStateOf(false)
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

            TextButton(
                onClick = {
                    currentDirectory = rootDirectory
                }
            ) {
                Text("Game Folder")
            }

            TextButton(
                onClick = {
                    File(rootDirectory, "mods")
                        .takeIf(File::exists)
                        ?.let {
                            currentDirectory = it
                        }
                }
            ) {
                Text("Mods")
            }

            TextButton(
                onClick = {
                    File(rootDirectory, "resourcepacks")
                        .takeIf(File::exists)
                        ?.let {
                            currentDirectory = it
                        }
                }
            ) {
                Text("Resource Packs")
            }

            TextButton(
                onClick = {
                    File(rootDirectory, "screenshots")
                        .takeIf(File::exists)
                        ?.let {
                            currentDirectory = it
                        }
                }
            ) {
                Text("Screenshots")
            }
        }

        Spacer(
            modifier = Modifier.width(16.dp)
        )

        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = "File Manager",
                style = MaterialTheme.typography.headlineSmall
            )

            Text(
                text = buildString {

                    append("Game Folder")

                    val breadcrumbPath =
                        try {
                            currentDirectory
                                .relativeTo(rootDirectory)
                                .invariantSeparatorsPath
                        } catch (_: IllegalArgumentException) {
                            currentDirectory.absolutePath
                        }

                    breadcrumbPath
                        .split("/")
                        .filter { it.isNotBlank() }
                        .forEach {

                            append(" > ")
                            append(it)
                        }
                },

                style = MaterialTheme.typography.bodySmall
            )
            
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
            
                items(files) { file ->

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),

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
                                    },
                                    onLongClick = {
                                        selectedFile = file
                                        showFileMenu = true
                                    }
                                )
                                .padding(
                                    horizontal = 24.dp,
                                    vertical = 20.dp
                                ),
                            verticalAlignment = Alignment.CenterVertically
                        ) {

                            Text(
                                text = if (file.isDirectory) "📁" else "📄"
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

                            IconButton(
                                onClick = {

                                    selectedFile = file
                                    showFileMenu = true

                                }
                            ) {

                                Text(
                                    "⋮",
                                    style = MaterialTheme.typography.headlineSmall
                                )
                            }

                        }

                    }
                }
            }

            if (showFileMenu && selectedFile != null) {

                AlertDialog(

                    onDismissRequest = {
                        showFileMenu = false
                    },

                    title = {
                        Text(selectedFile!!.name)
                    },

                    text = {

                        Column {

                            TextButton(

                                onClick = {

                                    renameText = selectedFile!!.name

                                    showFileMenu = false

                                    showRenameDialog = true

                                }

                            ) {

                                Text("Rename")

                            }

                            TextButton(

                                onClick = {

                                    showFileMenu = false

                                    showDeleteDialog = true

                                }

                            ) {

                                Text("Delete")

                            }

                            TextButton(

                                onClick = {

                                    clipboardFile = selectedFile

                                    clipboardIsCut = false

                                    showFileMenu = false

                                }

                            ) {

                                Text("Copy")

                            }
                            
                            TextButton(

                                onClick = {
                                
                                    clipboardFile = selectedFile

                                    clipboardIsCut = true

                                    showFileMenu = false

                                }

                            ) {

                                Text("Cut")

                            }

                            TextButton(

                                onClick = {

                                    showFileMenu = false

                                }

                            ) {

                                Text("Cancel")

                            }

                        }

                    },

                    confirmButton = {},

                    dismissButton = {}

                )
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
        }
    }
}
