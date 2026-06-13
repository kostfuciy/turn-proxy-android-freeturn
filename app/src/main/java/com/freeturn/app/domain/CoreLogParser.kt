package com.freeturn.app.domain

/** Событие, распознанное в строке лога клиентского ядра. */
sealed interface CoreLogEvent {
    /** Ядро просит ручную капчу - открыть [url]. */
    data class CaptchaUrl(val url: String) : CoreLogEvent
    /** Auth-чейн завершился (успех/провал) - текущая капча-сессия больше не нужна. */
    data object CaptchaResolved : CoreLogEvent
    /** UDP-релей: поток установил DTLS-соединение. */
    data object StreamEstablished : CoreLogEvent
    /** UDP-релей: поток закрыл DTLS-соединение. */
    data object StreamClosed : CoreLogEvent
    /** TCP-режим: целевое число сессий из waiting-строки. */
    data class TcpTotal(val total: Int) : CoreLogEvent
    /** TCP-режим: агрегированное число активных сессий. */
    data class TcpActive(val active: Int) : CoreLogEvent
    /** Фатальная ошибка старта (panic/fatal/окончательный отказ creds). */
    data class FatalStartup(val line: String) : CoreLogEvent
    /** Quota-ошибка - сигнал на сброс сессии. */
    data object QuotaError : CoreLogEvent
}

/** Парсер логов клиентского ядра (чистая логика без Android-зависимостей). */
object CoreLogParser {

    // Жесткая привязка к формату капчи (старое и новое ядро).
    private val CAPTCHA_URL_REGEX =
        Regex("""(?:manually open this URL|Open this URL in your browser):\s*(https?://\S+)""")

    // Жизненный цикл соединений (UDP-релей: [STREAM N] Established/Closed).
    private val STREAM_ESTABLISHED_REGEX =
        Regex("""\[STREAM (\d+)\] Established DTLS connection""")
    private val STREAM_CLOSED_REGEX =
        Regex("""\[STREAM (\d+)\] Closed DTLS connection""")
    // TCP-режим: агрегированное число сессий.
    private val TCP_ACTIVE_REGEX =
        Regex("""\[session \d+\] (?:connected|disconnected) \(active: (\d+)\)""")
    // TCP-режим: ожидание сессий.
    private val TCP_TOTAL_REGEX =
        Regex("""TCP mode: waiting for sessions to connect \(total: (\d+)\)""")

    /** Все события одной строки (может быть несколько). */
    fun parse(line: String): List<CoreLogEvent> {
        val events = mutableListOf<CoreLogEvent>()

        CAPTCHA_URL_REGEX.find(line)?.let {
            events += CoreLogEvent.CaptchaUrl(it.groupValues[1])
        }

        // Капча-сессия закончилась (Failed/Success/timeout).
        if (line.contains("[VK Auth] Failed") ||
            line.contains("[VK Auth] Success") ||
            (line.contains("[Captcha]") && line.contains("failed"))
        ) {
            events += CoreLogEvent.CaptchaResolved
        }

        if (STREAM_ESTABLISHED_REGEX.containsMatchIn(line)) events += CoreLogEvent.StreamEstablished
        if (STREAM_CLOSED_REGEX.containsMatchIn(line)) events += CoreLogEvent.StreamClosed
        TCP_TOTAL_REGEX.find(line)?.let {
            events += CoreLogEvent.TcpTotal(it.groupValues[1].toInt())
        }
        TCP_ACTIVE_REGEX.find(line)?.let {
            events += CoreLogEvent.TcpActive(it.groupValues[1].toInt())
        }

        // Фатальный старт: не матчим "rate limit" (часть retry-цикла), ищем финальный отказ.
        val lower = line.lowercase()
        if (lower.startsWith("panic:") ||
            lower.startsWith("fatal error:") ||
            lower.contains("all vk credentials failed") ||
            lower.contains("fatal_captcha")
        ) {
            events += CoreLogEvent.FatalStartup(line)
        }

        if (lower.contains("quota")) events += CoreLogEvent.QuotaError

        return events
    }
}

/**
 * Счётчик активных соединений по [CoreLogEvent].
 * Режим (udp/tcp) уточняется по факту.
 */
class CoreConnectionTracker(
    /** Целевое число потоков UDP (threads == 0 -> 1 поток, raw-режим -> 0). */
    private val udpTotal: Int,
    tcpMode: Boolean
) {
    private var isTcp = tcpMode
    // Считаем инкрементами, а не Set, так как ядро может дублировать streamID (id=1).
    private var udpActive = 0
    private var tcpActive = 0
    private var tcpTotal = 0

    val active: Int get() = if (isTcp) tcpActive else udpActive
    val total: Int get() = if (isTcp) tcpTotal else udpTotal

    /** Хотя бы один живой канал - сигнал успешного старта. */
    val hasConnection: Boolean get() = active > 0

    /** Применяет событие; true - статистика изменилась и её надо опубликовать. */
    fun apply(event: CoreLogEvent): Boolean = when (event) {
        CoreLogEvent.StreamEstablished -> {
            udpActive += 1
            isTcp = false
            true
        }
        CoreLogEvent.StreamClosed -> {
            if (udpActive > 0) udpActive -= 1
            true
        }
        is CoreLogEvent.TcpTotal -> {
            tcpTotal = event.total
            isTcp = true
            true
        }
        is CoreLogEvent.TcpActive -> {
            tcpActive = event.active
            isTcp = true
            true
        }
        else -> false
    }
}
