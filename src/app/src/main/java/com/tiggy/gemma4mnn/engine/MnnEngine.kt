package com.tiggy.gemma4mnn.engine

import com.tiggy.gemma4mnn.data.ModelRepository
import com.tiggy.gemma4mnn.model.ChatMessage
import com.tiggy.gemma4mnn.model.ModelConfig
import com.tiggy.gemma4mnn.parser.ChannelCallback
import com.tiggy.gemma4mnn.parser.ChannelParser
import com.tiggy.gemma4mnn.parser.ChunkType

/**
 * Result emitted by the engine during streaming generation.
 */
sealed class EngineResult {
    data class Chunk(val type: ChunkType, val content: String) : EngineResult()
    data object Done : EngineResult()
    data class Error(val message: String) : EngineResult()
}

/**
 * High-level MNN engine interface.
 *
 * Manages the MnnSession lifecycle, builds prompts from chat history,
 * and routes streaming tokens through the ChannelParser.
 */
class MnnEngine(
    private val modelRepository: ModelRepository,
) {
    private var session: MnnSession? = null
    private var parser: ChannelParser? = null

    /**
     * Start streaming generation.
     *
     * @param messages Chat history to include in the prompt
     * @param modelConfig Model to use for generation
     * @param enableThinking Whether to enable thinking mode
     * @param callback Callback to receive parsed chunks
     */
    fun generateStream(
        messages: List<ChatMessage>,
        modelConfig: ModelConfig,
        enableThinking: Boolean,
        callback: ChannelCallback,
    ) {
        try {
            // Initialize session if needed
            if (session == null || session?.modelPath != modelConfig.path) {
                session?.release()
                session = createSession(modelConfig, callback)
            }

            // Build prompt from chat history
            val (prompt, history) = buildPrompt(messages, modelConfig, enableThinking)

            // Create fresh parser for this generation
            parser = ChannelParser(callback)

            // Start generation — JNI callback feeds tokens to parser
            session?.generate(prompt, history.toTypedArray())
        } catch (e: Exception) {
            callback.onChunk(ChunkType.ERROR, e.message ?: "Unknown error")
        }
    }

    fun stop() {
        session?.stopGeneration()
    }

    fun release() {
        stop()
        session?.release()
        session = null
    }

    private fun createSession(
        modelConfig: ModelConfig,
        callback: ChannelCallback,
    ): MnnSession {
        val session = MnnSession(
            modelPath = modelConfig.path,
            tokenCallback = { token -> parser?.pushToken(token) },
        )

        val configPath = modelConfig.configPath.ifEmpty { modelConfig.path }
        if (!session.loadModel(configPath)) {
            throw IllegalStateException("Failed to load model from $configPath")
        }

        return session
    }

    /**
     * Builds a prompt and history from chat messages.
     *
     * For Gemma 4, the format follows:
     * ```
     * <start_of_turn>user
     * {user message}<end_of_turn>
     * <start_of_turn>model
     * {model response}<end_of_turn>
     * ```
     *
     * When thinking is enabled, the model's Jinja template injects
     * the appropriate `<|think|>` markers.
     */
    private fun buildPrompt(
        messages: List<ChatMessage>,
        modelConfig: ModelConfig,
        enableThinking: Boolean,
    ): PromptResult {
        val history = mutableListOf<ChatDataItem>()
        val lastUserMessage = StringBuilder()

        for (msg in messages) {
            when (msg) {
                is ChatMessage.User -> {
                    history.add(ChatDataItem("user", msg.content))
                    lastUserMessage.append(msg.content)
                }
                is ChatMessage.Text -> {
                    history.add(ChatDataItem("assistant", msg.content))
                }
                is ChatMessage.Thinking -> {
                    // Skip thinking messages — they're internal to the model
                }
                is ChatMessage.Error -> {
                    // Skip error messages
                }
            }
        }

        // The last user message is passed separately to the generate() call
        // History contains everything up to (but not including) the final user turn
        val promptText = if (lastUserMessage.isNotEmpty()) {
            lastUserMessage.toString()
        } else {
            ""
        }

        // Remove the last user item from history since generate() adds it
        val historyForMnn = if (history.isNotEmpty() && history.last().role == "user") {
            history.dropLast(1)
        } else {
            history
        }

        return PromptResult(promptText, historyForMnn)
    }

    private data class PromptResult(
        val prompt: String,
        val history: List<ChatDataItem>,
    )
}
