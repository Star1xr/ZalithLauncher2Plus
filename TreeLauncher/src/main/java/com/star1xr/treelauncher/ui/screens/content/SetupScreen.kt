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

package com.star1xr.treelauncher.ui.screens.content

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.star1xr.treelauncher.BuildConfig
import com.star1xr.treelauncher.R
import com.star1xr.treelauncher.components.UnpackTasksManager
import com.star1xr.treelauncher.game.account.AccountsManager
import com.star1xr.treelauncher.game.version.installed.Version
import com.star1xr.treelauncher.game.version.installed.VersionsManager
import com.star1xr.treelauncher.setting.AllSettings
import com.star1xr.treelauncher.ui.base.BaseScreen
import com.star1xr.treelauncher.ui.screens.NormalNavKey
import com.star1xr.treelauncher.ui.screens.navigateTo
import com.star1xr.treelauncher.viewmodel.ScreenBackStackViewModel

@Composable
fun SetupScreen(
    backStackViewModel: ScreenBackStackViewModel,
    onFinished: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    LaunchedEffect(Unit) {
        UnpackTasksManager.initUnpackItems(context)
        if (!UnpackTasksManager.areAllTasksFinished()) {
            UnpackTasksManager.startAllTask(scope)
        }
    }

    val accounts by AccountsManager.accountsFlow.collectAsStateWithLifecycle()
    val versions by VersionsManager.versionsFlow.collectAsStateWithLifecycle()
    val unpackProgress by UnpackTasksManager.progress.collectAsStateWithLifecycle()

    var step by rememberSaveable {
        mutableIntStateOf(
            if (!UnpackTasksManager.areAllTasksFinished()) -1
            else if (BuildConfig.DEBUG) 0
            else if (accounts.isEmpty()) 1
            else 3
        )
    }

    LaunchedEffect(unpackProgress) {
        if (step == -1 && UnpackTasksManager.areAllTasksFinished()) {
            step = if (BuildConfig.DEBUG) 0 else 1
        }
    }

    LaunchedEffect(accounts) {
        if (step == 2 && accounts.isNotEmpty()) {
            step = 3
        }
    }

    BaseScreen(
        screenKey = NormalNavKey.Setup,
        currentKey = backStackViewModel.mainScreen.currentKey
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background.copy(alpha = 0.8f)
        ) {
            AnimatedContent(
                targetState = step,
                transitionSpec = {
                    fadeIn(animationSpec = tween(500)) togetherWith fadeOut(animationSpec = tween(500))
                },
                label = "SetupSteps"
            ) { targetStep ->
                Column(
                    modifier = Modifier.fillMaxSize().padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    when (targetStep) {
                        -1 -> RequiredFilesStep(unpackProgress)
                        0 -> DebugGreeting { step = 1 }
                        1 -> WelcomeStep { step = 2 }
                        2 -> AccountStep(
                            onCreateAccount = {
                                backStackViewModel.mainScreen.navigateTo(NormalNavKey.AccountManager(FirstLoginMenu.NORMAL))
                            },
                            onNext = { step = 3 }
                        )
                        3 -> VersionStep(
                            versions = versions,
                            onDownloadVersion = { backStackViewModel.navigateToDownload() },
                            onFinish = {
                                AllSettings.setupCompleted.save(true)
                                onFinished()
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RequiredFilesStep(progress: Float) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = stringResource(R.string.setup_required_files_title, (progress * 100).toInt()),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(24.dp))
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier.fillMaxWidth().height(8.dp),
            strokeCap = androidx.compose.ui.graphics.StrokeCap.Round
        )
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = stringResource(R.string.setup_required_files_subtitle),
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun DebugGreeting(onNext: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(painter = painterResource(R.drawable.ic_build_filled), contentDescription = null, modifier = Modifier.size(64.dp))
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = stringResource(R.string.launcher_version_debug_warning, "TreeLauncher"),
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(onClick = onNext) { Text(stringResource(R.string.setup_debug_next)) }
    }
}

@Composable
private fun WelcomeStep(onNext: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(stringResource(R.string.setup_welcome_title), style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(32.dp))
        Button(onClick = onNext, modifier = Modifier.fillMaxWidth(0.6f)) { Text(stringResource(R.string.setup_welcome_button)) }
    }
}

@Composable
private fun AccountStep(onCreateAccount: () -> Unit, onNext: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(stringResource(R.string.setup_account_title), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(32.dp))
        Button(onClick = onCreateAccount, modifier = Modifier.fillMaxWidth(0.6f)) { Text(stringResource(R.string.setup_account_add)) }
        Spacer(modifier = Modifier.height(12.dp))
        TextButton(onClick = onNext) { Text(stringResource(R.string.setup_account_skip)) }
    }
}

@Composable
private fun VersionStep(versions: List<Version>, onDownloadVersion: () -> Unit, onFinish: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxHeight()) {
        Text(stringResource(R.string.setup_version_title), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(16.dp))
        
        if (versions.isEmpty()) {
            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                Text(
                    text = stringResource(R.string.versions_manage_no_versions),
                    modifier = Modifier.alpha(0.6f)
                )
            }
        } else {
            LazyColumn(modifier = Modifier.weight(1f).fillMaxWidth()) {
                items(versions) { version ->
                    Surface(
                        modifier = Modifier.padding(vertical = 4.dp).fillMaxWidth(),
                        shape = MaterialTheme.shapes.medium,
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    ) {
                        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(painterResource(R.drawable.ic_sort), null, modifier = Modifier.size(24.dp))
                            Spacer(Modifier.width(12.dp))
                            Text(version.getVersionName(), fontWeight = FontWeight.Medium)
                        }
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = onDownloadVersion, modifier = Modifier.fillMaxWidth(0.6f)) { Text(stringResource(R.string.setup_version_download)) }
        Spacer(modifier = Modifier.height(8.dp))
        Button(
            onClick = onFinish, 
            modifier = Modifier.fillMaxWidth(0.6f),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
        ) { 
            Text(stringResource(R.string.setup_version_finish)) 
        }
    }
}
