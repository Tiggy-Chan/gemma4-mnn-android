package com.tiggy.gemma4mnn.ui.chat

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material.icons.outlined.ModelTraining
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.tiggy.gemma4mnn.model.ModelConfig
import com.tiggy.gemma4mnn.ui.model.ModelPicker

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatTopBar(
    selectedModel: ModelConfig?,
    thinkingEnabled: Boolean,
    onThinkingToggle: () -> Unit,
    onModelSelected: ((ModelConfig) -> Unit)? = null,
    availableModels: List<ModelConfig> = emptyList(),
    onOpenSettings: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    var showModelPicker by remember { mutableStateOf(false) }
    var showModelMenu by remember { mutableStateOf(false) }

    TopAppBar(
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.clickable(enabled = onModelSelected != null) {
                    showModelPicker = true
                },
            ) {
                Icon(
                    imageVector = Icons.Outlined.ModelTraining,
                    contentDescription = null,
                    modifier = Modifier.padding(end = 8.dp),
                )
                Text(text = selectedModel?.displayName ?: "Gemma4 MNN")
            }
        },
        actions = {
            IconButton(onClick = onThinkingToggle) {
                Icon(
                    imageVector = if (thinkingEnabled) Icons.Filled.Lightbulb else Icons.Outlined.Lightbulb,
                    contentDescription = if (thinkingEnabled) "Disable thinking" else "Enable thinking",
                )
            }

            IconButton(onClick = { showModelMenu = true }) {
                Icon(Icons.Outlined.Settings, contentDescription = "Settings")

                DropdownMenu(
                    expanded = showModelMenu,
                    onDismissRequest = { showModelMenu = false },
                ) {
                    DropdownMenuItem(
                        text = { Text("Model") },
                        onClick = {
                            showModelMenu = false
                            showModelPicker = true
                        },
                    )
                    if (onOpenSettings != null) {
                        DropdownMenuItem(
                            text = { Text("Settings") },
                            onClick = {
                                showModelMenu = false
                                onOpenSettings()
                            },
                        )
                    }
                }
            }
        },
        modifier = modifier.fillMaxWidth(),
    )

    if (showModelPicker && onModelSelected != null) {
        ModelPicker(
            models = availableModels,
            selectedModel = selectedModel,
            onModelSelected = onModelSelected,
            onDismiss = { showModelPicker = false },
        )
    }
}
