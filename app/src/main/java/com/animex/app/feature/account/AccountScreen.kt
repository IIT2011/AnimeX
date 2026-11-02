package com.animex.app.feature.account

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.animex.app.R
import com.animex.app.core.auth.GoogleAuthManager
import com.animex.app.core.designsystem.*
import com.animex.app.core.download.AnimeDownloadGroup
import com.animex.app.core.download.DownloadRepository
import com.animex.app.core.network.NetworkClient
import com.animex.app.core.session.UserSessionRepository
import com.animex.app.core.network.model.AnimeDto
import com.animex.app.core.network.model.WatchHistoryEntry

@Composable
fun AccountScreen(
    onAnimeClick: (Long) -> Unit,
    onPlayClick: (episodeId: Long, animeTitle: String?, episodeNumber: Int?, poster: String?, localPath: String?) -> Unit,
    viewModel: AccountViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var showManualEmailDialog by remember { mutableStateOf(false) }
    var manualEmailInput by remember { mutableStateOf("") }
    var showEditUsernameDialog by remember { mutableStateOf(false) }
    var isWatchlistExpanded by remember { mutableStateOf(false) }
    var isDownloadsExpanded by remember { mutableStateOf(false) }

    val accountChooserLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val account = GoogleAuthManager.parseAccountFromIntent(result.data)
            if (account != null) {
                viewModel.onGoogleAccountSelected(account)
            }
        }
    }

    LaunchedEffect(Unit) {
        viewModel.refreshAccounts()
    }

    if (isWatchlistExpanded) {
        ExpandedWatchlistView(
            onBackClick = { isWatchlistExpanded = false },
            onAnimeClick = onAnimeClick,
            onPlayClick = { epId, title, epNum, poster ->
                onPlayClick(epId, title, epNum, poster, null)
            }
        )
        return
    }

    if (isDownloadsExpanded) {
        ExpandedDownloadsView(
            onBackClick = { isDownloadsExpanded = false },
            onPlayOffline = { localPath, animeTitle, epNum ->
                onPlayClick(0L, animeTitle, epNum, null, localPath)
            }
        )
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(VoidDark)
            .statusBarsPadding()
    ) {
        // Top Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Image(
                    painter = painterResource(id = R.drawable.app_logo),
                    contentDescription = "AnimeX Logo",
                    modifier = Modifier
                        .size(30.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .border(1.dp, GlassBorder, RoundedCornerShape(6.dp))
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "ACCOUNT",
                    color = TextPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 1.sp
                )
                Text(
                    text = "CLOUD",
                    color = NeonCyan,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 1.sp
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                StatusBeacon(color = if (uiState.isLoggedIn) NeonLime else NeonAmber)
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (uiState.isLoggedIn) "SYNCED" else "GUEST",
                    color = if (uiState.isLoggedIn) NeonLime else NeonAmber,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.width(12.dp))
                IconButton(
                    onClick = { viewModel.refreshAccounts() },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Refresh",
                        tint = NeonCyan,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 32.dp)
        ) {
            // Google Login or User Profile Card
            item {
                Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                    if (!uiState.isLoggedIn) {
                        GoogleLoginCard(
                            isSyncing = uiState.isSyncing,
                            detectedAccounts = uiState.detectedDeviceAccounts,
                            onConnectDetected = { email ->
                                viewModel.onDirectEmailSignIn(email)
                            },
                            onLaunchChooser = {
                                try {
                                    accountChooserLauncher.launch(GoogleAuthManager.createChooseAccountIntent())
                                } catch (e: Exception) {
                                    showManualEmailDialog = true
                                }
                            },
                            onManualEntryClick = {
                                showManualEmailDialog = true
                            },
                            onEditUsernameClick = {
                                showEditUsernameDialog = true
                            }
                        )
                    } else {
                        GoogleProfileCard(
                            state = uiState,
                            onSyncClick = { viewModel.triggerCloudSync() },
                            onLogoutClick = { viewModel.logout() },
                            onEditUsernameClick = {
                                showEditUsernameDialog = true
                            }
                        )
                    }
                }
            }

            // Continue Watching Section
            item {
                Column(modifier = Modifier.padding(top = 20.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "CONTINUE WATCHING",
                            color = TextPrimary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            letterSpacing = 1.sp
                        )
                        CyberBadge(text = "${uiState.continueWatching.size} ACTIVE", accentColor = NeonCyan)
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    if (uiState.continueWatching.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp, vertical = 8.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(ObsidianCard)
                                .border(1.dp, GlassBorder, RoundedCornerShape(10.dp))
                                .padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "NO ACTIVE SESSIONS • START STREAMING TO RECORD PROGRESS",
                                color = TextSecondary,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    } else {
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(uiState.continueWatching) { entry ->
                                ContinueWatchingCard(
                                    entry = entry,
                                    onPlayClick = { onPlayClick(entry.episodeId, entry.title, entry.episodeNumber, entry.poster, null) }
                                )
                            }
                        }
                    }
                }
            }

            // Watchlist Section
            item {
                Column(modifier = Modifier.padding(top = 24.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "MY WATCHLIST",
                            color = TextPrimary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            letterSpacing = 1.sp
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            CyberBadge(text = "${uiState.watchlist.size} SAVED", accentColor = NeonMagenta)
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(NeonMagenta.copy(alpha = 0.15f))
                                    .border(1.dp, NeonMagenta, RoundedCornerShape(6.dp))
                                    .clickable { isWatchlistExpanded = true }
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = "EXPAND ⤢",
                                    color = NeonMagenta,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    if (uiState.watchlist.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp, vertical = 8.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(ObsidianCard)
                                .border(1.dp, GlassBorder, RoundedCornerShape(10.dp))
                                .padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "WATCHLIST IS EMPTY • TAP '+ ADD TO WATCHLIST' ON ANY ANIME",
                                color = TextSecondary,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    } else {
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(uiState.watchlist) { anime ->
                                WatchlistAnimeCard(
                                    anime = anime,
                                    onClick = { onAnimeClick(anime.id) },
                                    onRemoveClick = { viewModel.removeFromWatchlist(anime.id) }
                                )
                            }
                        }
                    }
                }
            }

            // In-App Downloads Section
            item {
                val allTasks by DownloadRepository.tasks.collectAsState()
                val inAppGroups = remember(allTasks) {
                    DownloadRepository.getInAppGroups()
                }

                Column(modifier = Modifier.padding(top = 24.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "IN-APP DOWNLOADS",
                            color = TextPrimary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            letterSpacing = 1.sp
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            CyberBadge(text = "${inAppGroups.size} ANIMES", accentColor = NeonCyan)
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(NeonCyan.copy(alpha = 0.15f))
                                    .border(1.dp, NeonCyan, RoundedCornerShape(6.dp))
                                    .clickable { isDownloadsExpanded = true }
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = "EXPAND ⤢",
                                    color = NeonCyan,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    if (inAppGroups.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp, vertical = 8.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(ObsidianCard)
                                .border(1.dp, GlassBorder, RoundedCornerShape(10.dp))
                                .padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "NO IN-APP DOWNLOADS • DOWNLOAD ANY EPISODE TO PLAY OFFLINE",
                                color = TextSecondary,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    } else {
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(inAppGroups) { group ->
                                InAppDownloadRailCard(
                                    group = group,
                                    onClick = { isDownloadsExpanded = true }
                                )
                            }
                        }
                    }
                }
            }

            // Cloud Sync Preferences Section
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 24.dp)
                ) {
                    Text(
                        text = "PLAYBACK & CLOUD PREFERENCES",
                        color = TextPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 1.sp
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(ObsidianCard)
                            .border(1.dp, GlassBorder, RoundedCornerShape(12.dp))
                            .padding(16.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            PreferenceToggle(
                                title = "Auto-Skip Intro / Outro",
                                subtitle = "Automatically jump past anime opening and ending sequences",
                                isChecked = uiState.preferences.autoSkipIntro,
                                onCheckedChange = { viewModel.updatePreferences(autoSkip = it) }
                            )

                            HorizontalDivider(color = GlassBorder, thickness = 1.dp)

                            PreferenceToggle(
                                title = "Auto-Play Next Episode",
                                subtitle = "Automatically load the next episode in sequence",
                                isChecked = uiState.preferences.autoPlayNextEpisode,
                                onCheckedChange = { viewModel.updatePreferences(autoPlayNext = it) }
                            )

                            HorizontalDivider(color = GlassBorder, thickness = 1.dp)

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Preferred Audio Track",
                                        color = TextPrimary,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "Default audio when resolving streams",
                                        color = TextSecondary,
                                        fontSize = 11.sp
                                    )
                                }

                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    val isSub = uiState.preferences.preferredAudioTrack == "sub"
                                    CyberBadge(
                                        text = "SUB",
                                        accentColor = if (isSub) NeonCyan else TextSecondary,
                                        modifier = Modifier.clickable { viewModel.updatePreferences(audioTrack = "sub") }
                                    )
                                    CyberBadge(
                                        text = "DUB",
                                        accentColor = if (!isSub) NeonMagenta else TextSecondary,
                                        modifier = Modifier.clickable { viewModel.updatePreferences(audioTrack = "dub") }
                                    )
                                }
                            }

                            HorizontalDivider(color = GlassBorder, thickness = 1.dp)

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Default Video Resolution",
                                        color = TextPrimary,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "Target playback video quality",
                                        color = TextSecondary,
                                        fontSize = 11.sp
                                    )
                                }

                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    listOf("1080p", "720p", "480p").forEach { quality ->
                                        val isSelected = uiState.preferences.defaultVideoQuality == quality
                                        CyberBadge(
                                            text = quality,
                                            accentColor = if (isSelected) NeonCyan else TextSecondary,
                                            modifier = Modifier.clickable { viewModel.updatePreferences(quality = quality) }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Manual Google Email Dialog
    if (showManualEmailDialog) {
        AlertDialog(
            onDismissRequest = { showManualEmailDialog = false },
            containerColor = ObsidianCard,
            title = {
                Text(
                    text = "LINK GOOGLE ACCOUNT",
                    color = NeonCyan,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            },
            text = {
                Column {
                    Text(
                        text = "Enter your Google Account email. Your watchlist, history, and preferences will sync directly to your private Google storage:",
                        color = TextSecondary,
                        fontSize = 12.sp
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(VoidDark)
                            .border(1.dp, NeonCyan, RoundedCornerShape(8.dp))
                            .padding(12.dp)
                    ) {
                        BasicTextField(
                            value = manualEmailInput,
                            onValueChange = { manualEmailInput = it },
                            textStyle = TextStyle(color = TextPrimary, fontSize = 14.sp, fontFamily = FontFamily.Monospace),
                            cursorBrush = SolidColor(NeonCyan),
                            modifier = Modifier.fillMaxWidth(),
                            decorationBox = { innerTextField ->
                                if (manualEmailInput.isEmpty()) {
                                    Text("yourname@gmail.com", color = TextSecondary, fontSize = 14.sp)
                                }
                                innerTextField()
                            }
                        )
                    }
                }
            },
            confirmButton = {
                CyberButton(
                    text = "LINK & SYNC",
                    onClick = {
                        val trimmed = manualEmailInput.trim()
                        if (trimmed.isNotBlank() && trimmed.contains("@")) {
                            viewModel.onDirectEmailSignIn(trimmed)
                            showManualEmailDialog = false
                            manualEmailInput = ""
                        }
                    }
                )
            },
            dismissButton = {
                TextButton(onClick = { showManualEmailDialog = false }) {
                    Text("CANCEL", color = TextSecondary, fontFamily = FontFamily.Monospace)
                }
            }
        )
    }

    if (showEditUsernameDialog) {
        EditUsernameDialog(
            initialUsername = UserSessionRepository.getDisplayName(),
            onDismiss = { showEditUsernameDialog = false },
            onUsernameUpdated = {
                viewModel.refreshAccounts()
            }
        )
    }
}

@Composable
fun GoogleLoginCard(
    isSyncing: Boolean,
    detectedAccounts: List<String>,
    onConnectDetected: (String) -> Unit,
    onLaunchChooser: () -> Unit,
    onManualEntryClick: () -> Unit,
    onEditUsernameClick: () -> Unit = {}
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(ObsidianCard)
            .border(1.dp, CardBorderGlowBrush, RoundedCornerShape(14.dp))
            .padding(18.dp)
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(NeonCyan.copy(alpha = 0.2f))
                            .border(1.dp, NeonCyan, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = UserSessionRepository.getDisplayName().take(1).uppercase(),
                            color = NeonCyan,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                    Text(
                        text = "Alias: ${UserSessionRepository.getDisplayName()}",
                        color = TextPrimary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(NeonCyan.copy(alpha = 0.15f))
                        .border(1.dp, NeonCyan, RoundedCornerShape(6.dp))
                        .clickable(onClick = onEditUsernameClick)
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "EDIT ✏️",
                        color = NeonCyan,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            HorizontalDivider(color = GlassBorder, thickness = 1.dp)
            Spacer(modifier = Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Image(
                    painter = painterResource(id = R.drawable.app_logo),
                    contentDescription = "AnimeX Logo",
                    modifier = Modifier
                        .size(42.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .border(1.dp, GlassBorder, RoundedCornerShape(10.dp))
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "LINK GOOGLE ACCOUNT",
                        color = TextPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = "Cloud Sync & Watchlist Backup",
                        color = NeonCyan,
                        fontSize = 11.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "Backs up your Watchlist, Episode Progress, and personal preferences securely to your private Google account space. Everything restores automatically on any device.",
                color = TextSecondary,
                fontSize = 12.sp,
                lineHeight = 18.sp
            )

            // Detected accounts for 1-tap connection
            if (detectedAccounts.isNotEmpty()) {
                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    text = "DETECTED GOOGLE ACCOUNTS (1-TAP CONNECT):",
                    color = NeonLime,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))

                detectedAccounts.forEach { email ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(VoidDark)
                            .border(1.dp, GlassBorder, RoundedCornerShape(8.dp))
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.AccountCircle,
                                contentDescription = null,
                                tint = NeonCyan,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = email,
                                color = TextPrimary,
                                fontSize = 12.sp,
                                fontFamily = FontFamily.Monospace,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        CyberButton(
                            text = "CONNECT",
                            onClick = { onConnectDetected(email) }
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            if (isSyncing) {
                CircularProgressIndicator(color = NeonCyan, modifier = Modifier.size(28.dp))
            } else {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    CyberButton(
                        text = "CHOOSE GOOGLE ACCOUNT",
                        isPrimary = true,
                        onClick = onLaunchChooser,
                        modifier = Modifier.weight(1f)
                    )
                    CyberButton(
                        text = "ENTER EMAIL",
                        isPrimary = false,
                        onClick = onManualEntryClick,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

/**
 * User Profile Card: Clean & straightforward with just two options: SYNC and LOGOUT.
 */
@Composable
fun GoogleProfileCard(
    state: AccountUiState,
    onSyncClick: () -> Unit,
    onLogoutClick: () -> Unit,
    onEditUsernameClick: () -> Unit = {}
) {
    val account = state.googleAccount ?: return

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(ObsidianCard)
            .border(1.dp, CardBorderGlowBrush, RoundedCornerShape(14.dp))
            .padding(18.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(Color(0x3300F0FF))
                            .border(1.5.dp, NeonCyan, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = UserSessionRepository.getDisplayName().take(1).uppercase(),
                            color = NeonCyan,
                            fontWeight = FontWeight.Black,
                            fontSize = 18.sp
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = UserSessionRepository.getDisplayName(),
                                color = TextPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                            Box(
                                modifier = Modifier
                                    .size(22.dp)
                                    .clip(CircleShape)
                                    .background(VoidDark)
                                    .border(1.dp, GlassBorder, CircleShape)
                                    .clickable(onClick = onEditUsernameClick),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = "Edit Username",
                                    tint = NeonCyan,
                                    modifier = Modifier.size(12.dp)
                                )
                            }
                        }
                        Text(
                            text = account.email,
                            color = TextSecondary,
                            fontSize = 11.sp
                        )
                    }
                }

                Icon(
                    imageVector = Icons.Default.CloudDone,
                    contentDescription = "Synced",
                    tint = NeonLime,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Sync Status Beacon
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0x28000000))
                    .border(1.dp, GlassBorder, RoundedCornerShape(8.dp))
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    StatusBeacon(color = NeonLime)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "SYNCED TO GOOGLE",
                        color = NeonLime,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                }

                Text(
                    text = state.lastSyncTime,
                    color = NeonCyan,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Two simple options: SYNC and LOGOUT
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                CyberButton(
                    text = if (state.isSyncing) "SYNCING..." else "SYNC NOW",
                    onClick = onSyncClick,
                    isPrimary = true,
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Sync,
                            contentDescription = null,
                            tint = NeonCyan,
                            modifier = Modifier.size(16.dp)
                        )
                    },
                    modifier = Modifier.weight(1f)
                )

                CyberButton(
                    text = "LOGOUT",
                    onClick = onLogoutClick,
                    isPrimary = false,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
fun ContinueWatchingCard(
    entry: WatchHistoryEntry,
    onPlayClick: () -> Unit
) {
    val posterUrl = NetworkClient.getFullPosterUrl(entry.poster)

    Column(
        modifier = Modifier
            .width(180.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(ObsidianCard)
            .border(1.dp, CardBorderGlowBrush, RoundedCornerShape(10.dp))
            .clickable(onClick = onPlayClick)
            .padding(8.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(100.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(Color.Black)
        ) {
            AsyncImage(
                model = posterUrl,
                contentDescription = entry.title,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )

            // Play icon overlay
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .align(Alignment.Center)
                    .clip(CircleShape)
                    .background(Color(0x9907080D))
                    .border(1.dp, NeonCyan, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = null,
                    tint = NeonCyan,
                    modifier = Modifier.size(20.dp)
                )
            }

            // Episode Tag
            Box(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(6.dp)
            ) {
                CyberBadge(text = "EP ${entry.episodeNumber}", accentColor = NeonAmber)
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Progress bar
        LinearProgressIndicator(
            progress = { entry.progressPercent },
            color = NeonCyan,
            trackColor = GlassSurface,
            modifier = Modifier
                .fillMaxWidth()
                .height(3.dp)
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = entry.title,
            color = TextPrimary,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun WatchlistAnimeCard(
    anime: AnimeDto,
    onClick: () -> Unit,
    onRemoveClick: () -> Unit
) {
    val posterUrl = NetworkClient.getFullPosterUrl(anime.poster)

    Column(
        modifier = Modifier
            .width(125.dp)
            .cyberBounceClick(onClick = onClick)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(170.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(ObsidianCard)
                .border(1.dp, CardBorderGlowBrush, RoundedCornerShape(8.dp))
        ) {
            AsyncImage(
                model = posterUrl,
                contentDescription = anime.title,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )

            // Quick Remove Button
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(4.dp)
                    .size(26.dp)
                    .clip(CircleShape)
                    .background(VoidDark.copy(alpha = 0.85f))
                    .clickable(onClick = onRemoveClick),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Remove",
                    tint = NeonMagenta,
                    modifier = Modifier.size(14.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = anime.title,
            color = TextPrimary,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun PreferenceToggle(
    title: String,
    subtitle: String,
    isChecked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = TextPrimary,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = subtitle,
                color = TextSecondary,
                fontSize = 11.sp
            )
        }

        Switch(
            checked = isChecked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = VoidDark,
                checkedTrackColor = NeonCyan,
                uncheckedThumbColor = TextSecondary,
                uncheckedTrackColor = ObsidianSurface
            )
        )
    }
}

@Composable
fun InAppDownloadRailCard(
    group: AnimeDownloadGroup,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .width(135.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(ObsidianCard)
            .border(1.dp, CardBorderGlowBrush, RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
            ) {
                AsyncImage(
                    model = NetworkClient.resolvePosterUrl(group.animePoster),
                    contentDescription = group.animeTitle,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(6.dp)
                ) {
                    CyberBadge(text = "${group.completedCount} EPS", accentColor = NeonCyan)
                }
            }
            Column(modifier = Modifier.padding(8.dp)) {
                Text(
                    text = group.animeTitle,
                    color = TextPrimary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = group.formattedTotalSize,
                    color = TextSecondary,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    }
}
