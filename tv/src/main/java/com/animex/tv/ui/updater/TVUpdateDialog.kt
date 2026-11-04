package com.animex.tv.ui.updater

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.animex.tv.core.updater.TVAppUpdateManager
import com.animex.tv.core.updater.TVUpdateCheckResult
import com.animex.tv.theme.*
import kotlinx.coroutines.launch

@Composable
fun TVUpdateDialog(
    updateInfo: TVUpdateCheckResult,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val updateButtonFocus = remember { FocusRequester() }

    var isDownloading by remember { mutableStateOf(false) }
    var downloadProgress by remember { mutableFloatStateOf(0f) }
    var downloadError by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        try {
            updateButtonFocus.requestFocus()
        } catch (_: Exception) {}
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .width(580.dp)
                .wrapContentHeight()
                .clip(RoundedCornerShape(20.dp))
                .background(CyberVoid)
                .border(2.dp, CyberCyan, RoundedCornerShape(20.dp))
                .padding(28.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // TV Cyber Badge
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50.dp))
                        .background(CyberCyan.copy(alpha = 0.15f))
                        .border(1.dp, CyberCyan.copy(alpha = 0.5f), RoundedCornerShape(50.dp))
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.SystemUpdate,
                            contentDescription = "Update",
                            tint = CyberCyan,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (updateInfo.hasUpdate) "NEW TV UPDATE AVAILABLE" else "SYSTEM UP TO DATE",
                            color = CyberCyan,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            letterSpacing = 1.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                Text(
                    text = updateInfo.releaseTitle ?: "AnimeX TV v${updateInfo.latestVersion}",
                    color = CyberTextPrimary,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "New Version: v${updateInfo.latestVersion}  •  Current: v${updateInfo.currentVersion}",
                    color = CyberCyan,
                    fontSize = 13.sp,
                    fontFamily = FontFamily.Monospace
                )

                if (!updateInfo.changelog.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(14.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 140.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(CyberCardBg)
                            .border(1.dp, CyberBorderSubtle, RoundedCornerShape(10.dp))
                            .padding(14.dp)
                    ) {
                        Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                            Text(
                                text = updateInfo.changelog,
                                color = CyberTextSecondary,
                                fontSize = 12.sp,
                                lineHeight = 18.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                if (isDownloading) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        LinearProgressIndicator(
                            progress = { downloadProgress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp)),
                            color = CyberCyan,
                            trackColor = CyberCardBg
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "DOWNLOADING UPDATE: ${(downloadProgress * 100).toInt()}%...",
                            color = CyberCyan,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                } else if (downloadError != null) {
                    Text(
                        text = downloadError!!,
                        color = CyberMagenta,
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                }

                Spacer(modifier = Modifier.height(16.dp))

                if (!isDownloading) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        CyberTVButton(
                            text = "UPDATE NOW",
                            modifier = Modifier
                                .weight(1f)
                                .focusRequester(updateButtonFocus),
                            accentColor = CyberCyan,
                            onClick = {
                                if (!updateInfo.downloadUrl.isNullOrBlank()) {
                                    isDownloading = true
                                    downloadError = null
                                    coroutineScope.launch {
                                        val ok = TVAppUpdateManager.downloadAndInstallApk(
                                            context = context,
                                            downloadUrl = updateInfo.downloadUrl,
                                            fileName = updateInfo.fileName ?: "AnimeX-tv-v${updateInfo.latestVersion}.apk",
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

                        CyberTVButton(
                            text = "LATER",
                            modifier = Modifier.weight(1f),
                            accentColor = CyberBorderSubtle,
                            onClick = onDismiss
                        )
                    }
                }
            }
        }
    }
}
