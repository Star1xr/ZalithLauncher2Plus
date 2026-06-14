package com.movtery.zalithlauncher.ui.screens.content

import androidx.compose.material3.OutlinedTextField
import androidx.compose.foundation.combinedClickable
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.foundation.clickable
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
    val files =
        currentDirectory
            .listFiles()
            ?.sortedWith(
                compareBy<File>(
                    { !it.isDirectory },
                    { it.name.lowercase() }
                )
            )
            ?: emptyList()
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        
        Text(
            text = "File Manager",
            style = MaterialTheme.typography.headlineSmall
        )

        Text(
            text = currentDirectory.absolutePath,
            style = MaterialTheme.typography.bodySmall
        )

        Text(
            text = "⬅ Parent Folder",
            modifier = Modifier
                .padding(vertical = 12.dp)
                .clickable {

                    currentDirectory.parentFile?.let { parent ->

                        if (
                            parent.absolutePath.startsWith(
                                rootDirectory.absolutePath
                            )
                        ) {
                            currentDirectory = parent
                        }

                    }

                }
        )

        LazyColumn {
            
            items(files) { file ->

                Text(
                    text = if (file.isDirectory) {
                        "📁 ${file.name}"
                    } else {
                        "📄 ${file.name}"
                    },
                    modifier = Modifier
                        .padding(vertical = 6.dp)
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
                )
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
                    Text("Choose an action")
                },

                confirmButton = {
                    TextButton(
                        onClick = {

                            renameText = selectedFile!!.name

                            showFileMenu = false

                            showRenameDialog = true

                        }
                    ) {
                        Text("Rename")
                    }
                },

                dismissButton = {
                    TextButton(
                        onClick = {
                            showFileMenu = false
                        }
                    ) {
                        Text("Cancel")
                    }
                }
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
    }
}
