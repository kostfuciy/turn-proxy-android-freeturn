package com.freeturn.app.domain.server

/**
 * Команда, которую мы хотим выполнить на сервере. Соответствует аргументам
 * `free-turn-control.sh`. Скрипт стримится через SSH stdin (см. [ServerControl]).
 */
sealed class ServerCommand {
    data object Probe : ServerCommand()
    data object Install : ServerCommand()

    /**
     * Идемпотентная настройка WireGuard-бэкенда: бутстрап на [port] либо пир в
     * существующем conf; [endpoint] уходит в клиентский конфиг (локальный
     * free-turn-proxy клиент устройства). [adopt] — WG уже обнаружен probe-ом:
     * скрипт не бутстрапит новый, при недоступном conf отвечает ok без конфига.
     */
    data class WgSetup(
        val port: Int,
        val endpoint: String,
        val adopt: Boolean = false
    ) : ServerCommand()
    data class Start(val opts: ServerOptions) : ServerCommand()
    data object Stop : ServerCommand()
    data class FetchLogs(val lines: Int = 80) : ServerCommand()

    fun toArgv(): List<String> = when (this) {
        is Probe -> listOf("probe")
        is Install -> listOf("install")
        is WgSetup -> buildList {
            add("wg-setup")
            add("--port=$port")
            add("--endpoint=$endpoint")
            if (adopt) add("--adopt=1")
        }
        is Start -> buildList {
            add("start")
            add("--listen=${opts.listen}")
            add("--connect=${opts.connect}")
            // tcp-режим (Xray/sing-box) → -mode tcp; иначе udp-релей (дефолт).
            // Bond на сервере не задаётся — ядро детектит его по magic-префиксу.
            if (opts.tcpMode) add("--mode=tcp")
            // Обфускация: -obf-profile <profile> -obf-key <key>. Профиль none → не шлём.
            if (opts.obfProfile != "none" && opts.obfKey.isNotBlank()) {
                add("--obf-profile=${opts.obfProfile}")
                add("--obf-key=${opts.obfKey}")
            }
        }
        is Stop -> listOf("stop")
        is FetchLogs -> listOf("logs", "--tail=$lines")
    }
}

data class ServerOptions(
    val listen: String,
    val connect: String,
    /** true → -mode tcp (TCP-форвард). false → udp-релей (дефолт ядра). */
    val tcpMode: Boolean = false,
    /** Wire-профиль обфускации: none | rtpopus. */
    val obfProfile: String = "none",
    /** 64-hex obf-ключ. Пустая строка → запуск без обфускации. */
    val obfKey: String = ""
)
