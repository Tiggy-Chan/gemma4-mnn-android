package com.tiggy.gemma4mnn.ui.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun SettingsScreen(
    contextLength: Int = 4096,
    onContextLengthChange: (Int) -> Unit = {},
    temperature: Float = 0.7f,
    onTemperatureChange: (Float) -> Unit = {},
    topP: Float = 0.95f,
    onTopPChange: (Float) -> Unit = {},
    onClearChat: () -> Unit = {},
) {
    var localContextLength by remember { mutableIntStateOf(contextLength) }
    var localTemperature by remember { mutableFloatStateOf(temperature) }
    var localTopP by remember { mutableFloatStateOf(topP) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
    ) {
        Text(
            text = "Generation Settings",
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(bottom = 16.dp),
        )

        // Context Length
        SettingCard(title = "Context Length", value = "$localContextLength tokens") {
            Slider(
                value = localContextLength.toFloat(),
                onValueChange = {
                    localContextLength = it.toInt()
                    onContextLengthChange(localContextLength)
                },
                valueRange = 1024f..16384f,
                steps = 14,
            )
        }

        // Temperature
        SettingCard(title = "Temperature", value = String.format("%.2f", localTemperature)) {
            Slider(
                value = localTemperature,
                onValueChange = {
                    localTemperature = it
                    onTemperatureChange(localTemperature)
                },
                valueRange = 0.1f..2.0f,
                steps = 18,
            )
        }

        // Top P
        SettingCard(title = "Top P", value = String.format("%.2f", localTopP)) {
            Slider(
                value = localTopP,
                onValueChange = {
                    localTopP = it
                    onTopPChange(localTopP)
                },
                valueRange = 0.1f..1.0f,
                steps = 8,
            )
        }

        // Clear Chat
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            onClick = onClearChat,
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Clear Chat History",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.error,
                    fontWeight = FontWeight.Medium,
                )
            }
        }
    }
}

@Composable
private fun SettingCard(
    title: String,
    value: String,
    content: @Composable () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = value,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            content()
        }
    }
}
