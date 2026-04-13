package com.tiggy.gemma4mnn.parser

/**
 * Types of chunks that can be emitted by the channel parser.
 */
enum class ChunkType {
    THINKING,
    NORMAL,
    ERROR,
}

/**
 * Callback interface for the channel parser.
 */
interface ChannelCallback {
    fun onChunk(type: ChunkType, content: String)
    fun onDone()
}

/**
 * Parse state for the GPT-OSS channel format.
 *
 * Gemma 4 outputs tokens wrapped in channel markers:
 * - `<|channel>thought` ... content ... `<channel|>` for thinking
 * - `<|message|>` for message boundaries
 *
 * This parser maintains a simple state machine, scanning incoming tokens
 * for state transitions and routing content to the appropriate callback.
 */
class ChannelParser(
    private val callback: ChannelCallback,
) {
    private enum class ParseState {
        NORMAL,
        THINKING,
    }

    private val tagBuffer = StringBuilder() // Accumulates partial token matches
    private var state: ParseState = ParseState.NORMAL

    /**
     * Push a new token from the LLM stream.
     *
     * The parser scans for `<|channel>` (enter thinking) and `<channel|>` (exit thinking)
     * markers. Partial markers that span multiple tokens are handled via the tagBuffer.
     */
    fun pushToken(token: String) {
        tagBuffer.append(token)
        val fullText = tagBuffer.toString()

        when (state) {
            ParseState.NORMAL -> {
                val idx = fullText.indexOf("<|channel>")
                if (idx >= 0) {
                    // Flush content before the tag as normal
                    val beforeTag = fullText.substring(0, idx)
                    if (beforeTag.isNotEmpty()) {
                        callback.onChunk(ChunkType.NORMAL, beforeTag)
                    }
                    // Reset buffer with remaining content after the tag
                    tagBuffer.clear()
                    tagBuffer.append(fullText.substring(idx + "<|channel>".length))
                    state = ParseState.THINKING
                } else {
                    // No tag found yet — check if we might have a partial match
                    // Keep last N chars that could be the start of "<|channel>"
                    val overlap = minOf(fullText.length, "<|channel>".length - 1)
                    val toFlush = fullText.substring(0, fullText.length - overlap)
                    if (toFlush.isNotEmpty()) {
                        callback.onChunk(ChunkType.NORMAL, toFlush)
                        tagBuffer.clear()
                        tagBuffer.append(fullText.substring(fullText.length - overlap))
                    }
                }
            }

            ParseState.THINKING -> {
                val idx = fullText.indexOf("<channel|>")
                if (idx >= 0) {
                    // Flush thinking content before the closing tag
                    val thinkingContent = fullText.substring(0, idx)
                    if (thinkingContent.isNotEmpty()) {
                        callback.onChunk(ChunkType.THINKING, thinkingContent)
                    }
                    // Reset buffer with remaining content
                    tagBuffer.clear()
                    tagBuffer.append(fullText.substring(idx + "<channel|>".length))
                    state = ParseState.NORMAL
                } else {
                    // Partial match handling
                    val overlap = minOf(fullText.length, "<channel|>".length - 1)
                    val toFlush = fullText.substring(0, fullText.length - overlap)
                    if (toFlush.isNotEmpty()) {
                        callback.onChunk(ChunkType.THINKING, toFlush)
                        tagBuffer.clear()
                        tagBuffer.append(fullText.substring(fullText.length - overlap))
                    }
                }
            }
        }
    }

    /**
     * Reset the parser state for a new generation session.
     */
    fun reset() {
        tagBuffer.clear()
        state = ParseState.NORMAL
    }
}
