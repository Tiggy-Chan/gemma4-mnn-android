package com.tiggy.gemma4mnn.data

import android.content.Context
import com.tiggy.gemma4mnn.model.ModelConfig
import java.io.File

/**
 * Repository for discovering and managing available LLM models.
 *
 * Models are stored in the app's external files directory:
 * `{context.getExternalFilesDir(null)}/models/`
 */
class ModelRepository(context: Context) {
    private val modelsDir = File(context.getExternalFilesDir(null), "models")

    /**
     * Returns all available models. Currently hardcoded for Gemma 4 variants.
     * In production, this would scan the models directory or fetch from a remote catalog.
     */
    fun getAvailableModels(): List<ModelConfig> {
        if (!modelsDir.exists()) {
            modelsDir.mkdirs()
        }

        // Scan for .mnn model files
        val modelFiles = modelsDir.listFiles { f ->
            f.isDirectory && f.listFiles()?.any { it.name.endsWith(".mnn") } == true
        }?.toList() ?: emptyList()

        return modelFiles.map { dir ->
            val mnnFile = dir.listFiles { f -> f.name.endsWith(".mnn") }?.firstOrNull()
            val configFile = dir.listFiles { f -> f.name == "config.json" }?.firstOrNull()

            ModelConfig(
                name = dir.name,
                displayName = dir.name.replace("-", " ").replaceFirstChar { it.uppercase() },
                path = mnnFile?.absolutePath ?: "",
                configPath = configFile?.absolutePath ?: "",
                sizeBytes = mnnFile?.length() ?: 0L,
                enableThinking = dir.name.contains("gemma", ignoreCase = true),
                isDownloaded = true,
            )
        }
    }

    /**
     * Returns the default model to use when no selection has been made.
     */
    fun getDefaultModel(): ModelConfig? {
        return getAvailableModels().firstOrNull()
    }
}
