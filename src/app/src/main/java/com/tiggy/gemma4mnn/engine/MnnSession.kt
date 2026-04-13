package com.tiggy.gemma4mnn.engine

import android.util.Log

/**
 * Data item for MNN prompt history.
 * Matches the C++ PromptItem type (role, content).
 */
data class ChatDataItem(
    val role: String,  // "user", "assistant", "system"
    val content: String,
)

/**
 * JNI wrapper for MNN LLM inference session.
 *
 * Bridges Kotlin to the MNN C++ inference engine via JNI.
 * Model loading, prompt submission, and streaming token generation
 * are all handled through native method calls.
 *
 * The engine delivers tokens one-at-a-time via the [tokenCallback],
 * which should forward them to a [com.tiggy.gemma4mnn.parser.ChannelParser].
 */
class MnnSession(
    val modelPath: String,
    val tokenCallback: (String) -> Unit,
) {
    private val TAG = "MnnSession"

    init {
        try {
            System.loadLibrary("mnn_llm_jni")
        } catch (e: UnsatisfiedLinkError) {
            Log.e(TAG, "Failed to load native library: ${e.message}")
        }
    }

    /**
     * Load the model from the given config path.
     * @return true if successful
     */
    external fun loadModel(configPath: String): Boolean

    /**
     * Generate a response for the given prompt and history.
     * Tokens are delivered incrementally via [onTokenFromNative].
     *
     * @param prompt The user's latest message
     * @param history Prior conversation context
     * @return Number of tokens generated (for debugging)
     */
    external fun generate(prompt: String, history: Array<ChatDataItem>): Long

    /**
     * Stop the current generation.
     */
    external fun stopGeneration()

    /**
     * Release all resources.
     */
    external fun release()

    /**
     * Called from native code when a new token is generated.
     * Forwards to the engine's callback for parsing.
     */
    @Suppress("unused")
    @JvmStatic
    fun onTokenFromNative(token: String) {
        tokenCallback(token)
    }
}
