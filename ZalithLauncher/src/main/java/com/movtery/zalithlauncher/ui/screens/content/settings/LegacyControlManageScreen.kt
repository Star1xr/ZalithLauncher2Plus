package com.movtery.zalithlauncher.ui.screens.content.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.movtery.zalithlauncher.R
import com.movtery.zalithlauncher.ui.theme.itemColor
import com.movtery.zalithlauncher.ui.theme.onItemColor
import com.movtery.zalithlauncher.game.control.legacy.LegacyControlData
import com.movtery.zalithlauncher.game.control.legacy.LegacyControlInfo
import com.movtery.zalithlauncher.game.control.legacy.LegacyControlManager
import com.movtery.zalithlauncher.setting.AllSettings
import com.movtery.zalithlauncher.ui.activities.startLegacyEditorActivity
import com.movtery.zalithlauncher.ui.components.AnimatedRow
import com.movtery.zalithlauncher.ui.components.BackgroundCard
import com.movtery.zalithlauncher.ui.components.CardTitleLayout
import com.movtery.zalithlauncher.ui.components.EdgeDirection
import com.movtery.zalithlauncher.ui.components.IconTextButton
import com.movtery.zalithlauncher.ui.components.MarqueeText
import com.movtery.zalithlauncher.ui.components.OwnOutlinedTextField
import com.movtery.zalithlauncher.ui.components.ScalingActionButton
import com.movtery.zalithlauncher.ui.components.ScalingLabel
import com.movtery.zalithlauncher.ui.components.SimpleAlertDialog
import com.movtery.zalithlauncher.ui.components.fadeEdge
import com.movtery.zalithlauncher.utils.animation.getAnimateTween
import com.movtery.zalithlauncher.utils.file.shareFile
import com.movtery.zalithlauncher.utils.string.getMessageOrToString
import com.movtery.zalithlauncher.utils.string.isEmptyOrBlank
import com.movtery.zalithlauncher.viewmodel.ErrorViewModel
import kotlinx.coroutines.launch

private sealed interface LegacyOperation {
    data object None : LegacyOperation
    data object CreateNew : LegacyOperation
    data class Delete(val data: LegacyControlData) : LegacyOperation
    data class EditInfo(val data: LegacyControlData) : LegacyOperation
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun LegacyControlManageContent(
    modifier: Modifier = Modifier,
    isVisible: Boolean,
    submitError: (ErrorViewModel.ThrowableMessage) -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val dataList by LegacyControlManager.dataList.collectAsStateWithLifecycle()
    val selectedLayout by LegacyControlManager.selectedLayout.collectAsStateWithLifecycle()
    val isRefreshing by LegacyControlManager.isRefreshing.collectAsStateWithLifecycle()

    var operation by remember { mutableStateOf<LegacyOperation>(LegacyOperation.None) }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uris ->
        if (uris.isEmpty()) return@rememberLauncherForActivityResult
        coroutineScope.launch {
            uris.forEach { uri ->
                context.contentResolver.openInputStream(uri)?.let { stream ->
                    LegacyControlManager.importControl(
                        inputStream = stream,
                        onNotLegacy = {
                            submitError(
                                ErrorViewModel.ThrowableMessage(
                                    title = context.getString(R.string.generic_warning),
                                    message = context.getString(R.string.legacy_control_manage_not_legacy)
                                )
                            )
                        },
                        onError = { e ->
                            submitError(
                                ErrorViewModel.ThrowableMessage(
                                    title = context.getString(R.string.generic_error),
                                    message = context.getString(
                                        R.string.legacy_control_manage_import_failed,
                                        e.getMessageOrToString()
                                    )
                                )
                            )
                        }
                    )
                }
            }
        }
    }

    when (val op = operation) {
        is LegacyOperation.None -> {}
        is LegacyOperation.CreateNew -> {
            LegacyCreateNewDialog(
                onDismissRequest = { operation = LegacyOperation.None },
                onCreate = { name ->
                    LegacyControlManager.createNew(name)
                    operation = LegacyOperation.None
                }
            )
        }
        is LegacyOperation.Delete -> {
            val layoutName = op.data.info.name.ifEmpty { op.data.file.name }
            SimpleAlertDialog(
                title = stringResource(R.string.generic_warning),
                text = stringResource(R.string.legacy_control_manage_delete_message, layoutName),
                onDismiss = { operation = LegacyOperation.None },
                onConfirm = {
                    LegacyControlManager.deleteControl(op.data)
                    operation = LegacyOperation.None
                }
            )
        }
        is LegacyOperation.EditInfo -> {
            LegacyEditInfoDialog(
                data = op.data,
                onDismissRequest = { operation = LegacyOperation.None },
                onSave = { newInfo ->
                    LegacyControlManager.saveInfo(op.data, newInfo)
                    operation = LegacyOperation.None
                }
            )
        }
    }

    AnimatedRow(
        modifier = modifier,
        isVisible = isVisible
    ) { scope ->
        AnimatedItem(scope) { xOffset ->
            LegacyLayoutList(
                modifier = Modifier
                    .weight(0.5f)
                    .offset { IntOffset(x = xOffset.roundToPx(), y = 0) },
                dataList = dataList,
                isLoading = isRefreshing,
                selectedLayout = selectedLayout,
                onRefresh = { LegacyControlManager.refresh() },
                onImport = { importLauncher.launch(arrayOf("application/json", "*/*")) },
                onCreate = { operation = LegacyOperation.CreateNew },
                onSelect = { data -> LegacyControlManager.selectControl(data) },
                onDuplicate = { data -> LegacyControlManager.duplicate(data) },
                onDelete = { data -> operation = LegacyOperation.Delete(data) }
            )
        }
        AnimatedItem(scope) { xOffset ->
            LegacyLayoutInfo(
                modifier = Modifier
                    .weight(0.5f)
                    .offset { IntOffset(x = xOffset.roundToPx(), y = 0) },
                isLoading = isRefreshing,
                data = selectedLayout,
                onShareLayout = { data -> shareFile(context, data.file) },
                onEditInfo = { data -> operation = LegacyOperation.EditInfo(data) },
                onOpenEditor = { data -> startLegacyEditorActivity(context, data.file) }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun LegacyLayoutList(
    modifier: Modifier = Modifier,
    dataList: List<LegacyControlData>,
    selectedLayout: LegacyControlData?,
    isLoading: Boolean,
    onRefresh: () -> Unit,
    onImport: () -> Unit,
    onCreate: () -> Unit,
    onSelect: (LegacyControlData) -> Unit,
    onDuplicate: (LegacyControlData) -> Unit = {},
    onDelete: (LegacyControlData) -> Unit
) {
    BackgroundCard(
        modifier = modifier.fillMaxHeight(),
        shape = MaterialTheme.shapes.extraLarge
    ) {
        if (isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                LoadingIndicator()
            }
        } else {
            CardTitleLayout {
                val scrollState = rememberScrollState()
                Row(
                    modifier = Modifier
                        .fadeEdge(
                            state = scrollState,
                            length = 32.dp,
                            direction = EdgeDirection.Horizontal
                        )
                        .horizontalScroll(state = scrollState)
                        .padding(all = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconTextButton(
                        onClick = onRefresh,
                        painter = painterResource(R.drawable.ic_refresh),
                        contentDescription = stringResource(R.string.generic_refresh),
                        text = stringResource(R.string.generic_refresh)
                    )
                    IconTextButton(
                        onClick = onImport,
                        painter = painterResource(R.drawable.ic_download),
                        contentDescription = stringResource(R.string.legacy_control_manage_import),
                        text = stringResource(R.string.legacy_control_manage_import)
                    )
                    IconTextButton(
                        onClick = onCreate,
                        painter = painterResource(R.drawable.ic_add),
                        contentDescription = stringResource(R.string.legacy_control_manage_create_new),
                        text = stringResource(R.string.legacy_control_manage_create_new)
                    )
                }
            }

            if (dataList.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    ScalingLabel(text = stringResource(R.string.legacy_control_manage_empty))
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    items(dataList, key = { it.file.name }) { data ->
                        val isSelected = selectedLayout?.file?.name == data.file.name
                        LegacyLayoutItem(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp),
                            data = data,
                            isSelected = isSelected,
                            onSelect = { onSelect(data) },
                            onDuplicate = { onDuplicate(data) },
                            onDelete = { onDelete(data) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LegacyLayoutItem(
    modifier: Modifier = Modifier,
    data: LegacyControlData,
    isSelected: Boolean,
    onSelect: () -> Unit,
    onDuplicate: () -> Unit,
    onDelete: () -> Unit
) {
    val scale = remember { Animatable(initialValue = 0.95f) }
    LaunchedEffect(Unit) {
        scale.animateTo(targetValue = 1f, animationSpec = getAnimateTween())
    }

    Surface(
        modifier = modifier.graphicsLayer(scaleY = scale.value, scaleX = scale.value),
        color = itemColor(),
        contentColor = onItemColor(),
        shape = MaterialTheme.shapes.large,
        onClick = {
            if (!isSelected) onSelect()
        }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(shape = MaterialTheme.shapes.large)
                .padding(all = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RadioButton(
                selected = isSelected,
                onClick = {
                    if (!isSelected) onSelect()
                }
            )
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.height(IntrinsicSize.Min),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    MarqueeText(
                        modifier = Modifier.weight(1f, fill = false),
                        text = data.info.name.ifEmpty { data.file.name },
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                if (!data.info.author.isEmptyOrBlank()) {
                    MarqueeText(
                        text = data.info.author,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
            IconButton(onClick = onDuplicate) {
                Icon(
                    painter = painterResource(R.drawable.ic_copy_all_outlined),
                    contentDescription = stringResource(R.string.legacy_control_manage_duplicate)
                )
            }
            IconButton(onClick = onDelete) {
                Icon(
                    painter = painterResource(R.drawable.ic_delete_outlined),
                    contentDescription = stringResource(R.string.generic_delete)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun LegacyLayoutInfo(
    modifier: Modifier = Modifier,
    isLoading: Boolean,
    data: LegacyControlData?,
    onShareLayout: (LegacyControlData) -> Unit,
    onEditInfo: (LegacyControlData) -> Unit,
    onOpenEditor: (LegacyControlData) -> Unit
) {
    BackgroundCard(
        modifier = modifier.fillMaxHeight(),
        shape = MaterialTheme.shapes.extraLarge
    ) {
        if (isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                LoadingIndicator()
            }
        } else if (data == null) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                ScalingLabel(text = stringResource(R.string.control_manage_info_empty))
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp),
                contentPadding = PaddingValues(all = 12.dp)
            ) {
                item {
                    LegacyInfoItem(
                        modifier = Modifier.fillMaxWidth(),
                        title = stringResource(R.string.control_manage_create_new_name),
                        value = data.info.name.ifEmpty { stringResource(R.string.generic_unspecified) }
                    )
                }
                item {
                    LegacyInfoItem(
                        modifier = Modifier.fillMaxWidth(),
                        title = stringResource(R.string.control_manage_create_new_author),
                        value = data.info.author.ifEmpty { stringResource(R.string.generic_unspecified) }
                    )
                }
                item {
                    LegacyInfoItem(
                        modifier = Modifier.fillMaxWidth(),
                        title = stringResource(R.string.control_manage_create_new_version_name),
                        value = data.info.version.ifEmpty { stringResource(R.string.generic_unspecified) }
                    )
                }
                if (!data.info.desc.isEmptyOrBlank()) {
                    item {
                        LegacyInfoItem(
                            modifier = Modifier.fillMaxWidth(),
                            title = stringResource(R.string.control_manage_info_description),
                            value = data.info.desc
                        )
                    }
                }
                item { HorizontalDivider() }
                item {
                    LegacyInfoItem(
                        modifier = Modifier.fillMaxWidth(),
                        title = stringResource(R.string.legacy_control_manage_info_buttons),
                        value = data.buttonCount.toString()
                    )
                }
                item {
                    LegacyInfoItem(
                        modifier = Modifier.fillMaxWidth(),
                        title = stringResource(R.string.legacy_control_manage_info_joysticks),
                        value = data.joystickCount.toString()
                    )
                }
                item {
                    LegacyInfoItem(
                        modifier = Modifier.fillMaxWidth(),
                        title = stringResource(R.string.legacy_control_manage_info_drawers),
                        value = data.drawerCount.toString()
                    )
                }
                item {
                    LegacyInfoItem(
                        modifier = Modifier.fillMaxWidth(),
                        title = stringResource(R.string.legacy_control_manage_info_format_version),
                        value = "v${data.formatVersion}"
                    )
                }
            }

            Row(
                modifier = Modifier
                    .align(Alignment.End)
                    .padding(horizontal = 12.dp)
                    .padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ScalingActionButton(
                    modifier = Modifier.weight(1f, fill = false),
                    onClick = { onShareLayout(data) }
                ) {
                    MarqueeText(text = stringResource(R.string.generic_share))
                }
                ScalingActionButton(
                    modifier = Modifier.weight(1f, fill = false),
                    onClick = { onEditInfo(data) }
                ) {
                    MarqueeText(text = stringResource(R.string.legacy_control_manage_edit_info))
                }
                ScalingActionButton(
                    modifier = Modifier.weight(1f, fill = false),
                    onClick = { onOpenEditor(data) }
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_edit_outlined),
                        contentDescription = null
                    )
                    MarqueeText(text = stringResource(R.string.legacy_control_manage_open_editor))
                }
            }
        }
    }
}

@Composable
private fun LegacyInfoItem(
    modifier: Modifier = Modifier,
    title: String,
    value: String
) {
    Surface(
        modifier = modifier,
        color = itemColor(),
        contentColor = onItemColor(),
        shape = MaterialTheme.shapes.large,
        onClick = {}
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(shape = MaterialTheme.shapes.large)
                .padding(all = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(0.4f)
            )
            MarqueeText(
                modifier = Modifier.weight(0.6f),
                text = value,
                textAlign = TextAlign.End,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
private fun LegacyCreateNewDialog(
    onDismissRequest: () -> Unit,
    onCreate: (String) -> Unit
) {
    var name by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismissRequest) {
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
                Text(
                    text = stringResource(R.string.legacy_control_manage_create_new),
                    style = MaterialTheme.typography.titleMedium
                )
                OwnOutlinedTextField(
                    modifier = Modifier.fillMaxWidth(),
                    value = name,
                    onValueChange = { name = it.take(64) },
                    label = { Text(stringResource(R.string.control_manage_create_new_name)) },
                    singleLine = true,
                    shape = MaterialTheme.shapes.large
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilledTonalButton(
                        modifier = Modifier.weight(1f),
                        onClick = onDismissRequest
                    ) { MarqueeText(text = stringResource(R.string.generic_cancel)) }
                    Button(
                        modifier = Modifier.weight(1f),
                        onClick = { onCreate(name.ifEmpty { "New Layout" }) },
                        enabled = true
                    ) { MarqueeText(text = stringResource(R.string.legacy_control_manage_create_new)) }
                }
            }
        }
    }
}

@Composable
private fun LegacyEditInfoDialog(
    data: LegacyControlData,
    onDismissRequest: () -> Unit,
    onSave: (LegacyControlInfo) -> Unit
) {
    var name by remember { mutableStateOf(data.info.name) }
    var author by remember { mutableStateOf(data.info.author) }
    var version by remember { mutableStateOf(data.info.version) }
    var desc by remember { mutableStateOf(data.info.desc) }

    Dialog(onDismissRequest = onDismissRequest) {
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
                Text(
                    text = stringResource(R.string.legacy_control_manage_edit_info),
                    style = MaterialTheme.typography.titleMedium
                )

                val scrollState = rememberScrollState()
                Column(
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .fadeEdge(state = scrollState)
                        .verticalScroll(scrollState)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
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
                        value = author,
                        onValueChange = { author = it.take(64) },
                        label = { Text(stringResource(R.string.control_manage_create_new_author)) },
                        singleLine = true,
                        shape = MaterialTheme.shapes.large
                    )
                    OwnOutlinedTextField(
                        modifier = Modifier.fillMaxWidth(),
                        value = version,
                        onValueChange = { version = it.take(32) },
                        label = { Text(stringResource(R.string.control_manage_create_new_version_name)) },
                        singleLine = true,
                        shape = MaterialTheme.shapes.large
                    )
                    OwnOutlinedTextField(
                        modifier = Modifier.fillMaxWidth(),
                        value = desc,
                        onValueChange = { desc = it.take(256) },
                        label = { Text(stringResource(R.string.control_manage_info_description)) },
                        singleLine = false,
                        shape = MaterialTheme.shapes.large
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilledTonalButton(
                        modifier = Modifier.weight(1f),
                        onClick = onDismissRequest
                    ) { MarqueeText(text = stringResource(R.string.generic_cancel)) }
                    Button(
                        modifier = Modifier.weight(1f),
                        onClick = {
                            onSave(LegacyControlInfo(name = name, author = author, version = version, desc = desc))
                        }
                    ) { MarqueeText(text = stringResource(R.string.generic_save)) }
                }
            }
        }
    }
}
