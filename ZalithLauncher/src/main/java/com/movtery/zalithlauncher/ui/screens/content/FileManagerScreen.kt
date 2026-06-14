package com.movtery.zalithlauncher.ui.screens.content

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
                        .clickable {
                            if (file.isDirectory) {
                               currentDirectory = file
                            }
                        }
                )
            }
        }
    }
}
