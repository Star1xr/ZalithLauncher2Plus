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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.star1xr.treelauncher.BuildConfig
import com.star1xr.treelauncher.R
import com.star1xr.treelauncher.components.UnpackTasksManager
import com.star1xr.treelauncher.game.account.AccountsManager
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
        Icon(painter = painterResource(R.drawable.ic_build_filled), contentDescription = null)
        Text(
            text = stringResource(R.string.launcher_version_debug_warning, "TreeLauncher"),
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = onNext) { Text("Devam Et") }
    }
}

@Composable
private fun WelcomeStep(onNext: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text("TreeLauncher'a Hoş Geldiniz", style = MaterialTheme.typography.headlineMedium)
        Button(onClick = onNext) { Text("Başla") }
    }
}

@Composable
private fun AccountStep(onCreateAccount: () -> Unit, onNext: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text("Hesap Oluşturun", style = MaterialTheme.typography.headlineMedium)
        Button(onClick = onCreateAccount) { Text("Hesap Ekle") }
        Button(onClick = onNext) { Text("Zaten Hesabım Var") }
    }
}

@Composable
private fun VersionStep(onDownloadVersion: () -> Unit, onFinish: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text("Oyun İndirin", style = MaterialTheme.typography.headlineMedium)
        Button(onClick = onDownloadVersion) { Text("Sürüm İndir") }
        Button(onClick = onFinish) { Text("Bitir") }
    }
}
