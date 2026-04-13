package com.tiggy.gemma4mnn.model

/**
 * Configuration for a single LLM model.
 */
data class ModelConfig(
    val name: String,
    val displayName: String = name,
    val path: String,                    // Absolute path to the .mnn model file
    val configPath: String = "",         // Path to config.json (if separate)
    val contextLength: Int = 4096,
    val enableThinking: Boolean = false,
    val chatTemplate: String? = null,    // Jinja template string (null = use default)
    val sizeBytes: Long = 0L,
    val isDownloaded: Boolean = false,
    val downloadUrl: String = "",
) {
    val isLoaded: Boolean = path.isNotEmpty() && (path.startsWith("/") || path.startsWith("assets"))
}
