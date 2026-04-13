package com.tiggy.gemma4mnn

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.ui.Modifier
import com.tiggy.gemma4mnn.di.AppModule
import com.tiggy.gemma4mnn.ui.chat.ChatScreen
import com.tiggy.gemma4mnn.ui.theme.Gemma4MnnTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            Gemma4MnnTheme {
                ChatScreen(
                    viewModel = AppModule.chatViewModel,
                    modifier = Modifier
                        .fillMaxSize()
                        .safeContentPadding()
                )
            }
        }
    }
}
