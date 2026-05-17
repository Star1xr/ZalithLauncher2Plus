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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.star1xr.treelauncher.BuildConfig
import com.star1xr.treelauncher.R
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
    var step by remember { mutableIntStateOf(if (BuildConfig.DEBUG) 0 else 1) }

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
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    when (targetStep) {
                        0 -> DebugGreeting { step = 1 }
                        1 -> WelcomeStep { step = 2 }
                        2 -> AccountStep(
                            onCreateAccount = {
                                backStackViewModel.mainScreen.navigateTo(
                                    screenKey = NormalNavKey.AccountManager(FirstLoginMenu.NORMAL)
                                )
                            },
                            onNext = { step = 3 }
                        )
                        3 -> VersionStep(
                            onDownloadVersion = {
                                // Navigate to download screen
                                // This requires a TitledNavKey for target, using null for now as per DownloadScreen usage
                                backStackViewModel.mainScreen.navigateTo(
                                    screenKey = com.star1xr.treelauncher.ui.screens.NestedNavKey.Download(null)
                                )
                            },
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
private fun DebugGreeting(onNext: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(
            painter = painterResource(R.drawable.ic_build_filled),
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = "Bu bir test sürümüdür.\nTreeLauncher'i denediğiniz için teşekkürler.",
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(48.dp))
        Button(onClick = onNext) {
            Text("Devam Et")
        }
    }
}

@Composable
private fun WelcomeStep(onNext: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = "TreeLauncher'a Hoş Geldiniz",
            style = MaterialTheme.typography.displaySmall,
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "En iyi Minecraft deneyimi için hazır mısın?",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(64.dp))
        Button(
            onClick = onNext,
            modifier = Modifier
                .height(56.dp)
                .width(200.dp),
            shape = MaterialTheme.shapes.large
        ) {
            Text("Başla", fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun AccountStep(onCreateAccount: () -> Unit, onNext: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = "Hesap Oluşturmak İster misiniz?",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Oyuna girmek için bir hesaba ihtiyacınız olacak.",
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(48.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Button(
                onClick = onCreateAccount,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
            ) {
                Text("Evet, Oluştur")
            }
            Button(onClick = onNext) {
                Text("Daha Sonra")
            }
        }
    }
}

@Composable
private fun VersionStep(onDownloadVersion: () -> Unit, onFinish: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = "Sürüm İndirmek İster misiniz?",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Minecraft oynamak için en az bir sürüm yüklü olmalıdır.",
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(48.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Button(
                onClick = onDownloadVersion,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
            ) {
                Text("Evet, İndir")
            }
            Button(onClick = onFinish) {
                Text("Bitir ve Başla")
            }
        }
    }
}
