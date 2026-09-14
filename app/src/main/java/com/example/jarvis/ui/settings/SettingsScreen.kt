package com.example.jarvis.ui.settings

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.jarvis.data.JarvisRepository
import com.example.jarvis.data.model.MemoryItem
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
import com.example.jarvis.voice.TextToSpeechEngine

@Composable
fun SettingsScreen(
    repository: JarvisRepository,
    ttsEngine: TextToSpeechEngine,
    memories: List<MemoryItem>,
    onDeleteMemory: (Long) -> Unit,
    onClearMemories: () -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var wakeWordEnabled by remember { mutableStateOf(repository.preferences.wakeWordEnabled) }
    var sensitivity by remember { mutableStateOf(repository.preferences.wakeWordSensitivity) }
    var speechRate by remember { mutableStateOf(repository.preferences.speechRate) }
    var speechPitch by remember { mutableStateOf(repository.preferences.speechPitch) }
    var aiProvider by remember { mutableStateOf(repository.preferences.aiProvider) }
    var geminiKey by remember { mutableStateOf(repository.secureStorage.geminiApiKey) }
    var memoryEnabled by remember { mutableStateOf(repository.preferences.memoryEnabled) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(JarvisBackground)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Top App Bar
        item {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(JarvisSurfaceGlass)
                        .border(1.dp, JarvisSurfaceBorder, CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Back",
                        tint = JarvisCyan
                    )
                }
                Spacer(modifier = Modifier.size(12.dp))
                Text(
                    text = "JARVIS CONFIGURATION",
                    color = JarvisCyan,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }
        }

        // Section 1: Voice & Vocal Resonance
        item {
            SettingsCard(title = "VOCAL SYNTHESIS", icon = Icons.Default.VolumeUp) {
                Text(
                    text = "Speech Cadence: ${String.format("%.2fx", speechRate)}",
                    color = JarvisTextSecondary,
                    fontSize = 12.sp
                )
                Slider(
                    value = speechRate,
                    onValueChange = {
                        speechRate = it
                        repository.preferences.speechRate = it
                        ttsEngine.setSpeed(it)
                    },
                    valueRange = 0.8f..1.5f,
                    colors = SliderDefaults.colors(thumbColor = JarvisCyan, activeTrackColor = JarvisCyan)
                )

                Text(
                    text = "Vocal Pitch: ${String.format("%.2fx", speechPitch)}",
                    color = JarvisTextSecondary,
                    fontSize = 12.sp
                )
                Slider(
                    value = speechPitch,
                    onValueChange = {
                        speechPitch = it
                        repository.preferences.speechPitch = it
                        ttsEngine.setPitch(it)
                    },
                    valueRange = 0.7f..1.3f,
                    colors = SliderDefaults.colors(thumbColor = JarvisCyan, activeTrackColor = JarvisCyan)
                )

                Button(
                    onClick = {
                        ttsEngine.speak("Audio output systems operating within optimal parameters, sir.")
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = JarvisCyan.copy(alpha = 0.2f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, JarvisCyan, RoundedCornerShape(8.dp))
                ) {
                    Text("Test Vocal Calibration", color = JarvisCyan)
                }
            }
        }

        // Section 2: Wake Word Detection
        item {
            SettingsCard(title = "WAKE WORD (JARVIS)", icon = Icons.Default.Mic) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Continuous Acoustic Detection", color = JarvisTextPrimary, fontSize = 14.sp)
                        Text("Triggers immediately on 'Jarvis'", color = JarvisTextSecondary, fontSize = 12.sp)
                    }
                    Switch(
                        checked = wakeWordEnabled,
                        onCheckedChange = {
                            wakeWordEnabled = it
                            repository.preferences.wakeWordEnabled = it
                        },
                        colors = SwitchDefaults.colors(checkedThumbColor = JarvisCyan, checkedTrackColor = JarvisCyan.copy(alpha = 0.5f))
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Sensitivity: ${String.format("%.0f%%", sensitivity * 100)}",
                    color = JarvisTextSecondary,
                    fontSize = 12.sp
                )
                Slider(
                    value = sensitivity,
                    onValueChange = {
                        sensitivity = it
                        repository.preferences.wakeWordSensitivity = it
                    },
                    valueRange = 0.2f..1.0f,
                    colors = SliderDefaults.colors(thumbColor = JarvisCyan, activeTrackColor = JarvisCyan)
                )
            }
        }

        // Section 3: AI Cognitive Core
        item {
            SettingsCard(title = "COGNITIVE ENGINE", icon = Icons.Default.Psychology) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val providers = listOf("LOCAL" to "Local Engine", "GEMINI" to "Google Gemini")
                    for ((key, label) in providers) {
                        val isSelected = aiProvider == key
                        Button(
                            onClick = {
                                aiProvider = key
                                repository.preferences.aiProvider = key
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isSelected) JarvisCyan.copy(alpha = 0.25f) else JarvisSurface
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .border(
                                    1.dp,
                                    if (isSelected) JarvisCyan else JarvisSurfaceBorder,
                                    RoundedCornerShape(8.dp)
                                )
                        ) {
                            Text(
                                text = label,
                                color = if (isSelected) JarvisCyan else JarvisTextSecondary,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }

                if (aiProvider == "GEMINI") {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Google Gemini API Key (Encrypted in Keystore):",
                        color = JarvisTextSecondary,
                        fontSize = 12.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedTextField(
                        value = geminiKey,
                        onValueChange = {
                            geminiKey = it
                            repository.secureStorage.geminiApiKey = it
                        },
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = JarvisTextPrimary,
                            unfocusedTextColor = JarvisTextPrimary,
                            focusedBorderColor = JarvisCyan,
                            unfocusedBorderColor = JarvisSurfaceBorder,
                            focusedContainerColor = JarvisSurface,
                            unfocusedContainerColor = JarvisSurface
                        ),
                        placeholder = { Text("Enter AI Studio API Key", color = JarvisTextMuted) }
                    )
                }
            }
        }

        // Section 4: Long-Term Memory
        item {
            SettingsCard(title = "STRUCTURED MEMORY", icon = Icons.Default.Memory) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Contextual Fact Retention", color = JarvisTextPrimary, fontSize = 14.sp)
                        Text("Extracts projects, preferences & facts", color = JarvisTextSecondary, fontSize = 12.sp)
                    }
                    Switch(
                        checked = memoryEnabled,
                        onCheckedChange = {
                            memoryEnabled = it
                            repository.preferences.memoryEnabled = it
                        },
                        colors = SwitchDefaults.colors(checkedThumbColor = JarvisCyan, checkedTrackColor = JarvisCyan.copy(alpha = 0.5f))
                    )
                }

                if (memories.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = onClearMemories,
                        colors = ButtonDefaults.buttonColors(containerColor = JarvisCrimson.copy(alpha = 0.15f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, JarvisCrimson.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                    ) {
                        Text("Purge All Memories (${memories.size})", color = JarvisCrimson, fontSize = 12.sp)
                    }
                }
            }
        }

        // List individual memories if present
        if (memories.isNotEmpty() && memoryEnabled) {
            items(memories) { mem ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(JarvisSurfaceGlass)
                        .border(1.dp, JarvisSurfaceBorder, RoundedCornerShape(8.dp))
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "${mem.category}: ${mem.key}",
                            color = JarvisCyan,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = mem.value,
                            color = JarvisTextPrimary,
                            fontSize = 13.sp
                        )
                    }
                    IconButton(onClick = { onDeleteMemory(mem.id) }) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete Memory",
                            tint = JarvisCrimson.copy(alpha = 0.8f),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }

        // Section 5: Permissions Status
        item {
            SettingsCard(title = "SECURITY & PERMISSIONS", icon = Icons.Default.Security) {
                PermissionRow(
                    label = "Microphone (Acoustic Capture)",
                    isGranted = PermissionManager.hasRecordAudioPermission(context),
                    onFix = {
                        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                            data = Uri.fromParts("package", context.packageName, null)
                        }
                        context.startActivity(intent)
                    }
                )

                PermissionRow(
                    label = "Device Contacts (Address Book)",
                    isGranted = PermissionManager.hasContactsPermission(context),
                    onFix = {
                        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                            data = Uri.fromParts("package", context.packageName, null)
                        }
                        context.startActivity(intent)
                    }
                )

                PermissionRow(
                    label = "Foreground Notifications",
                    isGranted = PermissionManager.hasNotificationPermission(context),
                    onFix = {
                        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                            data = Uri.fromParts("package", context.packageName, null)
                        }
                        context.startActivity(intent)
                    }
                )
            }
        }

        item {
            Spacer(modifier = Modifier.height(24.dp))
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "JARVIS PERSONAL AI AGENT // VER 1.0",
                    color = JarvisTextMuted,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun SettingsCard(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, JarvisSurfaceBorder, RoundedCornerShape(14.dp)),
        colors = CardDefaults.cardColors(containerColor = JarvisSurfaceGlass),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = JarvisCyan,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.size(8.dp))
                Text(
                    text = title,
                    color = JarvisCyan,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            content()
        }
    }
}

@Composable
private fun PermissionRow(
    label: String,
    isGranted: Boolean,
    onFix: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = if (isGranted) Icons.Default.CheckCircle else Icons.Default.Warning,
                contentDescription = null,
                tint = if (isGranted) JarvisCyan else JarvisAmber,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.size(8.dp))
            Text(text = label, color = JarvisTextPrimary, fontSize = 12.sp)
        }
        if (!isGranted) {
            Button(
                onClick = onFix,
                colors = ButtonDefaults.buttonColors(containerColor = JarvisAmber.copy(alpha = 0.2f)),
                modifier = Modifier.height(30.dp)
            ) {
                Text("Grant", color = JarvisAmber, fontSize = 11.sp)
            }
        }
    }
}
