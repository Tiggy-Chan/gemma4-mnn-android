package com.tiggy.gemma4mnn.data.db

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Room entity for an individual message within a chat session.
 *
 * The `type` field maps to [com.tiggy.gemma4mnn.model.ChatMessage] subtypes:
 * "user", "text", "thinking", "error"
 */
@Entity(
    tableName = "chat_message",
    foreignKeys = [
        ForeignKey(
            entity = ChatSessionEntity::class,
            parentColumns = ["id"],
            childColumns = ["sessionId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("sessionId")],
)
data class ChatMessageEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val sessionId: Long,
    val type: String,       // "user", "text", "thinking", "error"
    val content: String,
    val orderIndex: Int,
    val timestamp: Long = System.currentTimeMillis(),
    val isInProgress: Boolean = false, // For thinking messages
)
