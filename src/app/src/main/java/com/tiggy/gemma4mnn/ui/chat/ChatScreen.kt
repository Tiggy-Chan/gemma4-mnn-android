package com.tiggy.gemma4mnn.ui.chat

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tiggy.gemma4mnn.di.AppModule
import com.tiggy.gemma4mnn.viewmodel.ChatViewModel

/**
 * Navigation destinations for the main screen.
 */
private enum class ScreenDestination {
    CHAT,
    SETTINGS,
}

@Composable
fun ChatScreen(
    viewModel: ChatViewModel = AppModule.chatViewModel,
    modifier: Modifier = Modifier,
) {
    val messages by viewModel.messages.collectAsStateWithLifecycle()
    val isGenerating by viewModel.isGenerating.collectAsStateWithLifecycle()
    val thinkingEnabled by viewModel.thinkingEnabled.collectAsStateWithLifecycle()
    val selectedModel by viewModel.selectedModel.collectAsStateWithLifecycle()

    var currentDestination by remember { mutableStateOf(ScreenDestination.CHAT) }

    // Auto-select the first available model on launch
    LaunchedEffect(Unit) {
        if (selectedModel == null) {
            val defaultModel = AppModule.modelRepository.getDefaultModel()
            if (defaultModel != null) {
                viewModel.selectModel(defaultModel)
            }
        }
    }

    Scaffold(
        topBar = {
            ChatTopBar(
                selectedModel = selectedModel,
                thinkingEnabled = thinkingEnabled,
                onThinkingToggle = { viewModel.toggleThinking() },
                availableModels = AppModule.modelRepository.getAvailableModels(),
                onModelSelected = { viewModel.selectModel(it) },
                onOpenSettings = { currentDestination = ScreenDestination.SETTINGS },
            )
        },
        bottomBar = {
            if (currentDestination == ScreenDestination.CHAT) {
                ChatInput(
                    onSend = { viewModel.sendMessage(it) },
                    enabled = !isGenerating,
                    thinkingEnabled = thinkingEnabled,
                    onThinkingToggle = { viewModel.toggleThinking() },
                )
            }
        },
    ) { padding ->
        AnimatedContent(
            targetState = currentDestination,
            modifier = Modifier.padding(padding),
            label = "screen_transition",
        ) { destination ->
            when (destination) {
                ScreenDestination.CHAT -> {
                    if (messages.isEmpty()) {
                        EmptyChat(modifier = Modifier.fillMaxSize())
                    } else {
                        MessageList(
                            messages = messages,
                            isGenerating = isGenerating,
                            onStopGeneration = { viewModel.stopGeneration() },
                            modifier = Modifier.fillMaxSize(),
                        )
                    }
                }

                ScreenDestination.SETTINGS -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        // Placeholder for settings — full settings screen in Phase 4
                        androidx.compose.material3.Text("Settings screen coming in Phase 4")
                    }
                }
            }
        }
    }
}
