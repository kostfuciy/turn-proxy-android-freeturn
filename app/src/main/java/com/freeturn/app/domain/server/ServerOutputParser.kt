package com.freeturn.app.domain.server

/** Парсит stdout серверной команды в [CmdResult]: `LOG:` префиксы, k=v значения и `RESULT`. */
object ServerOutputParser {
    fun parse(raw: String): CmdResult {
        // Проброс транспортных ошибок от SSHManager.
        if (raw.startsWith("ERROR:")) {
            return CmdResult.Err(raw.removePrefix("ERROR:").trim(), emptyList())
        }
        val kv = LinkedHashMap<String, String>()
        val logs = mutableListOf<String>()
        raw.lines().forEach { rawLine ->
            val line = rawLine.trimEnd('\r')
            when {
                line.isEmpty() -> { /* пропускаем */ }
                line.startsWith("LOG: ") -> logs += line.removePrefix("LOG: ")
                // k=v: ключ - только [A-Z0-9_], всё остальное считаем логом.
                line.contains('=') && line.substringBefore('=').matches(Regex("[A-Z][A-Z0-9_]*")) -> {
                    val k = line.substringBefore('=')
                    val v = line.substringAfter('=')
                    kv[k] = v
                }
                else -> logs += line
            }
        }
        return when (kv["RESULT"]) {
            "ok"  -> CmdResult.Ok(kv, logs)
            "err" -> CmdResult.Err(kv["ERR"] ?: "unknown error", logs)
            else  -> CmdResult.Err(
                "no RESULT marker (output truncated?)",
                logs
            )
        }
    }
}

/** base64 -> UTF-8 текст; битое значение -> null. */
fun decodeBase64(s: String): String? =
    runCatching { String(java.util.Base64.getDecoder().decode(s.trim()), Charsets.UTF_8) }.getOrNull()
