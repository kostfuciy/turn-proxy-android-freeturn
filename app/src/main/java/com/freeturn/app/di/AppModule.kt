package com.freeturn.app.di

import com.freeturn.app.data.AppPreferences
import com.freeturn.app.domain.update.AppUpdater
import com.freeturn.app.domain.share.LinkImportBus
import com.freeturn.app.domain.proxy.LocalProxyManager
import com.freeturn.app.domain.proxy.ProxyOrchestrator
import com.freeturn.app.domain.ssh.SSHManager
import com.freeturn.app.domain.server.ServerSetupRepository
import com.freeturn.app.domain.share.ShareRepository
import com.freeturn.app.domain.ssh.SshRepository
import com.freeturn.app.viewmodel.ImportViewModel
import com.freeturn.app.viewmodel.ProxyViewModel
import com.freeturn.app.viewmodel.ServerSetupViewModel
import com.freeturn.app.viewmodel.ServerViewModel
import com.freeturn.app.viewmodel.SettingsViewModel
import com.freeturn.app.viewmodel.ShareViewModel
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val appModule = module {
    single { AppPreferences(androidContext()) }
    single { LocalProxyManager(androidContext()) }
    // factory: каждому потребителю свой SSHManager - lastSeenFingerprint (TOFU) не должен
    // делиться между живой сессией и мастером/шарингом.
    factory { SSHManager() }
    single { SshRepository(androidContext(), get()) }
    single { AppUpdater(androidContext()) }
    single { ProxyOrchestrator(get(), get(), get()) }
    // factory: своя SSH-сессия на каждый прогон мастера, живой SshRepository не трогаем.
    factory { ServerSetupRepository(androidContext(), get()) }
    // factory по той же причине: SSH-операции шаринга не делят сессию с активным сервером.
    factory { ShareRepository(androidContext(), get()) }
    single { LinkImportBus() }

    viewModelOf(::ProxyViewModel)
    viewModelOf(::ServerViewModel)
    viewModelOf(::SettingsViewModel)
    viewModelOf(::ServerSetupViewModel)
    viewModelOf(::ShareViewModel)
    viewModelOf(::ImportViewModel)
}
