package com.freeturn.app.domain.backup

import com.freeturn.app.data.AppPreferences
import com.freeturn.app.data.backup.BackupCrypto
import com.freeturn.app.data.backup.SettingsBackup
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Экспорт/импорт настроек в зашифрованный паролем файл.
 * Крипто (PBKDF2 210k) тяжёлая - гоним на Dispatchers.Default.
 */
class BackupManager(private val prefs: AppPreferences) {

    /** Сериализует и шифрует все настройки. Возвращает байты файла. */
    suspend fun export(password: String): ByteArray = withContext(Dispatchers.Default) {
        val payload = SettingsBackup.encode(prefs.exportData())
        BackupCrypto.encrypt(payload.toByteArray(Charsets.UTF_8), password)
    }

    /**
     * Расшифровывает и добавляет серверы из бэкапа. Возвращает число добавленных.
     * Бросает [BackupCrypto.BadPasswordException] / [BackupCrypto.FormatException].
     */
    suspend fun import(bytes: ByteArray, password: String): Int = withContext(Dispatchers.Default) {
        val payload = BackupCrypto.decrypt(bytes, password)
        val data = SettingsBackup.decode(String(payload, Charsets.UTF_8))
        prefs.importServers(data.servers)
    }
}
