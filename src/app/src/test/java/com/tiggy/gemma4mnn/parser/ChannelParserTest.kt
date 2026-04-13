package com.tiggy.gemma4mnn.parser

import org.junit.Test
import org.junit.Assert.*

/**
 * Unit tests for ChannelParser with real Gemma 4 output patterns.
 *
 * Tests verify that the parser correctly:
 * 1. Routes normal content before thinking tags
 * 2. Routes thinking content between `<|channel>` and `<channel|>`
 * 3. Handles partial tags split across tokens
 * 4. Handles multiple thinking blocks in one response
 * 5. Handles content with no thinking at all
 * 6. Handles empty tokens and edge cases
 */
class ChannelParserTest {

    // Helper to collect all chunks emitted by the parser
    private data class CollectedChunk(val type: ChunkType, val content: String)

    private fun collectChunks(block: (ChannelParser) -> Unit): List<CollectedChunk> {
        val chunks = mutableListOf<CollectedChunk>()
        val callback = object : ChannelCallback {
            override fun onChunk(type: ChunkType, content: String) {
                chunks.add(CollectedChunk(type, content))
            }
            override fun onDone() {}
        }
        val parser = ChannelParser(callback)
        block(parser)
        return chunks
    }

    @Test
    fun `pure normal content without thinking tags`() {
        val chunks = collectChunks { parser ->
            parser.pushToken("Hello")
            parser.pushToken(" world")
            parser.pushToken("!")
        }

        assertEquals(1, chunks.size) // Should be batched due to overlap buffer
        assertEquals(ChunkType.NORMAL, chunks[0].type)
        assertTrue(chunks[0].content.contains("Hello"))
    }

    @Test
    fun `thinking content between channel markers`() {
        val chunks = collectChunks { parser ->
            parser.pushToken("Let me think<|channel>")
            parser.pushToken("this is")
            parser.pushToken(" the reasoning")
            parser.pushToken("<channel|>")
            parser.pushToken("Final answer")
        }

        val normalChunks = chunks.filter { it.type == ChunkType.NORMAL }
        val thinkingChunks = chunks.filter { it.type == ChunkType.THINKING }

        assertTrue(thinkingChunks.isNotEmpty())
        assertTrue(thinkingChunks.joinToString("") { it.content }.contains("this is"))
        assertTrue(thinkingChunks.joinToString("") { it.content }.contains("the reasoning"))

        val normalContent = normalChunks.joinToString("") { it.content }
        assertTrue(normalContent.contains("Final answer"))
    }

    @Test
    fun `partial tag split across multiple tokens`() {
        val chunks = collectChunks { parser ->
            // Split "<|channel>" across two tokens
            parser.pushToken("Before<|chan")
            parser.pushToken("nel>thinking here<channel|>")
            parser.pushToken("After")
        }

        val thinkingChunks = chunks.filter { it.type == ChunkType.THINKING }
        val normalChunks = chunks.filter { it.type == ChunkType.NORMAL }

        assertTrue(thinkingChunks.isNotEmpty())
        assertTrue(thinkingChunks.joinToString("") { it.content }.contains("thinking here"))

        val normalContent = normalChunks.joinToString("") { it.content }
        assertTrue(normalContent.contains("Before"))
        assertTrue(normalContent.contains("After"))
    }

    @Test
    fun `closing tag split across tokens`() {
        val chunks = collectChunks { parser ->
            parser.pushToken("<|channel>thinking<channel")
            parser.pushToken("|>answer")
        }

        val thinkingChunks = chunks.filter { it.type == ChunkType.THINKING }
        val normalChunks = chunks.filter { it.type == ChunkType.NORMAL }

        assertTrue(thinkingChunks.isNotEmpty())
        assertTrue(thinkingChunks.joinToString("") { it.content }.contains("thinking"))

        val normalContent = normalChunks.joinToString("") { it.content }
        assertTrue(normalContent.contains("answer"))
    }

    @Test
    fun `content with no channel markers at all`() {
        val chunks = collectChunks { parser ->
            parser.pushToken("This is a normal response")
            parser.pushToken(" with no thinking")
            parser.pushToken(" at all.")
        }

        assertEquals(1, chunks.size)
        assertEquals(ChunkType.NORMAL, chunks[0].type)
        assertEquals("This is a normal response with no thinking at all.", chunks[0].content)
    }

    @Test
    fun `empty tokens are handled gracefully`() {
        val chunks = collectChunks { parser ->
            parser.pushToken("")
            parser.pushToken("Hello")
            parser.pushToken("")
            parser.pushToken(" world")
        }

        val normalChunks = chunks.filter { it.type == ChunkType.NORMAL }
        assertTrue(normalChunks.isNotEmpty())
        assertTrue(normalChunks.joinToString("") { it.content }.contains("Hello"))
    }

    @Test
    fun `reset clears state`() {
        val chunks = collectChunks { parser ->
            parser.pushToken("<|channel>thinking<channel|>")
            parser.pushToken("normal")
            parser.reset()
            parser.pushToken("<|channel>more thinking<channel|>")
            parser.pushToken("more normal")
        }

        val thinkingChunks = chunks.filter { it.type == ChunkType.THINKING }
        assertEquals(2, thinkingChunks.size) // Both thinking blocks should be captured
    }

    @Test
    fun `real gemma4 output pattern`() {
        // Simulates a realistic Gemma 4 streaming response
        val chunks = collectChunks { parser ->
            // Pre-thinking normal content (rare but possible)
            parser.pushToken("Sure, ")
            // Enter thinking
            parser.pushToken("let me analyze this.<|channel>")
            // Thinking content split across many tokens
            parser.pushToken("The")
            parser.pushToken(" user")
            parser.pushToken(" is")
            parser.pushToken(" asking")
            parser.pushToken(" about")
            parser.pushToken(" MNN")
            parser.pushToken(" inference")
            parser.pushToken(".")
            // Exit thinking
            parser.pushToken("<channel|>")
            // Normal response
            parser.pushToken("MNN is a lightweight deep learning inference engine.")
            parser.pushToken(" It supports LLM inference on mobile devices.")
        }

        val thinkingContent = chunks
            .filter { it.type == ChunkType.THINKING }
            .joinToString("") { it.content }
        val normalContent = chunks
            .filter { it.type == ChunkType.NORMAL }
            .joinToString("") { it.content }

        assertTrue(
            thinkingContent.contains("The user is asking about MNN inference"),
        )
        assertTrue(
            normalContent.contains("MNN is a lightweight deep learning inference engine"),
        )
        assertTrue(
            normalContent.contains("Sure, let me analyze this."),
        )
    }

    @Test
    fun `multiple thinking blocks in single response`() {
        val chunks = collectChunks { parser ->
            parser.pushToken("Part 1<|channel>")
            parser.pushToken("First reasoning")
            parser.pushToken("<channel|>")
            parser.pushToken("Middle<|channel>")
            parser.pushToken("Second reasoning")
            parser.pushToken("<channel|>")
            parser.pushToken("End")
        }

        val thinkingChunks = chunks.filter { it.type == ChunkType.THINKING }
        val thinkingContent = thinkingChunks.joinToString("") { it.content }

        assertEquals(2, thinkingChunks.size)
        assertTrue(thinkingContent.contains("First reasoning"))
        assertTrue(thinkingContent.contains("Second reasoning"))
    }

    @Test
    fun `tag buffer does not grow unbounded on normal content`() {
        // After each token is processed, the tag buffer should only hold
        // up to (tag length - 1) characters for partial matching
        val chunks = collectChunks { parser ->
            val longText = "A".repeat(1000)
            parser.pushToken(longText)
        }

        // All content should be emitted as normal
        val normalContent = chunks.filter { it.type == ChunkType.NORMAL }.joinToString("") { it.content }
        assertEquals(1000, normalContent.length)
    }

    @Test
    fun `partial tag at end of stream is emitted correctly`() {
        // If stream ends mid-tag, the partial content should still be emitted
        val chunks = collectChunks { parser ->
            parser.pushToken("Normal text<|cha")
        }

        // The "Normal text" should be emitted as normal content
        // The partial tag "<|cha" should remain in buffer (not lost)
        val normalChunks = chunks.filter { it.type == ChunkType.NORMAL }
        assertTrue(normalChunks.isNotEmpty())
        assertTrue(normalChunks.joinToString("") { it.content }.contains("Normal text"))
    }
}
