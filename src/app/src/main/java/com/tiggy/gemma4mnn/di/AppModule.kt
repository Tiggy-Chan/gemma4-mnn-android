package com.tiggy.gemma4mnn.di

import android.content.Context
import com.tiggy.gemma4mnn.data.ChatRepository
import com.tiggy.gemma4mnn.data.ModelRepository
import com.tiggy.gemma4mnn.data.SettingsRepository
import com.tiggy.gemma4mnn.engine.MnnEngine
import com.tiggy.gemma4mnn.viewmodel.ChatViewModel

/**
 * Manual dependency injection module.
 *
 * For a small project like this (~10 singletons, 2 ViewModels), manual DI
 * is simpler, faster to compile, and has zero runtime overhead.
 */
object AppModule {
    private lateinit var appContext: Context

    val modelRepository: ModelRepository by lazy { ModelRepository(appContext) }
    val settingsRepository: SettingsRepository by lazy { SettingsRepository(appContext) }
    val chatRepository: ChatRepository by lazy { ChatRepository(appContext) }
    val mnnEngine: MnnEngine by lazy { MnnEngine(modelRepository) }
    val chatViewModel: ChatViewModel by lazy { ChatViewModel(mnnEngine, settingsRepository, chatRepository) }

    fun init(context: Context) {
        appContext = context.applicationContext
    }
}
