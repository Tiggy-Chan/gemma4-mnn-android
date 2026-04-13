package com.tiggy.gemma4mnn.model

/**
 * Sealed class representing different types of chat messages.
 */
sealed class ChatMessage {
    abstract val id: String
    abstract val timestamp: Long

    data class User(
        override val id: String = java.util.UUID.randomUUID().toString(),
        val content: String,
        override val timestamp: Long = System.currentTimeMillis(),
    ) : ChatMessage()

    data class Text(
        override val id: String = java.util.UUID.randomUUID().toString(),
        val content: String,
        override val timestamp: Long = System.currentTimeMillis(),
    ) : ChatMessage()

    data class Thinking(
        override val id: String = java.util.UUID.randomUUID().toString(),
        val content: String = "",
        val inProgress: Boolean = true,
        override val timestamp: Long = System.currentTimeMillis(),
    ) : ChatMessage()

    data class Error(
        override val id: String = java.util.UUID.randomUUID().toString(),
        val message: String,
        override val timestamp: Long = System.currentTimeMillis(),
    ) : ChatMessage()
}
