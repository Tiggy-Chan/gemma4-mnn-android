package com.tiggy.gemma4mnn.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room entity representing a complete chat session.
 * Each session is tied to a specific model and contains ordered messages.
 */
@Entity(tableName = "chat_session")
data class ChatSessionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val modelName: String,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val title: String = "",
)
