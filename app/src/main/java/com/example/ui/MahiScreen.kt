package com.example.ui

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.automirrored.rounded.VolumeMute
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Keyboard
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.MicOff
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.StopCircle
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.VoiceState
import com.example.ui.components.GlowingOrb
import com.example.ui.components.QuickActionChips
import com.example.ui.components.SettingsDialog
import com.example.ui.components.ToolCard
import com.example.ui.components.TranscriptOverlay
import com.example.ui.components.WaveformVisualizer
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkCardBorder
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceElevated
import com.example.ui.theme.DarkSurfaceVariant
import com.example.ui.theme.ElectricViolet
import com.example.ui.theme.NeonAmber
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonMagenta
import com.example.ui.theme.NeonPink
import com.example.ui.theme.NeonPurple
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun MahiScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    var hasAudioPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasAudioPermission = isGranted
        if (isGranted) {
            viewModel.toggleSession()
        }
    }

    var typedInput by remember { mutableStateOf("") }

    val statusBarPadding = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val navBarPadding = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = DarkBackground,
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            ElectricViolet.copy(alpha = 0.18f),
                            DarkBackground
                        ),
                        radius = 1200f
                    )
                ),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = 600.dp)
                    .padding(top = statusBarPadding, bottom = navBarPadding),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Top Header Bar
                TopBar(
                    voiceState = state.voiceState,
                    sassEmoji = state.sassLevel.emoji,
                    sassName = state.sassLevel.displayName,
                    isMuted = state.isMuted,
                    isApiKeySet = state.isApiKeyConfigured,
                    onSassClick = { viewModel.openSettings(true) },
                    onMuteToggle = { viewModel.toggleMute() },
                    onKeyboardToggle = { viewModel.openKeyboard(!state.isKeyboardOpen) },
                    onSettingsClick = { viewModel.openSettings(true) }
                )

                // Central Active Arena
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    // Tool Execution Banner (if active)
                    ToolCard(
                        toolExecution = state.activeTool,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    // Central Glowing Voice Orb
                    GlowingOrb(
                        voiceState = state.voiceState,
                        audioLevel = state.audioLevel,
                        onClick = {
                            if (!hasAudioPermission) {
                                permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                            } else {
                                viewModel.toggleSession()
                            }
                        }
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Audio Waveform Reactive Visualizer
                    WaveformVisualizer(
                        voiceState = state.voiceState,
                        audioLevel = state.audioLevel,
                        modifier = Modifier.padding(horizontal = 24.dp)
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Transcript and Status Overlay
                    TranscriptOverlay(
                        voiceState = state.voiceState,
                        statusText = state.statusText,
                        liveTranscript = state.liveTranscript,
                        lastMahiSpeech = state.lastMahiSpeech
                    )
                }

                // Bottom Section: Controls & Quick Prompts
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Stop / Interrupt Button (when Mahi is actively speaking)
                    AnimatedVisibility(
                        visible = state.voiceState == VoiceState.SPEAKING,
                        enter = fadeIn() + slideInVertically(initialOffsetY = { it / 2 }),
                        exit = fadeOut() + slideOutVertically(targetOffsetY = { it / 2 })
                    ) {
                        Surface(
                            onClick = { viewModel.interruptMahi() },
                            shape = RoundedCornerShape(24.dp),
                            color = NeonPink.copy(alpha = 0.2f),
                            border = BorderStroke(1.dp, NeonPink),
                            modifier = Modifier.testTag("interrupt_button")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.StopCircle,
                                    contentDescription = "Stop",
                                    tint = NeonPink,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = "Hush, Mahi! 🤫",
                                    color = TextPrimary,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    // Optional Text Input Drawer (for quiet environments)
                    AnimatedVisibility(
                        visible = state.isKeyboardOpen,
                        enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                        exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
                    ) {
                        Surface(
                            color = DarkSurfaceElevated,
                            border = BorderStroke(1.dp, DarkCardBorder),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp)
                                .testTag("keyboard_input_container")
                        ) {
                            Row(
                                modifier = Modifier.padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedTextField(
                                    value = typedInput,
                                    onValueChange = { typedInput = it },
                                    placeholder = {
                                        Text("Type something witty to Mahi...", color = TextMuted, fontSize = 13.sp)
                                    },
                                    singleLine = true,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = NeonCyan,
                                        unfocusedBorderColor = Color.Transparent,
                                        focusedTextColor = TextPrimary,
                                        unfocusedTextColor = TextPrimary
                                    ),
                                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                                    keyboardActions = KeyboardActions(onSend = {
                                        if (typedInput.isNotBlank()) {
                                            viewModel.processUserInput(typedInput)
                                            typedInput = ""
                                            viewModel.openKeyboard(false)
                                        }
                                    }),
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("typed_prompt_input")
                                )

                                IconButton(
                                    onClick = {
                                        if (typedInput.isNotBlank()) {
                                            viewModel.processUserInput(typedInput)
                                            typedInput = ""
                                            viewModel.openKeyboard(false)
                                        }
                                    },
                                    modifier = Modifier.testTag("send_typed_prompt_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Rounded.Send,
                                        contentDescription = "Send",
                                        tint = NeonCyan
                                    )
                                }
                            }
                        }
                    }

                    // Quick Action Starter Chips
                    QuickActionChips(
                        onChipSelected = { prompt ->
                            viewModel.processUserInput(prompt)
                        }
                    )

                    // Micro instruction text
                    Text(
                        text = when (state.voiceState) {
                            VoiceState.DISCONNECTED -> "Tap the orb to start talking"
                            VoiceState.CONNECTING -> "Connecting..."
                            VoiceState.LISTENING -> "Mahi is listening to you..."
                            VoiceState.THINKING -> "Thinking of something brilliant..."
                            VoiceState.SPEAKING -> "Tap orb or say 'Hush' to interrupt"
                        },
                        color = TextMuted,
                        fontSize = 11.sp,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                }
            }
        }

        // Settings Dialog
        if (state.isSettingsOpen) {
            SettingsDialog(
                currentSass = state.sassLevel,
                onSassSelected = { viewModel.setSassLevel(it) },
                voicePitch = state.voicePitch,
                onVoicePitchChange = { viewModel.setVoicePitch(it) },
                voiceSpeed = state.voiceSpeed,
                onVoiceSpeedChange = { viewModel.setVoiceSpeed(it) },
                isContinuousMode = state.isContinuousMode,
                onContinuousModeToggle = { viewModel.toggleContinuousMode(it) },
                isApiKeySet = state.isApiKeyConfigured,
                customApiKey = state.customApiKey,
                onSaveApiKey = { viewModel.setCustomApiKey(it) },
                notes = state.notes,
                onTestVoice = { viewModel.testVoice() },
                onDismiss = { viewModel.openSettings(false) }
            )
        }
    }
}

@Composable
private fun TopBar(
    voiceState: VoiceState,
    sassEmoji: String,
    sassName: String,
    isMuted: Boolean,
    isApiKeySet: Boolean,
    onSassClick: () -> Unit,
    onMuteToggle: () -> Unit,
    onKeyboardToggle: () -> Unit,
    onSettingsClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp)
            .testTag("mahi_top_bar"),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // App Title & Live Status
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Surface(
                shape = CircleShape,
                color = ElectricViolet.copy(alpha = 0.35f),
                border = BorderStroke(1.dp, NeonPink),
                modifier = Modifier.size(34.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Rounded.AutoAwesome,
                        contentDescription = "Mahi Logo",
                        tint = NeonPink,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Column {
                Text(
                    text = "Mahi AI",
                    color = TextPrimary,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    val dotColor = if (voiceState != VoiceState.DISCONNECTED) NeonCyan else Color(0xFF64748B)
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(dotColor)
                    )
                    Text(
                        text = if (voiceState != VoiceState.DISCONNECTED) "LIVE VOICE" else "OFFLINE",
                        color = if (voiceState != VoiceState.DISCONNECTED) NeonCyan else TextMuted,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                }
            }
        }

        // Action Icons & Persona Chip
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // Sass Chip
            Surface(
                onClick = onSassClick,
                shape = RoundedCornerShape(20.dp),
                color = DarkSurfaceVariant,
                border = BorderStroke(1.dp, NeonPink.copy(alpha = 0.4f)),
                modifier = Modifier.testTag("sass_pill_button")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(text = sassEmoji, fontSize = 12.sp)
                    Text(
                        text = sassName.split(" ").firstOrNull() ?: "Sassy",
                        color = TextPrimary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            // Mute / Unmute
            IconButton(
                onClick = onMuteToggle,
                modifier = Modifier
                    .size(40.dp)
                    .testTag("mute_toggle_button")
            ) {
                Icon(
                    imageVector = if (isMuted) Icons.AutoMirrored.Rounded.VolumeMute else Icons.AutoMirrored.Rounded.VolumeUp,
                    contentDescription = if (isMuted) "Unmute" else "Mute",
                    tint = if (isMuted) NeonPink else TextSecondary,
                    modifier = Modifier.size(20.dp)
                )
            }

            // Keyboard Toggle
            IconButton(
                onClick = onKeyboardToggle,
                modifier = Modifier
                    .size(40.dp)
                    .testTag("keyboard_toggle_button")
            ) {
                Icon(
                    imageVector = Icons.Rounded.Keyboard,
                    contentDescription = "Toggle Keyboard",
                    tint = TextSecondary,
                    modifier = Modifier.size(20.dp)
                )
            }

            // Settings
            IconButton(
                onClick = onSettingsClick,
                modifier = Modifier
                    .size(40.dp)
                    .testTag("settings_button")
            ) {
                Icon(
                    imageVector = Icons.Rounded.Settings,
                    contentDescription = "Settings",
                    tint = TextSecondary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}
