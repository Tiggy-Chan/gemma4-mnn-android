package com.tiggy.gemma4mnn.data

import android.content.Context
import com.tiggy.gemma4mnn.data.db.ChatDatabase
import com.tiggy.gemma4mnn.data.db.ChatMessageEntity
import com.tiggy.gemma4mnn.data.db.ChatSessionEntity
import com.tiggy.gemma4mnn.model.ChatMessage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Repository bridging Room database with the chat UI.
 *
 * Converts between [ChatMessageEntity]/[ChatSessionEntity] and domain
 * [ChatMessage] / session models.
 */
class ChatRepository(context: Context) {
    private val db = ChatDatabase.getInstance(context)
    private val sessionDao = db.sessionDao()
    private val messageDao = db.messageDao()

    fun getSessions(): Flow<List<ChatSessionSummary>> {
        return sessionDao.getAllSessions().map { sessions ->
            sessions.map { it.toSummary() }
        }
    }

    fun getMessages(sessionId: Long): Flow<List<ChatMessage>> {
        return messageDao.getMessagesForSession(sessionId).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    suspend fun createSession(modelName: String): Long {
        val session = ChatSessionEntity(modelName = modelName)
        return sessionDao.insertSession(session)
    }

    suspend fun saveMessage(sessionId: Long, message: ChatMessage, orderIndex: Int): Long {
        val entity = message.toEntity(sessionId, orderIndex)
        return messageDao.insertMessage(entity)
    }

    suspend fun saveMessages(sessionId: Long, messages: List<ChatMessage>) {
        val entities = messages.mapIndexed { index, msg ->
            msg.toEntity(sessionId, index)
        }
        messageDao.insertMessages(entities)
    }

    suspend fun updateMessageContent(messageId: Long, content: String) {
        messageDao.updateMessageContent(messageId, content)
    }

    suspend fun deleteSession(sessionId: Long) {
        sessionDao.deleteSession(sessionId)
    }

    suspend fun clearAll() {
        sessionDao.deleteAllSessions()
    }

    // ---- Conversion helpers ----

    private fun ChatSessionEntity.toSummary() = ChatSessionSummary(
        id = id,
        modelName = modelName,
        title = title.ifEmpty { modelName },
        updatedAt = updatedAt,
    )

    private fun ChatMessageEntity.toDomain(): ChatMessage = when (type) {
        "user" -> ChatMessage.User(id = "db-$id", content = content, timestamp = timestamp)
        "text" -> ChatMessage.Text(id = "db-$id", content = content, timestamp = timestamp)
        "thinking" -> ChatMessage.Thinking(
            id = "db-$id",
            content = content,
            inProgress = isInProgress,
            timestamp = timestamp,
        )
        "error" -> ChatMessage.Error(id = "db-$id", message = content, timestamp = timestamp)
        else -> ChatMessage.Error(id = "db-$id", message = "Unknown message type: $type")
    }

    private fun ChatMessage.toEntity(sessionId: Long, orderIndex: Int): ChatMessageEntity =
        ChatMessageEntity(
            sessionId = sessionId,
            type = when (this) {
                is ChatMessage.User -> "user"
                is ChatMessage.Text -> "text"
                is ChatMessage.Thinking -> "thinking"
                is ChatMessage.Error -> "error"
            },
            content = when (this) {
                is ChatMessage.User -> content
                is ChatMessage.Text -> content
                is ChatMessage.Thinking -> content
                is ChatMessage.Error -> message
            },
            orderIndex = orderIndex,
            timestamp = this.timestamp,
            isInProgress = if (this is ChatMessage.Thinking) inProgress else false,
        )
}

/**
 * Lightweight summary of a chat session for the session list UI.
 */
data class ChatSessionSummary(
    val id: Long,
    val modelName: String,
    val title: String,
    val updatedAt: Long,
)
