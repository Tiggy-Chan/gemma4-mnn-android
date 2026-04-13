package com.tiggy.gemma4mnn.ui.chat

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Circle
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

/**
 * Typing indicator shown while the model is preparing its first token.
 */
@Composable
fun TypingIndicator(
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.padding(horizontal = 8.dp, vertical = 4.dp),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            AnimatedDot(delay = 0)
            Spacer(modifier = Modifier.width(4.dp))
            AnimatedDot(delay = 150)
            Spacer(modifier = Modifier.width(4.dp))
            AnimatedDot(delay = 300)
        }
    }
}

@Composable
private fun AnimatedDot(delay: Int) {
    val scale = remember { Animatable(0.6f) }

    LaunchedEffect(delay) {
        delay(delay.toLong())
        scale.animateTo(
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = keyframes {
                    durationMillis = 800
                    0.6f at 0
                    1f at 200
                    0.6f at 400
                },
            ),
        )
    }

    Icon(
        imageVector = Icons.Default.Circle,
        contentDescription = null,
        modifier = Modifier
            .size(8.dp)
            .graphicsLayer {
                scaleX = scale.value
                scaleY = scale.value
            }
            .clip(androidx.compose.foundation.shape.CircleShape),
        tint = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}
