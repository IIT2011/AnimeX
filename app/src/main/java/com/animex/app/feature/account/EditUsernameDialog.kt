package com.animex.app.feature.account

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.animex.app.core.designsystem.*
import com.animex.app.core.session.UserSessionRepository
import com.animex.app.core.session.UsernameCheckResult

@Composable
fun EditUsernameDialog(
    initialUsername: String,
    onDismiss: () -> Unit,
    onUsernameUpdated: (String) -> Unit
) {
    val context = LocalContext.current
    var inputUsername by remember { mutableStateOf(initialUsername) }
    var checkResult by remember {
        mutableStateOf<UsernameCheckResult>(UsernameCheckResult.Available)
    }

    LaunchedEffect(inputUsername) {
        checkResult = UserSessionRepository.checkUsernameAvailability(inputUsername)
    }

    val isAvailable = checkResult is UsernameCheckResult.Available

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .clip(RoundedCornerShape(16.dp))
                .background(VoidDark)
                .border(1.dp, CardBorderGlowBrush, RoundedCornerShape(16.dp))
                .padding(22.dp)
        ) {
            Column {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "EDIT PROFILE USERNAME",
                        color = TextPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 1.sp
                    )

                    Box(
                        modifier = Modifier
                            .size(30.dp)
                            .clip(CircleShape)
                            .background(ObsidianCard)
                            .border(1.dp, GlassBorder, CircleShape)
                            .clickable(onClick = onDismiss),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = TextSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                Text(
                    text = "DESIRED USERNAME",
                    color = TextSecondary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Input Box
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(ObsidianCard)
                        .border(
                            width = 1.dp,
                            color = when (checkResult) {
                                is UsernameCheckResult.Available -> NeonCyan
                                else -> NeonMagenta
                            },
                            shape = RoundedCornerShape(8.dp)
                        )
                        .padding(horizontal = 14.dp, vertical = 4.dp)
                ) {
                    TextField(
                        value = inputUsername,
                        onValueChange = { inputUsername = it },
                        placeholder = {
                            Text(
                                text = "Enter username (3-20 chars)",
                                color = TextSecondary.copy(alpha = 0.5f),
                                fontSize = 14.sp
                            )
                        },
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            disabledContainerColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            cursorColor = NeonCyan
                        ),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Availability feedback
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    when (val res = checkResult) {
                        is UsernameCheckResult.Available -> {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = NeonCyan,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "Username available! Ready to claim.",
                                color = NeonCyan,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                        is UsernameCheckResult.Invalid -> {
                            Icon(
                                imageVector = Icons.Default.ErrorOutline,
                                contentDescription = null,
                                tint = NeonMagenta,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = res.reason,
                                color = NeonMagenta,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                        is UsernameCheckResult.Taken -> {
                            Icon(
                                imageVector = Icons.Default.ErrorOutline,
                                contentDescription = null,
                                tint = NeonMagenta,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = res.reason,
                                color = NeonMagenta,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(22.dp))

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    CyberButton(
                        text = "CANCEL",
                        isPrimary = false,
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    )

                    CyberButton(
                        text = "CLAIM USERNAME",
                        isPrimary = isAvailable,
                        onClick = {
                            if (isAvailable) {
                                UserSessionRepository.updateUsername(context, inputUsername)
                                onUsernameUpdated(inputUsername.trim())
                                onDismiss()
                            }
                        },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}
