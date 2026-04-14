package com.tiggy.gemma4mnn.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tiggy.gemma4mnn.data.ChatRepository
import com.tiggy.gemma4mnn.data.SettingsRepository
import com.tiggy.gemma4mnn.engine.MnnEngine
import com.tiggy.gemma4mnn.model.ChatMessage
import com.tiggy.gemma4mnn.model.ModelConfig
import com.tiggy.gemma4mnn.parser.ChannelCallback
import com.tiggy.gemma4mnn.parser.ChunkType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlowF
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

/**
 * ViewModel for the chat screen.
 *
 * Manages message state, generation lifecycle, and user settings.
 * Persists messages to Room database via [ChatRepository].
 */
class ChatViewModel(
    private val engine: MnnEngine,
    private val settings: SettingsRepository,
    private val chatRepository: ChatRepository,
) : ViewModel() {

    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    private val _isGenerating = MutableStateFlow(false)
    val isGenerating: StateFlow<Boolean> = _isGenerating.asStateFlow()

    private val _thinkingEnabled = MutableStateFlow(false)
    val thinkingEnabled: StateFlow<Boolean> = _thinkingEnabled.asStateFlow()

    private val _selectedModel = MutableStateFlow<ModelConfig?>(null)
    val selectedModel: StateFlow<ModelConfig?> = _selectedModel.asStateFlow()

    // Current active session ID (for persistence)
    private var currentSessionId: Long = 0

    // Current streaming indices (used to update in-progress messages)
    private var currentThinkingIndex = -1
    private var currentTextIndex = -1
    private var messageOrderCounter = 0

    init {
        viewModelScope.launch {
            settings.thinkingEnabled.collect { enabled ->
                _thinkingEnabled.value = enabled
            }
        }
    }

    /**
     * Load a session from the database.
     */
    fun loadSession(sessionId: Long) {
        viewModelScope.launch {
            chatRepository.getMessages(sessionId).onEach { msgs ->
                _messages.value = msgs
                messageOrderCounter = msgs.size
            }.launchIn(viewModelScope)
        }
        currentSessionId = sessionId
    }

    fun sendMessage(text: String) {
        if (text.isBlank() || _isGenerating.value) return

        if (text.startsWith("/search ")) {
            val query = text.removePrefix("/search ").trim()
            _isGenerating.value = true // Lock the UI while searching
            
            viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
                try {
                    val client = okhttp3.OkHttpClient()
                    val request = okhttp3.Request.Builder()
                        .url("https://html.duckduckgo.com/html/?q=$query")
                        .build()
                        
                    val response = client.newCall(request).execute()
                    val htmlData = response.body?.string() ?: ""
                    
                    // Clean HTML tags and limit size
                    val cleanText = htmlData.replace(Regex("<[^>]*>"), " ").replace(Regex("\\s+"), " ").take(1500)
                    val injectedText = "Here is real-time web data: $cleanText \n\nBased on that, answer this: $query"
                    
                    // Switch back to Main thread to send to Gemma
                    kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                        _isGenerating.value = false
                        executeSend(injectedText)
                    }
                } catch (e: Exception) {
                    kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                        _isGenerating.value = false
                        executeSend("Error searching the web: ${e.message}\n\nOriginal prompt: $text") 
                    }
                }
            }
        } else {
            // Normal message handling
            executeSend(text)
        }
    }

    private fun executeSend(finalText: String) {
        val userMsg = ChatMessage.User(content = finalText)
        _messages.value = _messages.value + userMsg
        messageOrderCounter++

        viewModelScope.launch {
            if (currentSessionId == 0L) {
                val modelName = _selectedModel.value?.name ?: "unknown"
                currentSessionId = chatRepository.createSession(modelName)
            }
            chatRepository.saveMessage(currentSessionId, userMsg, messageOrderCounter - 1)
        }

        startGeneration()
    }

    fun selectModel(model: ModelConfig) {
        _selectedModel.value = model
        viewModelScope.launch {
            settings.setSelectedModel(model.name)
        }
    }

    fun toggleThinking() {
        _thinkingEnabled.value = !_thinkingEnabled.value
        viewModelScope.launch {
            settings.setThinkingEnabled(_thinkingEnabled.value)
        }
    }

    fun stopGeneration() {
        engine.stop()
        _isGenerating.value = false
        finalizeInProgress()
    }

    private fun startGeneration() {
        _isGenerating.value = true
        currentThinkingIndex = -1
        currentTextIndex = -1

        val model = _selectedModel.value ?: run {
            _messages.value = _messages.value + ChatMessage.Error("No model selected")
            _isGenerating.value = false
            return
        }

        val callback = object : ChannelCallback {
            override fun onChunk(type: ChunkType, content: String) {
                val currentMessages = _messages.value.toMutableList()

                when (type) {
                    ChunkType.THINKING -> {
                        if (currentThinkingIndex < 0 || currentThinkingIndex >= currentMessages.size) {
                            currentThinkingIndex = currentMessages.size
                            currentMessages.add(ChatMessage.Thinking(content = content, inProgress = true))
                        } else {
                            val existing = currentMessages[currentThinkingIndex] as? ChatMessage.Thinking
                            if (existing != null) {
                                currentMessages[currentThinkingIndex] = existing.copy(content = existing.content + content)
                            }
                        }
                    }

                    ChunkType.NORMAL -> {
                        if (currentTextIndex < 0 || currentTextIndex >= currentMessages.size) {
                            currentTextIndex = currentMessages.size
                            currentMessages.add(ChatMessage.Text(content = content))
                        } else {
                            val existing = currentMessages[currentTextIndex] as? ChatMessage.Text
                            if (existing != null) {
                                currentMessages[currentTextIndex] = existing.copy(content = existing.content + content)
                            }
                        }
                    }

                    ChunkType.ERROR -> {
                        currentMessages.add(ChatMessage.Error(message = content))
                    }
                }

                _messages.value = currentMessages
            }

            override fun onDone() {
                _isGenerating.value = false
                finalizeInProgress()

                // Persist final messages to database
                viewModelScope.launch {
                    if (currentSessionId == 0L) {
                        val modelName = model.name
                        currentSessionId = chatRepository.createSession(modelName)
                    }
                    // Only save new messages (those without "db-" prefix)
                    _messages.value.filter { !it.id.startsWith("db-") }.forEachIndexed { index, msg ->
                        chatRepository.saveMessage(currentSessionId, msg, index)
                    }
                }
            }
        }

        engine.generateStream(
            messages = _messages.value,
            modelConfig = model,
            enableThinking = _thinkingEnabled.value,
            callback = callback,
        )
    }

    private fun finalizeInProgress() {
        val msgs = _messages.value.toMutableList()
        for (i in msgs.indices) {
            if (msgs[i] is ChatMessage.Thinking && (msgs[i] as ChatMessage.Thinking).inProgress) {
                msgs[i] = (msgs[i] as ChatMessage.Thinking).copy(inProgress = false)
            }
        }
        _messages.value = msgs
        currentThinkingIndex = -1
        currentTextIndex = -1
    }

    fun clearChat() {
        _messages.value = emptyList()
        currentSessionId = 0
        messageOrderCounter = 0
        viewModelScope.launch {
            chatRepository.clearAll()
        }
    }

    override fun onCleared() {
        super.onCleared()
        engine.release()
    }
}
