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

import android.content.Context
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.movtery.zalithlauncher.R
import com.movtery.zalithlauncher.context.COPY_LABEL_ACCOUNT_UUID
import com.movtery.zalithlauncher.game.account.Account
import com.movtery.zalithlauncher.game.account.AccountsManager
import com.movtery.zalithlauncher.game.account.auth_server.data.AuthServer
import com.movtery.zalithlauncher.path.URL_ELY_BY_AUTH
import com.movtery.zalithlauncher.game.account.isAuthServerAccount
import com.movtery.zalithlauncher.game.account.isMicrosoftLogging
import com.movtery.zalithlauncher.game.account.yggdrasil.PlayerProfile
import com.movtery.zalithlauncher.setting.AllSettings
import com.movtery.zalithlauncher.setting.enums.ChromaMode
import com.movtery.zalithlauncher.ui.base.BaseScreen
import com.movtery.zalithlauncher.ui.components.BackgroundCard
import com.movtery.zalithlauncher.ui.components.MarqueeText
import com.movtery.zalithlauncher.ui.components.ModelAnimation
import com.movtery.zalithlauncher.ui.components.PlayerSkin
import com.movtery.zalithlauncher.ui.components.ScalingActionButton
import com.movtery.zalithlauncher.ui.components.ScalingLabel
import com.movtery.zalithlauncher.ui.components.SimpleAlertDialog
import com.movtery.zalithlauncher.ui.components.SimpleEditDialog
import com.movtery.zalithlauncher.ui.components.SimpleListDialog
import com.movtery.zalithlauncher.ui.components.WarningCard
import com.movtery.zalithlauncher.ui.screens.NormalNavKey
import com.movtery.zalithlauncher.ui.screens.content.elements.AccountItem
import com.movtery.zalithlauncher.ui.screens.content.elements.AccountOperation
import com.movtery.zalithlauncher.ui.screens.content.elements.AccountSkinOperation
import com.movtery.zalithlauncher.ui.screens.content.elements.ChangeSkinDialog
import com.movtery.zalithlauncher.ui.screens.content.elements.LocalLoginDialog
import com.movtery.zalithlauncher.ui.screens.content.elements.LocalLoginOperation
import com.movtery.zalithlauncher.ui.screens.content.elements.LoginMenuDialog
import com.movtery.zalithlauncher.ui.screens.content.elements.LoginMenuOperation
import com.movtery.zalithlauncher.ui.screens.content.elements.MicrosoftLoginOperation
import com.movtery.zalithlauncher.ui.screens.content.elements.MicrosoftLoginTipDialog
import com.movtery.zalithlauncher.ui.screens.content.elements.OtherLoginOperation
import com.movtery.zalithlauncher.ui.screens.content.elements.OtherServerLoginDialog
import com.movtery.zalithlauncher.ui.screens.content.elements.ServerOperation
import com.movtery.zalithlauncher.ui.screens.main.control_editor.InfoLayoutTextItem
import com.movtery.zalithlauncher.utils.animation.swapAnimateDpAsState
import com.movtery.zalithlauncher.utils.checkStoragePermissions
import com.movtery.zalithlauncher.utils.copyText
import com.movtery.zalithlauncher.utils.file.shareFile
import com.movtery.zalithlauncher.utils.settings.SettingsTransferUtils
import com.movtery.zalithlauncher.utils.string.getMessageOrToString
import com.movtery.zalithlauncher.viewmodel.AccountManageEffect
import com.movtery.zalithlauncher.viewmodel.AccountManageIntent
import com.movtery.zalithlauncher.viewmodel.AccountManageViewModel
import com.movtery.zalithlauncher.viewmodel.ErrorViewModel
import com.movtery.zalithlauncher.viewmodel.LocalBackgroundViewModel
import com.movtery.zalithlauncher.viewmodel.ScreenBackStackViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * å°è£è´¦å·çé¢ UI äº¤äºçåè°å½æ°
 * 
 * @property onIntent åé MVI Intent å° ViewModel
 * @property openLink æå¼å¤é¨é¾æ¥
 * @property backToMainScreen è¿åä¸»çé¢
 * @property navigateToWeb å¯¼èªå°åºç¨åæµè§å¨çé¢
 * @property checkIfInWebScreen æ£æ¥å½åæ¯å¦å¨æµè§å¨çé¢ä¸­ï¼ç¨äºå¾®è½¯ç»å½é»è¾å¤æ­ï¼
 * @property formatError æ ¼å¼åå¼å¸¸ä¸ºæ¬å°åå­ç¬¦ä¸²
 * @property submitError æäº¤éè¯¯å°å¨å±éè¯¯å±ç¤ºç³»ç»
 */
private data class AccountActions(
    val onIntent: (AccountManageIntent) -> Unit,
    val openLink: (url: String) -> Unit,
    val backToMainScreen: () -> Unit,
    val navigateToWeb: (url: String) -> Unit,
    val checkIfInWebScreen: () -> Boolean,
    val formatError: (Context, Throwable) -> String,
    val submitError: (ErrorViewModel.ThrowableMessage) -> Unit,
)

/**
 * è¿å¥è´¦å·ç®¡çå¨æ¶ï¼å¯éå çæå¼ç»å½èåéé¡¹
 */
enum class FirstLoginMenu {
    /** ä¸æå¼èå */
    NONE,
    /** æå¼å¾®è½¯ç»å½èå */
    MICROSOFT,
    /** æå¼æ»ç»å½èå */
    NORMAL
}

/**
 * è´¦å·ç®¡çä¸»çé¢
 *
 * @param backStackViewModel å±å¹å æ ç®¡çå¨
 * @param backToMainScreen è¿åä¸»å±å¹çåè°
 * @param openLink å¤é¨é¾æ¥è·³è½¬åè°
 * @param submitError å¨å±éè¯¯æäº¤åè°
 * @param viewModel è´¦å·ç®¡ç ViewModel (Hilt èªå¨æ³¨å¥)
 */
@Composable
fun AccountManageScreen(
    key: NormalNavKey.AccountManager,
    backStackViewModel: ScreenBackStackViewModel,
    backToMainScreen: () -> Unit,
    openLink: (url: String) -> Unit,
    submitError: (ErrorViewModel.ThrowableMessage) -> Unit,
    viewModel: AccountManageViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val loginUiState by viewModel.loginUiState.collectAsStateWithLifecycle()
    val profileUiState by viewModel.profileUiState.collectAsStateWithLifecycle()
    val operationUiState by viewModel.operationUiState.collectAsStateWithLifecycle()

    val actions = remember(
        viewModel,
        backToMainScreen,
        openLink,
        backStackViewModel,
        submitError
    ) {
        AccountActions(
            onIntent = viewModel::onIntent,
            openLink = openLink,
            backToMainScreen = backToMainScreen,
            navigateToWeb = { url -> backStackViewModel.mainScreen.backStack.navigateToWeb(url) },
            checkIfInWebScreen = { backStackViewModel.mainScreen.currentKey is NormalNavKey.WebScreen },
            formatError = { _, th -> viewModel.formatAccountError(th) },
            submitError = submitError,
        )
    }

    LaunchedEffect(Unit) {
        when (key.loginMenu) {
            FirstLoginMenu.NONE -> {}
            FirstLoginMenu.MICROSOFT -> {
                actions.onIntent(AccountManageIntent.UpdateMicrosoftLoginOp(MicrosoftLoginOperation.Tip))
            }
            FirstLoginMenu.NORMAL -> {
                actions.onIntent(AccountManageIntent.UpdateLoginMenuOp(LoginMenuOperation.Login))
            }
        }

        viewModel.effect.collect { effect ->
            when (effect) {
                is AccountManageEffect.ShowError -> {
                    submitError(ErrorViewModel.ThrowableMessage(effect.title, effect.message))
                }

                is AccountManageEffect.ShowToast -> {
                    val message = if (effect.formatArgs.isEmpty()) {
                        context.getString(effect.messageRes)
                    } else {
                        context.getString(effect.messageRes, *effect.formatArgs.toTypedArray())
                    }
                    Toast.makeText(context, message, effect.duration).show()
                }
            }
        }
    }

    BaseScreen(
        screenKey = key,
        currentKey = backStackViewModel.mainScreen.currentKey
    ) { isVisible ->
        AccountManageContent(
            isVisible = isVisible,
            loginUiState = loginUiState,
            profileUiState = profileUiState,
            operationUiState = operationUiState,
            actions = actions
        )
    }
}

/**
 * è´¦å·ç®¡ççé¢çå®éåå®¹å¸å±
 */
@Composable
private fun AccountManageContent(
    isVisible: Boolean,
    loginUiState: AccountManageViewModel.LoginUiState,
    profileUiState: AccountManageViewModel.ProfileUiState,
    operationUiState: AccountManageViewModel.OperationUiState,
    actions: AccountActions
) {
    Row(
        modifier = Modifier.fillMaxSize()
    ) {
        ActionsLayout(
            isVisible = isVisible,
            modifier = Modifier
                .fillMaxHeight()
                .padding(all = 12.dp)
                .weight(3f),
            currentAccount = profileUiState.currentAccount,
            isOffline = profileUiState.isOffline,
            actions = actions
        )

        AccountsLayout(
            isVisible = isVisible,
            modifier = Modifier
                .fillMaxHeight()
                .padding(top = 12.dp, end = 12.dp, bottom = 12.dp)
                .weight(7f),
            accounts = profileUiState.accounts,
            currentAccount = profileUiState.currentAccount,
            isOffline = profileUiState.isOffline,
            accountOperation = operationUiState.accountOp,
            accountSkinOperation = operationUiState.accountSkinOp,
            accountSkinDialogState = operationUiState.accountSkinDialogState,
            accountCapes = profileUiState.accountCapeOpMap,
            actions = actions
        )
    }

    LoginMenuOperation(loginUiState.menuOp, actions, profileUiState.authServers)
    MicrosoftLoginOperation(loginUiState.microsoftOp, actions)
    LocalLoginOperation(loginUiState.localOp, actions)
    OtherLoginOperation(loginUiState.otherOp, actions)
    ServerTypeOperation(operationUiState.serverOp, actions)
}

/**
 * å·¦ä¾§ç»å½æ¹å¼èåç»ä»¶
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun ActionsLayout(
    isVisible: Boolean,
    modifier: Modifier = Modifier,
    currentAccount: Account?,
    isOffline: Boolean,
    actions: AccountActions
) {
    val xOffset by swapAnimateDpAsState(
        targetValue = (-40).dp,
        swapIn = isVisible,
        isHorizontal = true
    )

    Column(
        modifier = modifier
            .offset { IntOffset(x = xOffset.roundToPx(), y = 0) }
            .fillMaxHeight()
    ) {
        //ç©å®¶æ¨¡åé¢è§
        val refreshWardrobe by AccountsManager.refreshWardrobe.collectAsStateWithLifecycle()
        val accountSkin = remember(currentAccount, refreshWardrobe) {
            currentAccount?.getSkinFile()?.takeIf { it.exists() }
        }
        val accountCape = remember(currentAccount, refreshWardrobe) {
            currentAccount?.getCapeFile()?.takeIf { it.exists() }
        }
        val context = LocalContext.current
        val playerSkin = remember {
            PlayerSkin(context)
        }
        var pageFinished by remember { mutableStateOf(false) }

        DisposableEffect(Unit) {
            onDispose {
                playerSkin.destroy()
            }
        }

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { context ->
                    playerSkin.loadWebView(
                        context = context,
                        onPageFinished = {
                            pageFinished = true
                            playerSkin.startAnim(ModelAnimation.NewIdle)
                            playerSkin.setAzimuthAndPitch(-35, 10)
                        }
                    )
                },
                update = {
                    if (pageFinished) {
                        runCatching {
                            accountSkin?.inputStream().use { inputStream ->
                                playerSkin.loadSkin(inputStream, currentAccount?.skinModelType)
                            }
                        }
                        runCatching {
                            accountCape?.inputStream().use { inputStream ->
                                playerSkin.loadCape(inputStream)
                            }
                        }
                    }
                }
            )
            if (!pageFinished) {
                LoadingIndicator()
            }
        }

        //æ·»å è´¦å·
        ScalingActionButton(
            modifier = Modifier
                .fillMaxWidth(),
            onClick = {
                if (isOffline) {
                    //éæ­£çç¶æä¸ï¼åªåè®¸åå»ºå¾®è½¯è´¦å·
                    actions.onIntent(AccountManageIntent.UpdateMicrosoftLoginOp(MicrosoftLoginOperation.Tip))
                } else {
                    actions.onIntent(AccountManageIntent.UpdateLoginMenuOp(LoginMenuOperation.Login))
                }
            }
        ) {
            MarqueeText(text = stringResource(R.string.account_add_new_account))
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Chroma toggle
        var showChromaSelector by remember { mutableStateOf(false) }
        
        InfoLayoutTextItem(
            modifier = Modifier.fillMaxWidth(),
            title = stringResource(R.string.settings_chroma_name_title),
            icon = {
                Icon(
                    modifier = Modifier.size(22.dp),
                    painter = painterResource(R.drawable.ic_styler),
                    contentDescription = null
                )
            },
            onClick = {
                showChromaSelector = true
            }
        )

        if (showChromaSelector) {
            val modes = ChromaMode.entries
            SimpleListDialog(
                title = stringResource(R.string.settings_chroma_name_title),
                items = modes,
                itemTextProvider = { mode ->
                    when (mode) {
                        ChromaMode.NONE -> context.getString(R.string.generic_none)
                        ChromaMode.RGB -> "RGB (Classic)"
                        ChromaMode.RED_BLUE -> context.getString(R.string.chroma_mode_red_blue)
                        ChromaMode.SUNSET -> context.getString(R.string.chroma_mode_sunset)
                        ChromaMode.OCEAN -> context.getString(R.string.chroma_mode_ocean)
                        ChromaMode.FOREST -> context.getString(R.string.chroma_mode_forest)
                        ChromaMode.NEON -> context.getString(R.string.chroma_mode_neon)
                    }
                },
                onItemSelected = { mode ->
                    AllSettings.chromaMode.save(mode)
                    showChromaSelector = false
                },
                current = AllSettings.chromaMode.state,
                onDismissRequest = {
                    showChromaSelector = false
                }
            )
        }
    }
}

@Composable
private fun LoginMenuOperation(
    operation: LoginMenuOperation,
    actions: AccountActions,
    authServers: List<AuthServer>
) {
    when (operation) {
        LoginMenuOperation.None -> {}
        LoginMenuOperation.Login -> {
            LoginMenuDialog(
                onDismissRequest = {
                    actions.onIntent(
                        AccountManageIntent.UpdateLoginMenuOp(LoginMenuOperation.None)
                    )
                },
                authServers = authServers,
                onMicrosoftLogin = {
                    if (!isMicrosoftLogging()) {
                        actions.onIntent(
                            AccountManageIntent.UpdateMicrosoftLoginOp(
                                MicrosoftLoginOperation.Tip
                            )
                        )
                    }
                },
                onLocalLogin = {
                    actions.onIntent(AccountManageIntent.UpdateLocalLoginOp(LocalLoginOperation.Edit))
                },
                onElyByLogin = {
                    actions.onIntent(
                        AccountManageIntent.UpdateOtherLoginOp(
                            OtherLoginOperation.OnLogin(
                                AuthServer(
                                    baseUrl = URL_ELY_BY_AUTH,
                                    serverName = "Ely.by",
                                    register = "https://ely.by/registration"
                                )
                            )
                        )
                    )
                },
                onAuthServerLogin = { server ->
                    actions.onIntent(
                        AccountManageIntent.UpdateOtherLoginOp(
                            OtherLoginOperation.OnLogin(server)
                        )
                    )
                },
                onAddAuthServer = {
                    actions.onIntent(AccountManageIntent.UpdateServerOp(ServerOperation.AddNew))
                },
                onDeleteAuthServer = { server ->
                    actions.onIntent(
                        AccountManageIntent.UpdateServerOp(
                            ServerOperation.Delete(server)
                        )
                    )
                }
            )
        }
    }
}

/**
 * å¾®è½¯ç»å½ç¸å³é»è¾å¤ç
 */
@Composable
private fun MicrosoftLoginOperation(
    operation: MicrosoftLoginOperation,
    actions: AccountActions
) {
    when (operation) {
        is MicrosoftLoginOperation.None -> {}
        is MicrosoftLoginOperation.Tip -> {
            MicrosoftLoginTipDialog(
                onDismissRequest = {
                    actions.onIntent(
                        AccountManageIntent.UpdateMicrosoftLoginOp(
                            MicrosoftLoginOperation.None
                        )
                    )
                },
                onConfirm = {
                    actions.onIntent(
                        AccountManageIntent.UpdateMicrosoftLoginOp(
                            MicrosoftLoginOperation.None
                        )
                    )
                    actions.onIntent(
                        AccountManageIntent.PerformMicrosoftLogin(
                            toWeb = actions.navigateToWeb,
                            backToMain = actions.backToMainScreen,
                            checkIfInWebScreen = actions.checkIfInWebScreen
                        )
                    )
                },
                openLink = actions.openLink
            )
        }
    }
}

/**
 * ç¦»çº¿è´¦å·ç»å½ç¸å³é»è¾å¤ç
 */
@Composable
private fun LocalLoginOperation(
    operation: LocalLoginOperation,
    actions: AccountActions
) {
    when (operation) {
        is LocalLoginOperation.None -> {}
        is LocalLoginOperation.Edit -> {
            LocalLoginDialog(
                onDismissRequest = {
                    actions.onIntent(
                        AccountManageIntent.UpdateLocalLoginOp(
                            LocalLoginOperation.None
                        )
                    )
                },
                onConfirm = { isInvalid, name, uuid ->
                    val nextOp = if (isInvalid) LocalLoginOperation.Alert(
                        name,
                        uuid
                    ) else LocalLoginOperation.Create(name, uuid)
                    actions.onIntent(AccountManageIntent.UpdateLocalLoginOp(nextOp))
                },
                openLink = actions.openLink
            )
        }

        is LocalLoginOperation.Create -> {
            LaunchedEffect(operation) {
                actions.onIntent(
                    AccountManageIntent.CreateLocalAccount(
                        operation.userName,
                        operation.userUUID
                    )
                )
            }
        }

        is LocalLoginOperation.Alert -> {
            SimpleAlertDialog(
                title = stringResource(R.string.account_supporting_username_invalid_title),
                text = {
                    Text(text = stringResource(R.string.account_supporting_username_invalid_local_message_hint1))
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = stringResource(R.string.account_supporting_username_invalid_local_message_hint2),
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(text = stringResource(R.string.account_supporting_username_invalid_local_message_hint3))
                    Text(text = stringResource(R.string.account_supporting_username_invalid_local_message_hint4))
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = stringResource(R.string.account_supporting_username_invalid_local_message_hint5),
                        fontWeight = FontWeight.Bold
                    )
                },
                confirmText = stringResource(R.string.account_supporting_username_invalid_still_use),
                onConfirm = {
                    actions.onIntent(
                        AccountManageIntent.UpdateLocalLoginOp(
                            LocalLoginOperation.Create(operation.userName, operation.userUUID)
                        )
                    )
                },
                onCancel = {
                    actions.onIntent(
                        AccountManageIntent.UpdateLocalLoginOp(
                            LocalLoginOperation.None
                        )
                    )
                }
            )
        }
    }
}

/**
 * ç¬¬ä¸æ¹éªè¯æå¡å¨ç»å½é»è¾å¤ç
 */
@Composable
private fun OtherLoginOperation(
    operation: OtherLoginOperation,
    actions: AccountActions
) {
    val context = LocalContext.current
    val loggingInFailedTitle = stringResource(R.string.account_logging_in_failed)

    when (operation) {
        is OtherLoginOperation.None -> {}
        is OtherLoginOperation.OnLogin -> {
            OtherServerLoginDialog(
                server = operation.server,
                onRegisterClick = { url ->
                    actions.openLink(url)
                    actions.onIntent(AccountManageIntent.UpdateOtherLoginOp(OtherLoginOperation.None))
                },
                onDismissRequest = {
                    actions.onIntent(
                        AccountManageIntent.UpdateOtherLoginOp(
                            OtherLoginOperation.None
                        )
                    )
                },
                onConfirm = { email, password ->
                    actions.onIntent(AccountManageIntent.UpdateOtherLoginOp(OtherLoginOperation.None))
                    actions.onIntent(
                        AccountManageIntent.LoginWithOtherServer(
                            operation.server,
                            email,
                            password
                        )
                    )
                }
            )
        }

        is OtherLoginOperation.OnFailed -> {
            val message = actions.formatError(context, operation.th)
            LaunchedEffect(operation) {
                actions.submitError(
                    ErrorViewModel.ThrowableMessage(
                        title = loggingInFailedTitle,
                        message = message
                    )
                )
                actions.onIntent(AccountManageIntent.UpdateOtherLoginOp(OtherLoginOperation.None))
            }
        }

        is OtherLoginOperation.SelectRole -> {
            SimpleListDialog(
                title = stringResource(R.string.account_other_login_select_role),
                items = operation.profiles,
                itemTextProvider = { it.name },
                onItemSelected = { operation.selected(it) },
                onDismissRequest = {
                    actions.onIntent(
                        AccountManageIntent.UpdateOtherLoginOp(
                            OtherLoginOperation.None
                        )
                    )
                }
            )
        }
    }
}

/**
 * éªè¯æå¡å¨ç®¡çæä½é»è¾å¤ç
 */
@Composable
private fun ServerTypeOperation(
    operation: ServerOperation,
    actions: AccountActions
) {
    val addingFailureTitle = stringResource(R.string.account_other_login_adding_failure)

    when (operation) {
        is ServerOperation.AddNew -> {
            var serverUrl by rememberSaveable { mutableStateOf("") }
            SimpleEditDialog(
                title = stringResource(R.string.account_add_new_server),
                value = serverUrl,
                onValueChange = { serverUrl = it.trim() },
                label = { Text(text = stringResource(R.string.account_label_server_url)) },
                singleLine = true,
                onDismissRequest = {
                    actions.onIntent(
                        AccountManageIntent.UpdateServerOp(
                            ServerOperation.None
                        )
                    )
                },
                onConfirm = {
                    if (serverUrl.isNotEmpty()) {
                        actions.onIntent(AccountManageIntent.AddServer(serverUrl))
                    }
                }
            )
        }

        is ServerOperation.Delete -> {
            SimpleAlertDialog(
                title = stringResource(R.string.account_other_login_delete_server_title),
                text = stringResource(
                    R.string.account_other_login_delete_server_message,
                    operation.server.serverName
                ),
                onDismiss = { actions.onIntent(AccountManageIntent.UpdateServerOp(ServerOperation.None)) },
                onConfirm = { actions.onIntent(AccountManageIntent.DeleteServer(operation.server)) }
            )
        }

        is ServerOperation.OnThrowable -> {
            val message = operation.throwable.getMessageOrToString()
            LaunchedEffect(operation) {
                actions.submitError(
                    ErrorViewModel.ThrowableMessage(
                        title = addingFailureTitle,
                        message = message
                    )
                )
                actions.onIntent(AccountManageIntent.UpdateServerOp(ServerOperation.None))
            }
        }

        is ServerOperation.None -> {}
    }
}

/**
 * è´¦å·åè¡¨ç»ä»¶
 */
@Composable
private fun AccountsLayout(
    isVisible: Boolean,
    modifier: Modifier = Modifier,
    accounts: List<Account>,
    currentAccount: Account?,
    isOffline: Boolean,
    accountOperation: AccountOperation,
    accountSkinOperation: AccountSkinOperation,
    accountSkinDialogState: AccountManageViewModel.AccountSkinDialogState,
    accountCapes: Map<String, List<PlayerProfile.Cape>>,
    actions: AccountActions
) {
    val yOffset by swapAnimateDpAsState(targetValue = (-40).dp, swapIn = isVisible)
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            scope.launch {
                val success = SettingsTransferUtils.importData(context, it)
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        context,
                        if (success) R.string.settings_import_success else R.string.settings_import_failed,
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    AccountOperation(accountOperation, actions)

    AccountSkinOperation(
        accountSkinOperation = accountSkinOperation,
        skinDialogState = accountSkinDialogState,
        accountCapes = accountCapes,
        actions = actions
    )

    Column(
        modifier = modifier.offset { IntOffset(x = 0, y = yOffset.roundToPx()) }
    ) {
        if (AllSettings.showSettingsTip.state) {
            WarningCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                title = stringResource(R.string.generic_info),
                text = { Text(stringResource(R.string.settings_tip_import_export)) },
                onDismiss = { AllSettings.showSettingsTip.save(false) }
            )
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            BackgroundCard(
                modifier = Modifier.fillMaxSize(),
                shape = MaterialTheme.shapes.extraLarge
            ) {
                if (accounts.isNotEmpty()) {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(MaterialTheme.shapes.extraLarge),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        items(accounts, key = { it.uniqueUUID }) { account ->
                            AccountItem(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 6.dp),
                                currentAccount = currentAccount,
                                account = account,
                                enabled = !isOffline, //éæ­£çç¶æä¸ä¸åè®¸éæ©ä»»ä½ç¶æ
                                onSelected = { AccountsManager.setCurrentAccount(it) },
                                openChangeSkinDialog = {
                                    if (!account.isAuthServerAccount()) {
                                        actions.onIntent(
                                            AccountManageIntent.UpdateAccountSkinOp(
                                                AccountSkinOperation.ChangeSkin(account)
                                            )
                                        )
                                    }
                                },
                                onRefreshClick = {
                                    actions.onIntent(
                                        AccountManageIntent.RefreshAccount(
                                            account
                                        )
                                    )
                                },
                                onCopyUUID = {
                                    copyText(COPY_LABEL_ACCOUNT_UUID, account.profileId, context, false)
                                    android.widget.Toast.makeText(
                                        context,
                                        context.getString(
                                            R.string.account_local_uuid_copied,
                                            account.username
                                        ),
                                        android.widget.Toast.LENGTH_SHORT
                                    ).show()
                                },
                                onDeleteClick = {
                                    actions.onIntent(
                                        AccountManageIntent.UpdateAccountOp(
                                            AccountOperation.Delete(account)
                                        )
                                    )
                                }
                            )
                        }
                    }
                } else {
                    Box(modifier = Modifier.fillMaxSize()) {
                        ScalingLabel(
                            modifier = Modifier.align(Alignment.Center),
                            text = stringResource(R.string.account_no_account)
                        )
                    }
                }
            }

            // Import/Export buttons at bottom right
            Row(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilledTonalButton(
                    onClick = {
                        val activity = context as? android.app.Activity ?: return@FilledTonalButton
                        checkStoragePermissions(
                            activity = activity,
                            title = R.string.storage_permission_request_title,
                            message = context.getString(R.string.storage_permission_request_message),
                            hasPermission = {
                                scope.launch {
                                    val file = SettingsTransferUtils.exportAccounts(context)
                                    withContext(Dispatchers.Main) {
                                        if (file != null) {
                                            Toast.makeText(context, context.getString(R.string.settings_export_success, file.absolutePath), Toast.LENGTH_LONG).show()
                                        } else {
                                            Toast.makeText(context, R.string.settings_export_failed, Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                }
                            }
                        )
                    }
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_share_filled),
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.size(4.dp))
                    Text(stringResource(R.string.settings_export_accounts), style = MaterialTheme.typography.labelMedium)
                }
                FilledTonalButton(
                    onClick = {
                        importLauncher.launch("application/json")
                    }
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_upload),
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.size(4.dp))
                    Text(stringResource(R.string.settings_import_accounts), style = MaterialTheme.typography.labelMedium)
                }
            }
        }
    }
}

/**
 * è´¦å·ç®è¤æä½é»è¾å¤ç
 */
@Composable
private fun AccountSkinOperation(
    accountSkinOperation: AccountSkinOperation,
    skinDialogState: AccountManageViewModel.AccountSkinDialogState,
    accountCapes: Map<String, List<PlayerProfile.Cape>>,
    actions: AccountActions
) {
    when (accountSkinOperation) {
        is AccountSkinOperation.None -> {}
        is AccountSkinOperation.ChangeSkin -> {
            val account = accountSkinOperation.account
            ChangeSkinDialog(
                account = account,
                availableCapes = accountCapes[account.uniqueUUID] ?: emptyList(),
                skinState = skinDialogState.pendingSkinData,
                onSkinStateChange = { skinState ->
                    actions.onIntent(
                        AccountManageIntent.UpdatePendingSkinData(
                            skinState
                        )
                    )
                },
                capeState = skinDialogState.pendingCapeData,
                onCapeStateChange = { capeState ->
                    actions.onIntent(
                        AccountManageIntent.UpdatePendingCapeData(
                            capeState
                        )
                    )
                },
                isImportingSkin = skinDialogState.importingSkin,
                isImportingCape = skinDialogState.importingCape,
                onSkinPicked = { uri ->
                    actions.onIntent(
                        AccountManageIntent.OnSkinPicked(uri)
                    )
                },
                onCapePicked = { account, uri ->
                    actions.onIntent(
                        AccountManageIntent.OnCapePicked(account, uri)
                    )
                },
                onDismissRequest = {
                    actions.onIntent(AccountManageIntent.ResetAccountSkinDialogState)
                    actions.onIntent(AccountManageIntent.UpdateAccountSkinOp(AccountSkinOperation.None))
                },
                onResetSkin = {
                    actions.onIntent(AccountManageIntent.ResetSkin(account))
                },
                onResetCape = {
                    actions.onIntent(AccountManageIntent.ResetCape(account))
                },
                onFetchCapes = {
                    actions.onIntent(AccountManageIntent.FetchMicrosoftCapes(account))
                },
                onApplySkin = { file, model ->
                    actions.onIntent(AccountManageIntent.ApplySkin(account, file, model))
                },
                onApplyCape = { cape ->
                    actions.onIntent(AccountManageIntent.ApplyMicrosoftCape(account, cape))
                },
                onApplyCustomCape = { file ->
                    actions.onIntent(AccountManageIntent.ApplyCustomCape(account, file))
                }
            )
        }
    }
}

/**
 * éç¨è´¦å·ç®¡çæä½é»è¾å¤çï¼å¦å é¤ç¡®è®¤ï¼
 */
@Composable
private fun AccountOperation(
    operation: AccountOperation,
    actions: AccountActions
) {
    val context = LocalContext.current
    val loggingInFailedTitle = stringResource(R.string.account_logging_in_failed)

    when (operation) {
        is AccountOperation.Delete -> {
            SimpleAlertDialog(
                title = stringResource(R.string.account_delete_title),
                text = stringResource(R.string.account_delete_message, operation.account.username),
                onConfirm = { actions.onIntent(AccountManageIntent.DeleteAccount(operation.account)) },
                onDismiss = { actions.onIntent(AccountManageIntent.UpdateAccountOp(AccountOperation.None)) }
            )
        }

        is AccountOperation.OnFailed -> {
            val message = actions.formatError(context, operation.th)
            LaunchedEffect(operation) {
                actions.submitError(
                    ErrorViewModel.ThrowableMessage(
                        title = loggingInFailedTitle,
                        message = message
                    )
                )
                actions.onIntent(AccountManageIntent.UpdateAccountOp(AccountOperation.None))
            }
        }

        is AccountOperation.None -> {}
    }
}

@Preview(showBackground = true, widthDp = 800, heightDp = 480)
@Composable
private fun AccountManageContentPreview() {
    CompositionLocalProvider(LocalBackgroundViewModel provides null) {
        MaterialExpressiveTheme {
            Surface {
                AccountManageContent(
                    isVisible = true,
                    loginUiState = AccountManageViewModel.LoginUiState(),
                    profileUiState = AccountManageViewModel.ProfileUiState(),
                    operationUiState = AccountManageViewModel.OperationUiState(),
                    actions = AccountActions(
                        onIntent = {},
                        openLink = {},
                        backToMainScreen = {},
                        navigateToWeb = {},
                        checkIfInWebScreen = { false },
                        formatError = { _, _ -> "" },
                        submitError = {},
                    )
                )
            }
        }
    }
}
