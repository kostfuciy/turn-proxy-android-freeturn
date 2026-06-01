package com.freeturn.app

import android.app.Application
import net.i2p.crypto.eddsa.EdDSASecurityProvider
import java.security.Security
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin
import com.freeturn.app.di.appModule

class App : Application() {
    override fun onCreate() {
        super.onCreate()
        if (Security.getProvider("EdDSA") == null) {
            Security.addProvider(EdDSASecurityProvider())
        }
        
        startKoin {
            androidLogger()
            androidContext(this@App)
            modules(appModule)
        }
    }
}
