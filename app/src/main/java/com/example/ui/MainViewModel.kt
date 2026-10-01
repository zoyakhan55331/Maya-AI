package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.gemini.GeminiApiClient
import com.example.data.gemini.GeminiResponse
import com.example.data.gemini.MahiPersona
import com.example.data.model.ChatMessage
import com.example.data.model.MahiNote
import com.example.data.model.SassLevel
import com.example.data.model.ToolExecution
import com.example.data.model.VoiceState
import com.example.data.tools.DeviceToolManager
import com.example.voice.MahiVoiceEngine
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.random.Random

data class MainUiState(
    val voiceState: VoiceState = VoiceState.DISCONNECTED,
    val audioLevel: Float = 0f,
    val liveTranscript: String = "",
    val lastMahiSpeech: String = "Hey there, handsome! Tap my orb or say anything to start teasing me. 😉",
    val statusText: String = "Tap to talk to Mahi",
    val sassLevel: SassLevel = SassLevel.SASSY,
    val voicePitch: Float = 1.15f,
    val voiceSpeed: Float = 1.05f,
    val isContinuousMode: Boolean = true,
    val isMuted: Boolean = false,
    val isApiKeyConfigured: Boolean = false,
    val customApiKey: String = "",
    val activeTool: ToolExecution? = null,
    val notes: List<MahiNote> = emptyList(),
    val isSettingsOpen: Boolean = false,
    val isKeyboardOpen: Boolean = false
)

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val toolManager = DeviceToolManager(application.applicationContext)
    val geminiApiClient = GeminiApiClient(toolManager)
    val voiceEngine = MahiVoiceEngine(application.applicationContext, viewModelScope)

    private val _uiState = MutableStateFlow(
        MainUiState(
            isApiKeyConfigured = geminiApiClient.isApiKeyConfigured()
        )
    )
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    private val conversationTurns = mutableListOf<Pair<String, String>>()

    init {
        // Collect voice engine state
        viewModelScope.launch {
            voiceEngine.voiceState.collect { state ->
                val newStatus = when (state) {
                    VoiceState.DISCONNECTED -> "Tap orb to wake Mahi"
                    VoiceState.CONNECTING -> "Connecting to Mahi..."
                    VoiceState.LISTENING -> getRandomItem(MahiPersona.LISTENING_STATUS_LINES)
                    VoiceState.THINKING -> getRandomItem(MahiPersona.THINKING_STATUS_LINES)
                    VoiceState.SPEAKING -> "Mahi is speaking..."
                }
                _uiState.value = _uiState.value.copy(
                    voiceState = state,
                    statusText = newStatus
                )
            }
        }

        viewModelScope.launch {
            voiceEngine.audioLevel.collect { level ->
                _uiState.value = _uiState.value.copy(audioLevel = level)
            }
        }

        viewModelScope.launch {
            voiceEngine.liveTranscript.collect { transcript ->
                _uiState.value = _uiState.value.copy(liveTranscript = transcript)
            }
        }

        voiceEngine.onUserSpeechCompleted = { text ->
            processUserInput(text)
        }
    }

    private fun getRandomItem(list: List<String>): String {
        return list[Random.nextInt(list.size)]
    }

    fun toggleSession() {
        val current = _uiState.value.voiceState
        if (current == VoiceState.DISCONNECTED) {
            voiceEngine.startSession()
        } else if (current == VoiceState.SPEAKING) {
            voiceEngine.interrupt()
            voiceEngine.startListening()
        } else {
            voiceEngine.stopSession()
        }
    }

    fun interruptMahi() {
        voiceEngine.interrupt()
        if (_uiState.value.isContinuousMode) {
            voiceEngine.startListening()
        } else {
            _uiState.value = _uiState.value.copy(voiceState = VoiceState.DISCONNECTED)
        }
    }

    fun processUserInput(text: String) {
        if (text.isBlank()) return

        _uiState.value = _uiState.value.copy(
            liveTranscript = text,
            voiceState = VoiceState.THINKING,
            statusText = getRandomItem(MahiPersona.THINKING_STATUS_LINES)
        )
        voiceEngine.setThinkingState()

        viewModelScope.launch {
            val response = geminiApiClient.sendVoicePrompt(
                userPrompt = text,
                sassLevel = _uiState.value.sassLevel,
                conversationHistory = conversationTurns
            )

            when (response) {
                is GeminiResponse.Success -> {
                    conversationTurns.add("user" to text)
                    conversationTurns.add("model" to response.spokenText)

                    if (response.toolExecution != null) {
                        _uiState.value = _uiState.value.copy(activeTool = response.toolExecution)
                        if (response.toolExecution.name == "saveNote") {
                            val newNotes = _uiState.value.notes + MahiNote(text = response.toolExecution.detail)
                            _uiState.value = _uiState.value.copy(notes = newNotes)
                        }
                    }

                    _uiState.value = _uiState.value.copy(
                        lastMahiSpeech = response.spokenText,
                        liveTranscript = ""
                    )

                    voiceEngine.speak(response.spokenText)
                }
                is GeminiResponse.Error -> {
                    val fallback = "Oops, got a little tongue-tied! Try asking me again, handsome. 😉"
                    _uiState.value = _uiState.value.copy(lastMahiSpeech = fallback)
                    voiceEngine.speak(fallback)
                }
            }
        }
    }

    fun setSassLevel(level: SassLevel) {
        _uiState.value = _uiState.value.copy(sassLevel = level)
    }

    fun setVoicePitch(pitch: Float) {
        voiceEngine.voicePitch = pitch
        _uiState.value = _uiState.value.copy(voicePitch = pitch)
    }

    fun setVoiceSpeed(speed: Float) {
        voiceEngine.voiceSpeed = speed
        _uiState.value = _uiState.value.copy(voiceSpeed = speed)
    }

    fun toggleContinuousMode(enabled: Boolean) {
        voiceEngine.isContinuousMode = enabled
        _uiState.value = _uiState.value.copy(isContinuousMode = enabled)
    }

    fun toggleMute() {
        val newMute = !_uiState.value.isMuted
        voiceEngine.isMuted = newMute
        _uiState.value = _uiState.value.copy(isMuted = newMute)
        if (newMute) {
            voiceEngine.interrupt()
        }
    }

    fun setCustomApiKey(key: String) {
        geminiApiClient.customApiKey = key.trim()
        _uiState.value = _uiState.value.copy(
            customApiKey = key.trim(),
            isApiKeyConfigured = geminiApiClient.isApiKeyConfigured()
        )
    }

    fun testVoice() {
        val testLine = when (_uiState.value.sassLevel) {
            SassLevel.PLAYFUL -> "Hi cutie! I'm Mahi. Do I sound sweet enough for you?"
            SassLevel.SASSY -> "Hey handsome! I'm Mahi. Try not to fall in love with my voice, okay?"
            SassLevel.SAVAGE -> "I'm Mahi. Don't waste my time unless you've got something witty to say, darling."
        }
        voiceEngine.speak(testLine)
    }

    fun openSettings(open: Boolean) {
        _uiState.value = _uiState.value.copy(isSettingsOpen = open)
    }

    fun openKeyboard(open: Boolean) {
        _uiState.value = _uiState.value.copy(isKeyboardOpen = open)
    }

    fun dismissTool() {
        _uiState.value = _uiState.value.copy(activeTool = null)
    }

    override fun onCleared() {
        super.onCleared()
        toolManager.cleanup()
        voiceEngine.destroy()
    }
}
