package com.tiggy.gemma4mnn.ui.chat

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.tiggy.gemma4mnn.model.ChatMessage

@Composable
fun MessageList(
    messages: List<ChatMessage>,
    isGenerating: Boolean,
    modifier: Modifier = Modifier,
    onStopGeneration: (() -> Unit)? = null,
) {
    val listState = rememberLazyListState()
    var previousSize by remember { mutableIntOf(0) }

    // Auto-scroll to bottom when new messages arrive
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty() && messages.size > previousSize) {
            listState.animateScrollToItem(messages.size - 1)
        }
        previousSize = messages.size
    }

    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 56.dp), // Space for stop button
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            items(
                count = messages.size,
                key = { index -> messages[index].id },
            ) { index ->
                MessageBubble(messages[index])
            }
        }

        // Stop generation FAB
        AnimatedVisibility(
            visible = isGenerating && onStopGeneration != null,
            enter = fadeIn() + slideInVertically(initialOffsetY = { it }),
            exit = fadeOut() + slideOutVertically(targetOffsetY = { it }),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 16.dp),
        ) {
            FloatingActionButton(
                onClick = { onStopGeneration?.invoke() },
                containerColor = MaterialTheme.colorScheme.errorContainer,
                contentColor = MaterialTheme.colorScheme.onErrorContainer,
            ) {
                Icon(
                    imageVector = Icons.Default.Stop,
                    contentDescription = "Stop generation",
                    modifier = Modifier.size(20.dp),
                )
            }
        }
    }
}
