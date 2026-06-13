package com.freeturn.app.domain.server

/**
 * Результат серверной команды: k=v значения, LOG-строки и статус (RESULT=ok/err).
 * Строится из stdout в [ServerOutputParser].
 */
sealed class CmdResult {
    abstract val logs: List<String>
    data class Ok(val kv: Map<String, String>, override val logs: List<String>) : CmdResult()
    data class Err(val message: String, override val logs: List<String>) : CmdResult()
}

/** Ошибка серверной команды; message - текст из скрипта/SSH для UI. */
class ServerCommandException(message: String) : Exception(message)

/** Текст ошибки + хвост LOG-строк для диагностики. */
fun CmdResult.Err.errMessage(): String {
    val tail = logs.takeLast(2).filter { it.isNotBlank() }
    return if (tail.isEmpty()) message else message + "\n" + tail.joinToString("\n")
}

fun CmdResult.Err.toFailure(): Result<Nothing> =
    Result.failure(ServerCommandException(errMessage()))

fun CmdResult.asUnit(): Result<Unit> = when (this) {
    is CmdResult.Ok -> Result.success(Unit)
    is CmdResult.Err -> toFailure()
}
