package com.example.jarvis.ui.main

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.jarvis.agent.AgentEngine
import com.example.jarvis.agent.JarvisState
import com.example.jarvis.agent.JarvisUiState
import com.example.jarvis.agent.PendingConfirmation
import com.example.jarvis.data.JarvisRepository
import com.example.jarvis.data.model.ChatMessage
import com.example.jarvis.data.model.MemoryItem
import com.example.jarvis.permissions.PermissionManager
import com.example.jarvis.tools.ToolRegistry
import com.example.jarvis.voice.AudioFeedbackManager
import com.example.jarvis.voice.SpeechRecognizerEngine
import com.example.jarvis.voice.TextToSpeechEngine
import com.example.jarvis.voice.WakeWordDetector
import com.example.jarvis.voice.impl.AndroidSpeechRecognizer
import com.example.jarvis.voice.impl.AndroidTextToSpeech
import com.example.jarvis.voice.impl.KeywordWakeWordDetector
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class JarvisViewModel(application: Application) : AndroidViewModel(application) {

    val repository = JarvisRepository(application)
    val toolRegistry = ToolRegistry(repository)
    val agentEngine = AgentEngine(repository, toolRegistry)

    val feedbackManager = AudioFeedbackManager(application)
    val ttsEngine: TextToSpeechEngine = AndroidTextToSpeech(
        application,
        speechRate = repository.preferences.speechRate,
        speechPitch = repository.preferences.speechPitch
    )
    val speechRecognizer: SpeechRecognizerEngine = AndroidSpeechRecognizer(application)
    val wakeWordDetector: WakeWordDetector = KeywordWakeWordDetector(
        application,
        sensitivity = repository.preferences.wakeWordSensitivity
    )

    private val _uiState = MutableStateFlow(JarvisUiState())
    val uiState: StateFlow<JarvisUiState> = _uiState.asStateFlow()

    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    private val _memories = MutableStateFlow<List<MemoryItem>>(emptyList())
    val memories: StateFlow<List<MemoryItem>> = _memories.asStateFlow()

    init {
        loadHistoryAndMemories()
        startWakeWordListeningIfEnabled()
    }

    fun loadHistoryAndMemories() {
        viewModelScope.launch(Dispatchers.IO) {
            _messages.value = repository.getMessages()
            _memories.value = repository.getMemories()
        }
    }

    fun startWakeWordListeningIfEnabled() {
        if (repository.preferences.wakeWordEnabled &&
            PermissionManager.hasRecordAudioPermission(getApplication()) &&
            !wakeWordDetector.isListening
        ) {
            wakeWordDetector.startListening {
                onWakeWordDetected()
            }
            _uiState.update { it.copy(isWakeWordActive = true) }
        }
    }

    fun stopWakeWordListening() {
        wakeWordDetector.stopListening()
        _uiState.update { it.copy(isWakeWordActive = false) }
    }

    fun onWakeWordDetected() {
        viewModelScope.launch(Dispatchers.Main) {
            feedbackManager.playWakeChime()
            startListeningForCommand()
        }
    }

    fun triggerManualListening() {
        // Barge-in: if currently speaking, interrupt TTS immediately
        if (_uiState.value.state == JarvisState.SPEAKING) {
            ttsEngine.stop()
        }
        feedbackManager.playWakeChime()
        startListeningForCommand()
    }

    private fun startListeningForCommand() {
        wakeWordDetector.stopListening()

        _uiState.update {
            it.copy(
                state = JarvisState.LISTENING,
                transcript = "",
                spokenText = "Listening, sir...",
                activeToolName = null,
                errorMessage = null
            )
        }

        speechRecognizer.startListening(
            onPartialResult = { partial ->
                _uiState.update { it.copy(transcript = partial) }
            },
            onFinalResult = { finalTranscript ->
                _uiState.update {
                    it.copy(
                        transcript = finalTranscript,
                        state = JarvisState.THINKING
                    )
                }
                processCommand(finalTranscript)
            },
            onRmsChanged = { rms ->
                _uiState.update { it.copy(rmsLevel = rms) }
            },
            onError = { error ->
                _uiState.update {
                    it.copy(
                        state = JarvisState.IDLE,
                        spokenText = "Standing by, sir.",
                        errorMessage = error
                    )
                }
                startWakeWordListeningIfEnabled()
            }
        )
    }

    fun submitPrompt(prompt: String) {
        if (_uiState.value.state == JarvisState.SPEAKING) {
            ttsEngine.stop()
        }
        _uiState.update {
            it.copy(
                transcript = prompt,
                state = JarvisState.THINKING
            )
        }
        processCommand(prompt)
    }

    private fun processCommand(command: String) {
        viewModelScope.launch {
            agentEngine.processUserPrompt(
                userInput = command,
                context = getApplication(),
                onStateChange = { state, toolName ->
                    _uiState.update {
                        it.copy(
                            state = state,
                            activeToolName = toolName
                        )
                    }
                },
                onSpokenResponse = { responseText ->
                    _uiState.update {
                        it.copy(
                            spokenText = responseText,
                            state = JarvisState.SPEAKING
                        )
                    }
                    speakResponse(responseText)
                    loadHistoryAndMemories()
                },
                onConfirmationNeeded = { pending ->
                    _uiState.update {
                        it.copy(
                            pendingConfirmation = pending,
                            spokenText = pending.prompt
                        )
                    }
                    speakResponse(pending.prompt)
                }
            )
        }
    }

    private fun speakResponse(text: String) {
        ttsEngine.speak(
            text = text,
            onStart = {
                _uiState.update { it.copy(state = JarvisState.SPEAKING) }
            },
            onDone = {
                _uiState.update {
                    it.copy(
                        state = JarvisState.IDLE,
                        activeToolName = null
                    )
                }
                startWakeWordListeningIfEnabled()
            },
            onError = {
                _uiState.update { it.copy(state = JarvisState.IDLE) }
                startWakeWordListeningIfEnabled()
            }
        )
    }

    fun confirmPendingAction() {
        val pending = _uiState.value.pendingConfirmation
        _uiState.update { it.copy(pendingConfirmation = null) }
        pending?.onConfirm?.invoke()
    }

    fun cancelPendingAction() {
        val pending = _uiState.value.pendingConfirmation
        _uiState.update { it.copy(pendingConfirmation = null) }
        pending?.onCancel?.invoke()
    }

    fun clearHistory() {
        viewModelScope.launch(Dispatchers.IO) {
            repository.clearMessages()
            loadHistoryAndMemories()
        }
    }

    fun deleteMemory(id: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteMemory(id)
            loadHistoryAndMemories()
        }
    }

    fun clearMemories() {
        viewModelScope.launch(Dispatchers.IO) {
            repository.clearMemories()
            loadHistoryAndMemories()
        }
    }

    override fun onCleared() {
        super.onCleared()
        wakeWordDetector.stopListening()
        speechRecognizer.cancel()
        ttsEngine.shutdown()
    }
}
