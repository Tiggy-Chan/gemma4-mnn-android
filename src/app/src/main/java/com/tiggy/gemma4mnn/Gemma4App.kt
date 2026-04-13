package com.tiggy.gemma4mnn

import android.app.Application
import com.tiggy.gemma4mnn.di.AppModule

class Gemma4App : Application() {

    override fun onCreate() {
        super.onCreate()
        AppModule.init(this)
    }
}
