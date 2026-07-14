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

import android.app.Activity
import android.content.Intent
import android.net.VpnService
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SecondaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.scrollbar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.movtery.zalithlauncher.R
import com.movtery.zalithlauncher.notification.NotificationManager
import com.movtery.zalithlauncher.path.PathManager
import com.movtery.zalithlauncher.path.URL_EASYTIER
import com.movtery.zalithlauncher.setting.AllSettings
import com.movtery.zalithlauncher.terracotta.Terracotta
import com.movtery.zalithlauncher.terracotta.TerracottaState
import com.movtery.zalithlauncher.terracotta.TerracottaVPNService
import com.movtery.zalithlauncher.terracotta.fetchNodes
import net.burningtnt.terracotta.TerracottaAndroidAPI
import com.movtery.zalithlauncher.terracotta.profile.TerracottaProfile
import com.movtery.zalithlauncher.ui.AndroidStringText
import com.movtery.zalithlauncher.ui.androidText
import com.movtery.zalithlauncher.ui.base.BaseScreen
import com.movtery.zalithlauncher.ui.components.AnimatedRow
import com.movtery.zalithlauncher.ui.components.BackgroundCard
import com.movtery.zalithlauncher.ui.components.MarqueeText
import com.movtery.zalithlauncher.ui.components.NotificationCheck
import com.movtery.zalithlauncher.ui.components.OwnOutlinedTextField
import com.movtery.zalithlauncher.ui.components.SimpleAlertDialog
import com.movtery.zalithlauncher.ui.components.influencedByBackgroundColor
import com.movtery.zalithlauncher.ui.components.verticalScrollWithBar
import com.movtery.zalithlauncher.ui.screens.NormalNavKey
import com.movtery.zalithlauncher.ui.screens.content.settings.layouts.CardPosition
import com.movtery.zalithlauncher.ui.screens.content.settings.layouts.SettingsCard
import com.movtery.zalithlauncher.ui.screens.content.settings.layouts.SettingsCardColumn
import com.movtery.zalithlauncher.ui.screens.content.settings.layouts.SwitchSettingsCard
import com.movtery.zalithlauncher.ui.screens.game.multiplayer.GuestWaitingOperation
import com.movtery.zalithlauncher.ui.theme.cardTitleColor
import com.movtery.zalithlauncher.ui.theme.itemColor
import com.movtery.zalithlauncher.ui.theme.onItemColor
import com.movtery.zalithlauncher.context.COPY_LABEL_TERRACOTTA_INVITE_CODE
import com.movtery.zalithlauncher.context.COPY_LABEL_TERRACOTTA_SERVER_ADDRESS
import com.movtery.zalithlauncher.utils.copyText
import com.movtery.zalithlauncher.utils.file.shareFile
import com.movtery.zalithlauncher.utils.logging.Logger
import com.movtery.zalithlauncher.viewmodel.EventViewModel
import com.movtery.zalithlauncher.viewmodel.ScreenBackStackViewModel
import com.movtery.zalithlauncher.viewmodel.sendToast
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val TAG = "MultiplayerScreen"

private sealed interface RightPanelMode {
    data object Settings : RightPanelMode
    data object Multiplayer : RightPanelMode
}

private class MultiplayerViewModel(
    private val eventViewModel: EventViewModel
) : ViewModel() {
    var dialogState by mutableStateOf<TerracottaState.Ready?>(null)
    var profiles by mutableStateOf<List<TerracottaProfile>>(emptyList())
    var terracottaVer by mutableStateOf<String?>(null)
    var easyTierVer by mutableStateOf<String?>(null)
    var isWaitingInteractive by mutableStateOf(false)

    init {
        initialize()
    }

    private fun initialize() {
        if (Terracotta.initialized) return
        Terracotta.initialize(viewModelScope, eventViewModel)
        Terracotta.setWaiting(true)
        val metadata = Terracotta.getMetadata()
        terracottaVer = metadata.terracottaVersion
        easyTierVer = metadata.easyTierVersion

        viewModelScope.launch {
            Terracotta.stateChanges.collect { (old, new) ->
                when (new) {
                    is TerracottaState.Waiting -> {
                        if (old !is TerracottaState.Waiting) isWaitingInteractive = true
                    }
                    is TerracottaState.HostOK -> {
                        if (old !is TerracottaState.HostOK) profiles = new.profiles ?: emptyList()
                        else if (new.isForkOf(old)) {
                            profiles = new.profiles ?: emptyList()
                            return@collect
                        }
                    }
                    is TerracottaState.GuestOK -> {
                        if (old !is TerracottaState.GuestOK) profiles = new.profiles ?: emptyList()
                        else if (new.isForkOf(old)) {
                            profiles = new.profiles ?: emptyList()
                            return@collect
                        }
                    }
                    else -> {
                        if (profiles.isNotEmpty()) profiles = emptyList()
                    }
                }
                dialogState = new
            }
        }
    }

    fun closeMultiplayer() {
        Terracotta.setWaiting(true)
        dialogState = null
    }

    fun hostGame(userName: String) {
        viewModelScope.launch {
            isWaitingInteractive = false
            val nodes = fetchNodes()
            val nodeList = withContext(Dispatchers.Default) {
                nodes.map { node -> node.toString() }
            }
            runCatching {
                Terracotta.setScanning(null, userName, nodeList)
                isWaitingInteractive = false
            }.onFailure { e ->
                Logger.warning(TAG, "hostGame error: ${e.message}")
                isWaitingInteractive = true
            }
        }
    }

    fun joinGame(roomCode: String, userName: String) {
        viewModelScope.launch {
            isWaitingInteractive = false
            val nodes = fetchNodes()
            val nodeList = withContext(Dispatchers.Default) {
                nodes.map { node -> node.toString() }
            }
            runCatching {
                val success = Terracotta.setGuesting(roomCode, userName, nodeList)
                isWaitingInteractive = false
                if (!success) {
                    eventViewModel.sendToast(
                        androidText(R.string.terracotta_status_waiting_guest_prompt_invalid)
                    )
                }
            }.onFailure { e ->
                Logger.warning(TAG, "joinGame error: ${e.message}")
                isWaitingInteractive = true
            }
        }
    }

    fun resetToWaiting() {
        Terracotta.setWaiting(true)
        isWaitingInteractive = true
    }
}

@Composable
private fun rememberMultiplayerViewModel(
    key: String,
    eventViewModel: EventViewModel
): MultiplayerViewModel {
    return viewModel(key = key) {
        MultiplayerViewModel(eventViewModel)
    }
}

@Composable
fun MultiplayerScreen(
    backScreenViewModel: ScreenBackStackViewModel,
    eventViewModel: EventViewModel
) {
    val context = LocalContext.current
    var panelMode by remember { mutableStateOf<RightPanelMode>(RightPanelMode.Settings) }

    val viewModel = rememberMultiplayerViewModel(
        key = "MultiplayerLauncher",
        eventViewModel = eventViewModel
    )

    val vpnLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val vpnIntent = Intent(context, TerracottaVPNService::class.java).apply {
                action = TerracottaVPNService.ACTION_START
            }
            ContextCompat.startForegroundService(context, vpnIntent)
        } else {
            TerracottaAndroidAPI.getPendingVpnServiceRequest().reject()
            Terracotta.setWaiting(true)
            eventViewModel.sendToast(
                androidText(R.string.terracotta_permission_vpn),
                Toast.LENGTH_SHORT
            )
        }
    }

    LaunchedEffect(Unit) {
        eventViewModel.events
            .filterIsInstance<EventViewModel.Event.Terracotta>()
            .collect { event ->
                when (event) {
                    is EventViewModel.Event.Terracotta.RequestVPN -> {
                        val intent = VpnService.prepare(context)
                        if (intent != null) {
                            vpnLauncher.launch(intent)
                        } else {
                            val vpnIntent = Intent(context, TerracottaVPNService::class.java)
                                .setAction(TerracottaVPNService.ACTION_START)
                            ContextCompat.startForegroundService(context, vpnIntent)
                        }
                    }
                    is EventViewModel.Event.Terracotta.VPNUpdateState -> {
                        val vpnIntent = Intent(context, TerracottaVPNService::class.java)
                            .setAction(TerracottaVPNService.ACTION_UPDATE_STATE)
                            .putExtra(TerracottaVPNService.EXTRA_STATE_TEXT, event.stringRes)
                        context.startForegroundService(vpnIntent)
                    }
                    is EventViewModel.Event.Terracotta.StopVPN -> {
                        if (TerracottaVPNService.isRunning()) {
                            val vpnIntent = Intent(context, TerracottaVPNService::class.java)
                                .setAction(TerracottaVPNService.ACTION_STOP)
                            context.startForegroundService(vpnIntent)
                        }
                    }
                }
            }
    }

    BaseScreen(
        screenKey = NormalNavKey.Multiplayer,
        currentKey = backScreenViewModel.mainScreen.currentKey
    ) { isVisible ->
        AnimatedRow(
            modifier = Modifier.fillMaxSize(),
            isVisible = isVisible,
            delayIncrement = 0
        ) { scope ->
            AnimatedItem(scope) { xOffset ->
                TutorialMenu(
                    modifier = Modifier
                        .weight(0.5f)
                        .offset { IntOffset(x = -xOffset.roundToPx(), y = 0) }
                        .padding(start = 12.dp)
                )
            }

            AnimatedItem(scope) { xOffset ->
                when (panelMode) {
                    is RightPanelMode.Settings -> {
                        SettingsPanel(
                            modifier = Modifier
                                .weight(0.5f)
                                .offset { IntOffset(x = xOffset.roundToPx(), y = 0) }
                                .padding(end = 12.dp),
                            eventViewModel = eventViewModel,
                            onShareLogs = {
                                val logFile = PathManager.FILE_TERRACOTTA_LOG
                                if (logFile.exists()) {
                                    shareFile(context, logFile)
                                } else {
                                    eventViewModel.sendToast(androidText(R.string.terracotta_export_log_share_null))
                                }
                            },
                            onOpenMultiplayer = {
                                panelMode = RightPanelMode.Multiplayer
                            }
                        )
                    }
                    is RightPanelMode.Multiplayer -> {
                        MultiplayerControlsPanel(
                            modifier = Modifier
                                .weight(0.5f)
                                .offset { IntOffset(x = xOffset.roundToPx(), y = 0) }
                                .padding(end = 12.dp),
                            viewModel = viewModel,
                            onClose = {
                                viewModel.closeMultiplayer()
                                panelMode = RightPanelMode.Settings
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsPanel(
    modifier: Modifier = Modifier,
    eventViewModel: EventViewModel,
    onShareLogs: () -> Unit,
    onOpenMultiplayer: () -> Unit
) {
    val context = LocalContext.current
    var operation by remember { mutableStateOf<MultiplayerOperation>(MultiplayerOperation.None) }

    MultiplayerOperation(
        operation = operation,
        onChange = { operation = it },
        onNoticeRead = {
            AllSettings.terracottaNoticeVer.save(Terracotta.TERRACOTTA_USER_NOTICE_VERSION)
            operation = if (!NotificationManager.checkNotificationEnabled(context)) {
                MultiplayerOperation.WarningNotification
            } else {
                MultiplayerOperation.None
            }
        },
        onNoticeRefused = {
            AllSettings.enableTerracotta.save(false)
            operation = MultiplayerOperation.None
        }
    )

    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        SettingsCardColumn(
            modifier = Modifier.fillMaxWidth()
        ) {
            SwitchSettingsCard(
                modifier = Modifier.fillMaxWidth(),
                position = CardPosition.Top,
                unit = AllSettings.enableTerracotta,
                title = stringResource(R.string.terracotta_enable),
                verticalAlignment = Alignment.CenterVertically,
                onCheckedChange = { value ->
                    if (value) {
                        when {
                            AllSettings.terracottaNoticeVer.getValue() < Terracotta.TERRACOTTA_USER_NOTICE_VERSION -> {
                                operation = MultiplayerOperation.Notice
                            }
                            !NotificationManager.checkNotificationEnabled(context) -> {
                                operation = MultiplayerOperation.WarningNotification
                            }
                        }
                    }
                }
            )

            SwitchSettingsCard(
                modifier = Modifier.fillMaxWidth(),
                position = CardPosition.Middle,
                unit = AllSettings.enableTerracottaNodes,
                title = stringResource(R.string.terracotta_custom_note_list),
                verticalAlignment = Alignment.CenterVertically,
                enabled = AllSettings.enableTerracotta.state,
                columnLayout = {
                    AnimatedVisibility(
                        visible = AllSettings.enableTerracotta.state && AllSettings.enableTerracottaNodes.state,
                    ) {
                        OwnOutlinedTextField(
                            modifier = Modifier.fillMaxWidth(),
                            value = AllSettings.terracottaNodes.state,
                            onValueChange = { value ->
                                AllSettings.terracottaNodes.save(value)
                            },
                            label = {
                                Text(text = stringResource(R.string.terracotta_custom_note_list_hint))
                            },
                            textStyle = MaterialTheme.typography.labelMedium,
                            singleLine = true,
                            shape = MaterialTheme.shapes.large,
                        )
                    }
                }
            )

            val terracottaEnabled = AllSettings.enableTerracotta.state

            SettingsCard(
                modifier = Modifier.fillMaxWidth(),
                position = CardPosition.Middle,
                title = stringResource(R.string.terracotta_export_log_share),
                innerPadding = PaddingValues(horizontal = 16.dp, vertical = 24.dp),
                onClick = onShareLogs,
                enabled = terracottaEnabled
            )

            SettingsCard(
                modifier = Modifier.fillMaxWidth(),
                position = CardPosition.Bottom,
                title = stringResource(R.string.terracotta_easytier),
                innerPadding = PaddingValues(horizontal = 16.dp, vertical = 24.dp),
                onClick = {
                    eventViewModel.sendEvent(EventViewModel.Event.OpenLink(URL_EASYTIER))
                }
            )
        }

        if (AllSettings.enableTerracotta.state) {
            BackgroundCard(
                modifier = Modifier.fillMaxWidth(),
                onClick = onOpenMultiplayer,
                colors = CardDefaults.cardColors(
                    containerColor = itemColor(false),
                    contentColor = onItemColor()
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_group_filled),
                        contentDescription = null
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.terracotta_open_multiplayer),
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            modifier = Modifier.alpha(0.7f),
                            text = stringResource(R.string.terracotta_open_multiplayer_desc),
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun MultiplayerControlsPanel(
    modifier: Modifier = Modifier,
    viewModel: MultiplayerViewModel,
    onClose: () -> Unit
) {
    val context = LocalContext.current
    var guestOperation by remember { mutableStateOf<GuestWaitingOperation>(GuestWaitingOperation.None) }
    val userName = remember {
        AllSettings.currentAccount.state.takeIf { !it.isNullOrBlank() }
            ?: context.getString(R.string.terracotta_player_anonymous)
    }

    GuestWaitingOperation(
        operation = guestOperation,
        onChange = { guestOperation = it },
        onPositive = { roomCode ->
            viewModel.joinGame(roomCode, userName)
        },
        onShowToast = { text ->
            Toast.makeText(context, text.getText(context), Toast.LENGTH_SHORT).show()
        }
    )

    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.terracotta_menu),
                style = MaterialTheme.typography.titleLarge
            )
            TextButton(onClick = onClose) {
                Text(text = stringResource(R.string.generic_close))
            }
        }

        val state = viewModel.dialogState
        val profiles = viewModel.profiles

        when (state) {
            null -> {
                Box(
                    modifier = Modifier.fillMaxWidth().heightIn(min = 200.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        LoadingIndicator()
                        Text(
                            text = stringResource(R.string.generic_loading),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
            is TerracottaState.Waiting -> {
                WaitingControls(
                    isInteractive = viewModel.isWaitingInteractive,
                    onHostClick = { viewModel.hostGame(userName) },
                    onGuestClick = { guestOperation = GuestWaitingOperation.OnClick }
                )
            }
            is TerracottaState.HostScanning, is TerracottaState.HostStarting -> {
                ProgressState(
                    text = stringResource(
                        if (state is TerracottaState.HostScanning) R.string.terracotta_status_host_scanning
                        else R.string.terracotta_status_host_starting
                    ),
                    onBack = { viewModel.resetToWaiting() }
                )
            }
            is TerracottaState.HostOK -> {
                OkRoomPanel(
                    title = stringResource(R.string.terracotta_status_host_ok),
                    code = state.code ?: "",
                    codeLabel = stringResource(R.string.terracotta_status_host_ok_code),
                    profiles = profiles,
                    onCopy = {
                        val code = state.code
                        if (code != null) {
                            copyText(
                                label = COPY_LABEL_TERRACOTTA_INVITE_CODE,
                                text = code,
                                context = context,
                                showToast = false
                            )
                            Toast.makeText(
                                context,
                                context.getString(R.string.terracotta_status_host_ok_code_copy_toast),
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    },
                    onExit = { viewModel.resetToWaiting() }
                )
            }
            is TerracottaState.GuestConnecting, is TerracottaState.GuestStarting -> {
                ProgressState(
                    text = stringResource(R.string.terracotta_status_guest_starting),
                    onBack = { viewModel.resetToWaiting() }
                )
            }
            is TerracottaState.GuestOK -> {
                OkRoomPanel(
                    title = stringResource(R.string.terracotta_status_guest_ok),
                    code = state.url ?: "",
                    codeLabel = stringResource(R.string.terracotta_status_guest_ok_address),
                    profiles = profiles,
                    onCopy = {
                        val url = state.url
                        if (url != null) {
                            copyText(
                                label = COPY_LABEL_TERRACOTTA_SERVER_ADDRESS,
                                text = url,
                                context = context,
                                showToast = false
                            )
                            Toast.makeText(
                                context,
                                context.getString(R.string.terracotta_status_guest_ok_address_copy_toast),
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    },
                    onExit = { viewModel.resetToWaiting() }
                )
            }
            is TerracottaState.Exception -> {
                ExceptionPanel(
                    title = stringResource(state.getEnumType().textRes),
                    onExit = { viewModel.resetToWaiting() },
                    onClose = onClose
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(
                        R.string.terracotta_metadata_ver,
                        viewModel.terracottaVer ?: stringResource(R.string.generic_loading)
                    ),
                    style = MaterialTheme.typography.labelMedium
                )
                Text(
                    text = stringResource(
                        R.string.terracotta_metadata_easytier_ver,
                        viewModel.easyTierVer ?: stringResource(R.string.generic_loading)
                    ),
                    style = MaterialTheme.typography.labelMedium
                )
            }
        }
    }
}

@Composable
private fun WaitingControls(
    isInteractive: Boolean,
    onHostClick: () -> Unit,
    onGuestClick: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        SimpleCardButton(
            modifier = Modifier.fillMaxWidth(),
            icon = painterResource(R.drawable.ic_home_filled),
            title = stringResource(R.string.terracotta_status_waiting_host_title),
            description = stringResource(R.string.terracotta_status_waiting_host_desc),
            onClick = onHostClick,
            enabled = isInteractive
        )
        SimpleCardButton(
            modifier = Modifier.fillMaxWidth(),
            icon = painterResource(R.drawable.ic_group_filled),
            title = stringResource(R.string.terracotta_status_waiting_guest_title),
            description = stringResource(R.string.terracotta_status_waiting_guest_desc),
            onClick = onGuestClick,
            enabled = isInteractive
        )

        if (!isInteractive) {
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                LoadingIndicator()
            }
        }
    }
}

@Composable
private fun ProgressState(
    text: String,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(text = text)
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        }
        SimpleCardButton(
            modifier = Modifier.fillMaxWidth(),
            icon = painterResource(R.drawable.ic_arrow_left_rounded),
            title = stringResource(R.string.terracotta_back),
            description = stringResource(R.string.terracotta_status_host_scanning_back),
            onClick = onBack
        )
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun OkRoomPanel(
    title: String,
    code: String,
    codeLabel: String,
    profiles: List<TerracottaProfile>,
    onCopy: () -> Unit,
    onExit: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScrollWithBar(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(text = title)
                HorizontalDivider(modifier = Modifier.fillMaxWidth())
                Text(
                    text = codeLabel,
                    style = MaterialTheme.typography.labelMedium
                )
                Text(
                    text = code,
                    style = MaterialTheme.typography.labelMedium
                )
            }

            Column(modifier = Modifier.fillMaxWidth()) {
                RowButton(
                    modifier = Modifier.fillMaxWidth(),
                    icon = painterResource(R.drawable.ic_copy_all_filled),
                    title = stringResource(R.string.terracotta_status_host_ok_code_copy),
                    description = stringResource(R.string.terracotta_status_host_ok_code_desc),
                    onClick = onCopy
                )
                RowButton(
                    modifier = Modifier.fillMaxWidth(),
                    icon = painterResource(R.drawable.ic_arrow_back),
                    title = stringResource(R.string.terracotta_back),
                    description = stringResource(R.string.terracotta_status_host_ok_back),
                    onClick = onExit
                )
            }
        }

        ProfileListPanel(
            modifier = Modifier.weight(1f),
            profiles = profiles
        )
    }
}

@Composable
private fun ProfileListPanel(
    profiles: List<TerracottaProfile>,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(text = stringResource(R.string.terracotta_player_list))
        HorizontalDivider()

        val scrollState = rememberLazyListState()
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .scrollbar(
                    state = scrollState.scrollIndicatorState,
                    orientation = Orientation.Vertical,
                ),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            state = scrollState,
        ) {
            items(items = profiles, key = { it.toString() }) { profile ->
                TerracottaProfileLayout(profile = profile)
            }
        }
    }
}

@Composable
private fun TerracottaProfileLayout(
    profile: TerracottaProfile,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            maxLines = 2
        ) {
            MarqueeText(text = profile.name ?: stringResource(R.string.terracotta_player_anonymous))
            Text(text = stringResource(profile.type.textRes))
        }
        MarqueeText(
            modifier = Modifier.alpha(0.7f),
            text = profile.vendor,
            style = MaterialTheme.typography.labelSmall
        )
    }
}

@Composable
private fun ExceptionPanel(
    title: String,
    onExit: () -> Unit,
    onClose: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(text = title)
        RowButton(
            modifier = Modifier.fillMaxWidth(),
            icon = painterResource(R.drawable.ic_arrow_back),
            title = stringResource(R.string.terracotta_back),
            description = stringResource(R.string.terracotta_status_exception_back),
            onClick = onExit
        )
    }
}

@Composable
private fun SimpleCardButton(
    modifier: Modifier = Modifier,
    icon: Painter,
    title: String,
    description: String,
    onClick: () -> Unit,
    enabled: Boolean = true
) {
    BackgroundCard(
        modifier = modifier,
        influencedByBackground = false,
        onClick = onClick,
        enabled = enabled,
        colors = CardDefaults.cardColors(
            containerColor = itemColor(false),
            contentColor = onItemColor(),
            disabledContainerColor = itemColor(false)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                painter = icon,
                contentDescription = title
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    modifier = Modifier.fillMaxWidth(),
                    text = title,
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    modifier = Modifier
                        .fillMaxWidth()
                        .alpha(0.7f),
                    text = description,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

@Composable
private fun RowButton(
    modifier: Modifier = Modifier,
    icon: Painter,
    title: String,
    description: String,
    onClick: () -> Unit
) {
    Row(
        modifier = modifier
            .padding(all = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(
            modifier = Modifier.padding(4.dp),
            painter = icon,
            contentDescription = title
        )
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                modifier = Modifier.fillMaxWidth(),
                text = title,
                style = MaterialTheme.typography.titleSmall
            )
            MarqueeText(
                modifier = Modifier
                    .fillMaxWidth()
                    .alpha(0.7f),
                text = description,
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

private sealed interface MultiplayerOperation {
    data object None : MultiplayerOperation
    data object Notice : MultiplayerOperation
    data object WarningNotification : MultiplayerOperation
}

@Composable
private fun MultiplayerOperation(
    operation: MultiplayerOperation,
    onChange: (MultiplayerOperation) -> Unit,
    onNoticeRead: () -> Unit,
    onNoticeRefused: () -> Unit
) {
    when (operation) {
        is MultiplayerOperation.None -> {}
        is MultiplayerOperation.Notice -> {
            SimpleAlertDialog(
                title = stringResource(R.string.generic_warning),
                text = stringResource(R.string.terracotta_status_uninitialized_desc),
                dismissByDialog = false,
                onDismiss = onNoticeRefused,
                onConfirm = onNoticeRead
            )
        }
        is MultiplayerOperation.WarningNotification -> {
            NotificationCheck(
                text = stringResource(R.string.notification_data_terracotta_message),
                onGranted = {
                    onChange(MultiplayerOperation.None)
                },
                onIgnore = {
                    onChange(MultiplayerOperation.None)
                },
                onDismiss = {
                    onChange(MultiplayerOperation.None)
                }
            )
        }
    }
}

private data class TabItem(
    val text: Int
)

@Composable
private fun TutorialMenu(
    modifier: Modifier = Modifier
) {
    BackgroundCard(
        modifier = modifier
            .fillMaxHeight()
            .padding(vertical = 12.dp),
        shape = MaterialTheme.shapes.extraLarge
    ) {
        val tabs = remember {
            listOf(
                TabItem(R.string.terracotta_confirm_title),
                TabItem(R.string.terracotta_tutorial_host_tab),
                TabItem(R.string.terracotta_tutorial_guest_tab)
            )
        }

        val pagerState = rememberPagerState(pageCount = { tabs.size })
        var selectedTabIndex by remember { mutableIntStateOf(0) }

        LaunchedEffect(selectedTabIndex) {
            pagerState.animateScrollToPage(selectedTabIndex)
        }

        SecondaryTabRow(
            containerColor = influencedByBackgroundColor(
                color = cardTitleColor(),
                influencedAlpha = 0.5f * (AllSettings.launcherBackgroundOpacity.state.toFloat() / 100f)
            ),
            selectedTabIndex = selectedTabIndex
        ) {
            tabs.forEachIndexed { index, item ->
                Tab(
                    selected = index == selectedTabIndex,
                    onClick = {
                        selectedTabIndex = index
                    },
                    text = {
                        MarqueeText(text = stringResource(item.text))
                    }
                )
            }
        }

        HorizontalPager(
            state = pagerState,
            userScrollEnabled = false,
            modifier = Modifier.fillMaxWidth()
        ) { page ->
            when(page) {
                0 -> {
                    SingleTitleColumn(
                        modifier = Modifier.fillMaxSize(),
                        title = stringResource(R.string.terracotta_confirm_title),
                        text = {
                            BodyText(stringResource(R.string.terracotta_confirm_software))
                            BodyText(stringResource(R.string.terracotta_confirm_p2p))
                            BodyText(stringResource(R.string.terracotta_confirm_law))
                        }
                    )
                }
                1 -> {
                    DoubleTitleColumn(
                        modifier = Modifier.fillMaxSize(),
                        firstTitle = stringResource(R.string.terracotta_tutorial_host_tip),
                        firstText = {
                            BodyText(stringResource(R.string.terracotta_tutorial_step_enable_multiplayer))
                            BodyText(stringResource(R.string.terracotta_tutorial_step_open_multiplayer_menu))
                            BodyText(stringResource(R.string.terracotta_tutorial_host_step_become_host))
                            BodyText(stringResource(R.string.terracotta_tutorial_host_step_open_lan))
                            BodyText(stringResource(R.string.terracotta_tutorial_step_vpn_permission))
                            BodyText(stringResource(R.string.terracotta_tutorial_host_step_copy_invite))
                            BodyText(stringResource(R.string.terracotta_tutorial_host_step_send_invite))
                        },
                        secondTitle = stringResource(R.string.terracotta_tutorial_note_title),
                        secondText = {
                            BodyText(stringResource(R.string.terracotta_tutorial_step_offline_account_support))
                            BodyText(stringResource(R.string.terracotta_tutorial_step_interoperability))
                        }
                    )
                }
                2 -> {
                    DoubleTitleColumn(
                        modifier = Modifier.fillMaxSize(),
                        firstTitle = stringResource(R.string.terracotta_tutorial_guest_tip),
                        firstText = {
                            BodyText(stringResource(R.string.terracotta_tutorial_step_enable_multiplayer))
                            BodyText(stringResource(R.string.terracotta_tutorial_step_open_multiplayer_menu))
                            BodyText(stringResource(R.string.terracotta_tutorial_guest_step_become_guest))
                            BodyText(stringResource(R.string.terracotta_tutorial_step_vpn_permission))
                            BodyText(stringResource(R.string.terracotta_tutorial_guest_step_join_room))
                        },
                        secondTitle = stringResource(R.string.terracotta_tutorial_note_title),
                        secondText = {
                            BodyText(stringResource(R.string.terracotta_tutorial_step_offline_account_support))
                            BodyText(stringResource(R.string.terracotta_tutorial_step_interoperability))
                            BodyText(stringResource(R.string.terracotta_tutorial_guest_step_alternate_server))
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun SingleTitleColumn(
    modifier: Modifier = Modifier,
    title: String,
    text: @Composable ColumnScope.() -> Unit,
    scrollState: ScrollState = rememberScrollState()
) {
    TitleTextLayout(
        modifier = modifier
            .verticalScrollWithBar(scrollState)
            .padding(all = 16.dp),
        title = title,
        text = text
    )
}

@Composable
private fun DoubleTitleColumn(
    modifier: Modifier = Modifier,
    firstTitle: String,
    secondTitle: String,
    firstText: @Composable ColumnScope.() -> Unit,
    secondText: @Composable ColumnScope.() -> Unit,
    scrollState: ScrollState = rememberScrollState()
) {
    Column(
        modifier = modifier
            .verticalScrollWithBar(scrollState)
            .padding(all = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        TitleTextLayout(firstTitle, firstText)
        TitleTextLayout(secondTitle, secondText)
    }
}

@Composable
private fun TitleTextLayout(
    title: String,
    text: @Composable ColumnScope.() -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium
        )

        Column(
            modifier = Modifier.fillMaxWidth(),
            content = text
        )
    }
}

@Composable
private fun BodyText(
    text: String,
    modifier: Modifier = Modifier
) {
    Text(
        modifier = modifier,
        text = text,
        style = MaterialTheme.typography.bodySmall
    )
}
