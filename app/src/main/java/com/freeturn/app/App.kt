package com.freeturn.app

import android.app.Application
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin
import com.freeturn.app.di.appModule

class App : Application() {
    override fun onCreate() {
        super.onCreate()
        // ed25519/curve25519 работает через Bouncy Castle в classpath. jsch 2.x подхватывает его сам.
        startKoin {
            androidLogger()
            androidContext(this@App)
            modules(appModule)
        }
    }
}
