package com.example.jarvis.ui.main

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.jarvis.agent.JarvisState
import com.example.jarvis.permissions.PermissionManager
import com.example.jarvis.theme.JarvisAmber
import com.example.jarvis.theme.JarvisBackground
import com.example.jarvis.theme.JarvisBlue
import com.example.jarvis.theme.JarvisCrimson
import com.example.jarvis.theme.JarvisCyan
import com.example.jarvis.theme.JarvisSurface
import com.example.jarvis.theme.JarvisSurfaceBorder
import com.example.jarvis.theme.JarvisSurfaceGlass
import com.example.jarvis.theme.JarvisTextMuted
import com.example.jarvis.theme.JarvisTextPrimary
import com.example.jarvis.theme.JarvisTextSecondary
import com.example.jarvis.theme.JarvisViolet
import com.example.jarvis.ui.chat.ChatHistorySheet
import com.example.jarvis.ui.main.components.ArcReactorOrb
import com.example.jarvis.ui.main.components.StatusHud
import com.example.jarvis.ui.main.components.WaveformVisualizer

@Composable
fun MainScreen(
    onNavigateToSettings: () -> Unit,
    viewModel: JarvisViewModel = viewModel(),
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    val messages by viewModel.messages.collectAsState()
    var showChatSheet by remember { mutableStateOf(false) }

    // Permission Launcher for Microphone
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            viewModel.startWakeWordListeningIfEnabled()
        }
    }

    LaunchedEffect(Unit) {
        if (!PermissionManager.hasRecordAudioPermission(context)) {
            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        } else {
            viewModel.startWakeWordListeningIfEnabled()
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(JarvisBackground)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top HUD Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                StatusHud(
                    isWakeWordActive = uiState.isWakeWordActive,
                    onSettingsClick = onNavigateToSettings
                )

                // Dialog History Toggle Button
                IconButton(
                    onClick = {
                        viewModel.loadHistoryAndMemories()
                        showChatSheet = true
                    },
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(JarvisSurfaceGlass)
                        .border(1.dp, JarvisSurfaceBorder, CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.Chat,
                        contentDescription = "Dialog Log",
                        tint = JarvisCyan,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            // Permission Warning Banner if mic is denied
            if (!PermissionManager.hasRecordAudioPermission(context)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(JarvisAmber.copy(alpha = 0.15f))
                        .border(1.dp, JarvisAmber, RoundedCornerShape(8.dp))
                        .padding(10.dp)
                        .clickable { permissionLauncher.launch(Manifest.permission.RECORD_AUDIO) },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Warning, contentDescription = null, tint = JarvisAmber)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Microphone access required to hear you, sir. Tap to grant.",
                        color = JarvisAmber,
                        fontSize = 12.sp
                    )
                }
            }

            // Center Area: Arc Reactor & State
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // State Badge
                val (stateLabel, stateColor) = when (uiState.state) {
                    JarvisState.IDLE -> "SYSTEM STANDBY" to JarvisCyan
                    JarvisState.LISTENING -> "ACOUSTIC INTAKE" to JarvisCyan
                    JarvisState.THINKING -> "COGNITIVE SYNTHESIS" to JarvisViolet
                    JarvisState.EXECUTING -> "EXECUTING [${uiState.activeToolName ?: "ACTION"}]" to JarvisBlue
                    JarvisState.SPEAKING -> "VOCAL EMISSION" to JarvisCyan
                    JarvisState.ERROR -> "SYSTEM ALERT" to JarvisCrimson
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(JarvisSurfaceGlass)
                        .border(1.dp, stateColor.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
                        .padding(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = stateLabel,
                        color = stateColor,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 1.sp
                    )
                }

                Spacer(modifier = Modifier.height(28.dp))

                // The Arc Reactor Orb
                ArcReactorOrb(
                    state = uiState.state,
                    rmsLevel = uiState.rmsLevel,
                    size = 240.dp,
                    onClick = {
                        viewModel.triggerManualListening()
                    }
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Waveform Audio Visualizer
                WaveformVisualizer(
                    rmsLevel = uiState.rmsLevel,
                    isSpeaking = uiState.state == JarvisState.SPEAKING,
                    modifier = Modifier.padding(horizontal = 24.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Transcript or Output Text Display
                val displayText = when {
                    uiState.transcript.isNotBlank() -> "\"${uiState.transcript}\""
                    uiState.spokenText.isNotBlank() -> uiState.spokenText
                    else -> "Say \"Jarvis\" or tap the core to speak, sir."
                }

                Text(
                    text = displayText,
                    color = if (uiState.transcript.isNotBlank()) JarvisCyan else JarvisTextPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .padding(horizontal = 16.dp)
                        .fillMaxWidth()
                )

                // Confirmation UI if safety gate requires confirmation
                AnimatedVisibility(
                    visible = uiState.pendingConfirmation != null,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    val pending = uiState.pendingConfirmation
                    if (pending != null) {
                        Row(
                            modifier = Modifier
                                .padding(top = 16.dp)
                                .fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally)
                        ) {
                            Button(
                                onClick = { viewModel.confirmPendingAction() },
                                colors = ButtonDefaults.buttonColors(containerColor = JarvisCyan.copy(alpha = 0.3f)),
                                modifier = Modifier.border(1.dp, JarvisCyan, RoundedCornerShape(8.dp))
                            ) {
                                Text("Confirm, proceed", color = JarvisCyan, fontSize = 12.sp)
                            }
                            Button(
                                onClick = { viewModel.cancelPendingAction() },
                                colors = ButtonDefaults.buttonColors(containerColor = JarvisCrimson.copy(alpha = 0.2f)),
                                modifier = Modifier.border(1.dp, JarvisCrimson, RoundedCornerShape(8.dp))
                            ) {
                                Text("Cancel", color = JarvisCrimson, fontSize = 12.sp)
                            }
                        }
                    }
                }
            }

            // Bottom Area: Quick Suggestions & Manual Trigger Pill
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Suggestion Chips (Horizontal Scrollable)
                val suggestions = listOf(
                    "What's my battery level?",
                    "Open YouTube",
                    "What time is it?",
                    "Set a timer for 5 minutes",
                    "Remember that my project is called VAMANA",
                    "What are my reminders?"
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(bottom = 14.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    for (prompt in suggestions) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(JarvisSurfaceGlass)
                                .border(1.dp, JarvisSurfaceBorder, RoundedCornerShape(20.dp))
                                .clickable { viewModel.submitPrompt(prompt) }
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = prompt,
                                color = JarvisTextSecondary,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.SansSerif
                            )
                        }
                    }
                }

                // Central Glow Action Trigger
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier
                        .clip(RoundedCornerShape(32.dp))
                        .background(JarvisSurfaceGlass)
                        .border(1.dp, JarvisCyan.copy(alpha = 0.6f), RoundedCornerShape(32.dp))
                        .clickable { viewModel.triggerManualListening() }
                        .padding(horizontal = 24.dp, vertical = 12.dp)
                ) {
                    Icon(
                        imageVector = if (uiState.state == JarvisState.LISTENING) Icons.Default.Stop else Icons.Default.Mic,
                        contentDescription = "Voice Input",
                        tint = JarvisCyan,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = if (uiState.state == JarvisState.LISTENING) "STOP LISTENING" else "INITIALIZE VOICE",
                        color = JarvisCyan,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 1.sp
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))
            }
        }

        // Chat History Dialog Sheet
        if (showChatSheet) {
            ChatHistorySheet(
                messages = messages,
                onDismiss = { showChatSheet = false },
                onClear = { viewModel.clearHistory() }
            )
        }
    }
}
