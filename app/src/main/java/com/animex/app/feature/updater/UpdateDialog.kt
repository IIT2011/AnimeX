package com.animex.app.feature.updater

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.animex.app.core.designsystem.*
import com.animex.app.core.updater.AppUpdateManager
import com.animex.app.core.updater.UpdateCheckResult
import kotlinx.coroutines.launch

@Composable
fun UpdateDialog(
    updateInfo: UpdateCheckResult,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var isDownloading by remember { mutableStateOf(false) }
    var downloadProgress by remember { mutableFloatStateOf(0f) }
    var downloadError by remember { mutableStateOf<String?>(null) }

    Dialog(
        onDismissRequest = {
            if (!isDownloading) onDismiss()
        },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .wrapContentHeight()
                .clip(RoundedCornerShape(20.dp))
                .background(VoidDark)
                .border(1.dp, CardBorderGlowBrush, RoundedCornerShape(20.dp))
                .padding(24.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Cyber Badge Header
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50.dp))
                        .background(NeonCyan.copy(alpha = 0.15f))
                        .border(1.dp, NeonCyan.copy(alpha = 0.4f), RoundedCornerShape(50.dp))
                        .padding(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.SystemUpdate,
                            contentDescription = "Update",
                            tint = NeonCyan,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (updateInfo.hasUpdate) "NEW CORE UPDATE DETECTED" else "SYSTEM UP TO DATE",
                            color = NeonCyan,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            letterSpacing = 1.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                if (updateInfo.hasUpdate) {
                    Text(
                        text = updateInfo.releaseTitle ?: "AnimeX v${updateInfo.latestVersion}",
                        color = TextPrimary,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Version: v${updateInfo.latestVersion} • Current: v${updateInfo.currentVersion}",
                        color = NeonCyan,
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Changelog box
                    if (!updateInfo.changelog.isNullOrBlank()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 160.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(ObsidianCard)
                                .border(1.dp, GlassBorder, RoundedCornerShape(10.dp))
                                .padding(12.dp)
                        ) {
                            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                                Text(
                                    text = updateInfo.changelog,
                                    color = TextSecondary,
                                    fontSize = 12.sp,
                                    lineHeight = 18.sp
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    if (isDownloading) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            LinearProgressIndicator(
                                progress = { downloadProgress },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(3.dp)),
                                color = NeonCyan,
                                trackColor = ObsidianCard
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "DOWNLOADING UPDATE: ${(downloadProgress * 100).toInt()}%...",
                                color = NeonCyan,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    } else if (downloadError != null) {
                        Text(
                            text = downloadError!!,
                            color = NeonMagenta,
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    if (!isDownloading) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            CyberButton(
                                text = "LATER",
                                modifier = Modifier.weight(1f),
                                isPrimary = false,
                                onClick = onDismiss
                            )

                            CyberButton(
                                text = "UPDATE NOW",
                                modifier = Modifier.weight(1f),
                                onClick = {
                                    if (!updateInfo.downloadUrl.isNullOrBlank()) {
                                        isDownloading = true
                                        downloadError = null
                                        coroutineScope.launch {
                                            val ok = AppUpdateManager.downloadAndInstallApk(
                                                context = context,
                                                downloadUrl = updateInfo.downloadUrl,
                                                fileName = updateInfo.fileName ?: "AnimeX-v${updateInfo.latestVersion}.apk",
                                                onProgress = { p -> downloadProgress = p }
                                            )
                                            isDownloading = false
                                            if (ok) {
                                                onDismiss()
                                            } else {
                                                downloadError = "Download failed. Please check network."
                                            }
                                        }
                                    }
                                }
                            )
                        }
                    }
                } else {
                    Text(
                        text = "AnimeX v${updateInfo.currentVersion}",
                        color = TextPrimary,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "No updates available. You are running the latest production build.",
                        color = TextSecondary,
                        fontSize = 13.sp,
                        lineHeight = 18.sp
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    CyberButton(
                        text = "DISMISS",
                        modifier = Modifier.fillMaxWidth(0.5f),
                        onClick = onDismiss
                    )
                }
            }
        }
    }
}
