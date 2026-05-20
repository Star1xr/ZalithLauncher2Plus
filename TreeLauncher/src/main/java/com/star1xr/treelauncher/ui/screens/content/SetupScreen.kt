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

import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
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
import com.star1xr.treelauncher.components.InstallableItem
import com.star1xr.treelauncher.components.UnpackTasksManager
import com.star1xr.treelauncher.game.account.AccountsManager
import com.star1xr.treelauncher.game.version.installed.Version
import com.star1xr.treelauncher.game.version.installed.VersionsManager
import com.star1xr.treelauncher.ui.screens.content.elements.VersionIconImage
import com.star1xr.treelauncher.setting.AllSettings
import com.star1xr.treelauncher.setting.enums.VersionIconStyle
import com.star1xr.treelauncher.ui.base.BaseScreen
import com.star1xr.treelauncher.ui.screens.NormalNavKey
import com.star1xr.treelauncher.ui.screens.navigateTo
import com.star1xr.treelauncher.ui.screens.content.navigateToDownload
import com.star1xr.treelauncher.viewmodel.ScreenBackStackViewModel
import kotlinx.coroutines.delay

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
    val unpackItems = UnpackTasksManager.unpackItems

    var step by rememberSaveable {
        mutableIntStateOf(
            if (!UnpackTasksManager.areAllTasksFinished()) -1
            else if (AllSettings.setupStep.state != -2 && AllSettings.setupStep.state != -1) AllSettings.setupStep.state
            else if (BuildConfig.DEBUG) 0
            else if (accounts.isEmpty()) 1
            else 4 // Now it is 4 because we added IconStyleStep as step 3
        )
    }

    LaunchedEffect(step) {
        AllSettings.setupStep.save(step)
    }

    LaunchedEffect(unpackProgress) {
        if (step == -1 && UnpackTasksManager.areAllTasksFinished()) {
            delay(1000) // Give a moment to see the completion
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
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            AnimatedContent(
                targetState = step,
                transitionSpec = {
                    (fadeIn(animationSpec = tween(600)) + scaleIn(initialScale = 0.92f)) togetherWith
                            (fadeOut(animationSpec = tween(600)) + scaleOut(targetScale = 0.92f))
                },
                modifier = Modifier.fillMaxSize(),
                label = "SetupSteps"
            ) { targetStep ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 24.dp, vertical = 32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    when (targetStep) {
                        -1 -> RequiredFilesStep(unpackProgress, unpackItems)
                        0 -> DebugGreeting { step = 1 }
                        1 -> WelcomeStep { step = 2 }
                        2 -> AccountStep(
                            onCreateAccount = {
                                backStackViewModel.mainScreen.navigateTo(NormalNavKey.AccountManager(FirstLoginMenu.NORMAL))
                            },
                            onNext = { step = 3 }
                        )
                        3 -> IconStyleStep { step = 4 }
                        4 -> VersionStep(
                            versions = versions,
                            onDownloadVersion = { backStackViewModel.navigateToDownload() },
                            onFinish = {
                                AllSettings.setupStep.save(-2)
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
private fun IconStyleStep(onNext: () -> Unit) {
    var selectedStyle by remember { mutableStateOf(AllSettings.versionIconStyle.state) }
    
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            stringResource(R.string.setup_icon_style_title),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            stringResource(R.string.setup_icon_style_subtitle),
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            modifier = Modifier.alpha(0.7f)
        )
        Spacer(modifier = Modifier.height(32.dp))
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            IconStyleCard(
                modifier = Modifier.weight(1f),
                title = stringResource(R.string.setup_icon_style_current),
                iconRes = R.drawable.img_version_fabric,
                selected = selectedStyle == VersionIconStyle.CURRENT,
                onClick = {
                    selectedStyle = VersionIconStyle.CURRENT
                    AllSettings.versionIconStyle.save(VersionIconStyle.CURRENT)
                }
            )
            IconStyleCard(
                modifier = Modifier.weight(1f),
                title = stringResource(R.string.setup_icon_style_official),
                iconRes = R.drawable.img_loader_fabric,
                selected = selectedStyle == VersionIconStyle.OFFICIAL,
                onClick = {
                    selectedStyle = VersionIconStyle.OFFICIAL
                    AllSettings.versionIconStyle.save(VersionIconStyle.OFFICIAL)
                }
            )
        }
        
        Spacer(modifier = Modifier.height(48.dp))
        Button(
            onClick = onNext,
            modifier = Modifier.fillMaxWidth(0.7f).height(56.dp),
            shape = MaterialTheme.shapes.large
        ) {
            Text(stringResource(R.string.setup_debug_next), fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun IconStyleCard(
    modifier: Modifier = Modifier,
    title: String,
    iconRes: Int,
    selected: Boolean,
    onClick: () -> Unit
) {
    OutlinedCard(
        modifier = modifier,
        onClick = onClick,
        shape = MaterialTheme.shapes.large,
        colors = if (selected) CardDefaults.outlinedCardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
            contentColor = MaterialTheme.colorScheme.primary
        ) else CardDefaults.outlinedCardColors(),
        border = CardDefaults.outlinedCardBorder(enabled = true).copy(
            brush = if (selected) Brush.linearGradient(listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.secondary))
            else Brush.linearGradient(listOf(MaterialTheme.colorScheme.outline.copy(alpha = 0.5f), MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)))
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(16.dp))
            Surface(
                modifier = Modifier.size(64.dp),
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Image(
                        painter = painterResource(iconRes),
                        contentDescription = null,
                        modifier = Modifier.size(48.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            RadioButton(
                selected = selected,
                onClick = null
            )
        }
    }
}

@Composable
private fun RequiredFilesStep(progress: Float, items: List<InstallableItem>) {
    val animatedProgress by animateFloatAsState(targetValue = progress, label = "ProgressAnimation")
    
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = stringResource(R.string.setup_required_files_title, (progress * 100).toInt()),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.primary
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(120.dp)) {
            CircularProgressIndicator(
                progress = { animatedProgress },
                modifier = Modifier.fillMaxSize(),
                strokeWidth = 12.dp,
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
                strokeCap = androidx.compose.ui.graphics.StrokeCap.Round
            )
            Text(
                text = "${(progress * 100).toInt()}%",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.ExtraBold
            )
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        
        Text(
            text = stringResource(R.string.setup_required_files_subtitle),
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            modifier = Modifier.alpha(0.7f)
        )
        
        Spacer(modifier = Modifier.height(48.dp))
        
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f, fill = false),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
            )
        ) {
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(items) { item ->
                    UnpackItemRow(item)
                }
            }
        }
    }
}

@Composable
private fun UnpackItemRow(item: InstallableItem) {
    val state by item.state.collectAsStateWithLifecycle()
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(
                    when (state) {
                        InstallableItem.State.FINISHED -> MaterialTheme.colorScheme.primaryContainer
                        InstallableItem.State.RUNNING -> MaterialTheme.colorScheme.secondaryContainer
                        InstallableItem.State.NOT_EXISTS -> MaterialTheme.colorScheme.errorContainer
                        else -> MaterialTheme.colorScheme.surfaceVariant
                    }
                ),
            contentAlignment = Alignment.Center
        ) {
            when (state) {
                InstallableItem.State.FINISHED -> Icon(
                    painter = painterResource(R.drawable.ic_check),
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                InstallableItem.State.RUNNING -> CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.secondary
                )
                InstallableItem.State.NOT_EXISTS -> Icon(
                    painter = painterResource(R.drawable.ic_warning_outlined),
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.error
                )
                else -> Icon(
                    painter = painterResource(R.drawable.ic_schedule_outlined),
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                )
            }
        }
        
        Spacer(modifier = Modifier.width(16.dp))
        
        Column {
            Text(
                text = item.name,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold
            )
            if (item.summary != null) {
                Text(
                    text = item.summary,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.alpha(0.6f)
                )
            }
        }
    }
}

@Composable
private fun DebugGreeting(onNext: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Surface(
            modifier = Modifier.size(100.dp),
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primaryContainer
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    painter = painterResource(R.drawable.ic_build_filled),
                    contentDescription = null,
                    modifier = Modifier.size(48.dp),
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
        Spacer(modifier = Modifier.height(32.dp))
        Text(
            text = stringResource(R.string.setup_debug_title),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = stringResource(R.string.launcher_version_debug_warning, "TreeLauncher"),
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 16.dp).alpha(0.8f)
        )
        Spacer(modifier = Modifier.height(48.dp))
        Button(
            onClick = onNext,
            modifier = Modifier.fillMaxWidth(0.6f).height(56.dp),
            shape = MaterialTheme.shapes.large
        ) {
            Text(stringResource(R.string.setup_debug_next), fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun WelcomeStep(onNext: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(
            painter = painterResource(R.drawable.ic_launcher_foreground),
            contentDescription = null,
            modifier = Modifier.size(120.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            stringResource(R.string.setup_welcome_title),
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(48.dp))
        Button(
            onClick = onNext,
            modifier = Modifier.fillMaxWidth(0.7f).height(64.dp),
            shape = MaterialTheme.shapes.extraLarge,
            elevation = ButtonDefaults.buttonElevation(defaultElevation = 8.dp)
        ) {
            Text(stringResource(R.string.setup_welcome_button), fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun AccountStep(onCreateAccount: () -> Unit, onNext: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            stringResource(R.string.setup_account_title),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            stringResource(R.string.setup_add_minecraft_account), 
            style = MaterialTheme.typography.bodyLarge, 
            modifier = Modifier.alpha(0.7f)
        )
        Spacer(modifier = Modifier.height(48.dp))
        Button(
            onClick = onCreateAccount,
            modifier = Modifier.fillMaxWidth(0.7f).height(56.dp),
            shape = MaterialTheme.shapes.large
        ) {
            Icon(painterResource(R.drawable.ic_person_outlined), null)
            Spacer(Modifier.width(8.dp))
            Text(stringResource(R.string.setup_account_add), fontWeight = FontWeight.Bold)
        }
        Spacer(modifier = Modifier.height(16.dp))
        FilledTonalButton(
            onClick = onNext,
            modifier = Modifier.fillMaxWidth(0.7f).height(56.dp),
            shape = MaterialTheme.shapes.large
        ) {
            Text(stringResource(R.string.setup_account_skip), fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun VersionStep(
    versions: List<Version>, 
    onDownloadVersion: () -> Unit, 
    onFinish: () -> Unit
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            stringResource(R.string.setup_version_title),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            stringResource(R.string.setup_add_minecraft_account_desc),
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            modifier = Modifier.alpha(0.7f)
        )
        
        if (versions.isNotEmpty()) {
            Spacer(modifier = Modifier.height(24.dp))
            Card(
                modifier = Modifier.heightIn(max = 200.dp).fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                shape = MaterialTheme.shapes.large
            ) {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(versions) { version ->
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = MaterialTheme.shapes.medium,
                            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
                            tonalElevation = 2.dp
                        ) {
                            Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                VersionIconImage(
                                    version = version,
                                    modifier = Modifier.size(24.dp).clip(MaterialTheme.shapes.extraSmall)
                                )
                                Spacer(Modifier.width(12.dp))
                                Text(version.getVersionName(), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(48.dp))

        Button(
            onClick = onDownloadVersion,
            modifier = Modifier.fillMaxWidth(0.7f).height(56.dp),
            shape = MaterialTheme.shapes.large
        ) {
            Icon(painterResource(R.drawable.ic_download_2_outlined), null)
            Spacer(Modifier.width(8.dp))
            Text(stringResource(R.string.setup_version_download), fontWeight = FontWeight.Bold)
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        OutlinedButton(
            onClick = onFinish, 
            modifier = Modifier.fillMaxWidth(0.7f).height(56.dp),
            shape = MaterialTheme.shapes.large
        ) { 
            Text(stringResource(R.string.setup_version_finish), fontWeight = FontWeight.Bold) 
        }
    }
}
