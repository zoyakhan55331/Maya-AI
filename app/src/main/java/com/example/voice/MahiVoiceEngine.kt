package com.example.voice

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import com.example.data.model.VoiceState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.Locale
import kotlin.random.Random

class MahiVoiceEngine(
    private val context: Context,
    private val scope: CoroutineScope
) : RecognitionListener, TextToSpeech.OnInitListener {

    private val tag = "MahiVoiceEngine"

    private var speechRecognizer: SpeechRecognizer? = null
    private var textToSpeech: TextToSpeech? = null
    private var isTtsReady = false

    private val _voiceState = MutableStateFlow(VoiceState.DISCONNECTED)
    val voiceState: StateFlow<VoiceState> = _voiceState.asStateFlow()

    private val _audioLevel = MutableStateFlow(0f)
    val audioLevel: StateFlow<Float> = _audioLevel.asStateFlow()

    private val _liveTranscript = MutableStateFlow("")
    val liveTranscript: StateFlow<String> = _liveTranscript.asStateFlow()

    var onUserSpeechCompleted: ((String) -> Unit)? = null
    var isContinuousMode: Boolean = true
    var voicePitch: Float = 1.15f
    var voiceSpeed: Float = 1.05f
    var isMuted: Boolean = false

    private var speakingAnimationJob: Job? = null
    private var isListeningActive = false

    init {
        initTts()
    }

    private fun initTts() {
        textToSpeech = TextToSpeech(context, this)
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            textToSpeech?.let { tts ->
                val result = tts.setLanguage(Locale.US)
                if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    tts.language = Locale.getDefault()
                }

                // Try to find a pleasant female voice
                val voices = tts.voices
                if (voices != null) {
                    val femaleVoice = voices.firstOrNull { voice ->
                        voice.name.lowercase().contains("female") ||
                                voice.name.lowercase().contains("en-us-x-sfg") ||
                                voice.name.lowercase().contains("en-us-x-tpd")
                    }
                    if (femaleVoice != null) {
                        tts.voice = femaleVoice
                    }
                }

                tts.setPitch(voicePitch)
                tts.setSpeechRate(voiceSpeed)

                tts.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) {
                        _voiceState.value = VoiceState.SPEAKING
                        startSpeakingWaveformAnimation()
                    }

                    override fun onDone(utteranceId: String?) {
                        stopSpeakingWaveformAnimation()
                        scope.launch(Dispatchers.Main) {
                            if (isContinuousMode) {
                                delay(300)
                                startListening()
                            } else {
                                _voiceState.value = VoiceState.DISCONNECTED
                            }
                        }
                    }

                    @Deprecated("Deprecated in Java")
                    override fun onError(utteranceId: String?) {
                        stopSpeakingWaveformAnimation()
                        scope.launch(Dispatchers.Main) {
                            if (isContinuousMode) {
                                startListening()
                            } else {
                                _voiceState.value = VoiceState.DISCONNECTED
                            }
                        }
                    }
                })
                isTtsReady = true
            }
        }
    }

    fun startSession() {
        _voiceState.value = VoiceState.CONNECTING
        scope.launch(Dispatchers.Main) {
            delay(500)
            startListening()
        }
    }

    fun stopSession() {
        interrupt()
        stopListening()
        _voiceState.value = VoiceState.DISCONNECTED
        _audioLevel.value = 0f
    }

    fun startListening() {
        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            Log.e(tag, "Speech recognition not available")
            return
        }

        interrupt() // Stop speaking if currently speaking

        scope.launch(Dispatchers.Main) {
            try {
                if (speechRecognizer == null) {
                    speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
                        setRecognitionListener(this@MahiVoiceEngine)
                    }
                }

                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.US.toString())
                    putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                    putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
                }

                speechRecognizer?.startListening(intent)
                isListeningActive = true
                _voiceState.value = VoiceState.LISTENING
                _liveTranscript.value = ""
            } catch (e: Exception) {
                Log.e(tag, "Failed to start speech recognition", e)
                _voiceState.value = VoiceState.DISCONNECTED
            }
        }
    }

    fun stopListening() {
        isListeningActive = false
        try {
            speechRecognizer?.stopListening()
            speechRecognizer?.cancel()
        } catch (e: Exception) {
            Log.e(tag, "Error stopping recognizer", e)
        }
    }

    fun interrupt() {
        stopSpeakingWaveformAnimation()
        try {
            textToSpeech?.stop()
        } catch (e: Exception) {
            Log.e(tag, "Error stopping TTS", e)
        }
    }

    fun speak(text: String) {
        if (isMuted) {
            scope.launch(Dispatchers.Main) {
                _voiceState.value = VoiceState.SPEAKING
                delay(1500)
                if (isContinuousMode) startListening() else _voiceState.value = VoiceState.DISCONNECTED
            }
            return
        }

        interrupt()
        stopListening()

        _voiceState.value = VoiceState.SPEAKING
        textToSpeech?.setPitch(voicePitch)
        textToSpeech?.setSpeechRate(voiceSpeed)

        val params = Bundle().apply {
            putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, "mahi_${System.currentTimeMillis()}")
        }
        textToSpeech?.speak(text, TextToSpeech.QUEUE_FLUSH, params, "mahi_${System.currentTimeMillis()}")
    }

    fun setThinkingState() {
        _voiceState.value = VoiceState.THINKING
        _audioLevel.value = 0.15f
    }

    private fun startSpeakingWaveformAnimation() {
        speakingAnimationJob?.cancel()
        speakingAnimationJob = scope.launch(Dispatchers.Default) {
            var step = 0
            while (isActive) {
                step++
                val syllableEnvelope = (kotlin.math.sin(step * 0.14) * 0.32 + 0.58).toFloat()
                val vocalFlutter = (kotlin.math.sin(step * 0.45) * 0.14).toFloat()
                val microJitter = (Random.nextFloat() - 0.5f) * 0.1f
                val intensity = (syllableEnvelope + vocalFlutter + microJitter).coerceIn(0.15f, 0.98f)
                _audioLevel.value = intensity
                delay(25)
            }
        }
    }

    private fun stopSpeakingWaveformAnimation() {
        speakingAnimationJob?.cancel()
        speakingAnimationJob = null
        _audioLevel.value = 0f
    }

    // --- RecognitionListener Implementation ---

    override fun onReadyForSpeech(params: Bundle?) {
        _voiceState.value = VoiceState.LISTENING
    }

    override fun onBeginningOfSpeech() {
        _voiceState.value = VoiceState.LISTENING
    }

    override fun onRmsChanged(rmsdB: Float) {
        if (_voiceState.value == VoiceState.LISTENING) {
            // rmsdB typically ranges from -2 to 10
            val normalized = ((rmsdB + 2f) / 12f).coerceIn(0.05f, 1f)
            _audioLevel.value = normalized
        }
    }

    override fun onBufferReceived(buffer: ByteArray?) {}

    override fun onEndOfSpeech() {
        _audioLevel.value = 0f
    }

    override fun onError(error: Int) {
        Log.d(tag, "Speech recognition error: $error")
        _audioLevel.value = 0f

        if (error == SpeechRecognizer.ERROR_NO_MATCH || error == SpeechRecognizer.ERROR_SPEECH_TIMEOUT) {
            if (isContinuousMode && isListeningActive) {
                scope.launch(Dispatchers.Main) {
                    delay(300)
                    startListening()
                }
            }
        } else {
            if (isContinuousMode && isListeningActive) {
                scope.launch(Dispatchers.Main) {
                    delay(600)
                    startListening()
                }
            }
        }
    }

    override fun onResults(results: Bundle?) {
        val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
        val text = matches?.firstOrNull()?.trim()
        if (!text.isNullOrBlank()) {
            _liveTranscript.value = text
            onUserSpeechCompleted?.invoke(text)
        } else {
            if (isContinuousMode) {
                startListening()
            }
        }
    }

    override fun onPartialResults(partialResults: Bundle?) {
        val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
        val text = matches?.firstOrNull()?.trim()
        if (!text.isNullOrBlank()) {
            _liveTranscript.value = text
        }
    }

    override fun onEvent(eventType: Int, params: Bundle?) {}

    fun destroy() {
        interrupt()
        try {
            speechRecognizer?.destroy()
            speechRecognizer = null
            textToSpeech?.stop()
            textToSpeech?.shutdown()
            textToSpeech = null
        } catch (e: Exception) {
            Log.e(tag, "Destroy error", e)
        }
    }
}
