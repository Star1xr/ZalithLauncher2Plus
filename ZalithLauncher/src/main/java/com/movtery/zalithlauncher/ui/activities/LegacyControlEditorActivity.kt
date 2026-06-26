package com.movtery.zalithlauncher.ui.activities

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.movtery.zalithlauncher.R
import com.movtery.zalithlauncher.game.control.legacy.LegacyControlManager
import com.movtery.zalithlauncher.game.control.legacy.LegacyControlInfo
import com.movtery.zalithlauncher.ui.base.BaseAppCompatActivity
import com.movtery.zalithlauncher.ui.theme.ZalithLauncherTheme
import com.movtery.zalithlauncher.ui.theme.itemColor
import com.movtery.zalithlauncher.ui.theme.onItemColor
import com.movtery.zalithlauncher.ui.components.MarqueeText
import com.movtery.zalithlauncher.ui.components.OwnOutlinedTextField
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

private const val BUNDLE_LEGACY_CONTROL = "BUNDLE_LEGACY_CONTROL"

@AndroidEntryPoint
class LegacyControlEditorActivity : BaseAppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val controlPath = intent.extras?.getString(BUNDLE_LEGACY_CONTROL) ?: run { finish(); return }
        val controlFile = File(controlPath).takeIf { it.isFile && it.exists() } ?: run { finish(); return }

        setContent {
            ZalithLauncherTheme {
                LegacyControlEditorScreen(
                    controlFile = controlFile,
                    onExit = { finish() }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LegacyControlEditorScreen(
    controlFile: File,
    onExit: () -> Unit
) {
    data class ButtonEntry(
        val index: Int,
        val displayName: String,
        val keyCodes: String
    )

    var buttonList by remember { mutableStateOf<List<ButtonEntry>>(emptyList()) }
    var jsonRoot by remember { mutableStateOf<JSONObject?>(null) }
    var showAddDialog by remember { mutableStateOf(false) }
    var editingEntry by remember { mutableStateOf<ButtonEntry?>(null) }
    var saveError by remember { mutableStateOf<String?>(null) }

    fun reloadFromFile() {
        runCatching {
            val json = JSONObject(controlFile.readText())
            jsonRoot = json
            val arr = json.optJSONArray("mControlDataList") ?: JSONArray()
            buttonList = (0 until arr.length()).map { i ->
                val obj = arr.getJSONObject(i)
                ButtonEntry(
                    index = i,
                    displayName = obj.optString("name", "Button $i"),
                    keyCodes = obj.optString("keyCodes", "")
                )
            }
        }
    }

    fun saveToFile(root: JSONObject) {
        runCatching {
            controlFile.writeText(root.toString(2))
            LegacyControlManager.refresh()
        }.onFailure { e ->
            saveError = e.message ?: "Unknown error"
        }
    }

    fun addButton(name: String, keyCodes: String) {
        val root = jsonRoot ?: JSONObject()
        val arr = root.optJSONArray("mControlDataList") ?: JSONArray().also {
            root.put("mControlDataList", it)
        }
        val newButton = JSONObject().apply {
            put("name", name)
            put("keyCodes", keyCodes)
            put("x", 0.5)
            put("y", 0.5)
            put("width", 80)
            put("height", 80)
            put("opacity", 100)
            put("isToggle", false)
            put("passThrough", false)
            put("bgName", "default_btn")
            put("displayName", name)
        }
        arr.put(newButton)
        root.put("mControlDataList", arr)
        jsonRoot = root
        saveToFile(root)
        reloadFromFile()
    }

    fun deleteButton(index: Int) {
        val root = jsonRoot ?: return
        val arr = root.optJSONArray("mControlDataList") ?: return
        val newArr = JSONArray()
        for (i in 0 until arr.length()) {
            if (i != index) newArr.put(arr.get(i))
        }
        root.put("mControlDataList", newArr)
        jsonRoot = root
        saveToFile(root)
        reloadFromFile()
    }

    fun updateButton(index: Int, name: String, keyCodes: String) {
        val root = jsonRoot ?: return
        val arr = root.optJSONArray("mControlDataList") ?: return
        val obj = arr.getJSONObject(index)
        obj.put("name", name)
        obj.put("displayName", name)
        obj.put("keyCodes", keyCodes)
        arr.put(index, obj)
        root.put("mControlDataList", arr)
        jsonRoot = root
        saveToFile(root)
        reloadFromFile()
    }

    LaunchedEffect(Unit) { reloadFromFile() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = controlFile.nameWithoutExtension,
                        style = MaterialTheme.typography.titleMedium
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onExit) {
                        Icon(
                            painter = painterResource(R.drawable.ic_arrow_back),
                            contentDescription = stringResource(R.string.generic_back)
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }) {
                Icon(
                    painter = painterResource(R.drawable.ic_add),
                    contentDescription = stringResource(R.string.legacy_control_manage_create_new)
                )
            }
        }
    ) { padding ->
        if (buttonList.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(R.string.legacy_control_editor_no_buttons),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(buttonList, key = { it.index }) { entry ->
                    Surface(
                        color = itemColor(),
                        contentColor = onItemColor(),
                        shape = MaterialTheme.shapes.large,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(MaterialTheme.shapes.large)
                                .padding(horizontal = 16.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = entry.displayName,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                if (entry.keyCodes.isNotEmpty()) {
                                    Text(
                                        text = entry.keyCodes,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            IconButton(onClick = { editingEntry = entry }) {
                                Icon(
                                    painter = painterResource(R.drawable.ic_edit_outlined),
                                    contentDescription = stringResource(R.string.control_manage_info_edit)
                                )
                            }
                            IconButton(onClick = { deleteButton(entry.index) }) {
                                Icon(
                                    painter = painterResource(R.drawable.ic_delete_outlined),
                                    contentDescription = stringResource(R.string.generic_delete)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        LegacyButtonEditDialog(
            title = stringResource(R.string.legacy_control_editor_add_button),
            initialName = "",
            initialKeyCodes = "",
            onDismiss = { showAddDialog = false },
            onConfirm = { name, keyCodes ->
                addButton(name, keyCodes)
                showAddDialog = false
            }
        )
    }

    editingEntry?.let { entry ->
        LegacyButtonEditDialog(
            title = stringResource(R.string.legacy_control_editor_edit_button),
            initialName = entry.displayName,
            initialKeyCodes = entry.keyCodes,
            onDismiss = { editingEntry = null },
            onConfirm = { name, keyCodes ->
                updateButton(entry.index, name, keyCodes)
                editingEntry = null
            }
        )
    }

    saveError?.let { error ->
        AlertDialog(
            onDismissRequest = { saveError = null },
            title = { Text(stringResource(R.string.generic_error)) },
            text = { Text(error) },
            confirmButton = {
                Button(onClick = { saveError = null }) {
                    Text(stringResource(R.string.generic_confirm))
                }
            }
        )
    }
}

@Composable
private fun LegacyButtonEditDialog(
    title: String,
    initialName: String,
    initialKeyCodes: String,
    onDismiss: () -> Unit,
    onConfirm: (String, String) -> Unit
) {
    var name by remember { mutableStateOf(initialName) }
    var keyCodes by remember { mutableStateOf(initialKeyCodes) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = MaterialTheme.shapes.extraLarge,
            shadowElevation = 6.dp
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(text = title, style = MaterialTheme.typography.titleMedium)

                OwnOutlinedTextField(
                    modifier = Modifier.fillMaxWidth(),
                    value = name,
                    onValueChange = { name = it.take(64) },
                    label = { Text(stringResource(R.string.control_manage_create_new_name)) },
                    singleLine = true,
                    shape = MaterialTheme.shapes.large
                )
                OwnOutlinedTextField(
                    modifier = Modifier.fillMaxWidth(),
                    value = keyCodes,
                    onValueChange = { keyCodes = it.take(128) },
                    label = { Text(stringResource(R.string.legacy_control_editor_keycodes)) },
                    singleLine = true,
                    shape = MaterialTheme.shapes.large
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilledTonalButton(
                        modifier = Modifier.weight(1f),
                        onClick = onDismiss
                    ) { Text(stringResource(R.string.generic_cancel)) }
                    Button(
                        modifier = Modifier.weight(1f),
                        onClick = { onConfirm(name, keyCodes) },
                        enabled = name.isNotBlank()
                    ) { Text(stringResource(R.string.generic_save)) }
                }
            }
        }
    }
}

fun startLegacyEditorActivity(context: Context, file: File) {
    val intent = Intent(context, LegacyControlEditorActivity::class.java).apply {
        putExtra(BUNDLE_LEGACY_CONTROL, file.absolutePath)
    }
    context.startActivity(intent)
}
